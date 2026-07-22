package com.asianmobile.privatebrower.ui.browser

import android.graphics.Bitmap
import com.asianmobile.privatebrower.data.browser.DetectedVideo
import com.asianmobile.privatebrower.data.database.entity.BookmarkEntity

data class BrowserUiState(
    val url: String = "https://www.google.com",
    val title: String = "",
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isIncognito: Boolean = false,
    val favicon: Bitmap? = null,
    val showMoreMenu: Boolean = false,
    val tabCount: Int = 1,
    val isBookmarked: Boolean = false,
    val canBookmark: Boolean = false,
    val detectedVideos: List<DetectedVideo> = emptyList(),
    val showVideoSheet: Boolean = false,
    val isDesktopMode: Boolean = false,
    val showFindInPage: Boolean = false,
    val findInPageQuery: String = "",
    val findInPageCurrentMatch: Int = 0,
    val findInPageTotalMatches: Int = 0
)

sealed interface BrowserUiEvent {
    data object BookmarkAdded : BrowserUiEvent
    data class BookmarkRemoved(val bookmark: BookmarkEntity) : BrowserUiEvent
    data object BookmarkRestored : BrowserUiEvent
    data object BookmarkUnavailable : BrowserUiEvent
    data object BookmarkOperationFailed : BrowserUiEvent
    data class TabLimitReached(val maxTabs: Int, val isIncognito: Boolean) : BrowserUiEvent
}
