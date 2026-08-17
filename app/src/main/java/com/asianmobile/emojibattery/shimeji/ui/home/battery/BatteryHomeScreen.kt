package com.asianmobile.emojibattery.shimeji.ui.home.battery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.BatteryCatalogContent
import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.BatteryCatalogFlowHost
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView

/** Top-level Battery tab owned by the Home shell. */
@Composable
fun BatteryHomeScreen(
    onSearch: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onOpenCategory: (Int) -> Unit,
    onOpenTheme: (Int) -> Unit,
    accessibilityHowToUseResult: Boolean? = null,
    onAccessibilityHowToUseResultConsumed: () -> Unit = {},
    onNavigateToAccessibilityHowToUse: () -> Unit = {},
    viewModel: BatteryHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = uiState.catalog

    TrackScreenView(ScreenName.BATTERY_CATALOG)
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
        BatteryCatalogContent(
            state = state,
            onSearch = onSearch,
            onPremium = onNavigateToPremium,
            onBatteryToggle = viewModel::onBatteryToggle,
            onCustomizeStatusBar = viewModel::requestCurrentStyle,
            onOpenCategory = onOpenCategory,
            onFavorite = viewModel::toggleFavorite,
            onTheme = viewModel::requestTheme,
            onRetry = viewModel::refresh
        )
    }
}
