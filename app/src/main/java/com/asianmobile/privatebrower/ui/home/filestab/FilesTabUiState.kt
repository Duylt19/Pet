package com.asianmobile.privatebrower.ui.home.filestab

import com.asianmobile.privatebrower.ui.medialist.MediaAccessState

enum class MediaCategory { IMAGES, VIDEO, MUSIC, FILES }

data class StorageInfo(
    val usedBytes: Long,
    val totalBytes: Long,
    val imageBytes: Long = 0L,
    val videoBytes: Long = 0L,
    val musicBytes: Long = 0L,
    val filesBytes: Long = 0L
)

internal data class StorageBarSegments(
    val imageBytes: Long,
    val videoBytes: Long,
    val musicBytes: Long,
    val filesBytes: Long,
    val otherBytes: Long,
    val freeBytes: Long
)

internal fun StorageInfo.toBarSegments(): StorageBarSegments {
    val total = totalBytes.coerceAtLeast(0L)
    val used = usedBytes.coerceIn(0L, total)
    val categories = listOf(
        imageBytes.coerceAtLeast(0L),
        videoBytes.coerceAtLeast(0L),
        musicBytes.coerceAtLeast(0L),
        filesBytes.coerceAtLeast(0L)
    )
    val categoryTotal = categories.sum()
    val scale = if (categoryTotal > used && categoryTotal > 0L) {
        used.toDouble() / categoryTotal
    } else {
        1.0
    }
    val normalizedCategories = categories.map { (it * scale).toLong() }
    val normalizedCategoryTotal = normalizedCategories.sum()

    return StorageBarSegments(
        imageBytes = normalizedCategories[0],
        videoBytes = normalizedCategories[1],
        musicBytes = normalizedCategories[2],
        filesBytes = normalizedCategories[3],
        otherBytes = (used - normalizedCategoryTotal).coerceAtLeast(0L),
        freeBytes = (total - used).coerceAtLeast(0L)
    )
}

data class FilesTabUiState(
    val storageInfo: StorageInfo? = null,
    val isLoading: Boolean = false,
    val accessState: MediaAccessState = MediaAccessState.CHECKING,
    val permissionRequestCount: Int = 0
)
