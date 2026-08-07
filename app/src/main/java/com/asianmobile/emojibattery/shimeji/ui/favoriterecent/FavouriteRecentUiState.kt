package com.asianmobile.emojibattery.shimeji.ui.favoriterecent

import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry

enum class FavouriteRecentTab {
    FAVOURITE,
    RECENT
}

data class FavouriteRecentThemeUiState(
    val id: Int,
    val name: String,
    val thumbnailPath: String?,
    val isFavorite: Boolean
)

data class FavouriteRecentUiState(
    val selectedTab: FavouriteRecentTab = FavouriteRecentTab.FAVOURITE,
    val favouriteThemes: List<FavouriteRecentThemeUiState> = emptyList(),
    val recentThemes: List<FavouriteRecentThemeUiState> = emptyList(),
    val isLoading: Boolean = true
) {
    val visibleThemes: List<FavouriteRecentThemeUiState>
        get() = when (selectedTab) {
            FavouriteRecentTab.FAVOURITE -> favouriteThemes
            FavouriteRecentTab.RECENT -> recentThemes
        }
}

internal fun favouriteThemeUiStates(
    themes: List<BatteryThemeEntry>,
    favoriteThemeIds: Set<Int>
): List<FavouriteRecentThemeUiState> = themes
    .asSequence()
    .filter { it.assetsReady && it.id in favoriteThemeIds }
    .map { theme ->
        FavouriteRecentThemeUiState(
            id = theme.id,
            name = theme.name,
            thumbnailPath = theme.thumbnailPath,
            isFavorite = true
        )
    }
    .toList()
