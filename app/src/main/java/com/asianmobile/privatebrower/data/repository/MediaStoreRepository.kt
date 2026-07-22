package com.asianmobile.privatebrower.data.repository

import android.net.Uri
import android.content.IntentSender

interface MediaStoreRepository {
    suspend fun queryMediaSizes(): MediaSizes
    suspend fun queryImages(): List<MediaItem>
    suspend fun queryVideos(): List<MediaItem>
    suspend fun queryAudio(): List<MediaItem>
    suspend fun queryFiles(): List<MediaItem>
    suspend fun delete(item: MediaItem): MediaDeleteResult
}

data class MediaSizes(
    val imageBytes: Long = 0L,
    val videoBytes: Long = 0L,
    val audioBytes: Long = 0L,
    val fileBytes: Long = 0L
)

data class MediaItem(
    val id: Long,
    val name: String,
    val path: String,
    val uri: Uri?,
    val mimeType: String,
    val sizeBytes: Long,
    val dateModified: Long,
    val duration: Long = 0L,
    val artist: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val relativePath: String = "",
    val source: MediaSource = MediaSource.MEDIA_STORE,
    val dateAdded: Long = 0L
) {
    val newestTimestamp: Long
        get() = dateAdded.takeIf { it > 0L } ?: dateModified
}

enum class MediaSource {
    MEDIA_STORE,
    APP_DOWNLOAD
}

sealed interface MediaDeleteResult {
    data object Success : MediaDeleteResult
    data object Failed : MediaDeleteResult
    data class RequiresPermission(val intentSender: IntentSender) : MediaDeleteResult
}
