package com.asianmobile.privatebrower.ui.home.tabstab

data class TabSelectionUiState(
    val isIncognito: Boolean = false,
    val tabs: List<TabUi> = emptyList(),
    val selectedTabIds: Set<Long> = emptySet(),
    val showMoreMenu: Boolean = false,
    val isProcessing: Boolean = false
) {
    val selectedCount: Int
        get() = tabs.count { it.id in selectedTabIds }

    val hasSelection: Boolean
        get() = selectedCount > 0

    val areAllTabsSelected: Boolean
        get() = tabs.isNotEmpty() && tabs.all { it.id in selectedTabIds }
}

sealed interface TabSelectionEvent {
    data class TabsClosed(
        val count: Int,
        val exitSelection: Boolean
    ) : TabSelectionEvent

    data class BookmarksAdded(val count: Int) : TabSelectionEvent

    data object BookmarkFailed : TabSelectionEvent

    data class ShareRequested(val tabs: List<TabUi>) : TabSelectionEvent
}
