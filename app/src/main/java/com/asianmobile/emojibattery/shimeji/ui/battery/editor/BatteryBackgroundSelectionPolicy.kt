package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig

internal object BatteryBackgroundSelectionPolicy {

    fun selectColor(config: BatteryStatusConfig, colorArgb: Int): BatteryStatusConfig =
        config.copy(
            backgroundColorArgb = colorArgb,
            backgroundDecorationId = SOLID_BACKGROUND_ID
        )

    fun selectTheme(config: BatteryStatusConfig, decorationId: Int): BatteryStatusConfig =
        config.copy(backgroundDecorationId = decorationId)

    fun activeColor(config: BatteryStatusConfig): Int? =
        config.backgroundColorArgb.takeIf {
            config.backgroundDecorationId == SOLID_BACKGROUND_ID
        }

    private const val SOLID_BACKGROUND_ID = 0
}
