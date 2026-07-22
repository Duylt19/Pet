package com.asianmobile.privatebrower.service

import android.webkit.CookieManager
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * Downloads a DASH video that is split into separate audio-only and video-only files, then muxes
 * them into a single playable file via [MediaRemuxer] (copies encoded samples — no re-encode, no
 * ffmpeg). H.264 + AAC → MP4; VP9 + Opus/Vorbis → WebM.
 */
class DashMuxDownloader(
    private val client: OkHttpClient,
    private val storedHeaders: Map<String, String>,
) {
    /** [outputFile] is the file actually written (its extension may be MP4 or WebM). */
    data class Result(val success: Boolean, val message: String? = null, val outputFile: File? = null)

    suspend fun download(
        videoUrl: String,
        audioUrl: String,
        outputFile: File,
        isActive: () -> Boolean,
        onProgress: suspend (bytes: Long, total: Long) -> Unit,
    ): Result {
        val dir = outputFile.parentFile
        val tmpVideo = File(dir, "${outputFile.name}.v.tmp")
        val tmpAudio = File(dir, "${outputFile.name}.a.tmp")
        try {
            // Phase 1: video-only track
            val vLen = downloadTo(videoUrl, tmpVideo, isActive) { done ->
                onProgress(done, 0)
            } ?: return Result(false, "Video track download failed")
            if (!isActive()) return Result(false, "Cancelled")

            // Phase 2: audio-only track (optional — some sources are already combined)
            val total = vLen.coerceAtLeast(0)
            val aLen = if (audioUrl.isNotBlank()) {
                downloadTo(audioUrl, tmpAudio, isActive) { done ->
                    onProgress(vLen + done, total)
                } ?: return Result(false, "Audio track download failed")
            } else 0L
            if (!isActive()) return Result(false, "Cancelled")

            // Phase 3: mux the tracks into one container
            val muxResult = MediaRemuxer().remux(
                inputs = listOfNotNull(tmpVideo, tmpAudio.takeIf { it.exists() && it.length() > 0 }),
                requestedOutput = outputFile
            )
            if (!muxResult.success) return Result(false, muxResult.message ?: "Mux failed")
            onProgress(vLen + aLen, vLen + aLen)
            return Result(true, muxResult.message, muxResult.outputFile ?: outputFile)
        } catch (e: Exception) {
            return Result(false, e.message ?: "Mux failed")
        } finally {
            tmpVideo.delete()
            tmpAudio.delete()
        }
    }

    /** Streams [url] to [file]; returns total bytes written, or null on failure. */
    private suspend fun downloadTo(
        url: String,
        file: File,
        isActive: () -> Boolean,
        onBytes: suspend (Long) -> Unit,
    ): Long? {
        client.newCall(buildRequest(url)).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body ?: return null
            var written = 0L
            var lastReport = 0L
            body.byteStream().use { input ->
                FileOutputStream(file).use { output ->
                    val buf = ByteArray(64 * 1024)
                    while (isActive()) {
                        val n = input.read(buf)
                        if (n == -1) break
                        output.write(buf, 0, n)
                        written += n
                        if (written - lastReport > 256 * 1024) {
                            lastReport = written
                            onBytes(written)
                        }
                    }
                    output.flush()
                }
            }
            return written
        }
    }

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

    companion object {
        private const val UA =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
