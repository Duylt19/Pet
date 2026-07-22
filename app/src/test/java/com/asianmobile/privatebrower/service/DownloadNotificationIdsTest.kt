package com.asianmobile.privatebrower.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadNotificationIdsTest {

    @Test
    fun `each concurrent download gets a stable positive notification id`() {
        val first = DownloadNotificationIds.forDownload(41L)
        val second = DownloadNotificationIds.forDownload(42L)

        assertTrue(first > 0)
        assertNotEquals(DownloadNotificationIds.SUMMARY, first)
        assertNotEquals(first, second)
        assertEquals(first, DownloadNotificationIds.forDownload(41L))
        assertNotEquals(first, DownloadNotificationIds.forCompleted(41L))
    }

    @Test
    fun `large room ids remain valid notification ids`() {
        val id = DownloadNotificationIds.forDownload(Long.MAX_VALUE)

        assertTrue(id > 0)
        assertNotEquals(DownloadNotificationIds.SUMMARY, id)

        val completedId = DownloadNotificationIds.forCompleted(Long.MAX_VALUE)
        assertTrue(completedId > 0)
        assertNotEquals(id, completedId)
        assertTrue(id < DownloadNotificationIds.forCompleted(0L))
    }
}
