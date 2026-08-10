package com.asianmobile.emojibattery.shimeji.ui.battery.catalog

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest

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
        inlineBannerContent = { BatteryInlineBannerPreviewSlot() }
    )
}
