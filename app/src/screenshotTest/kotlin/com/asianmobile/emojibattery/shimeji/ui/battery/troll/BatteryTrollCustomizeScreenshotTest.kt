package com.asianmobile.emojibattery.shimeji.ui.battery.troll

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollBatteryOrientation
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollEntitlement
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollEntry
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollMode

@PreviewTest
@Preview(name = "Battery troll customize", widthDp = 360, heightDp = 1148)
@Composable
fun BatteryTrollCustomizeScreenshotTest() {
    PreviewTrollCustomize(previewTrollCustomizeState())
}

/** The two state deltas Figma calls out: Real Battery greys the Edit chip, Random dims both pickers. */
@PreviewTest
@Preview(name = "Battery troll customize real random", widthDp = 360, heightDp = 1148)
@Composable
fun BatteryTrollCustomizeRealRandomScreenshotTest() {
    val state = previewTrollCustomizeState()
    PreviewTrollCustomize(
        state.copy(
            draft = state.draft.copy(
                mode = BatteryTrollMode.REAL,
                randomArtwork = true
            )
        )
    )
}

@Composable
private fun PreviewTrollCustomize(state: BatteryTrollCustomizeUiState) {
    BatteryTrollCustomizeContent(
        uiState = state,
        onBack = {},
        onBatteryToggle = {},
        onModeChange = {},
        onEditPercentRequest = {},
        onEditPercentConfirm = {},
        onEditPercentDismiss = {},
        onShowPercentageToggle = {},
        onPercentSizeChange = {},
        onRandomArtworkChange = {},
        onEmojiLevelChange = {},
        onBatteryLevelChange = {},
        onDiscardDismiss = {},
        onDiscardConfirm = {},
        onApply = {}
    )
}

private fun previewTrollCustomizeState(): BatteryTrollCustomizeUiState {
    val troll = BatteryTrollEntry(
        id = 1,
        name = "Black Cat",
        slug = "black-cat",
        order = 1,
        entitlement = BatteryTrollEntitlement.FREE,
        batteryOrientation = BatteryTrollBatteryOrientation.LANDSCAPE,
        thumbnailPath = "thumb/TROLL_1.webp",
        emojiPaths = (1..5).map { "emoji/TROLL_1_$it.webp" },
        batteryPaths = (1..5).map { "battery/TROLL_1_$it.webp" }
    )
    val draft = BatteryTrollDraft(
        trollId = troll.id,
        mode = BatteryTrollMode.FAKE,
        fakePercent = 999,
        showPercentage = true,
        percentSizeDp = 16f,
        randomArtwork = false,
        emojiLevelIndex = 0,
        batteryLevelIndex = 0
    )
    return BatteryTrollCustomizeUiState(
        troll = troll,
        draft = draft,
        applied = draft,
        realBatteryLevel = 12,
        isBatteryEnabled = true,
        isLoading = false
    )
}
