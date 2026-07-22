package com.asianmobile.privatebrower.ui.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.privatebrower.data.database.entity.BookmarkEntity
import com.asianmobile.privatebrower.data.repository.BookmarkRepository
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
class BookmarksViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _showAddDialog = MutableStateFlow(false)
    private val _editingBookmark = MutableStateFlow<BookmarkEntity?>(null)
    private val _isSearchActive = MutableStateFlow(false)

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val debouncedQuery = _searchQuery
        .debounce(300)
        .distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val bookmarksFlow = debouncedQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            bookmarkRepository.observeAll()
        } else {
            bookmarkRepository.observeSearch("%$query%")
        }
    }

    val uiState: StateFlow<BookmarksUiState> = combine(
        _searchQuery,
        bookmarksFlow,
        _showAddDialog,
        _editingBookmark,
        _isSearchActive
    ) { query, bookmarks, showDialog, editing, isSearchActive ->
        BookmarksUiState(
            bookmarks = bookmarks,
            searchQuery = query,
            isLoading = false,
            showAddDialog = showDialog,
            editingBookmark = editing,
            isSearchActive = isSearchActive
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BookmarksUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun startSearch() {
        _isSearchActive.value = true
    }

    fun dismissSearch() {
        _isSearchActive.value = false
        _searchQuery.value = ""
    }

    fun showAddDialog() {
        _editingBookmark.value = null
        _showAddDialog.value = true
    }

    fun showEditDialog(bookmark: BookmarkEntity) {
        _editingBookmark.value = bookmark
        _showAddDialog.value = true
    }

    fun hideDialog() {
        _showAddDialog.value = false
        _editingBookmark.value = null
    }

    fun saveBookmark(name: String, url: String) {
        viewModelScope.launch {
            val editing = _editingBookmark.value
            if (editing != null) {
                bookmarkRepository.update(editing.copy(title = name, url = url))
            } else {
                bookmarkRepository.insert(
                    BookmarkEntity(title = name, url = url)
                )
            }
            hideDialog()
        }
    }

    fun onDeleteBookmark(id: Long) {
        viewModelScope.launch {
            bookmarkRepository.deleteById(id)
        }
    }
}
