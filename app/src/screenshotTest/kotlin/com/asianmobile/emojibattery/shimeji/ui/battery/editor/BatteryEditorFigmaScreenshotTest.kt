package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDecorationEntry
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDecorationType
import com.asianmobile.emojibattery.shimeji.data.model.BatteryAnimationEntry
import com.asianmobile.emojibattery.shimeji.data.model.BatteryAnimationType
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntitlement
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry

@PreviewTest
@Preview(name = "Status bar editor expanded", widthDp = 360, heightDp = 800)
@Composable
fun BatteryEditorOverviewScreenshotTest() {
    PreviewEditorPage(BatteryEditorPage.OVERVIEW)
}

@PreviewTest
@Preview(name = "Status bar Battery picker", widthDp = 360, heightDp = 800)
@Composable
fun BatteryEditorBatteryPickerScreenshotTest() {
    PreviewEditorPage(BatteryEditorPage.BATTERY_TEMPLATES)
}

@PreviewTest
@Preview(name = "Status bar Theme picker", widthDp = 360, heightDp = 800)
@Composable
fun BatteryEditorThemePickerScreenshotTest() {
    PreviewEditorPage(BatteryEditorPage.BACKGROUND_THEMES)
}

@PreviewTest
@Preview(name = "Status bar color picker", widthDp = 360, heightDp = 491)
@Composable
fun BatteryEditorColorPickerScreenshotTest() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000)),
        contentAlignment = Alignment.BottomCenter
    ) {
        StatusBarColorPickerSurface(
            selectedColor = 0xFFE8794D.toInt(),
            onColorChange = {},
            onDismiss = {}
        )
    }
}

@PreviewTest
@Preview(name = "Status bar Material slider", widthDp = 360, heightDp = 112)
@Composable
fun BatteryEditorMaterialSliderScreenshotTest() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        DesignSlider(
            label = "Size",
            value = 16f,
            range = 8f..24f,
            onValueChange = {}
        )
    }
}

@Composable
private fun PreviewEditorPage(page: BatteryEditorPage) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            BatteryEditorFigmaContent(
                state = previewEditorState(),
                page = page,
                onBack = {},
                onOpenPage = {},
                onPremium = {},
                onSelectTheme = { _, _ -> },
                onBackgroundColor = {},
                onBackgroundDecoration = {},
                onConfig = {},
                onApply = {}
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text("AD")
        }
    }
}

private fun previewEditorState(): BatteryEditorUiState {
    val themes = List(12) { index ->
        BatteryThemeEntry(
            id = index,
            name = "Style ${index + 1}",
            categoryId = 1,
            categoryName = "Cute",
            entitlement = if (index % 3 == 0) {
                BatteryThemeEntitlement.PREMIUM
            } else {
                BatteryThemeEntitlement.FREE
            },
            thumbnailPath = null,
            batteryPath = null,
            emojiPath = null,
            assetsReady = true
        )
    }
    val backgrounds = List(12) { index ->
        BatteryDecorationEntry(
            id = index + 1,
            name = "Theme ${index + 1}",
            assetPath = "",
            type = BatteryDecorationType.BACKGROUND
        )
    }
    val animations = List(5) { index ->
        BatteryAnimationEntry(
            id = index + 1,
            name = "Animation ${index + 1}",
            assetPath = "",
            type = BatteryAnimationType.GIF
        )
    }
    return BatteryEditorUiState(
        theme = themes[1],
        themes = themes,
        backgrounds = backgrounds,
        animations = animations,
        config = BatteryStatusConfig(
            enabled = false,
            selectedThemeId = themes[1].id,
            selectedBatteryThemeId = themes[1].id,
            selectedEmojiThemeId = themes[2].id,
            showAnimation = true,
            animationAssetName = animations.first().name,
            backgroundDecorationId = backgrounds.first().id,
            backgroundColorArgb = 0xFF111111.toInt(),
            foregroundColorArgb = 0xFFFFFFFF.toInt(),
            percentColorArgb = 0xFFFFFFFF.toInt(),
            dateTimeColorArgb = 0xFFFFFFFF.toInt(),
            wifiColorArgb = 0xFFFFFFFF.toInt(),
            dataColorArgb = 0xFFFFFFFF.toInt(),
            signalColorArgb = 0xFFFFFFFF.toInt()
        )
    )
}
