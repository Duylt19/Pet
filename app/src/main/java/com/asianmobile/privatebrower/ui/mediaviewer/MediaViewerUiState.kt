package com.asianmobile.privatebrower.ui.mediaviewer

import android.net.Uri
import com.asianmobile.privatebrower.data.database.entity.DownloadEntity
import com.asianmobile.privatebrower.data.download.DownloadStorage
import com.asianmobile.privatebrower.data.repository.MediaItem

enum class MediaViewerSource {
    DOWNLOADS,
    MEDIA_LIBRARY
}

enum class MediaViewerKind {
    VIDEO,
    IMAGE,
    AUDIO,
    OTHER;

    companion object {
        fun from(mimeType: String, path: String): MediaViewerKind {
            val extension = path.substringAfterLast('.', "").lowercase()
            return when {
                mimeType.startsWith("image/", ignoreCase = true) ||
                    extension in IMAGE_EXTENSIONS -> IMAGE
                mimeType.startsWith("audio/", ignoreCase = true) ||
                    extension in AUDIO_EXTENSIONS -> AUDIO
                mimeType.startsWith("video/", ignoreCase = true) ||
                    extension in VIDEO_EXTENSIONS -> VIDEO
                else -> OTHER
            }
        }

        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif")
        private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "wav", "ogg", "flac", "opus")
        private val VIDEO_EXTENSIONS = setOf("mp4", "m4v", "mkv", "webm", "mov", "avi", "ts", "flv")
    }
}

data class MediaViewerRequest(
    val source: MediaViewerSource,
    val id: Long,
    val name: String,
    val path: String,
    val uri: String,
    val mimeType: String,
    val sizeBytes: Long,
    val modifiedAt: Long,
    val initialDurationMs: Long,
    val initialWidth: Int,
    val initialHeight: Int,
    val mediaSource: String,
    val thumbnailUrl: String
) {
    val kind: MediaViewerKind = MediaViewerKind.from(mimeType, path.ifBlank { name })
}

data class MediaViewerMetadata(
    val sizeBytes: Long = 0L,
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val displayType: String = ""
)

data class MediaViewerUiState(
    val request: MediaViewerRequest,
    val controlsVisible: Boolean = true,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = true,
    val isMuted: Boolean = false,
    val positionMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val durationMs: Long = request.initialDurationMs,
    val isFullscreen: Boolean = false,
    val isVideoCropped: Boolean = false,
    val hasPlaybackError: Boolean = false,
    val metadata: MediaViewerMetadata = MediaViewerMetadata(
        sizeBytes = request.sizeBytes,
        durationMs = request.initialDurationMs,
        width = request.initialWidth,
        height = request.initialHeight,
        displayType = request.name.substringAfterLast('.', "")
    ),
    val artwork: ByteArray? = null
)

internal fun shouldAutoHideMediaViewerControls(
    kind: MediaViewerKind,
    isPlaying: Boolean
): Boolean = when (kind) {
    MediaViewerKind.IMAGE -> true
    MediaViewerKind.VIDEO,
    MediaViewerKind.AUDIO -> isPlaying
    MediaViewerKind.OTHER -> false
}

sealed interface MediaViewerEvent {
    data object Removed : MediaViewerEvent
    data class DeletePermissionRequired(val intentSender: android.content.IntentSender) : MediaViewerEvent
    data object ActionFailed : MediaViewerEvent
}

object MediaViewerRoute {
    const val ROUTE = "media_viewer"
    const val RESULT_MEDIA_LIBRARY_CHANGED = "media_viewer_result_media_library_changed"
    const val PATTERN =
        "$ROUTE?source={source}&id={id}&name={name}&path={path}&mediaUri={mediaUri}" +
            "&mimeType={mimeType}&sizeBytes={sizeBytes}&modifiedAt={modifiedAt}" +
            "&durationMs={durationMs}&width={width}&height={height}" +
            "&mediaSource={mediaSource}&thumbnailUrl={thumbnailUrl}"

    fun fromDownload(item: DownloadEntity): String {
        val isContentUri = DownloadStorage.isContentLocator(item.path)
        return build(
            source = MediaViewerSource.DOWNLOADS,
            id = item.id,
            name = item.fileName,
            path = item.path.takeUnless { isContentUri }.orEmpty(),
            uri = item.path.takeIf { isContentUri }.orEmpty(),
            mimeType = item.mimeType,
            sizeBytes = item.sizeBytes.coerceAtLeast(item.downloadedBytes),
            modifiedAt = item.completedAt ?: item.createdAt,
            durationMs = 0L,
            width = 0,
            height = 0,
            mediaSource = "",
            thumbnailUrl = item.thumbnailUrl
        )
    }

    fun fromMedia(item: MediaItem): String = build(
        source = MediaViewerSource.MEDIA_LIBRARY,
        id = item.id,
        name = item.name,
        path = item.path,
        uri = item.uri?.toString().orEmpty(),
        mimeType = item.mimeType,
        sizeBytes = item.sizeBytes,
        modifiedAt = item.dateModified * 1000L,
        durationMs = item.duration,
        width = item.width,
        height = item.height,
        mediaSource = item.source.name,
        thumbnailUrl = ""
    )

    private fun build(
        source: MediaViewerSource,
        id: Long,
        name: String,
        path: String,
        uri: String,
        mimeType: String,
        sizeBytes: Long,
        modifiedAt: Long,
        durationMs: Long,
        width: Int,
        height: Int,
        mediaSource: String,
        thumbnailUrl: String
    ): String = "$ROUTE?source=${source.name}&id=$id" +
        "&name=${Uri.encode(name)}&path=${Uri.encode(path)}&mediaUri=${Uri.encode(uri)}" +
        "&mimeType=${Uri.encode(mimeType)}&sizeBytes=$sizeBytes&modifiedAt=$modifiedAt" +
        "&durationMs=$durationMs&width=$width&height=$height" +
        "&mediaSource=${Uri.encode(mediaSource)}&thumbnailUrl=${Uri.encode(thumbnailUrl)}"
}
