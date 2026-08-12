package com.asianmobile.emojibattery.shimeji.ui.battery.troll

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(name = "Battery troll themes", widthDp = 360, heightDp = 800)
@Composable
fun BatteryTrollScreenshotTest() {
    BatteryTrollContent(
        state = previewBatteryTrollState(),
        onBack = {},
        onPremium = {},
        onTroll = {},
        onRetry = {},
        onDismissReward = {},
        onWatchReward = {},
        bannerAdContent = { BatteryTrollBannerPreviewSlot() },
        nativeAdContent = {}
    )
}
