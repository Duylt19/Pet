package com.asianmobile.emojibattery.shimeji.ui.settings.permissions

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
        requiredTarget = GrantPermissionsTarget.ACCESSIBILITY,
        onNavigateBack = {},
        onTargetClicked = {},
        onPrimaryAction = {}
    )
}

@PreviewTest
@Preview(widthDp = 360, heightDp = 1000)
@Composable
fun OverlayGrantPermissionsScreenshotTest() {
    GrantPermissionsContent(
        uiState = GrantPermissionsUiState(
            isAccessibilityEnabled = false,
            isOverlayGranted = false,
            isBatteryOptimizationIgnored = false,
            isBatteryRowVisible = true,
            isAutoStartRowVisible = true,
            isNotificationGranted = false,
            isNotificationRowVisible = true
        ),
        requiredTarget = GrantPermissionsTarget.OVERLAY,
        onNavigateBack = {},
        onTargetClicked = {},
        onPrimaryAction = {}
    )
}
