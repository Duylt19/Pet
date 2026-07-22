package com.asianmobile.privatebrower.ui.home.progresstab

import com.asianmobile.privatebrower.data.database.entity.DownloadEntity

enum class DownloadTab { ALL, DOWNLOADING, COMPLETED }

data class ProgressTabUiState(
    val allItems: List<DownloadEntity> = emptyList(),
    /** In-flight downloads (RUNNING / PENDING / PAUSED) — these count as "Downloading". */
    val activeItems: List<DownloadEntity> = emptyList(),
    /** FAILED downloads, shown in their own section with Retry/Remove. */
    val failedItems: List<DownloadEntity> = emptyList(),
    val completedItems: List<DownloadEntity> = emptyList(),
    val isEmpty: Boolean = true,
    val selectedTab: DownloadTab = DownloadTab.ALL,
    /** Live download speed per download id, in bytes/second (0 when unknown/idle). */
    val speeds: Map<Long, Long> = emptyMap()
)
