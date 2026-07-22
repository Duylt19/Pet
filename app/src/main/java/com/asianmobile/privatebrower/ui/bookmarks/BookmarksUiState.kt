package com.asianmobile.privatebrower.ui.bookmarks

import com.asianmobile.privatebrower.data.database.entity.BookmarkEntity

data class BookmarksUiState(
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val editingBookmark: BookmarkEntity? = null,
    val isSearchActive: Boolean = false
)
