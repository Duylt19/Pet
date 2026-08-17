package com.asianmobile.emojibattery.shimeji.ui.home.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryAccessibilityRecovery
import com.intuit.sdp.R as SdpR

/**
 * All three causes together: they share a layout but not a line count, and the card has to stay
 * readable at the longest of them.
 */
@PreviewTest
@Preview(widthDp = 360)
@Composable
fun BatteryAccessibilityRecoveryCardScreenshotTest() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.colors_FFFFFF))
            .padding(bottom = dimensionResource(SdpR.dimen._9sdp))
    ) {
        BatteryAccessibilityRecoveryCard(
            recovery = BatteryAccessibilityRecovery.APP_CLOSED,
            onTurnBackOn = {},
            onDismiss = {}
        )
        BatteryAccessibilityRecoveryCard(
            recovery = BatteryAccessibilityRecovery.DEVICE_KILLED,
            onTurnBackOn = {},
            onDismiss = {}
        )
        BatteryAccessibilityRecoveryCard(
            recovery = BatteryAccessibilityRecovery.UNKNOWN_CAUSE,
            onTurnBackOn = {},
            onDismiss = {}
        )
    }
}
