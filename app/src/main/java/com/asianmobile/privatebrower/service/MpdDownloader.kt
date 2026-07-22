package com.asianmobile.privatebrower.service

import android.webkit.CookieManager
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import java.io.FileOutputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Downloads a generic MPEG-DASH stream (`.mpd`): parses the manifest, picks the best video +
 * best audio representation, downloads their init + media segments, and muxes them into one
 * playable file via [MediaRemuxer] (H.264+AAC → MP4; VP9+Opus → WebM). No re-encode.
 *
 * Segment addressing supported: SegmentTemplate (SegmentTimeline or number/duration),
 * SegmentList, and a plain/SegmentBase single-file BaseURL. Not supported (fails gracefully):
 * Widevine/PlayReady DRM (ContentProtection), un-muxable codecs.
 */
class MpdDownloader(
    private val client: OkHttpClient,
    private val storedHeaders: Map<String, String>,
) {
    data class Result(val success: Boolean, val message: String? = null, val outputFile: File? = null)

    /** Concrete segment plan for one representation. */
    private class RepStream(val initUrl: HttpUrl?, val segmentUrls: List<HttpUrl>)

    suspend fun download(
        mpdUrl: String,
        outputFile: File,
        isActive: () -> Boolean,
        onProgress: suspend (done: Int, total: Int, bytes: Long) -> Unit,
    ): Result {
        val baseUrl = mpdUrl.toHttpUrlOrNull() ?: return Result(false, "Invalid MPD URL")
        val xml = fetchText(mpdUrl) ?: return Result(false, "Cannot fetch MPD")

        val doc = try {
            val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = false }
            factory.newDocumentBuilder().parse(xml.byteInputStream())
        } catch (e: Exception) {
            return Result(false, "Malformed MPD: ${e.message}")
        }
        val mpd = doc.documentElement ?: return Result(false, "Empty MPD")
        if (mpd.getElementsByTagName("ContentProtection").length > 0) {
            return Result(false, "DRM-protected stream")
        }
        val period = mpd.firstChildElement("Period") ?: return Result(false, "No Period in MPD")
        val presentationSeconds = parseIso8601Duration(
            mpd.getAttribute("mediaPresentationDuration")
        )

        // Resolve BaseURL chain: MPD → Period.
        val mpdBase = baseUrl.resolveChild(mpd.firstBaseUrl())
        val periodBase = mpdBase.resolveChild(period.firstBaseUrl())

        val videoRep = pickRepresentation(period, "video") ?: return Result(false, "No video track")
        val audioRep = pickRepresentation(period, "audio")

        val videoStream = buildStream(videoRep, periodBase, presentationSeconds)
            ?: return Result(false, "Unsupported video segmentation")
        val audioStream = audioRep?.let { buildStream(it, periodBase, presentationSeconds) }

        val parent = outputFile.parentFile
        val base = outputFile.nameWithoutExtension
        val totalSegments = videoStream.totalCount() + (audioStream?.totalCount() ?: 0)
        var done = 0
        var bytes = 0L
        val onSeg: suspend (Long) -> Unit = { n -> done++; bytes += n; onProgress(done, totalSegments, bytes) }

        val videoFile = File(parent, "$base.vdl")
        writeStream(videoStream, videoFile, isActive, onSeg)?.let {
            videoFile.delete(); return Result(false, it)
        }
        var audioFile: File? = null
        if (audioStream != null) {
            val af = File(parent, "$base.adl")
            writeStream(audioStream, af, isActive, onSeg)?.let {
                videoFile.delete(); af.delete(); return Result(false, it)
            }
            audioFile = af
        }

        val mux = MediaRemuxer().remux(listOfNotNull(videoFile, audioFile), outputFile)
        videoFile.delete(); audioFile?.delete()
        return if (mux.success) {
            Result(true, mux.message, mux.outputFile ?: outputFile)
        } else {
            Result(false, mux.message ?: "Mux failed")
        }
    }

    private fun RepStream.totalCount(): Int = segmentUrls.size + if (initUrl != null) 1 else 0

    /** Pick the highest-bandwidth Representation in an AdaptationSet of [contentKind]. */
    private fun pickRepresentation(period: Element, contentKind: String): RepContext? {
        var best: RepContext? = null
        var bestBandwidth = -1L
        period.childElements("AdaptationSet").forEach { adaptation ->
            if (!adaptationMatches(adaptation, contentKind)) return@forEach
            adaptation.childElements("Representation").forEach { rep ->
                val bw = rep.getAttribute("bandwidth").toLongOrNull()
                    ?: rep.getAttribute("width").toLongOrNull()?.times(1000) ?: 0L
                if (bw >= bestBandwidth) {
                    bestBandwidth = bw
                    best = RepContext(adaptation, rep)
                }
            }
        }
        return best
    }

    private fun adaptationMatches(adaptation: Element, contentKind: String): Boolean {
        val mime = adaptation.getAttribute("mimeType").ifEmpty {
            adaptation.firstChildElement("Representation")?.getAttribute("mimeType").orEmpty()
        }
        val contentType = adaptation.getAttribute("contentType")
        return contentType.startsWith(contentKind, true) || mime.startsWith("$contentKind/", true)
    }

    private class RepContext(val adaptation: Element, val representation: Element)

    /** Build the concrete init + segment URL list for a representation. */
    private fun buildStream(ctx: RepContext, periodBase: HttpUrl, presentationSeconds: Double): RepStream? {
        val repBase = periodBase
            .resolveChild(ctx.adaptation.firstBaseUrl())
            .resolveChild(ctx.representation.firstBaseUrl())
        val repId = ctx.representation.getAttribute("id")
        val bandwidth = ctx.representation.getAttribute("bandwidth").toLongOrNull() ?: 0L

        // SegmentTemplate may live on the Representation or the AdaptationSet.
        val template = ctx.representation.firstChildElement("SegmentTemplate")
            ?: ctx.adaptation.firstChildElement("SegmentTemplate")
        if (template != null) {
            return buildFromTemplate(template, repBase, repId, bandwidth, presentationSeconds)
        }

        val segmentList = ctx.representation.firstChildElement("SegmentList")
            ?: ctx.adaptation.firstChildElement("SegmentList")
        if (segmentList != null) {
            return buildFromSegmentList(segmentList, repBase)
        }

        // No template/list → the BaseURL is a single self-contained file (SegmentBase).
        return RepStream(initUrl = null, segmentUrls = listOf(repBase))
    }

    private fun buildFromTemplate(
        template: Element,
        base: HttpUrl,
        repId: String,
        bandwidth: Long,
        presentationSeconds: Double,
    ): RepStream? {
        val initTemplate = template.getAttribute("initialization").ifEmpty { null }
        val mediaTemplate = template.getAttribute("media").ifEmpty { null } ?: return null
        val timescale = template.getAttribute("timescale").toLongOrNull() ?: 1L
        val startNumber = template.getAttribute("startNumber").toLongOrNull() ?: 1L

        val initUrl = initTemplate?.let {
            base.resolve(expandTemplate(it, repId, bandwidth, 0L, 0L))
        }

        val timeline = template.firstChildElement("SegmentTimeline")
        val segmentUrls = mutableListOf<HttpUrl>()
        if (timeline != null) {
            var number = startNumber
            var currentTime = 0L
            timeline.childElements("S").forEach { s ->
                // An explicit @t resets the timeline cursor (first S, or after a gap).
                val t = s.getAttribute("t").toLongOrNull()
                val d = s.getAttribute("d").toLongOrNull() ?: 0L
                val r = s.getAttribute("r").toIntOrNull() ?: 0
                if (t != null) currentTime = t
                repeat(r + 1) {
                    base.resolve(expandTemplate(mediaTemplate, repId, bandwidth, number, currentTime))
                        ?.let(segmentUrls::add)
                    number++
                    currentTime += d
                }
            }
        } else {
            val duration = template.getAttribute("duration").toLongOrNull() ?: return null
            if (duration <= 0 || presentationSeconds <= 0) return null
            val segDurationSec = duration.toDouble() / timescale
            val count = Math.ceil(presentationSeconds / segDurationSec).toInt().coerceAtLeast(1)
            for (i in 0 until count) {
                val number = startNumber + i
                base.resolve(expandTemplate(mediaTemplate, repId, bandwidth, number, 0L))
                    ?.let(segmentUrls::add)
            }
        }
        if (segmentUrls.isEmpty()) return null
        return RepStream(initUrl, segmentUrls)
    }

    private fun buildFromSegmentList(segmentList: Element, base: HttpUrl): RepStream? {
        val initUrl = segmentList.firstChildElement("Initialization")
            ?.getAttribute("sourceURL")?.ifEmpty { null }
            ?.let { base.resolve(it) }
        val segments = segmentList.childElements("SegmentURL").mapNotNull { seg ->
            seg.getAttribute("media").ifEmpty { null }?.let { base.resolve(it) }
        }
        if (segments.isEmpty()) return null
        return RepStream(initUrl, segments)
    }

    private suspend fun writeStream(
        stream: RepStream,
        outFile: File,
        isActive: () -> Boolean,
        onSegment: suspend (segBytes: Long) -> Unit,
    ): String? {
        try {
            FileOutputStream(outFile, false).use { output ->
                stream.initUrl?.let { init ->
                    if (!isActive()) return "Cancelled"
                    val bytes = fetchBytes(init.toString()) ?: return "Init segment download failed"
                    output.write(bytes)
                    onSegment(bytes.size.toLong())
                }
                stream.segmentUrls.forEachIndexed { index, url ->
                    if (!isActive()) return "Cancelled"
                    val bytes = fetchBytes(url.toString())
                        ?: return "Segment ${index + 1} download failed"
                    output.write(bytes)
                    onSegment(bytes.size.toLong())
                }
                output.flush()
            }
        } catch (e: Exception) {
            return e.message ?: "DASH write failed"
        }
        return null
    }

    // ---- XML helpers ----

    private fun Element.childElements(tag: String): List<Element> {
        val result = mutableListOf<Element>()
        val nodes = childNodes
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            if (node.nodeType == Node.ELEMENT_NODE && (node as Element).tagName == tag) result.add(node)
        }
        return result
    }

    private fun Element.firstChildElement(tag: String): Element? = childElements(tag).firstOrNull()

    /** First direct BaseURL child's text, or null. */
    private fun Element.firstBaseUrl(): String? =
        firstChildElement("BaseURL")?.textContent?.trim()?.ifEmpty { null }

    private fun HttpUrl.resolveChild(relative: String?): HttpUrl =
        relative?.let { resolve(it) } ?: this

    private fun buildRequest(url: String): Request {
        val b = Request.Builder().url(url)
        storedHeaders.forEach { (k, v) ->
            if (!k.equals("Host", true) && !k.equals("Connection", true) &&
                !k.equals("Accept-Encoding", true) && !k.equals("Range", true)
            ) {
                b.addHeader(k, v)
            }
        }
        CookieManager.getInstance().getCookie(url)?.takeIf { it.isNotBlank() }?.let {
            b.header("Cookie", it)
        }
        if (storedHeaders.keys.none { it.equals("User-Agent", ignoreCase = true) }) {
            b.header("User-Agent", UA)
        }
        b.header("Accept-Encoding", "identity")
        return b.build()
    }

    private fun fetchText(url: String): String? = try {
        client.newCall(buildRequest(url)).execute().use { r ->
            if (r.isSuccessful) r.body?.string() else null
        }
    } catch (_: Exception) {
        null
    }

    private fun fetchBytes(url: String): ByteArray? = try {
        client.newCall(buildRequest(url)).execute().use { r ->
            if (r.isSuccessful) r.body?.bytes() else null
        }
    } catch (_: Exception) {
        null
    }

    companion object {
        private const val UA =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}

/** Expand $RepresentationID$, $Bandwidth$, $Number[%0Nd]$, $Time[%0Nd]$ and $$ in a template. */
internal fun expandTemplate(
    template: String,
    repId: String,
    bandwidth: Long,
    number: Long,
    time: Long,
): String {
    var out = template
        .replace("\$RepresentationID\$", repId)
        .replace("\$Bandwidth\$", bandwidth.toString())
    out = Regex("\\\$Number(%0\\d+d)?\\\$").replace(out) { m -> formatTemplateVar(m.groupValues[1], number) }
    out = Regex("\\\$Time(%0\\d+d)?\\\$").replace(out) { m -> formatTemplateVar(m.groupValues[1], time) }
    return out.replace("\$\$", "\$")
}

private fun formatTemplateVar(padFormat: String, value: Long): String {
    if (padFormat.isEmpty()) return value.toString()
    val width = Regex("%0(\\d+)d").find(padFormat)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    return value.toString().padStart(width, '0')
}

/** ISO-8601 duration (PT#H#M#S) → seconds. Returns 0 when absent/unparseable. */
internal fun parseIso8601Duration(value: String?): Double {
    if (value.isNullOrBlank()) return 0.0
    val m = Regex("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:([\\d.]+)S)?").find(value) ?: return 0.0
    val h = m.groupValues[1].toDoubleOrNull() ?: 0.0
    val min = m.groupValues[2].toDoubleOrNull() ?: 0.0
    val s = m.groupValues[3].toDoubleOrNull() ?: 0.0
    return h * 3600 + min * 60 + s
}
