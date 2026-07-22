package com.asianmobile.privatebrower.service

/** Stable IDs prevent concurrent downloads from overwriting each other's notifications. */
internal object DownloadNotificationIds {
    const val SUMMARY = 1001
    private const val DOWNLOAD_BASE = 10_000
    private const val COMPLETED_BASE = 1_000_010_000
    private const val DOWNLOAD_RANGE = 1_000_000_000

    fun forDownload(downloadId: Long): Int {
        return DOWNLOAD_BASE + foldedId(downloadId)
    }

    fun forCompleted(downloadId: Long): Int {
        return COMPLETED_BASE + foldedId(downloadId)
    }

    private fun foldedId(downloadId: Long): Int {
        val folded = downloadId xor (downloadId ushr 32)
        val positive = (folded and Long.MAX_VALUE) % DOWNLOAD_RANGE
        return positive.toInt()
    }
}
