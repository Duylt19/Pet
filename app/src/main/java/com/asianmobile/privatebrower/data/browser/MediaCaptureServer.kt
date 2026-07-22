package com.asianmobile.privatebrower.data.browser

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.thread

/**
 * A tiny loopback HTTP endpoint that receives the raw media bytes a page's player feeds to
 * MediaSource (Tier 3 capture). The injected MSE hook streams each `appendBuffer` chunk here
 * as a binary POST, which is far cheaper than marshalling base64 over a JS bridge and avoids
 * OOM on large videos.
 *
 * Wire protocol (loopback only, guarded by a random per-process token):
 *   POST /<token>/<captureId>/<track>   body = raw bytes  → appended to that track's file
 *   POST /<token>/<captureId>/end       (no body)         → marks the capture complete
 *
 * `track` is "v" (video), "a" (audio) or "m" (muxed). Files live in cacheDir/mse_capture.
 */
@Singleton
class MediaCaptureServer @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    /** Secret path segment so arbitrary page scripts can't POST into the capture. */
    val token: String = randomToken()

    private val started = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private val workers = Executors.newFixedThreadPool(4)

    // captureId -> per-track append streams, opened lazily.
    private val sinks = ConcurrentHashMap<String, MutableMap<String, OutputStream>>()
    private val completed = ConcurrentHashMap.newKeySet<String>()

    val captureDir: File by lazy {
        File(context.cacheDir, "mse_capture").apply { mkdirs() }
    }

    /** Start the server if needed; returns the loopback port. */
    @Synchronized
    fun ensureStarted(): Int {
        serverSocket?.let { if (!it.isClosed) return it.localPort }
        val socket = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
        serverSocket = socket
        started.set(true)
        thread(name = "mse-capture-accept", isDaemon = true) { acceptLoop(socket) }
        return socket.localPort
    }

    fun fileFor(captureId: String, track: String): File =
        File(captureDir, "cap_${captureId}_$track.bin")

    fun isComplete(captureId: String): Boolean = completed.contains(captureId)

    /** Close and delete every artifact of a capture once it has been consumed. */
    fun discard(captureId: String) {
        sinks.remove(captureId)?.values?.forEach { runCatching { it.close() } }
        completed.remove(captureId)
        captureDir.listFiles()?.filter { it.name.startsWith("cap_${captureId}_") }
            ?.forEach { it.delete() }
    }

    private fun acceptLoop(socket: ServerSocket) {
        while (!socket.isClosed) {
            val client = try {
                socket.accept()
            } catch (_: Exception) {
                break
            }
            workers.execute { runCatching { handle(client) }.also { runCatching { client.close() } } }
        }
    }

    private fun handle(client: Socket) {
        client.getInputStream().let { input ->
            val requestLine = readLine(input) ?: return
            // e.g. "POST /<token>/<captureId>/<track> HTTP/1.1"
            val path = requestLine.split(' ').getOrNull(1) ?: return
            var contentLength = 0L
            while (true) {
                val header = readLine(input) ?: break
                if (header.isEmpty()) break
                val lower = header.lowercase()
                if (lower.startsWith("content-length:")) {
                    contentLength = header.substringAfter(':').trim().toLongOrNull() ?: 0L
                }
            }

            val parts = path.trim('/').split('/')
            val ok = parts.size >= 3 && parts[0] == token
            if (ok) {
                val captureId = sanitize(parts[1])
                val track = sanitize(parts[2])
                if (track == "end") {
                    drain(input, contentLength)
                    sinks[captureId]?.values?.forEach { runCatching { it.flush() } }
                    completed.add(captureId)
                } else {
                    appendBody(captureId, track, input, contentLength)
                }
            } else {
                drain(input, contentLength)
            }
            respondOk(client.getOutputStream())
        }
    }

    private fun appendBody(captureId: String, track: String, input: InputStream, length: Long) {
        val out = sinkFor(captureId, track)
        val buffer = ByteArray(64 * 1024)
        var remaining = length
        synchronized(out) {
            while (remaining > 0) {
                val toRead = minOf(remaining, buffer.size.toLong()).toInt()
                val read = input.read(buffer, 0, toRead)
                if (read < 0) break
                out.write(buffer, 0, read)
                remaining -= read
            }
            out.flush()
        }
    }

    private fun sinkFor(captureId: String, track: String): OutputStream {
        val map = sinks.getOrPut(captureId) { ConcurrentHashMap() }
        return map.getOrPut(track) {
            BufferedOutputStream(fileFor(captureId, track).outputStream())
        }
    }

    private fun drain(input: InputStream, length: Long) {
        var remaining = length
        val buffer = ByteArray(64 * 1024)
        while (remaining > 0) {
            val read = input.read(buffer, 0, minOf(remaining, buffer.size.toLong()).toInt())
            if (read < 0) break
            remaining -= read
        }
    }

    private fun respondOk(out: OutputStream) {
        val body = "ok".toByteArray()
        val header = "HTTP/1.1 200 OK\r\n" +
            "Access-Control-Allow-Origin: *\r\n" +
            "Content-Length: ${body.size}\r\n" +
            "Connection: close\r\n\r\n"
        out.write(header.toByteArray())
        out.write(body)
        out.flush()
    }

    /** Reads a single CRLF-terminated line (ASCII) without over-reading the body. */
    private fun readLine(input: InputStream): String? {
        val sb = StringBuilder()
        var prev = -1
        while (true) {
            val b = input.read()
            if (b < 0) return if (sb.isEmpty()) null else sb.toString()
            if (b == '\n'.code) {
                if (prev == '\r'.code && sb.isNotEmpty()) sb.deleteCharAt(sb.length - 1)
                return sb.toString()
            }
            sb.append(b.toChar())
            prev = b
        }
    }

    private fun sanitize(value: String): String = value.filter { it.isLetterOrDigit() || it == '-' }

    private fun randomToken(): String =
        (1..24).map { TOKEN_CHARS.random() }.joinToString("")

    private companion object {
        private const val TOKEN_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789"
    }
}
