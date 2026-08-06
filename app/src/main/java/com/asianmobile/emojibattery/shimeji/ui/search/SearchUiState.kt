package com.asianmobile.emojibattery.shimeji.ui.search

data class SearchUiState(
    val query: String = "",
    val recommendedThemes: List<SearchThemeUiState> = emptyList(),
    val isLoading: Boolean = true,
    val hasError: Boolean = false
)

data class SearchThemeUiState(
    val id: Int,
    val name: String,
    val category: String,
    val thumbnailPath: String?,
    val isFavorite: Boolean
)
