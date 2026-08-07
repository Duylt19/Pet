package com.asianmobile.emojibattery.shimeji.ui.favoriterecent

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(name = "Favourite empty", widthDp = 360, heightDp = 800)
@Composable
fun FavouriteRecentEmptyScreenshotTest() {
    PreviewFavouriteRecent(FavouriteRecentUiState(isLoading = false))
}

@PreviewTest
@Preview(name = "Recent empty", widthDp = 360, heightDp = 800)
@Composable
fun RecentEmptyScreenshotTest() {
    PreviewFavouriteRecent(
        FavouriteRecentUiState(
            selectedTab = FavouriteRecentTab.RECENT,
            isLoading = false
        )
    )
}

@PreviewTest
@Preview(name = "Favourite populated", widthDp = 360, heightDp = 800)
@Composable
fun FavouriteRecentPopulatedScreenshotTest() {
    PreviewFavouriteRecent(
        FavouriteRecentUiState(
            favouriteThemes = List(6) { index ->
                FavouriteRecentThemeUiState(
                    id = index,
                    name = "Theme ${index + 1}",
                    thumbnailPath = null,
                    isFavorite = true
                )
            },
            isLoading = false
        )
    )
}

@Composable
private fun PreviewFavouriteRecent(state: FavouriteRecentUiState) {
    FavouriteRecentContent(
        uiState = state,
        onBack = {},
        onPremium = {},
        onSelectTab = {},
        onOpenTheme = {},
        onToggleFavorite = {},
        showNativeAd = false
    )
}
