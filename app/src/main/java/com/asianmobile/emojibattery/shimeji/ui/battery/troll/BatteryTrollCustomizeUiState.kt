package com.asianmobile.emojibattery.shimeji.ui.battery.troll

import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollEntry
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollMode
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_PERCENT_SIZE_DP
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_TROLL_FAKE_PERCENT
import com.asianmobile.emojibattery.shimeji.data.model.NO_BATTERY_TROLL_THEME_ID

/**
 * State of the Battery Troll Customize screen (Figma `8315:8232` / `8359:6992` / `8359:7165`).
 *
 * [draft] is what the controls show; [applied] is what the status bar is actually running. Apply
 * copies one onto the other, and Back compares them to decide whether the discard sheet is owed.
 */
data class BatteryTrollCustomizeUiState(
    val troll: BatteryTrollEntry? = null,
    val draft: BatteryTrollDraft = BatteryTrollDraft(),
    val applied: BatteryTrollDraft = BatteryTrollDraft(),
    val realBatteryLevel: Int = 100,
    val isBatteryEnabled: Boolean = false,
    val isEditingFakePercent: Boolean = false,
    val isDiscardVisible: Boolean = false,
    val isLoading: Boolean = true
) {
    val hasUnsavedChanges: Boolean get() = draft != applied

    /** The number the preview strip writes, which is the whole point of Fake mode. */
    val previewPercent: Int
        get() = when (draft.mode) {
            BatteryTrollMode.FAKE -> draft.fakePercent
            BatteryTrollMode.REAL -> realBatteryLevel
        }

    /** Real mode has nothing to edit, and Random takes both pickers away from the user. */
    val isEditEnabled: Boolean get() = draft.mode == BatteryTrollMode.FAKE
    val isArtworkPickerEnabled: Boolean get() = !draft.randomArtwork
}

data class BatteryTrollDraft(
    val trollId: Int = NO_BATTERY_TROLL_THEME_ID,
    val mode: BatteryTrollMode = BatteryTrollMode.FAKE,
    val fakePercent: Int = DEFAULT_BATTERY_TROLL_FAKE_PERCENT,
    val showPercentage: Boolean = true,
    val percentSizeDp: Float = DEFAULT_BATTERY_PERCENT_SIZE_DP,
    val randomArtwork: Boolean = false,
    val emojiLevelIndex: Int = 0,
    val batteryLevelIndex: Int = 0
)

sealed interface BatteryTrollCustomizeEffect {
    data object Close : BatteryTrollCustomizeEffect
    data object RequestBatteryAccessibility : BatteryTrollCustomizeEffect
}
