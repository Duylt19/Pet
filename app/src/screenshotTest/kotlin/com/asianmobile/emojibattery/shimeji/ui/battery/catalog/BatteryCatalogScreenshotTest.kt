package com.asianmobile.emojibattery.shimeji.ui.battery.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ui.shared.component.RewardOfferSheetSurface

@PreviewTest
@Preview(name = "Battery catalog", widthDp = 360, heightDp = 800)
@Composable
fun BatteryCatalogScreenshotTest() {
    BatteryCatalogContent(
        state = previewBatteryCatalogState(),
        onSearch = {},
        onPremium = {},
        onBatteryToggle = {},
        onCustomizeStatusBar = {},
        onOpenCategory = {},
        onFavorite = {},
        onTheme = {},
        onRetry = {},
        nativeAdContent = { BatteryAdPreviewSlot() }
    )
}

@PreviewTest
@Preview(name = "Battery category", widthDp = 360, heightDp = 800)
@Composable
fun BatteryCategoryScreenshotTest() {
    val state = previewBatteryCatalogState()
    val section = state.sections.first()
    val themes = previewBatteryDetailThemes(section)
    BatteryCategoryContent(
        category = section.category,
        themes = themes,
        selectedThemeId = themes[1].id,
        isPremium = false,
        rewardUnlockedThemeIds = emptySet(),
        onBack = {},
        onPremium = {},
        onTheme = {},
        isInlineBannerVisible = true,
        inlineBannerContent = { BatteryInlineBannerPreviewSlot() }
    )
}

@PreviewTest
@Preview(name = "Battery category banner unavailable", widthDp = 360, heightDp = 800)
@Composable
fun BatteryCategoryWithoutBannerScreenshotTest() {
    val state = previewBatteryCatalogState()
    val section = state.sections.first()
    val themes = previewBatteryDetailThemes(section)
    BatteryCategoryContent(
        category = section.category,
        themes = themes,
        selectedThemeId = themes[1].id,
        isPremium = false,
        rewardUnlockedThemeIds = emptySet(),
        onBack = {},
        onPremium = {},
        onTheme = {},
        isInlineBannerVisible = false,
        inlineBannerContent = {}
    )
}

@PreviewTest
@Preview(name = "Battery reward sheet", widthDp = 360, heightDp = 474)
@Composable
fun BatteryRewardSheetScreenshotTest() {
    val theme = previewBatteryCatalogState().themes.first()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_000000).copy(alpha = 0.5f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        RewardOfferSheetSurface {
            BatteryRewardUnlockSheetContent(
                theme = theme,
                isLoading = false,
                rewardNotEarned = false,
                onWatchReward = {},
                onPremium = {},
                nativeAdContent = { BatteryRewardAdPreviewSlot() }
            )
        }
    }
}
