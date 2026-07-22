package com.asianmobile.privatebrower.ui.home.progresstab

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.privatebrower.data.database.dao.DownloadDao
import com.asianmobile.privatebrower.data.database.entity.DownloadEntity
import com.asianmobile.privatebrower.data.download.DownloadStorage
import com.asianmobile.privatebrower.service.DownloadForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProgressTabViewModel @Inject constructor(
    private val downloadDao: DownloadDao,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(DownloadTab.ALL)

    // Live download speed tracking: last observed (bytes, timestampMs) and the derived
    // bytes/sec per download id. Speed can't come from the DB row (it only stores byte
    // counts), so we derive it from successive progress emissions.
    private val speedSamples = HashMap<Long, Pair<Long, Long>>()
    private val speeds = HashMap<Long, Long>()

    val uiState: StateFlow<ProgressTabUiState> = combine(
        downloadDao.observeAll(),
        _selectedTab
    ) { all, tab ->
        val active = all.filter { it.status == "RUNNING" || it.status == "PENDING" || it.status == "PAUSED" }
        val failed = all.filter { it.status == "FAILED" }
        val completed = all.filter { it.status == "COMPLETED" }
        ProgressTabUiState(
            allItems = all,
            activeItems = active,
            failedItems = failed,
            completedItems = completed,
            isEmpty = all.isEmpty(),
            selectedTab = tab,
            speeds = computeSpeeds(active)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProgressTabUiState()
    )

    /**
     * Derive bytes/sec for each running download from the change in downloaded bytes since
     * the previous emission. Emissions triggered by unrelated changes (e.g. a tab switch)
     * carry no byte delta, so the last known speed is kept instead of collapsing to 0.
     */
    private fun computeSpeeds(active: List<DownloadEntity>): Map<Long, Long> {
        val now = System.currentTimeMillis()
        val liveIds = active.mapTo(HashSet()) { it.id }
        speedSamples.keys.retainAll(liveIds)
        speeds.keys.retainAll(liveIds)

        for (item in active) {
            if (item.status != "RUNNING") {
                speeds[item.id] = 0
                speedSamples[item.id] = item.downloadedBytes to now
                continue
            }
            val prev = speedSamples[item.id]
            if (prev == null) {
                speedSamples[item.id] = item.downloadedBytes to now
            } else {
                val (prevBytes, prevTime) = prev
                val deltaBytes = item.downloadedBytes - prevBytes
                val deltaMs = now - prevTime
                if (deltaBytes > 0 && deltaMs > 0) {
                    speeds[item.id] = deltaBytes * 1000 / deltaMs
                    speedSamples[item.id] = item.downloadedBytes to now
                }
            }
        }
        return HashMap(speeds)
    }

    fun onTabSelected(tab: DownloadTab) {
        _selectedTab.value = tab
    }

    fun onCancelDownload(id: Long) {
        DownloadForegroundService.cancel(context, id)
    }

    fun onPauseDownload(id: Long) {
        DownloadForegroundService.pause(context, id)
    }

    fun onResumeDownload(id: Long) {
        DownloadForegroundService.resume(context, id)
    }

    /** Failed downloads reuse the resume flow: it resumes a partial file or restarts. */
    fun onRetryDownload(id: Long) {
        DownloadForegroundService.resume(context, id)
    }

    /** Remove a download row from the list (keeps the file on disk). */
    fun onRemoveFromList(id: Long) {
        viewModelScope.launch {
            downloadDao.deleteById(id)
        }
    }

    fun onDeleteCompleted(id: Long) = onRemoveFromList(id)

    /** Remove the row AND delete the file from the device. */
    fun onDeleteFile(id: Long, path: String?) {
        viewModelScope.launch {
            path?.takeIf { it.isNotBlank() }?.let { locator ->
                DownloadStorage.delete(context, locator)
            }
            downloadDao.deleteById(id)
        }
    }

    /** Remove every finished (completed or failed) download row. */
    fun onClearFinished() {
        viewModelScope.launch {
            downloadDao.deleteByStatuses(listOf("COMPLETED", "FAILED"))
        }
    }

    fun onOpenFile(path: String, fileName: String, mimeType: String) {
        try {
            val uri = DownloadStorage.shareUri(context, path) ?: return
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeForDownload(path, fileName, mimeType))
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onShare(path: String, fileName: String, mimeType: String) {
        try {
            val uri = DownloadStorage.shareUri(context, path) ?: return
            val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = mimeForDownload(path, fileName, mimeType)
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                android.content.Intent.createChooser(send, null)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onCopyLink(url: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
            as? android.content.ClipboardManager ?: return
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("link", url))
    }

    /** Open the system Downloads UI so the user can browse saved files. */
    fun onShowInFiles() {
        try {
            context.startActivity(
                android.content.Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Best mime for opening/sharing: HLS rows keep the playlist mime ("…mpegurl") but the file
     * on disk is an MP4/.ts, so derive from the real extension, then the system map, then fall
     * back to the stored mime.
     */
    private fun mimeForDownload(path: String, fileName: String, fallback: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase().ifBlank {
            path.takeUnless(DownloadStorage::isContentLocator)
                ?.let { locator -> java.io.File(locator) }
                ?.extension
                ?.lowercase()
                .orEmpty()
        }
        videoMimeOverrides[ext]?.let { return it }
        android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)?.let { return it }
        if (DownloadStorage.isContentLocator(path)) {
            context.contentResolver.getType(android.net.Uri.parse(path))?.let { return it }
        }
        return fallback.ifBlank { "*/*" }
    }

    private companion object {
        private val videoMimeOverrides = mapOf(
            "mp4" to "video/mp4", "m4v" to "video/mp4", "webm" to "video/webm",
            "mov" to "video/quicktime", "mkv" to "video/x-matroska",
            "ts" to "video/mp2t", "avi" to "video/x-msvideo", "flv" to "video/x-flv",
        )
    }
}
