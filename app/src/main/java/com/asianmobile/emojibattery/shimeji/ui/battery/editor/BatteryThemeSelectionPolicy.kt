package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry

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

    fun assetPath(
        theme: BatteryThemeEntry,
        component: BatteryThemeComponent
    ): String? = when (component) {
        BatteryThemeComponent.EMOJI -> theme.emojiPath
        BatteryThemeComponent.BATTERY -> theme.batteryPath
    }

    fun isMaterialized(
        theme: BatteryThemeEntry,
        materializedPath: String?
    ): Boolean = theme.isBuiltIn || materializedPath != null
}
