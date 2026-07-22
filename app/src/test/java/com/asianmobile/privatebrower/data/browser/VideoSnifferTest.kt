package com.asianmobile.privatebrower.data.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoSnifferTest {

    @Test
    fun `metadata enriches an intercepted video without duplicating it`() {
        val intercepted = DetectedVideo(
            url = "https://cdn.example.com/movie.mp4",
            mimeType = "video/mp4",
            refererUrl = "https://example.com/watch",
            headers = mapOf("Referer" to "https://example.com/watch")
        )
        val metadata = intercepted.copy(
            headers = emptyMap(),
            customName = "Movie title",
            thumbnailUrl = "https://example.com/poster.jpg"
        )

        val merged = listOf(intercepted).mergeVideo(metadata)

        assertEquals(1, merged.size)
        assertEquals("Movie title", merged.single().customName)
        assertEquals("https://example.com/poster.jpg", merged.single().thumbnailUrl)
        assertEquals("https://example.com/watch", merged.single().headers["Referer"])
    }

    @Test
    fun `existing stream-specific metadata wins over a later page fallback`() {
        val existing = DetectedVideo(
            url = "https://cdn.example.com/video",
            mimeType = "video/mp4",
            refererUrl = null,
            headers = emptyMap(),
            customName = "video_123",
            thumbnailUrl = "https://cdn.example.com/video-poster.jpg"
        )
        val fallback = existing.copy(
            customName = "Page title",
            thumbnailUrl = "https://example.com/og-image.jpg"
        )

        val merged = listOf(existing).mergeVideo(fallback).single()

        assertEquals("video_123", merged.customName)
        assertEquals("https://cdn.example.com/video-poster.jpg", merged.thumbnailUrl)
        assertNull(merged.audioUrl)
    }
}
