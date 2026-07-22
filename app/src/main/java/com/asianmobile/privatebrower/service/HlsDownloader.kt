package com.asianmobile.privatebrower.service

import android.webkit.CookieManager
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Downloads an HLS (m3u8) stream and writes a single playable file.
 *
 * Supported:
 *  - Master playlists (picks the highest-resolution/bandwidth variant + its audio rendition)
 *  - MPEG-TS segment concatenation → remux to MP4/WebM (H.264+AAC → MP4)
 *  - fMP4/CMAF segments (`#EXT-X-MAP` init segment) → remux to MP4/WebM
 *  - Separate audio rendition (`#EXT-X-MEDIA:TYPE=AUDIO` with a URI) → muxed with video
 *  - `#EXT-X-BYTERANGE` segments (single resource, per-segment byte ranges)
 *  - AES-128 encrypted segments (key fetched from EXT-X-KEY URI)
 *
 * Not supported (fails gracefully): SAMPLE-AES / Widevine DRM, un-muxable codecs.
 *
 * Video (and any separate audio) is remuxed via [MediaRemuxer] — copying encoded samples, no
 * re-encode. A plain MPEG-TS with no separate audio falls back to raw `.ts` if remux fails.
 */
class HlsDownloader(
    private val client: OkHttpClient,
    private val storedHeaders: Map<String, String>,
) {
    /** [outputFile] is the actual file written (MP4, WebM, or the .ts fallback). */
    data class Result(val success: Boolean, val message: String? = null, val outputFile: File? = null)

    private data class ByteRange(val offset: Long, val length: Long)
    private data class Segment(val url: HttpUrl, val range: ByteRange?)
    private class MediaPlaylist(
        val segments: List<Segment>,
        val initSegment: Segment?,
        val keyBytes: ByteArray?,
        val explicitIv: ByteArray?,
        val mediaSequence: Long,
        val isFmp4: Boolean,
    )
    private data class VariantSelection(val uri: HttpUrl, val audioGroupId: String?)
    private sealed interface PlaylistResult {
        data class Ok(val playlist: MediaPlaylist) : PlaylistResult
        data class Err(val message: String) : PlaylistResult
    }

    suspend fun download(
        playlistUrl: String,
        outputFile: File,
        isActive: () -> Boolean,
        onProgress: suspend (done: Int, total: Int, bytes: Long) -> Unit,
    ): Result {
        val masterUrl = playlistUrl.toHttpUrlOrNull()
            ?: return Result(false, "Invalid playlist URL")

        val masterText = fetchText(playlistUrl) ?: return Result(false, "Cannot fetch playlist")
        // Some sites serve an HTML player page (or an anti-bot wrapper) from a URL that only
        // happens to contain ".m3u8". Reject anything that isn't a real playlist so we never
        // parse HTML as segment URLs and leave a 0-byte file behind.
        if (!masterText.isHlsPlaylist()) return Result(false, "Not an HLS playlist")

        // Master playlist → choose the best variant + its (optional) separate audio rendition.
        var videoPlaylistUrl = masterUrl
        var audioPlaylistUrl: HttpUrl? = null
        if (masterText.contains("#EXT-X-STREAM-INF")) {
            val variant = pickVariant(masterText, masterUrl)
                ?: return Result(false, "No playable variant")
            videoPlaylistUrl = variant.uri
            audioPlaylistUrl = variant.audioGroupId
                ?.let { findAudioRendition(masterText, masterUrl, it) }
        }

        val videoPlaylist = when (val r = loadMediaPlaylist(videoPlaylistUrl)) {
            is PlaylistResult.Err -> return Result(false, r.message)
            is PlaylistResult.Ok -> r.playlist
        }
        val audioPlaylist = audioPlaylistUrl?.let {
            when (val r = loadMediaPlaylist(it)) {
                is PlaylistResult.Err -> return Result(false, r.message)
                is PlaylistResult.Ok -> r.playlist
            }
        }

        val parent = outputFile.parentFile
        val base = outputFile.nameWithoutExtension
        val totalSegments = videoPlaylist.segments.size + (audioPlaylist?.segments?.size ?: 0)
        var doneSegments = 0
        var totalBytes = 0L
        val onSegment: suspend (Long) -> Unit = { segBytes ->
            doneSegments++
            totalBytes += segBytes
            onProgress(doneSegments, totalSegments, totalBytes)
        }

        val videoFile = File(parent, "$base.vdl")
        writeStream(videoPlaylist, videoFile, isActive, onSegment)?.let {
            videoFile.delete()
            return Result(false, it)
        }

        var audioFile: File? = null
        if (audioPlaylist != null) {
            val af = File(parent, "$base.adl")
            writeStream(audioPlaylist, af, isActive, onSegment)?.let {
                videoFile.delete(); af.delete()
                return Result(false, it)
            }
            audioFile = af
        }

        // Remux video (+ separate audio) → MP4/WebM so it plays in the system player.
        val inputs = listOfNotNull(videoFile, audioFile)
        val mux = MediaRemuxer().remux(inputs, outputFile)
        if (mux.success) {
            videoFile.delete(); audioFile?.delete()
            return Result(true, message = mux.message, outputFile = mux.outputFile ?: outputFile)
        }

        // Fallback: a plain muxed MPEG-TS (no separate audio, no fMP4 init) is playable as-is,
        // so keep the raw .ts. fMP4 / split-audio streams are unusable un-muxed → real failure.
        if (audioFile == null && !videoPlaylist.isFmp4) {
            outputFile.delete()
            val tsOut = File(parent, "$base.ts")
            tsOut.delete()
            val kept = videoFile.renameTo(tsOut)
            return Result(true, message = "Saved as .ts", outputFile = if (kept) tsOut else videoFile)
        }
        videoFile.delete(); audioFile?.delete()
        return Result(false, mux.message ?: "Mux failed")
    }

    private fun loadMediaPlaylist(playlistUrl: HttpUrl): PlaylistResult {
        val text = fetchText(playlistUrl.toString())
            ?: return PlaylistResult.Err("Cannot fetch media playlist")
        if (!text.isHlsPlaylist()) return PlaylistResult.Err("Not an HLS playlist")
        return parseMediaPlaylist(text, playlistUrl)
    }

    /** Parse a media playlist: init segment (fMP4), encryption, byte ranges, and segment URLs. */
    private fun parseMediaPlaylist(text: String, mediaUrl: HttpUrl): PlaylistResult {
        val segments = mutableListOf<Segment>()
        var initSegment: Segment? = null
        var keyUrl: HttpUrl? = null
        var keyMethod: String? = null
        var explicitIv: ByteArray? = null
        var mediaSequence = 0L
        var isFmp4 = false
        var pendingRange: ByteRange? = null
        // EXT-X-BYTERANGE without an explicit offset continues from the previous sub-range end.
        var nextByteOffset = 0L

        for (raw in text.lines()) {
            val line = raw.trim()
            when {
                line.startsWith("#EXT-X-MEDIA-SEQUENCE:") ->
                    mediaSequence = line.substringAfter(':').trim().toLongOrNull() ?: 0L
                line.startsWith("#EXT-X-MAP:") -> {
                    isFmp4 = true
                    val uri = extractAttr(line, "URI")?.let { mediaUrl.resolve(it) }
                    val range = extractAttr(line, "BYTERANGE")?.let { parseByteRange(it, 0L).first }
                    if (uri != null) initSegment = Segment(uri, range)
                }
                line.startsWith("#EXT-X-KEY:") -> {
                    keyMethod = extractAttr(line, "METHOD")
                    keyUrl = extractAttr(line, "URI")?.let { mediaUrl.resolve(it) }
                    explicitIv = extractAttr(line, "IV")?.let {
                        hexToBytes(it.removePrefix("0x").removePrefix("0X"))
                    }
                }
                line.startsWith("#EXT-X-BYTERANGE:") -> {
                    val (range, newOffset) = parseByteRange(line.substringAfter(':').trim(), nextByteOffset)
                    pendingRange = range
                    nextByteOffset = newOffset
                }
                line.isNotEmpty() && !line.startsWith("#") -> {
                    mediaUrl.resolve(line)?.let { segments.add(Segment(it, pendingRange)) }
                    pendingRange = null
                }
            }
        }

        if (segments.isEmpty()) return PlaylistResult.Err("No segments found")
        if (keyMethod != null && keyMethod != "AES-128" && keyMethod != "NONE") {
            return PlaylistResult.Err("Unsupported encryption: $keyMethod")
        }
        val keyBytes = if (keyMethod == "AES-128" && keyUrl != null) {
            fetchBytes(keyUrl.toString()) ?: return PlaylistResult.Err("Cannot fetch decryption key")
        } else null

        return PlaylistResult.Ok(
            MediaPlaylist(segments, initSegment, keyBytes, explicitIv, mediaSequence, isFmp4)
        )
    }

    /** "len[@off]" → (range, offset+len). When off is omitted it continues from [continuation]. */
    private fun parseByteRange(spec: String, continuation: Long): Pair<ByteRange, Long> {
        val parts = spec.split('@')
        val length = parts[0].trim().toLongOrNull() ?: 0L
        val offset = parts.getOrNull(1)?.trim()?.toLongOrNull() ?: continuation
        return ByteRange(offset, length) to (offset + length)
    }

    /** Download a playlist's init + media segments (decrypt/align as needed) into [outFile]. */
    private suspend fun writeStream(
        playlist: MediaPlaylist,
        outFile: File,
        isActive: () -> Boolean,
        onSegment: suspend (segBytes: Long) -> Unit,
    ): String? {
        try {
            FileOutputStream(outFile, false).use { output ->
                // fMP4 init segment first — raw bytes, never TS-aligned.
                playlist.initSegment?.let { init ->
                    if (!isActive()) return "Cancelled"
                    val bytes = fetchBytes(init.url.toString(), init.range)
                        ?: return "Init segment download failed"
                    output.write(bytes)
                }
                playlist.segments.forEachIndexed { index, seg ->
                    if (!isActive()) return "Cancelled"
                    val raw = fetchBytes(seg.url.toString(), seg.range)
                        ?: return "Segment ${index + 1} download failed"
                    val decrypted = if (playlist.keyBytes != null) {
                        val iv = playlist.explicitIv ?: sequenceIv(playlist.mediaSequence + index)
                        decryptAes128(raw, playlist.keyBytes, iv)
                    } else raw
                    // TS segments may be disguised as images (fake header + padding) — strip to
                    // the first real TS packet. fMP4 payloads are written untouched.
                    val data = if (playlist.isFmp4) decrypted else alignToTsSync(decrypted)
                    output.write(data)
                    onSegment(data.size.toLong())
                }
                output.flush()
            }
        } catch (e: Exception) {
            return e.message ?: "HLS write failed"
        }
        return null
    }

    /**
     * Return [data] starting at the first real MPEG-TS packet boundary — the sync byte
     * 0x47 repeated at 188-byte intervals. Segments that already start on a sync byte are
     * returned untouched; disguised segments (e.g. a fake PNG header + padding prefix) get
     * their prefix stripped. Non-TS payloads (fMP4/other) are returned as-is.
     */
    private fun alignToTsSync(data: ByteArray): ByteArray {
        if (data.size < 377) return data
        if (data[0] == TS_SYNC && data[188] == TS_SYNC && data[376] == TS_SYNC) return data
        val limit = minOf(data.size - 377, MAX_TS_PREFIX_SCAN)
        var i = 0
        while (i <= limit) {
            if (data[i] == TS_SYNC && data[i + 188] == TS_SYNC && data[i + 376] == TS_SYNC) {
                return if (i == 0) data else data.copyOfRange(i, data.size)
            }
            i++
        }
        return data
    }

    /** Pick the best video variant (highest resolution, tie-break bandwidth) + its AUDIO group. */
    private fun pickVariant(masterText: String, base: HttpUrl): VariantSelection? {
        val lines = masterText.lines()
        var bestScore = -1L
        var bestUri: String? = null
        var bestAudio: String? = null
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.startsWith("#EXT-X-STREAM-INF")) {
                val bw = extractAttr(line, "BANDWIDTH")?.toLongOrNull()
                    ?: extractAttr(line, "AVERAGE-BANDWIDTH")?.toLongOrNull() ?: 0L
                val resScore = extractAttr(line, "RESOLUTION")?.let { res ->
                    val wh = res.split('x', 'X')
                    (wh.getOrNull(0)?.trim()?.toLongOrNull() ?: 0L) *
                        (wh.getOrNull(1)?.trim()?.toLongOrNull() ?: 0L)
                } ?: 0L
                // URI is on the next non-comment line.
                var j = i + 1
                var uri: String? = null
                while (j < lines.size) {
                    val l = lines[j].trim()
                    if (l.isEmpty()) { j++; continue }
                    if (!l.startsWith("#")) { uri = l; break }
                    j++
                }
                val score = resScore * 1_000_000L + bw
                if (!uri.isNullOrEmpty() && score >= bestScore) {
                    bestScore = score
                    bestUri = uri
                    bestAudio = extractAttr(line, "AUDIO")
                }
                i = j + 1
            } else {
                i++
            }
        }
        return bestUri?.let { base.resolve(it) }?.let { VariantSelection(it, bestAudio) }
    }

    /** Resolve the audio media playlist for [groupId] (prefers DEFAULT=YES). Null if muxed-in. */
    private fun findAudioRendition(masterText: String, base: HttpUrl, groupId: String): HttpUrl? {
        var firstMatch: HttpUrl? = null
        for (raw in masterText.lines()) {
            val line = raw.trim()
            if (!line.startsWith("#EXT-X-MEDIA:")) continue
            if (extractAttr(line, "TYPE") != "AUDIO") continue
            if (extractAttr(line, "GROUP-ID") != groupId) continue
            // No URI → audio is muxed into the video segments; nothing separate to fetch.
            val uri = extractAttr(line, "URI") ?: continue
            val resolved = base.resolve(uri) ?: continue
            if (extractAttr(line, "DEFAULT") == "YES") return resolved
            if (firstMatch == null) firstMatch = resolved
        }
        return firstMatch
    }

    /** Extracts ATTR=value or ATTR="value" from an HLS tag line. */
    private fun extractAttr(line: String, attr: String): String? {
        val m = Regex("$attr=(\"([^\"]*)\"|([^,]*))").find(line) ?: return null
        return m.groupValues[2].ifEmpty { m.groupValues[3] }.trim().ifEmpty { null }
    }

    /** Default IV when EXT-X-KEY has no explicit IV: the media sequence number, big-endian. */
    private fun sequenceIv(seq: Long): ByteArray {
        val iv = ByteArray(16)
        for (j in 0 until 8) {
            iv[15 - j] = ((seq shr (8 * j)) and 0xFF).toByte()
        }
        return iv
    }

    private fun hexToBytes(hex: String): ByteArray {
        val clean = if (hex.length % 2 != 0) "0$hex" else hex
        return ByteArray(clean.length / 2) {
            ((Character.digit(clean[it * 2], 16) shl 4) +
                Character.digit(clean[it * 2 + 1], 16)).toByte()
        }
    }

    private fun decryptAes128(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return cipher.doFinal(data)
    }

    private fun buildRequest(url: String, range: ByteRange? = null): Request {
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
        if (range != null && range.length > 0) {
            b.header("Range", "bytes=${range.offset}-${range.offset + range.length - 1}")
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

    private fun fetchBytes(url: String, range: ByteRange? = null): ByteArray? = try {
        client.newCall(buildRequest(url, range)).execute().use { r ->
            if (r.isSuccessful) r.body?.bytes() else null
        }
    } catch (_: Exception) {
        null
    }

    companion object {
        private const val UA =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        private const val TS_SYNC = 0x47.toByte()
        private const val MAX_TS_PREFIX_SCAN = 65536
    }
}

/** A real HLS playlist starts with the #EXTM3U tag (ignoring a leading BOM/whitespace). */
private fun String.isHlsPlaylist(): Boolean =
    trimStart('﻿', ' ', '\t', '\r', '\n').startsWith("#EXTM3U")
