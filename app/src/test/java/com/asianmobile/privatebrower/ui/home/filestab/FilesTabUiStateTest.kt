package com.asianmobile.privatebrower.ui.home.filestab

import org.junit.Assert.assertEquals
import org.junit.Test

class FilesTabUiStateTest {

    @Test
    fun `storage segments separate categorized other and free bytes`() {
        val segments = StorageInfo(
            usedBytes = 80L,
            totalBytes = 100L,
            imageBytes = 10L,
            videoBytes = 20L,
            musicBytes = 5L,
            filesBytes = 15L
        ).toBarSegments()

        assertEquals(10L, segments.imageBytes)
        assertEquals(20L, segments.videoBytes)
        assertEquals(5L, segments.musicBytes)
        assertEquals(15L, segments.filesBytes)
        assertEquals(30L, segments.otherBytes)
        assertEquals(20L, segments.freeBytes)
    }

    @Test
    fun `category segments are normalized when metadata exceeds used storage`() {
        val segments = StorageInfo(
            usedBytes = 80L,
            totalBytes = 100L,
            imageBytes = 50L,
            videoBytes = 50L
        ).toBarSegments()

        assertEquals(40L, segments.imageBytes)
        assertEquals(40L, segments.videoBytes)
        assertEquals(0L, segments.otherBytes)
        assertEquals(20L, segments.freeBytes)
    }

    @Test
    fun `invalid capacity values are clamped`() {
        val segments = StorageInfo(
            usedBytes = 120L,
            totalBytes = 100L,
            imageBytes = -10L
        ).toBarSegments()

        assertEquals(0L, segments.imageBytes)
        assertEquals(100L, segments.otherBytes)
        assertEquals(0L, segments.freeBytes)
    }
}
