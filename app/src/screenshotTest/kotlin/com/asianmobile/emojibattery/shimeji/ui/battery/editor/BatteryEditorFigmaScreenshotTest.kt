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
import com.asianmobile.emojibattery.shimeji.data.model.BATTERY_EMOTION_GROUPS
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntitlement
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry
import com.asianmobile.emojibattery.shimeji.ui.shared.component.RewardOfferSheetSurface
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryMobileDataBadge

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
@Preview(name = "Status bar Airplane option", widthDp = 360, heightDp = 800)
@Composable
fun BatteryAirplaneOptionScreenshotTest() = PreviewStatusOptionPage(BatteryEditorPage.AIRPLANE)

@PreviewTest
@Preview(name = "Status bar Ringer option", widthDp = 360, heightDp = 800)
@Composable
fun BatteryRingerOptionScreenshotTest() = PreviewStatusOptionPage(BatteryEditorPage.RINGER)

@PreviewTest
@Preview(name = "Status bar Date option", widthDp = 360, heightDp = 800)
@Composable
fun BatteryDateOptionScreenshotTest() = PreviewStatusOptionPage(BatteryEditorPage.DATE_TIME)

@PreviewTest
@Preview(name = "Status bar Hotspot option", widthDp = 360, heightDp = 800)
@Composable
fun BatteryHotspotOptionScreenshotTest() = PreviewStatusOptionPage(BatteryEditorPage.HOTSPOT)

@PreviewTest
@Preview(name = "Status bar Charge option", widthDp = 360, heightDp = 800)
@Composable
fun BatteryChargeOptionScreenshotTest() = PreviewStatusOptionPage(BatteryEditorPage.CHARGE)

@PreviewTest
@Preview(name = "Status bar Clock option", widthDp = 360, heightDp = 800)
@Composable
fun BatteryClockOptionScreenshotTest() = PreviewStatusOptionPage(BatteryEditorPage.CLOCK)

@PreviewTest
@Preview(name = "Status bar Animation option", widthDp = 360, heightDp = 800)
@Composable
fun BatteryAnimationOptionScreenshotTest() = PreviewStatusOptionPage(BatteryEditorPage.ANIMATION)

@PreviewTest
@Preview(name = "Status bar Wi-Fi option", widthDp = 360, heightDp = 800)
@Composable
fun BatteryWifiOptionScreenshotTest() = PreviewStatusOptionPage(BatteryEditorPage.WIFI)

@PreviewTest
@Preview(name = "Status bar Signal option", widthDp = 360, heightDp = 800)
@Composable
fun BatterySignalOptionScreenshotTest() = PreviewStatusOptionPage(BatteryEditorPage.SIGNAL)

@PreviewTest
@Preview(name = "Status bar Mobile data option", widthDp = 360, heightDp = 800)
@Composable
fun BatteryMobileDataOptionScreenshotTest() = PreviewStatusOptionPage(BatteryEditorPage.DATA)

@PreviewTest
@Preview(name = "Status bar Emotion groups", widthDp = 360, heightDp = 800)
@Composable
fun BatteryEmotionGroupsScreenshotTest() = PreviewEmotionPage(groupKey = null)

@PreviewTest
@Preview(name = "Status bar Emotion detail", widthDp = 360, heightDp = 800)
@Composable
fun BatteryEmotionDetailScreenshotTest() = PreviewEmotionPage(groupKey = "emoji")

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

@PreviewTest
@Preview(name = "Status bar discard changes", widthDp = 360, heightDp = 416)
@Composable
fun BatteryEditorDiscardChangesScreenshotTest() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000)),
        contentAlignment = Alignment.BottomCenter
    ) {
        RewardOfferSheetSurface {
            BatteryDiscardChangesSheetContent(
                onCancel = {},
                onExit = {},
                nativeAdContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(171.dp)
                            .background(Color(0xFFF2F2F2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Native ad 336×222")
                    }
                }
            )
        }
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

@Composable
private fun PreviewStatusOptionPage(page: BatteryEditorPage) {
    val baseState = previewEditorState()
    val optionState = baseState.copy(
        config = baseState.config.copy(
            showDateTime = true,
            dateTimeColorArgb = 0xFF000000.toInt(),
            clockColorArgb = 0xFF000000.toInt(),
            airplaneColorArgb = 0xFF000000.toInt(),
            hotspotColorArgb = 0xFF000000.toInt(),
            ringerColorArgb = 0xFF000000.toInt(),
            chargeColorArgb = 0xFF000000.toInt(),
            wifiColorArgb = 0xFF000000.toInt(),
            signalColorArgb = 0xFF000000.toInt(),
            dataColorArgb = 0xFF000000.toInt()
        )
    )
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            BatteryStatusOptionFigmaScreen(
                state = optionState,
                page = page,
                onBack = {},
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
            Text("COLLAPSIBLE AD")
        }
    }
}

@Composable
private fun PreviewEmotionPage(groupKey: String?) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            BatteryEmotionFigmaScreen(
                state = previewEditorState(),
                groupKey = groupKey,
                onBack = {},
                onPremium = {},
                onOpenGroup = {},
                onSelectEmotion = {},
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
        emotions = BATTERY_EMOTION_GROUPS.flatMap { group ->
            group.emotionIds.mapIndexed { order, id ->
                BatteryDecorationEntry(
                    id = id,
                    name = "${group.key}_${order + 1}",
                    assetPath = "",
                    groupKey = group.key,
                    order = order,
                    type = BatteryDecorationType.EMOTION
                )
            }
        },
        animations = animations,
        mobileDataBadge = BatteryMobileDataBadge.G5,
        config = BatteryStatusConfig(
            enabled = false,
            selectedThemeId = themes[1].id,
            selectedBatteryThemeId = themes[1].id,
            selectedEmojiThemeId = themes[2].id,
            showAnimation = true,
            animationAssetName = animations.first().name,
            emotionDecorationId = 21,
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
