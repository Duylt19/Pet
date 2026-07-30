package com.asianmobile.emojibattery.shimeji.ui.battery.editor

internal data class BatteryApplyUiState(
    val enabled: Boolean,
    val keepActiveAppearance: Boolean
)

internal object BatteryEditorLoadingPolicy {
    fun applyState(
        themeAvailable: Boolean,
        selectionInProgress: Boolean
    ): BatteryApplyUiState = BatteryApplyUiState(
        enabled = themeAvailable && !selectionInProgress,
        keepActiveAppearance = themeAvailable && selectionInProgress
    )
}
