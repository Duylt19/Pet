package com.asianmobile.privatebrower.ui.home.tabstab

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.privatebrower.data.browser.TabManager
import com.asianmobile.privatebrower.data.repository.SearchEngineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class TabsUiFlags(
    val showDialog: Boolean,
    val showDropdown: Boolean,
    val searchActive: Boolean,
    val searchQuery: String
)

private data class MappedTabs(
    val normal: List<TabUi>,
    val incognito: List<TabUi>,
    val activeTabId: Long?
)

@HiltViewModel
class TabsTabViewModel @Inject constructor(
    private val tabManager: TabManager,
    private val searchEngineRepository: SearchEngineRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _mode = MutableStateFlow(TabMode.NORMAL)
    private val _showCloseAllDialog = MutableStateFlow(false)
    private val _showModeDropdown = MutableStateFlow(false)

    // Search
    private val _isSearchActive = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")

    // More menu
    private val _showMoreMenu = MutableStateFlow(false)

    private val mappedTabs = combine(
        tabManager.allSessions,
        tabManager.activeTabId,
        tabManager.tabPreviews
    ) { sessions, activeId, previews ->
        val (incognito, normal) = sessions.partition { it.isIncognito }
        MappedTabs(
            normal = normal.map {
                it.toTabUi(context, activeId, previews[it.id])
            },
            incognito = incognito.map {
                it.toTabUi(context, activeId, previews[it.id])
            },
            activeTabId = activeId
        )
    }

    private val uiFlags = combine(
        _showCloseAllDialog,
        _showModeDropdown,
        _isSearchActive,
        _searchQuery
    ) { showDialog, showDropdown, searchActive, searchQuery ->
        TabsUiFlags(showDialog, showDropdown, searchActive, searchQuery)
    }

    val uiState: StateFlow<TabsTabUiState> = combine(
        mappedTabs,
        _mode,
        uiFlags,
        _showMoreMenu
    ) { mappedTabs, mode, flags, showMore ->
        val filtered = if (mode == TabMode.INCOGNITO) {
            mappedTabs.incognito
        } else {
            mappedTabs.normal
        }

        TabsTabUiState(
            mode = mode,
            tabs = filtered,
            activeTabId = mappedTabs.activeTabId,
            showCloseAllDialog = flags.showDialog,
            showModeDropdown = flags.showDropdown,
            isSearchActive = flags.searchActive,
            searchQuery = flags.searchQuery,
            showMoreMenu = showMore
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TabsTabUiState()
    )

    // region Mode Dropdown

    fun onToggleModeDropdown() {
        _showModeDropdown.value = !_showModeDropdown.value
    }

    fun onDismissModeDropdown() {
        _showModeDropdown.value = false
    }

    fun onModeChanged(mode: TabMode) {
        _showModeDropdown.value = false
        _mode.value = mode
        _showMoreMenu.value = false
    }

    // endregion

    // region Tab Actions

    fun onTabClicked(tabId: Long) {
        tabManager.switchTo(tabId)
    }

    fun onCloseTab(tabId: Long) {
        viewModelScope.launch {
            tabManager.closeTab(tabId)
        }
    }

    suspend fun onAddTab(): Boolean {
        val isIncognito = _mode.value == TabMode.INCOGNITO
        val homeUrl = searchEngineRepository.observeCurrent().first().homeUrl
        return tabManager.addTab(homeUrl, isIncognito) != null
    }

    fun onShowCloseAllDialog(show: Boolean) {
        _showCloseAllDialog.value = show
    }

    fun onCloseAllInMode() {
        val isIncognito = _mode.value == TabMode.INCOGNITO
        viewModelScope.launch {
            tabManager.closeAllInMode(isIncognito)
            _showCloseAllDialog.value = false
        }
    }

    // endregion

    // region Search

    fun onStartSearch() {
        _showModeDropdown.value = false
        _showMoreMenu.value = false
        _isSearchActive.value = true
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onDismissSearch() {
        _isSearchActive.value = false
        _searchQuery.value = ""
    }

    // endregion

    // region More Menu

    fun onToggleMoreMenu() {
        val show = !_showMoreMenu.value
        _showMoreMenu.value = show
        if (show) {
            _showModeDropdown.value = false
            _isSearchActive.value = false
            _searchQuery.value = ""
        }
    }

    fun onDismissMoreMenu() {
        _showMoreMenu.value = false
    }

    // endregion
}
