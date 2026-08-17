package com.asianmobile.emojibattery.shimeji.ui.permissions

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(widthDp = 360, heightDp = 882)
@Composable
fun AccessibilityHowToUseScreenshotTest() {
    AccessibilityHowToUseContent(
        uiState = AccessibilityHowToUseUiState(),
        onNavigateBack = {},
        onGoToSettings = {}
    )
}
