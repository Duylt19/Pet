package com.asianmobile.emojibattery.shimeji.ui.battery.troll

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollCatalogError

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
        nativeAdContent = {}
    )
}

/** The unpublished catalog must read as "coming soon" and never draw a retry affordance. */
@PreviewTest
@Preview(name = "Battery troll themes unpublished", widthDp = 360, heightDp = 400)
@Composable
fun BatteryTrollUnpublishedScreenshotTest() {
    BatteryTrollContent(
        state = previewBatteryTrollErrorState(
            BatteryTrollCatalogError.DISTRIBUTION_NOT_APPROVED
        ),
        onBack = {},
        onPremium = {},
        onTroll = {},
        onRetry = {},
        onDismissReward = {},
        onWatchReward = {},
        nativeAdContent = {}
    )
}
