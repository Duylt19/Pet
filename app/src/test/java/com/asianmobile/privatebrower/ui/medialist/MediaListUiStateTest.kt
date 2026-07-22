package com.asianmobile.privatebrower.ui.medialist

import com.asianmobile.privatebrower.data.repository.MediaItem
import com.asianmobile.privatebrower.ui.home.filestab.MediaCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaListUiStateTest {

    private val items = listOf(
        item(1, "Beach.jpg", "image/jpeg", 300, 30),
        item(2, "Concert.mp3", "audio/mpeg", 900, 10, artist = "Local Artist"),
        item(3, "Archive.zip", "application/zip", 600, 20)
    )

    @Test
    fun visibleItems_filtersByNameArtistAndMimeType() {
        assertEquals(
            listOf(items[0]),
            state(query = "beach").visibleItems
        )
        assertEquals(
            listOf(items[1]),
            state(query = "local artist").visibleItems
        )
        assertEquals(
            listOf(items[2]),
            state(query = "application/zip").visibleItems
        )
    }

    @Test
    fun visibleItems_trimsBlankQueryAndSortsNewestByDefault() {
        assertEquals(
            listOf(items[0], items[2], items[1]),
            state(query = "   ").visibleItems
        )
    }

    @Test
    fun visibleItems_sortsNewestByDateAddedAndFallsBackToDateModified() {
        val recentlyAdded = item(
            id = 4,
            name = "Recently added.jpg",
            mimeType = "image/jpeg",
            size = 100,
            modified = 10,
            added = 40
        )
        val recentlyModifiedOldFile = item(
            id = 5,
            name = "Old but modified.jpg",
            mimeType = "image/jpeg",
            size = 100,
            modified = 50,
            added = 20
        )
        val itemWithoutDateAdded = item(
            id = 6,
            name = "Legacy.jpg",
            mimeType = "image/jpeg",
            size = 100,
            modified = 30
        )

        assertEquals(
            listOf(recentlyAdded, itemWithoutDateAdded, recentlyModifiedOldFile),
            state(
                items = listOf(recentlyModifiedOldFile, itemWithoutDateAdded, recentlyAdded)
            ).visibleItems
        )
    }

    @Test
    fun visibleItems_supportsAllSortOrders() {
        assertEquals(
            listOf(items[1], items[2], items[0]),
            state(sort = MediaSortOrder.OLDEST).visibleItems
        )
        assertEquals(
            listOf(items[2], items[0], items[1]),
            state(sort = MediaSortOrder.NAME).visibleItems
        )
        assertEquals(
            listOf(items[1], items[2], items[0]),
            state(sort = MediaSortOrder.SIZE).visibleItems
        )
    }

    @Test
    fun supportsViewToggle_onlyForImagesAndVideo() {
        assertTrue(state(type = MediaCategory.IMAGES).supportsViewToggle)
        assertTrue(state(type = MediaCategory.VIDEO).supportsViewToggle)
        assertFalse(state(type = MediaCategory.MUSIC).supportsViewToggle)
        assertFalse(state(type = MediaCategory.FILES).supportsViewToggle)
    }

    @Test
    fun supportsSelection_forEveryMediaCategory() {
        assertTrue(state(type = MediaCategory.IMAGES).supportsSelection)
        assertTrue(state(type = MediaCategory.VIDEO).supportsSelection)
        assertTrue(state(type = MediaCategory.MUSIC).supportsSelection)
        assertTrue(state(type = MediaCategory.FILES).supportsSelection)
    }

    @Test
    fun audioPlayback_isInactiveByDefault() {
        val playback = state(type = MediaCategory.MUSIC).audioPlayback

        assertEquals(null, playback.activeItemId)
        assertFalse(playback.isPlaying)
        assertEquals(0L, playback.positionMs)
        assertEquals(0L, playback.durationMs)
    }

    private fun state(
        items: List<MediaItem> = this.items,
        query: String = "",
        sort: MediaSortOrder = MediaSortOrder.NEWEST,
        type: MediaCategory = MediaCategory.IMAGES
    ) = MediaListUiState(
        items = items,
        mediaType = type,
        searchQuery = query,
        sortOrder = sort,
        accessState = MediaAccessState.GRANTED
    )

    private fun item(
        id: Long,
        name: String,
        mimeType: String,
        size: Long,
        modified: Long,
        added: Long = 0L,
        artist: String = ""
    ) = MediaItem(
        id = id,
        name = name,
        path = "",
        uri = null,
        mimeType = mimeType,
        sizeBytes = size,
        dateModified = modified,
        dateAdded = added,
        artist = artist
    )
}
