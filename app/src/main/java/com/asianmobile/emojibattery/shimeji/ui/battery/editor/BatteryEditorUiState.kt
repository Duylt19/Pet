package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import com.asianmobile.emojibattery.shimeji.data.model.BUILT_IN_BATTERY_THEME
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry

data class BatteryEditorUiState(
    val theme: BatteryThemeEntry = BUILT_IN_BATTERY_THEME,
    val config: BatteryStatusConfig = BatteryStatusConfig(),
    val isThemeAvailable: Boolean = true
)
