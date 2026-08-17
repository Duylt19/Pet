package com.asianmobile.emojibattery.shimeji.ui.battery.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asianmobile.emojibattery.shimeji.ui.home.battery.BatteryHomeViewModel
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView

@Composable
fun BatteryCategoryScreen(
    categoryId: Int,
    onBack: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onOpenTheme: (Int) -> Unit,
    accessibilityHowToUseResult: Boolean? = null,
    onAccessibilityHowToUseResultConsumed: () -> Unit = {},
    onNavigateToAccessibilityHowToUse: () -> Unit = {},
    viewModel: BatteryHomeViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = uiState.catalog
    val category = state.categories.firstOrNull { it.id == categoryId }
    val themes = state.sections.firstOrNull { it.category.id == categoryId }?.themes.orEmpty()

    TrackScreenView(ScreenName.BATTERY_CATEGORY)
    BatteryCatalogFlowHost(
        state = state,
        viewModel = viewModel,
        onOpenTheme = onOpenTheme,
        onOpenBackground = {},
        onNavigateToPremium = onNavigateToPremium,
        accessibilityHowToUseResult = accessibilityHowToUseResult,
        onAccessibilityHowToUseResultConsumed = onAccessibilityHowToUseResultConsumed,
        onNavigateToAccessibilityHowToUse = onNavigateToAccessibilityHowToUse
    ) {
        BatteryCategoryContent(
            category = category,
            themes = themes,
            isLoading = state.isLoading,
            loadFailed = state.error != null,
            selectedThemeId = state.selectedThemeId,
            isPremium = state.isPremium,
            rewardUnlockedThemeIds = state.rewardUnlockedThemeIds,
            onBack = onBack,
            onPremium = onNavigateToPremium,
            onTheme = viewModel::requestTheme,
            onRetry = viewModel::refresh
        )
    }
}
