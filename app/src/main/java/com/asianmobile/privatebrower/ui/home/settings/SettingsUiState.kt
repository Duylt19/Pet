package com.asianmobile.privatebrower.ui.home.settings

import com.asianmobile.privatebrower.data.model.SearchEngine
import com.asianmobile.privatebrower.data.usecase.ClearBrowsingDataOptions

data class SettingsUiState(
    val currentSearchEngine: SearchEngine = SearchEngine.GOOGLE,
    val isDefaultBrowser: Boolean = false,
    val showSearchEngineSheet: Boolean = false,
    val showClearHistorySheet: Boolean = false,
    val clearHistoryOptions: ClearBrowsingDataOptions = ClearBrowsingDataOptions(),
    val profileIsolationSupported: Boolean = true,
    val isClearingBrowsingData: Boolean = false,
    val versionName: String = ""
)
