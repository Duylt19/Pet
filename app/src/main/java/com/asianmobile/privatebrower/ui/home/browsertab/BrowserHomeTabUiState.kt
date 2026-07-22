package com.asianmobile.privatebrower.ui.home.browsertab

import com.asianmobile.privatebrower.data.model.QuickAccessShortcut
import com.asianmobile.privatebrower.data.model.QuickAccessShortcuts
import com.asianmobile.privatebrower.data.model.SearchEngine

data class BrowserHomeTabUiState(
    val searchQuery: String = "",
    val searchEngine: SearchEngine = SearchEngine.GOOGLE,
    val shortcuts: List<QuickAccessShortcut> = QuickAccessShortcuts.DEFAULTS,
    val isPremium: Boolean = false
)
