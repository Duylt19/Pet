package com.asianmobile.emojibattery.shimeji.ui.permission

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(widthDp = 360, heightDp = 1000)
@Composable
fun GrantPermissionsScreenshotTest() {
    GrantPermissionsContent(
        uiState = GrantPermissionsUiState(
            isAccessibilityEnabled = true,
            isOverlayGranted = false,
            isBatteryOptimizationIgnored = false,
            isBatteryRowVisible = true,
            isAutoStartRowVisible = true,
            isNotificationGranted = false,
            isNotificationRowVisible = true
        ),
        onNavigateBack = {},
        onTargetClicked = {}
    )
}
