package com.asianmobile.emojibattery.shimeji.ui.battery.troll

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryPowerState
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryPreviewSystemState
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollBatteryOrientation
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollCatalogError
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollEntitlement
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollEntry
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollMode

@PreviewTest
@Preview(name = "Battery troll customize", widthDp = 360, heightDp = 800)
@Composable
fun BatteryTrollCustomizeScreenshotTest() {
    PreviewTrollCustomize(previewTrollCustomizeState())
}

/** Long translations must expand the Edit chip instead of clipping its label. */
@PreviewTest
@Preview(
    name = "Battery troll customize Vietnamese",
    widthDp = 360,
    heightDp = 800,
    locale = "vi"
)
@Composable
fun BatteryTrollCustomizeVietnameseScreenshotTest() {
    PreviewTrollCustomize(previewTrollCustomizeState())
}

/** The two state deltas Figma calls out: Real Battery greys the Edit chip, Random dims both pickers. */
@PreviewTest
@Preview(name = "Battery troll customize real random", widthDp = 360, heightDp = 800)
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

/** No troll resolved: the controls are replaced and Apply must read as unavailable. */
@PreviewTest
@Preview(name = "Battery troll customize unavailable", widthDp = 360, heightDp = 800)
@Composable
fun BatteryTrollCustomizeUnavailableScreenshotTest() {
    PreviewTrollCustomize(
        previewTrollCustomizeState().copy(
            troll = null,
            isLoading = false,
            catalogError = BatteryTrollCatalogError.CATALOG_UNAVAILABLE
        )
    )
}

/** Percentage off and the character off: both must disappear from the preview strip. */
@PreviewTest
@Preview(name = "Battery troll customize no percent no emoji", widthDp = 360, heightDp = 800)
@Composable
fun BatteryTrollCustomizeHiddenPartsScreenshotTest() {
    val state = previewTrollCustomizeState()
    PreviewTrollCustomize(
        state.copy(
            draft = state.draft.copy(showPercentage = false, showEmoji = false)
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
        onShowEmojiToggle = {},
        onRandomArtworkChange = {},
        onEmojiLevelChange = {},
        onBatteryLevelChange = {},
        onDiscardDismiss = {},
        onDiscardConfirm = {},
        onRetry = {},
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
        // Match the historical `_38sdp` golden baseline (45.6dp on this sw360 profile);
        // production inherits the user's stored dynamic height.
        storedConfig = BatteryStatusConfig(barHeightDp = 45.6f),
        // The device really is nearly flat: Fake mode has to write 999% over a 12% bar, which is
        // the only way the golden proves the two numbers stay independent.
        systemState = BatteryPreviewSystemState(powerState = BatteryPowerState(level = 12)),
        isBatteryEnabled = true,
        isLoading = false
    )
}
