package com.asianmobile.privatebrower.ui.home.tabstab

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.privatebrower.data.browser.TabManager
import com.asianmobile.privatebrower.data.database.entity.BookmarkEntity
import com.asianmobile.privatebrower.data.repository.BookmarkRepository
import com.asianmobile.privatebrower.ui.browser.faviconUrlFor
import com.asianmobile.privatebrower.ui.browser.isBookmarkableUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TabSelectionViewModel @Inject constructor(
    private val tabManager: TabManager,
    private val bookmarkRepository: BookmarkRepository,
    savedStateHandle: SavedStateHandle,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val isIncognito = savedStateHandle.get<Boolean>(KEY_INCOGNITO) ?: false
    private val selectedTabIds = MutableStateFlow<Set<Long>>(emptySet())
    private val showMoreMenu = MutableStateFlow(false)
    private val isProcessing = MutableStateFlow(false)
    private val eventChannel = Channel<TabSelectionEvent>(Channel.BUFFERED)

    val events = eventChannel.receiveAsFlow()

    private val mappedTabs = combine(
        tabManager.allSessions,
        tabManager.activeTabId,
        tabManager.tabPreviews
    ) { sessions, activeId, previews ->
        sessions
            .filter { it.isIncognito == isIncognito }
            .map {
                it.toTabUi(context, activeId, previews[it.id])
            }
    }

    val uiState: StateFlow<TabSelectionUiState> = combine(
        mappedTabs,
        selectedTabIds,
        showMoreMenu,
        isProcessing
    ) { tabs, selectedIds, menuVisible, processing ->
        val currentIds = tabs.mapTo(mutableSetOf()) { it.id }

        TabSelectionUiState(
            isIncognito = isIncognito,
            tabs = tabs,
            selectedTabIds = selectedIds.intersect(currentIds),
            showMoreMenu = menuVisible,
            isProcessing = processing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TabSelectionUiState(isIncognito = isIncognito)
    )

    fun onToggleMoreMenu() {
        if (isProcessing.value) return
        showMoreMenu.value = !showMoreMenu.value
    }

    fun onDismissMoreMenu() {
        showMoreMenu.value = false
    }

    fun onToggleTabSelection(tabId: Long) {
        if (isProcessing.value || uiState.value.tabs.none { it.id == tabId }) return

        selectedTabIds.value = selectedTabIds.value.let { current ->
            if (tabId in current) current - tabId else current + tabId
        }
    }

    fun onToggleSelectAll() {
        if (isProcessing.value) return

        val state = uiState.value
        selectedTabIds.value = if (state.areAllTabsSelected) {
            emptySet()
        } else {
            state.tabs.mapTo(mutableSetOf()) { it.id }
        }
        showMoreMenu.value = false
    }

    fun onCloseSelectedTabs() {
        val state = uiState.value
        val selectedTabs = state.tabs.filter { it.id in state.selectedTabIds }
        if (selectedTabs.isEmpty() || isProcessing.value) return

        showMoreMenu.value = false
        isProcessing.value = true
        viewModelScope.launch {
            try {
                selectedTabs.forEach { tabManager.closeTab(it.id) }
                selectedTabIds.value = emptySet()
                eventChannel.send(
                    TabSelectionEvent.TabsClosed(
                        count = selectedTabs.size,
                        exitSelection = selectedTabs.size == state.tabs.size
                    )
                )
            } finally {
                isProcessing.value = false
            }
        }
    }

    fun onBookmarkSelectedTabs() {
        val state = uiState.value
        val selectedTabs = state.tabs.filter { it.id in state.selectedTabIds }
        if (selectedTabs.isEmpty() || isProcessing.value) return

        showMoreMenu.value = false
        isProcessing.value = true
        viewModelScope.launch {
            var insertedCount = 0
            var hasFailure = false

            try {
                selectedTabs.forEach { tab ->
                    if (!isBookmarkableUrl(tab.url)) return@forEach

                    try {
                        if (bookmarkRepository.findByUrl(tab.url) == null) {
                            val insertedId = bookmarkRepository.insert(
                                BookmarkEntity(
                                    title = tab.title.ifBlank { tab.url },
                                    url = tab.url,
                                    faviconUrl = faviconUrlFor(tab.url)
                                )
                            )
                            if (insertedId >= 0L) insertedCount++ else hasFailure = true
                        }
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        hasFailure = true
                    }
                }

                if (hasFailure && insertedCount == 0) {
                    eventChannel.send(TabSelectionEvent.BookmarkFailed)
                } else {
                    eventChannel.send(TabSelectionEvent.BookmarksAdded(insertedCount))
                }
            } finally {
                isProcessing.value = false
            }
        }
    }

    fun onShareSelectedTabs() {
        if (isProcessing.value) return

        val state = uiState.value
        val selectedTabs = state.tabs.filter { it.id in state.selectedTabIds }
        if (selectedTabs.isEmpty()) return

        showMoreMenu.value = false
        viewModelScope.launch {
            eventChannel.send(TabSelectionEvent.ShareRequested(selectedTabs))
        }
    }

    private companion object {
        const val KEY_INCOGNITO = "incognito"
    }
}
