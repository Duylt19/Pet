package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig

class BatteryThemeSelectionPolicy {
    fun initializeStyle(
        config: BatteryStatusConfig,
        themeId: Int
    ): BatteryStatusConfig = config.copy(
        selectedThemeId = themeId,
        selectedBatteryThemeId = themeId,
        selectedEmojiThemeId = themeId
    )

    fun selectComponent(
        config: BatteryStatusConfig,
        themeId: Int,
        component: BatteryThemeComponent
    ): BatteryStatusConfig = when (component) {
        BatteryThemeComponent.EMOJI -> config.copy(selectedEmojiThemeId = themeId)
        BatteryThemeComponent.BATTERY -> config.copy(selectedBatteryThemeId = themeId)
    }
}
