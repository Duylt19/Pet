package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import com.asianmobile.emojibattery.shimeji.battery.settings.BatteryStatusBarHeightRange
import com.asianmobile.emojibattery.shimeji.battery.settings.resolveBatteryStatusBarHeightRange
import com.asianmobile.emojibattery.shimeji.data.model.BUILT_IN_BATTERY_THEME
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_BAR_HEIGHT_DP
import com.asianmobile.emojibattery.shimeji.data.model.BatteryAnimationEntry
import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogCategory
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDecorationEntry
import com.asianmobile.emojibattery.shimeji.data.model.BATTERY_EMOTION_GROUPS
import com.asianmobile.emojibattery.shimeji.data.model.BatteryEmotionGroup
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryMobileDataBadge
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryPreviewSystemState

data class BatteryEditorUiState(
    val isInitialized: Boolean = false,
    val isCatalogLoading: Boolean = true,
    val catalogLoadFailed: Boolean = false,
    val theme: BatteryThemeEntry = BUILT_IN_BATTERY_THEME,
    val themes: List<BatteryThemeEntry> = listOf(BUILT_IN_BATTERY_THEME),
    val trendingEmojiThemeIds: List<Int> = emptyList(),
    val categories: List<BatteryCatalogCategory> = emptyList(),
    val config: BatteryStatusConfig = BatteryStatusConfig(),
    val barHeightRange: BatteryStatusBarHeightRange =
        resolveBatteryStatusBarHeightRange(DEFAULT_BATTERY_BAR_HEIGHT_DP),
    val backgrounds: List<BatteryDecorationEntry> = emptyList(),
    val emotions: List<BatteryDecorationEntry> = emptyList(),
    val emotionGroups: List<BatteryEmotionGroup> = BATTERY_EMOTION_GROUPS,
    val animations: List<BatteryAnimationEntry> = emptyList(),
    val mobileDataBadge: BatteryMobileDataBadge? = null,
    val systemState: BatteryPreviewSystemState = BatteryPreviewSystemState(),
    val isThemeAvailable: Boolean = true,
    val hasUnsavedChanges: Boolean = false,
    val isPremium: Boolean = false,
    val pendingSelection: BatteryEditorThemeSelection? = null,
    val pendingBackgroundSelectionId: Int? = null,
    val assetSelectionInProgress: BatteryEditorThemeSelection? = null,
    val backgroundSelectionInProgress: Int? = null,
    val emotionSelectionInProgress: Int? = null,
    val isRewardInProgress: Boolean = false,
    val isApplyInProgress: Boolean = false,
    val message: BatteryEditorMessage? = null
) {
    val isApplyEnabled: Boolean get() =
        isThemeAvailable &&
            assetSelectionInProgress == null &&
            backgroundSelectionInProgress == null &&
            emotionSelectionInProgress == null &&
            !isApplyInProgress
}

data class BatteryEditorThemeSelection(
    val themeId: Int,
    val component: BatteryThemeComponent
)

enum class BatteryThemeComponent {
    EMOJI,
    BATTERY
}

enum class BatteryEditorMessage {
    REWARD_NOT_EARNED,
    THEME_UNAVAILABLE,
    ASSET_DOWNLOAD_FAILED
}

sealed interface BatteryEditorEffect {
    data object ShowRewardedAd : BatteryEditorEffect

    /** The UI shows confirmation, then returns to the destination that opened Customize. */
    data object ShowApplySuccess : BatteryEditorEffect
}
