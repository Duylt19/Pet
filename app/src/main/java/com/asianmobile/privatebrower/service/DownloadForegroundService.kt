package com.asianmobile.privatebrower.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.asianmobile.privatebrower.MainActivity
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.data.browser.MediaResourceKind
import com.asianmobile.privatebrower.data.browser.VideoSniffer.Companion.MSE_CAPTURE_SCHEME
import com.asianmobile.privatebrower.data.browser.classifyMediaResource
import com.asianmobile.privatebrower.data.database.dao.DownloadDao
import com.asianmobile.privatebrower.data.database.entity.DownloadEntity
import com.asianmobile.privatebrower.data.download.DownloadStorage
import com.asianmobile.privatebrower.utils.ToastHelper
import com.asianmobile.privatebrower.utils.permission.DownloadNotificationPermissionRequests
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

@AndroidEntryPoint
class DownloadForegroundService : Service() {

    @Inject lateinit var downloadDao: DownloadDao
    @Inject lateinit var okHttpClient: OkHttpClient
    @Inject lateinit var mediaCaptureServer: com.asianmobile.privatebrower.data.browser.MediaCaptureServer

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val pendingControlActions = ConcurrentHashMap.newKeySet<Long>()
    @Volatile private var isStoppingForTimeout = false
    private val notificationManager by lazy {
        getSystemService(NotificationManager::class.java)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        val downloadId = intent?.getLongExtra(EXTRA_ID, -1L) ?: -1L
        if (downloadId < 0 && action != ACTION_STOP_ALL) {
            // Service was restarted by the OS (START_STICKY, null intent) after being
            // killed mid-download. Resume any downloads that were left unfinished.
            resumeInterrupted()
            return START_STICKY
        }

        when (action) {
            ACTION_START -> {
                startDownload(downloadId)
                ToastHelper.show(this, getString(R.string.download_toast_started))
            }
            ACTION_PAUSE -> pauseDownload(downloadId)
            ACTION_RESUME -> resumeDownload(downloadId)
            ACTION_CANCEL -> cancelDownload(downloadId)
        }

        return START_STICKY
    }

    @Synchronized
    private fun startDownload(downloadId: Long) {
        if (isStoppingForTimeout) return
        if (activeJobs.containsKey(downloadId)) return

        // Cap parallel downloads. Over the limit, the row stays PENDING and is
        // picked up by startNextPendingOrStop() when a slot frees up.
        if (activeJobs.size >= MAX_CONCURRENT) return

        ensureForeground()

        val job = scope.launch(start = CoroutineStart.LAZY) {
            // Track bytes via the file on disk so pause/resume stays consistent.
            var downloadedBytes = 0L
            try {
                val entity = downloadDao.getById(downloadId) ?: return@launch
                debugLog(
                    "start id=$downloadId mime=${entity.mimeType} " +
                        "mux=${entity.audioUrl.isNotBlank()} url=${entity.url.take(140)}"
                )
                val targetFile = DownloadStorage.pendingFile(
                    context = this@DownloadForegroundService,
                    currentLocator = entity.path,
                    fileName = entity.fileName
                )
                if (targetFile.absolutePath != entity.path) {
                    downloadDao.updatePath(downloadId, targetFile.absolutePath)
                }
                downloadDao.updateStatus(downloadId, "RUNNING")
                notifyDownloadProgress(
                    downloadId = downloadId,
                    fileName = entity.fileName,
                    downloaded = 0,
                    total = entity.sizeBytes
                )
                targetFile.parentFile?.mkdirs()

                // These schemes aren't HTTP URLs, so handle them before building an OkHttp request.
                // Tier 3: bytes captured from the page's MediaSource → remux the capture files.
                if (entity.url.startsWith(MSE_CAPTURE_SCHEME)) {
                    handleCaptureDownload(downloadId, entity, targetFile)
                    return@launch
                }
                // blob: file read in-page and streamed to the capture server → move into place.
                if (entity.url.startsWith(BLOB_CAPTURE_SCHEME)) {
                    handleBlobCapture(downloadId, entity, targetFile)
                    return@launch
                }
                // data: URI → decode its embedded bytes directly.
                if (entity.url.startsWith("data:")) {
                    handleDataDownload(downloadId, entity, targetFile)
                    return@launch
                }

                val requestBuilder = Request.Builder().url(entity.url)
                val storedHeaders = parseHeaders(entity.requestHeaders)

                // Add stored request headers (referer, cookies, etc.)
                storedHeaders.forEach { (key, value) ->
                    if (!key.equals("Host", ignoreCase = true) &&
                        !key.equals("Connection", ignoreCase = true) &&
                        !key.equals("Accept-Encoding", ignoreCase = true) &&
                        !key.equals("Range", ignoreCase = true)
                    ) {
                        requestBuilder.addHeader(key, value)
                    }
                }

                // Add WebView cookies for the URL
                val cookies = android.webkit.CookieManager.getInstance().getCookie(entity.url)
                if (!cookies.isNullOrBlank()) {
                    requestBuilder.header("Cookie", cookies)
                }

                if (storedHeaders.keys.none { it.equals("User-Agent", ignoreCase = true) }) {
                    requestBuilder.header("User-Agent", DEFAULT_USER_AGENT)
                }

                // Disable transparent compression so Content-Length / Range stay accurate
                requestBuilder.header("Accept-Encoding", "identity")

                // HLS streams need playlist parsing + segment merge, not a plain stream.
                if (isHlsDownload(entity)) {
                    handleHlsDownload(downloadId, entity, targetFile, storedHeaders)
                    return@launch
                }

                // MPEG-DASH manifest: parse .mpd, pick best tracks, download + mux.
                if (isDashManifest(entity)) {
                    handleDashManifestDownload(downloadId, entity, targetFile, storedHeaders)
                    return@launch
                }

                // Facebook DASH: separate audio + video tracks to download and mux.
                if (entity.audioUrl.isNotBlank()) {
                    handleMuxDownload(downloadId, entity, targetFile, storedHeaders)
                    return@launch
                }

                // Source of truth for resume = the actual bytes already on disk,
                // NOT the (throttled, possibly stale) value in the DB.
                if (targetFile.exists()) {
                    downloadedBytes = targetFile.length()
                }
                val wantResume = downloadedBytes > 0
                if (wantResume) {
                    requestBuilder.addHeader("Range", "bytes=$downloadedBytes-")
                }

                val request = requestBuilder.build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful && response.code != 206) {
                        markFailed(
                            downloadId,
                            entity.fileName,
                            downloadedBytes,
                            getString(R.string.download_error_http, response.code)
                        )
                        return@launch
                    }

                    val body = response.body ?: run {
                        markFailed(
                            downloadId,
                            entity.fileName,
                            downloadedBytes,
                            getString(R.string.download_error_empty_response)
                        )
                        return@launch
                    }

                    // Server honored the Range request → append. Otherwise it sent the
                    // full file from byte 0, so we must overwrite and reset the counter.
                    // The server's Content-Type is the source of truth for the extension when
                    // the URL/Content-Disposition didn't give a real one (e.g. an extensionless
                    // image URL that otherwise falls back to ".bin").
                    val contentType = response.header("Content-Type")?.substringBefore(";")?.trim()

                    val appendMode = wantResume && response.code == 206
                    if (!appendMode) {
                        downloadedBytes = 0L
                    }

                    val totalBytes = if (response.code == 206) {
                        val contentRange = response.header("Content-Range")
                        contentRange?.substringAfter("/")?.toLongOrNull()
                            ?: (body.contentLength() + downloadedBytes)
                    } else {
                        body.contentLength().let { if (it > 0) it else 0L }
                    }

                    // Update total size in DB
                    if (totalBytes > 0) {
                        downloadDao.updateSize(downloadId, totalBytes)
                    }

                    val buffer = ByteArray(8192)
                    var lastUpdateTime = 0L
                    var lastBytes = downloadedBytes

                    body.byteStream().use { input ->
                        FileOutputStream(targetFile, appendMode).use { output ->
                            while (isActive) {
                                val bytesRead = input.read(buffer)
                                if (bytesRead == -1) break
                                output.write(buffer, 0, bytesRead)
                                downloadedBytes += bytesRead

                                // Update progress every 500ms to reduce DB writes
                                val now = System.currentTimeMillis()
                                if (now - lastUpdateTime > 500) {
                                    val speedBps = if (lastUpdateTime > 0) {
                                        ((downloadedBytes - lastBytes) * 1000) / (now - lastUpdateTime)
                                    } else 0L
                                    lastUpdateTime = now
                                    lastBytes = downloadedBytes
                                    downloadDao.updateProgress(downloadId, "RUNNING", downloadedBytes)
                                    notifyDownloadProgress(
                                        downloadId,
                                        entity.fileName,
                                        downloadedBytes,
                                        totalBytes,
                                        speedBps
                                    )
                                }
                            }
                            output.flush()
                        }
                    }

                    if (isActive) {
                        completeDownload(downloadId, entity, correctExtension(targetFile, contentType))
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    // Keep the partial file on disk so the user can resume later.
                    downloadDao.updateFailure(
                        downloadId,
                        downloadedBytes,
                        e.message ?: getString(R.string.download_notification_failed)
                    )
                    val fileName = downloadDao.getById(downloadId)?.fileName
                    if (fileName != null) showFailedNotification(downloadId, fileName)
                }
            } finally {
                activeJobs.remove(downloadId)
                refreshSummaryNotification()
                if (downloadId !in pendingControlActions) startNextPendingOrStop()
            }
        }
        activeJobs[downloadId] = job
        refreshSummaryNotification()
        job.start()
    }

    /**
     * Re-queue downloads that were RUNNING/PENDING when the process was killed.
     * Resume relies on the bytes already present on disk (see [startDownload]).
     */
    private fun resumeInterrupted() {
        // Must call startForeground promptly after the service starts.
        ensureForeground()
        scope.launch {
            val pending = downloadDao.getResumable()
            if (pending.isEmpty()) {
                stopSelfIfIdle()
                return@launch
            }
            // Normalize to PENDING so anything over MAX_CONCURRENT is left queued and
            // later picked up by startNextPendingOrStop() instead of becoming a zombie.
            pending.forEach { entity -> downloadDao.updateStatus(entity.id, "PENDING") }
            pending.forEach { entity -> startDownload(entity.id) }
        }
    }

    private fun isHlsDownload(entity: DownloadEntity): Boolean =
        classifyMediaResource(entity.url, entity.mimeType) == MediaResourceKind.HLS_MANIFEST

    private fun isDashManifest(entity: DownloadEntity): Boolean =
        classifyMediaResource(entity.url, entity.mimeType) == MediaResourceKind.DASH_MANIFEST

    private fun parseHeaders(json: String): Map<String, String> {
        if (json.isBlank()) return emptyMap()
        return try {
            val obj = org.json.JSONObject(json)
            buildMap {
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    put(k, obj.getString(k))
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /**
     * Download an HLS playlist: fetch + parse m3u8, download every segment (decrypting
     * AES-128 if needed) and concatenate into [targetFile]. Progress is segment-based.
     */
    private suspend fun handleHlsDownload(
        downloadId: Long,
        entity: DownloadEntity,
        targetFile: File,
        headers: Map<String, String>
    ) {
        val job = coroutineContext[Job]
        var lastNotify = 0L
        var lastBytes = 0L
        notifyDownloadProgress(downloadId, entity.fileName, 0, 0)

        val result = HlsDownloader(okHttpClient, headers).download(
            playlistUrl = entity.url,
            outputFile = targetFile,
            isActive = { job?.isActive != false },
            onProgress = { done, total, bytes ->
                val now = System.currentTimeMillis()
                if (now - lastNotify > 500 || done == total) {
                    val speedBps = if (lastNotify > 0 && now > lastNotify) {
                        ((bytes - lastBytes) * 1000) / (now - lastNotify)
                    } else 0L
                    lastNotify = now
                    lastBytes = bytes
                    downloadDao.updateProgress(downloadId, "RUNNING", bytes)
                    val pct = if (total > 0) (done * 100) / total else 0
                    val speedText = if (speedBps > 0) " · ${formatBytes(speedBps)}/s" else ""
                    notificationManager?.notify(
                        DownloadNotificationIds.forDownload(downloadId),
                        buildProgressNotification(
                            downloadId = downloadId,
                            text = "$pct% — $done/$total segments · ${formatBytes(bytes)}$speedText",
                            progress = pct,
                            indeterminate = total <= 0,
                            title = entity.fileName
                        )
                    )
                }
            }
        )

        if (result.success) {
            // The remux step may hand back a different file (the .ts fallback).
            val finalFile = result.outputFile ?: targetFile
            completeDownload(downloadId, entity, finalFile)
        } else if (job?.isActive != false) {
            // Genuine failure (not a user pause/cancel, which already set the status).
            markFailed(
                downloadId,
                entity.fileName,
                targetFile.length(),
                result.message ?: getString(R.string.download_notification_failed)
            )
        }
    }

    /**
     * Download a generic MPEG-DASH stream: parse the .mpd, pick the best video + audio
     * representation, download their segments and mux into [targetFile]. Segment-based progress.
     */
    private suspend fun handleDashManifestDownload(
        downloadId: Long,
        entity: DownloadEntity,
        targetFile: File,
        headers: Map<String, String>
    ) {
        val job = coroutineContext[Job]
        var lastNotify = 0L
        var lastBytes = 0L
        notifyDownloadProgress(downloadId, entity.fileName, 0, 0)

        val result = MpdDownloader(okHttpClient, headers).download(
            mpdUrl = entity.url,
            outputFile = targetFile,
            isActive = { job?.isActive != false },
            onProgress = { done, total, bytes ->
                val now = System.currentTimeMillis()
                if (now - lastNotify > 500 || done == total) {
                    val speedBps = if (lastNotify > 0 && now > lastNotify) {
                        ((bytes - lastBytes) * 1000) / (now - lastNotify)
                    } else 0L
                    lastNotify = now
                    lastBytes = bytes
                    downloadDao.updateProgress(downloadId, "RUNNING", bytes)
                    val pct = if (total > 0) (done * 100) / total else 0
                    val speedText = if (speedBps > 0) " · ${formatBytes(speedBps)}/s" else ""
                    notificationManager?.notify(
                        DownloadNotificationIds.forDownload(downloadId),
                        buildProgressNotification(
                            downloadId = downloadId,
                            text = "$pct% — $done/$total segments · ${formatBytes(bytes)}$speedText",
                            progress = pct,
                            indeterminate = total <= 0,
                            title = entity.fileName
                        )
                    )
                }
            }
        )

        if (result.success) {
            completeDownload(downloadId, entity, result.outputFile ?: targetFile)
        } else if (job?.isActive != false) {
            markFailed(
                downloadId,
                entity.fileName,
                targetFile.length(),
                result.message ?: getString(R.string.download_notification_failed)
            )
        }
    }

    /**
     * Download a Facebook DASH video: fetch the separate video + audio tracks and mux
     * them into [targetFile] with MediaMuxer. Progress is byte-based across both tracks.
     */
    private suspend fun handleMuxDownload(
        downloadId: Long,
        entity: DownloadEntity,
        targetFile: File,
        headers: Map<String, String>
    ) {
        val job = coroutineContext[Job]
        var lastNotify = 0L
        notifyDownloadProgress(downloadId, entity.fileName, 0, 0)

        val result = DashMuxDownloader(okHttpClient, headers).download(
            videoUrl = entity.url,
            audioUrl = entity.audioUrl,
            outputFile = targetFile,
            isActive = { job?.isActive != false },
            onProgress = { bytes, total ->
                val now = System.currentTimeMillis()
                if (now - lastNotify > 500 || (total > 0 && bytes >= total)) {
                    lastNotify = now
                    downloadDao.updateProgress(downloadId, "RUNNING", bytes)
                    if (total > 0) downloadDao.updateSize(downloadId, total)
                    notifyDownloadProgress(downloadId, entity.fileName, bytes, total)
                }
            }
        )

        if (result.success) {
            completeDownload(downloadId, entity, result.outputFile ?: targetFile)
        } else if (job?.isActive != false) {
            markFailed(
                downloadId,
                entity.fileName,
                targetFile.length(),
                result.message ?: getString(R.string.download_notification_failed)
            )
        }
    }

    /**
     * Tier 3: finalize an MSE capture. Bytes already streamed to the loopback server as the
     * page played; here we wait briefly for the page to signal completion, then remux the
     * captured fragmented-MP4/WebM file(s) into a standard MP4 or WebM (per codec).
     */
    private suspend fun handleCaptureDownload(
        downloadId: Long,
        entity: DownloadEntity,
        targetFile: File
    ) {
        val job = coroutineContext[Job]
        val captureId = entity.url.removePrefix(MSE_CAPTURE_SCHEME)
        notifyDownloadProgress(downloadId, entity.fileName, 0, 0)

        val deadline = System.currentTimeMillis() + CAPTURE_WAIT_MS
        while (!mediaCaptureServer.isComplete(captureId) && System.currentTimeMillis() < deadline) {
            if (job?.isActive == false) return
            kotlinx.coroutines.delay(500)
            val bytes = captureBytes(captureId)
            downloadDao.updateProgress(downloadId, "RUNNING", bytes)
            notifyDownloadProgress(downloadId, entity.fileName, bytes, 0)
        }

        val muxed = mediaCaptureServer.fileFor(captureId, "m")
        val video = mediaCaptureServer.fileFor(captureId, "v")
        val audio = mediaCaptureServer.fileFor(captureId, "a")
        val primary = if (muxed.exists() && muxed.length() > 0) muxed else video

        if (!primary.exists() || primary.length() == 0L) {
            markFailed(downloadId, entity.fileName, 0, "Nothing captured — play the video first")
            return
        }

        val result = MediaRemuxer().remux(
            inputs = listOfNotNull(primary, audio.takeIf { it.exists() && it.length() > 0 }),
            requestedOutput = targetFile
        )
        if (result.success) {
            completeDownload(downloadId, entity, result.outputFile ?: targetFile)
            mediaCaptureServer.discard(captureId)
        } else if (job?.isActive != false) {
            markFailed(
                downloadId,
                entity.fileName,
                0,
                result.message ?: getString(R.string.download_notification_failed)
            )
        }
    }

    private fun captureBytes(captureId: String): Long =
        listOf("m", "v", "a").sumOf { mediaCaptureServer.fileFor(captureId, it).length() }

    /** Move a blob captured in-page (streamed to the loopback server) into Downloads as-is. */
    private suspend fun handleBlobCapture(
        downloadId: Long,
        entity: DownloadEntity,
        targetFile: File
    ) {
        val job = coroutineContext[Job]
        val captureId = entity.url.removePrefix(BLOB_CAPTURE_SCHEME)
        notifyDownloadProgress(downloadId, entity.fileName, 0, 0)
        val deadline = System.currentTimeMillis() + CAPTURE_WAIT_MS
        while (!mediaCaptureServer.isComplete(captureId) && System.currentTimeMillis() < deadline) {
            if (job?.isActive == false) return
            kotlinx.coroutines.delay(300)
            val bytes = mediaCaptureServer.fileFor(captureId, "f").length()
            downloadDao.updateProgress(downloadId, "RUNNING", bytes)
            notifyDownloadProgress(downloadId, entity.fileName, bytes, 0)
        }
        val src = mediaCaptureServer.fileFor(captureId, "f")
        if (!src.exists() || src.length() == 0L) {
            markFailed(downloadId, entity.fileName, 0, "Blob download failed")
            return
        }
        src.copyTo(targetFile, overwrite = true)
        completeDownload(downloadId, entity, targetFile)
        mediaCaptureServer.discard(captureId)
    }

    /** Decode a data: URI's embedded bytes straight to disk. */
    private suspend fun handleDataDownload(
        downloadId: Long,
        entity: DownloadEntity,
        targetFile: File
    ) {
        try {
            val comma = entity.url.indexOf(',')
            require(comma >= 0) { "Malformed data URL" }
            val meta = entity.url.substring("data:".length, comma)
            val payload = entity.url.substring(comma + 1)
            val bytes = if (meta.contains("base64", ignoreCase = true)) {
                android.util.Base64.decode(payload, android.util.Base64.DEFAULT)
            } else {
                java.net.URLDecoder.decode(payload, "UTF-8").toByteArray(Charsets.ISO_8859_1)
            }
            targetFile.outputStream().use { it.write(bytes) }
            completeDownload(downloadId, entity, targetFile)
        } catch (e: Exception) {
            markFailed(
                downloadId,
                entity.fileName,
                targetFile.takeIf(File::exists)?.length() ?: 0L,
                e.message ?: "Data URL decode failed"
            )
        }
    }

    /**
     * Free slot handler: if there's room and a queued (PENDING) download waiting,
     * start it; otherwise stop the service when fully idle.
     */
    private suspend fun startNextPendingOrStop() {
        if (isStoppingForTimeout) return
        if (activeJobs.size < MAX_CONCURRENT) {
            val next = downloadDao.getNextPending(MAX_CONCURRENT)
                .firstOrNull { !activeJobs.containsKey(it.id) }
            if (next != null) {
                startDownload(next.id)
                return
            }
        }
        stopSelfIfIdle()
    }

    private fun pauseDownload(downloadId: Long) {
        pendingControlActions.add(downloadId)
        val job = activeJobs.remove(downloadId)
        scope.launch {
            try {
                // Wait for the loop before setting PAUSED so an in-flight RUNNING update cannot
                // overwrite the final state or re-post a progress notification.
                job?.cancelAndJoin()
                if (downloadDao.markPausedIfActive(downloadId) > 0) {
                    downloadDao.getById(downloadId)?.let(::showPausedNotification)
                }
                refreshSummaryNotification()
            } finally {
                pendingControlActions.remove(downloadId)
                startNextPendingOrStop()
            }
        }
    }

    private fun resumeDownload(downloadId: Long) {
        // Works for both a paused download and a failed retry: clear the error, queue it as
        // PENDING (so it isn't lost when all slots are busy), then start if there's a free slot.
        // startDownload resumes from the bytes already on disk via an HTTP Range request.
        scope.launch {
            downloadDao.updateStatus(downloadId, "PENDING")
            ensureForeground()
            startDownload(downloadId)
        }
    }

    private fun cancelDownload(downloadId: Long) {
        pendingControlActions.add(downloadId)
        val job = activeJobs.remove(downloadId)
        scope.launch {
            try {
                // Join first so the loop has released the file handle before we delete it.
                job?.cancelAndJoin()
                notificationManager?.cancel(DownloadNotificationIds.forDownload(downloadId))
                val entity = downloadDao.getById(downloadId)
                entity?.let { e ->
                    DownloadStorage.delete(this@DownloadForegroundService, e.path)
                }
                downloadDao.deleteById(downloadId)
                refreshSummaryNotification()
            } finally {
                pendingControlActions.remove(downloadId)
                startNextPendingOrStop()
            }
        }
    }

    private fun stopSelfIfIdle() {
        if (activeJobs.isEmpty() && pendingControlActions.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun notifyDownloadProgress(
        downloadId: Long,
        fileName: String,
        downloaded: Long,
        total: Long,
        speedBps: Long = 0
    ) {
        val progress = if (total > 0) ((downloaded * 100) / total).toInt() else 0
        val sizeText = "${formatBytes(downloaded)} / ${formatBytes(total)}"
        val speedText = if (speedBps > 0) " · ${formatBytes(speedBps)}/s" else ""
        val etaText = if (speedBps > 0 && total > downloaded) {
            " · ${formatEta((total - downloaded) / speedBps)}"
        } else ""
        val notification = buildProgressNotification(
            downloadId = downloadId,
            text = "$progress% — $sizeText$speedText$etaText",
            progress = progress,
            indeterminate = total <= 0,
            title = fileName
        )
        notificationManager?.notify(DownloadNotificationIds.forDownload(downloadId), notification)
        refreshSummaryNotification()
    }

    private fun ensureForeground() {
        startForeground(
            DownloadNotificationIds.SUMMARY,
            buildSummaryNotification(activeJobs.size.coerceAtLeast(1))
        )
    }

    private fun refreshSummaryNotification() {
        if (activeJobs.isEmpty()) return
        notificationManager?.notify(
            DownloadNotificationIds.SUMMARY,
            buildSummaryNotification(activeJobs.size)
        )
    }

    private fun formatEta(seconds: Long): String {
        return when {
            seconds >= 3600 -> "${seconds / 3600}h ${(seconds % 3600) / 60}m left"
            seconds >= 60 -> "${seconds / 60}m ${seconds % 60}s left"
            else -> "${seconds}s left"
        }
    }

    /**
     * Show a non-ongoing notification when download completes.
     * Clicking opens the Downloads tab in the app.
     */
    private fun showCompletedNotification(downloadId: Long, fileName: String) {
        ToastHelper.show(this, getString(R.string.download_toast_completed))
        notificationManager?.cancel(DownloadNotificationIds.forDownload(downloadId))
        val notification = NotificationCompat.Builder(this, RESULT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download_arrow)
            .setContentTitle(getString(R.string.download_toast_completed))
            .setContentText(fileName)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(
                openDownloadsPendingIntent(DownloadNotificationIds.forCompleted(downloadId))
            )
            .build()

        notificationManager?.notify(
            DownloadNotificationIds.forCompleted(downloadId),
            notification
        )
    }

    private fun showPausedNotification(entity: DownloadEntity) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download_arrow)
            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .setContentTitle(entity.fileName)
            .setContentText(getString(R.string.download_notification_paused))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(
                openDownloadsPendingIntent(DownloadNotificationIds.forDownload(entity.id))
            )
            .addAction(
                R.drawable.ic_resume,
                getString(R.string.progress_resume_label),
                servicePendingIntent(entity.id, ACTION_RESUME, 0x33)
            )
            .addAction(
                R.drawable.ic_close_x,
                getString(R.string.progress_cancel_label),
                servicePendingIntent(entity.id, ACTION_CANCEL, 0x22)
            )

        if (entity.sizeBytes > 0L) {
            val progress = ((entity.downloadedBytes * 100L) / entity.sizeBytes)
                .toInt()
                .coerceIn(0, 100)
            builder.setProgress(100, progress, false)
        }
        notificationManager?.notify(
            DownloadNotificationIds.forDownload(entity.id),
            builder.build()
        )
    }

    private fun showFailedNotification(downloadId: Long, fileName: String) {
        ToastHelper.show(this, getString(R.string.download_toast_failed))
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download_arrow)
            .setContentTitle(fileName)
            .setContentText(getString(R.string.download_notification_failed))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openDownloadsPendingIntent(DownloadNotificationIds.forDownload(downloadId)))
            .addAction(
                R.drawable.ic_resume,
                getString(R.string.progress_resume_label),
                servicePendingIntent(downloadId, ACTION_RESUME, 0x33)
            )
            .setGroup(NOTIFICATION_GROUP)
            .build()
        notificationManager?.notify(DownloadNotificationIds.forDownload(downloadId), notification)
    }

    private suspend fun markFailed(
        downloadId: Long,
        fileName: String,
        downloadedBytes: Long,
        message: String
    ) {
        debugLog("FAILED id=$downloadId file=$fileName: $message")
        downloadDao.updateFailure(downloadId, downloadedBytes, message)
        showFailedNotification(downloadId, fileName)
    }

    private suspend fun completeDownload(
        downloadId: Long,
        entity: DownloadEntity,
        outputFile: File
    ) {
        val published = DownloadStorage.publish(
            context = this,
            source = outputFile,
            displayName = outputFile.name.ifBlank { entity.fileName },
            mimeType = mimeForFile(outputFile)
        )
        try {
            downloadDao.markCompleted(
                id = downloadId,
                fileName = published.displayName,
                path = published.locator,
                sizeBytes = published.sizeBytes,
                completedAt = System.currentTimeMillis()
            )
        } catch (error: Throwable) {
            if (published.locator != outputFile.absolutePath) {
                DownloadStorage.delete(this, published.locator)
            }
            throw error
        }
        if (published.locator != outputFile.absolutePath) outputFile.delete()
        showCompletedNotification(downloadId, published.displayName)
    }

    private fun buildSummaryNotification(activeCount: Int): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download_arrow)
            .setContentTitle(getString(R.string.download_notification_title))
            .setContentText(getString(R.string.download_notification_summary, activeCount))
            .setStyle(
                NotificationCompat.InboxStyle()
                    .addLine(getString(R.string.download_notification_summary, activeCount))
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openDownloadsPendingIntent(DownloadNotificationIds.SUMMARY))
            .setGroup(NOTIFICATION_GROUP)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .build()

    /** Best-effort mime for a saved file, derived from its extension. */
    private fun mimeForFile(file: File): String {
        val ext = file.extension.lowercase()
        return when (ext) {
            "mp4", "m4v" -> "video/mp4"
            "webm" -> "video/webm"
            "mov" -> "video/quicktime"
            "mkv" -> "video/x-matroska"
            "ts" -> "video/mp2t"
            else -> android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
                ?: "application/octet-stream"
        }
    }

    /**
     * Rename [file] so its extension matches the server's Content-Type when the current one is
     * missing or the generic ".bin" fallback (e.g. an extensionless image URL). A real extension
     * from the URL/Content-Disposition is left untouched.
     */
    private fun correctExtension(file: File, contentType: String?): File {
        if (contentType.isNullOrBlank()) return file
        val ext = android.webkit.MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(contentType.lowercase())
            ?.let { if (it == "jpeg") "jpg" else it }
            ?.takeIf { it.isNotBlank() } ?: return file
        val currentExt = file.extension.lowercase()
        if (currentExt == ext) return file
        if (currentExt.isNotBlank() && currentExt != "bin") return file // keep a real extension
        val renamed = File(file.parentFile, "${file.nameWithoutExtension}.$ext")
        return if (file.renameTo(renamed)) renamed else file
    }

    private fun buildProgressNotification(
        downloadId: Long,
        text: String,
        progress: Int,
        indeterminate: Boolean,
        title: String? = null
    ): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download_arrow)
            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .setContentTitle(title ?: getString(R.string.download_notification_title))
            .setContentText(text)
            .setProgress(100, progress, indeterminate)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openDownloadsPendingIntent(DownloadNotificationIds.forDownload(downloadId)))
            .addAction(
                R.drawable.ic_pause,
                getString(R.string.progress_pause_label),
                servicePendingIntent(downloadId, ACTION_PAUSE, 0x11)
            )
            .addAction(
                R.drawable.ic_close_x,
                getString(R.string.progress_cancel_label),
                servicePendingIntent(downloadId, ACTION_CANCEL, 0x22)
            )
            .setGroup(NOTIFICATION_GROUP)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .build()
    }

    private fun openDownloadsPendingIntent(requestCode: Int): PendingIntent {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to_downloads", true)
        }
        return PendingIntent.getActivity(
            this,
            requestCode,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun servicePendingIntent(downloadId: Long, action: String, salt: Int): PendingIntent {
        val intent = Intent(this, DownloadForegroundService::class.java).apply {
            this.action = action
            putExtra(EXTRA_ID, downloadId)
        }
        return PendingIntent.getService(
            this,
            DownloadNotificationIds.forDownload(downloadId) xor salt,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onTimeout(startId: Int, fgsType: Int) {
        isStoppingForTimeout = true
        val timedOutJobs = activeJobs.toMap()
        timedOutJobs.values.forEach(Job::cancel)
        activeJobs.clear()

        // Persist a resumable state before Android enforces the dataSync timeout shutdown.
        runBlocking(Dispatchers.IO) {
            timedOutJobs.forEach { (downloadId, job) ->
                job.cancelAndJoin()
                if (downloadDao.markPausedIfActive(downloadId) > 0) {
                    downloadDao.getById(downloadId)?.let(::showPausedNotification)
                }
            }
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        pendingControlActions.clear()
        scope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val progressChannel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.download_notification_channel_progress_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.download_notification_channel_progress_description)
            }
            val resultChannel = NotificationChannel(
                RESULT_CHANNEL_ID,
                getString(R.string.download_notification_channel_results_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.download_notification_channel_results_description)
            }
            notificationManager?.createNotificationChannels(
                listOf(progressChannel, resultChannel)
            )
        }
    }

    /** Debug-only trace of download routing/failures; stripped from release builds. */
    private fun debugLog(message: String) {
        if (com.asianmobile.privatebrower.BuildConfig.DEBUG) {
            android.util.Log.d("PBDownload", message)
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> String.format("%.1f GB", bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    companion object {
        const val MAX_CONCURRENT = 3
        private const val BLOB_CAPTURE_SCHEME = "blob-capture://"
        /** How long to wait for a page to finish feeding its MediaSource before muxing. */
        private const val CAPTURE_WAIT_MS = 15_000L
        const val CHANNEL_ID = "downloads"
        const val RESULT_CHANNEL_ID = "download_results"
        const val NOTIFICATION_GROUP = "private_browser_downloads"
        private const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        const val EXTRA_ID = "extra_download_id"

        const val ACTION_START = "action_start"
        const val ACTION_PAUSE = "action_pause"
        const val ACTION_RESUME = "action_resume"
        const val ACTION_CANCEL = "action_cancel"
        const val ACTION_STOP_ALL = "action_stop_all"

        fun start(context: Context, downloadId: Long) {
            DownloadNotificationPermissionRequests.onDownloadStarted()
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ID, downloadId)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun pause(context: Context, downloadId: Long) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_PAUSE
                putExtra(EXTRA_ID, downloadId)
            }
            context.startService(intent)
        }

        fun resume(context: Context, downloadId: Long) {
            DownloadNotificationPermissionRequests.onDownloadStarted()
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_RESUME
                putExtra(EXTRA_ID, downloadId)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun cancel(context: Context, downloadId: Long) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_ID, downloadId)
            }
            context.startService(intent)
        }
    }
}
