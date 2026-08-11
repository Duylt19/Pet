package com.asianmobile.emojibattery.shimeji.ui.settings.mine

data class SettingsUiState(
    val versionName: String = "",
    val isBatteryEnabled: Boolean = false,
    val isAccessibilityEnabled: Boolean = false
)

sealed interface SettingsEffect {
    data object RequestBatteryAccessibility : SettingsEffect
}
