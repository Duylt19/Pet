package com.asianmobile.emojibattery.shimeji.ui.battery.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asianmobile.emojibattery.shimeji.ads.config.BANNER_BATTERY_CATEGORY_INLINE
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.BannerAd
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView

@Composable
fun BatteryCategoryScreen(
    categoryId: Int,
    onBack: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onOpenTheme: (Int) -> Unit,
    viewModel: BatteryCatalogViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val category = state.categories.firstOrNull { it.id == categoryId }
    val themes = state.sections.firstOrNull { it.category.id == categoryId }?.themes.orEmpty()
    var isInlineBannerVisible by remember { mutableStateOf(true) }

    TrackScreenView(ScreenName.BATTERY_CATEGORY)
    BatteryCatalogFlowHost(
        state = state,
        viewModel = viewModel,
        onOpenTheme = onOpenTheme,
        onNavigateToPremium = onNavigateToPremium
    ) {
        BatteryCategoryContent(
            category = category,
            themes = themes,
            selectedThemeId = state.selectedThemeId,
            isPremium = state.isPremium,
            rewardUnlockedThemeIds = state.rewardUnlockedThemeIds,
            onBack = onBack,
            onPremium = onNavigateToPremium,
            onTheme = viewModel::requestTheme,
            isInlineBannerVisible = isInlineBannerVisible,
            inlineBannerContent = {
                BannerAd(
                    adPosition = BANNER_BATTERY_CATEGORY_INLINE,
                    onVisibilityChanged = { isInlineBannerVisible = it }
                )
            }
        )
    }
}
