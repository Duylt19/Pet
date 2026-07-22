package com.asianmobile.privatebrower.ui.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.privatebrower.data.database.entity.HistoryEntity
import com.asianmobile.privatebrower.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val isSearchActive = MutableStateFlow(false)
    private val showClearAllDialog = MutableStateFlow(false)
    private val deletedItem = MutableStateFlow<HistoryEntity?>(null)

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val history = searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                historyRepository.observeAll()
            } else {
                historyRepository.observeSearch("%${query.trim()}%")
            }
        }

    val uiState: StateFlow<HistoryUiState> = combine(
        history,
        searchQuery,
        isSearchActive,
        showClearAllDialog,
        deletedItem
    ) { items, query, searchActive, showClearDialog, recentlyDeleted ->
        HistoryUiState(
            groups = groupHistoryByDay(items),
            searchQuery = query,
            isSearchActive = searchActive,
            isLoading = false,
            showClearAllDialog = showClearDialog,
            deletedItem = recentlyDeleted
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState()
    )

    fun startSearch() {
        isSearchActive.value = true
    }

    fun dismissSearch() {
        isSearchActive.value = false
        searchQuery.value = ""
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onDelete(item: HistoryEntity) {
        viewModelScope.launch {
            historyRepository.deleteById(item.id)
            deletedItem.value = item
        }
    }

    fun onUndoDelete() {
        val item = deletedItem.value ?: return
        deletedItem.value = null
        viewModelScope.launch { historyRepository.restore(item) }
    }

    fun onDeleteMessageShown() {
        deletedItem.value = null
    }

    fun showClearAllDialog() {
        showClearAllDialog.value = true
    }

    fun dismissClearAllDialog() {
        showClearAllDialog.value = false
    }

    fun clearAll() {
        viewModelScope.launch {
            historyRepository.deleteAll()
            showClearAllDialog.value = false
            deletedItem.value = null
        }
    }

}
