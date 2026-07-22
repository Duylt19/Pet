package com.asianmobile.privatebrower.ui.home.tabstab

import android.graphics.Bitmap

enum class TabMode { NORMAL, INCOGNITO }

data class TabUi(
    val id: Long,
    val title: String,
    val url: String,
    val thumbnailPath: String?,
    val isActive: Boolean,
    val thumbnailTimestamp: Long = 0L,
    val thumbnailBitmap: Bitmap? = null
) {
    val hasThumbnail: Boolean
        get() = thumbnailBitmap != null || thumbnailPath != null
}

data class TabsTabUiState(
    val mode: TabMode = TabMode.NORMAL,
    val tabs: List<TabUi> = emptyList(),
    val activeTabId: Long? = null,
    val showCloseAllDialog: Boolean = false,
    val showModeDropdown: Boolean = false,
    // Search
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    // More menu
    val showMoreMenu: Boolean = false
) {
    /** Tabs shown in the current mode, filtered by title or URL while searching. */
    val displayTabs: List<TabUi>
        get() = if (isSearchActive && searchQuery.isNotBlank()) {
            val query = searchQuery.trim()
            tabs.filter { tab ->
                tab.title.contains(query, ignoreCase = true) ||
                    tab.url.contains(query, ignoreCase = true)
            }
        } else {
            tabs
        }
}
