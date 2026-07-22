package com.asianmobile.privatebrower.ui.medialist

import com.asianmobile.privatebrower.data.repository.MediaItem
import com.asianmobile.privatebrower.ui.home.filestab.MediaCategory

enum class MediaSortOrder {
    NEWEST,
    OLDEST,
    NAME,
    SIZE
}

enum class MediaViewMode {
    GRID,
    LIST
}

enum class MediaAccessState {
    CHECKING,
    GRANTED,
    DENIED
}

data class MediaListUiState(
    val items: List<MediaItem> = emptyList(),
    val mediaType: MediaCategory = MediaCategory.IMAGES,
    val isLoading: Boolean = false,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val sortOrder: MediaSortOrder = MediaSortOrder.NEWEST,
    val viewMode: MediaViewMode = MediaViewMode.GRID,
    val accessState: MediaAccessState = MediaAccessState.CHECKING,
    val permissionRequestCount: Int = 0,
    val hasError: Boolean = false,
    val audioPlayback: AudioPlaybackUiState = AudioPlaybackUiState()
) {
    val visibleItems: List<MediaItem>
        get() {
            val query = searchQuery.trim()
            val filtered = if (query.isBlank()) {
                items
            } else {
                items.filter { item ->
                    item.name.contains(query, ignoreCase = true) ||
                        item.artist.contains(query, ignoreCase = true) ||
                        item.mimeType.contains(query, ignoreCase = true)
                }
            }

            return when (sortOrder) {
                MediaSortOrder.NEWEST -> filtered.sortedWith(
                    compareByDescending<MediaItem>(MediaItem::newestTimestamp)
                        .thenByDescending(MediaItem::dateModified)
                        .thenByDescending(MediaItem::id)
                )
                MediaSortOrder.OLDEST -> filtered.sortedWith(
                    compareBy<MediaItem>(MediaItem::newestTimestamp)
                        .thenBy(MediaItem::dateModified)
                        .thenBy(MediaItem::id)
                )
                MediaSortOrder.NAME -> filtered.sortedWith(
                    compareBy(String.CASE_INSENSITIVE_ORDER, MediaItem::name)
                )
                MediaSortOrder.SIZE -> filtered.sortedByDescending(MediaItem::sizeBytes)
            }
        }

    val supportsViewToggle: Boolean
        get() = mediaType == MediaCategory.IMAGES || mediaType == MediaCategory.VIDEO

    val supportsSelection: Boolean
        get() = mediaType == MediaCategory.IMAGES ||
            mediaType == MediaCategory.VIDEO ||
            mediaType == MediaCategory.MUSIC ||
            mediaType == MediaCategory.FILES
}

data class AudioPlaybackUiState(
    val activeItemId: Long? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)
