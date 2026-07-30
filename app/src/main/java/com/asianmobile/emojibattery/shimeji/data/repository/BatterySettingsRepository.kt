package com.asianmobile.emojibattery.shimeji.data.repository

import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import kotlinx.coroutines.flow.StateFlow

interface BatterySettingsRepository {
    val config: StateFlow<BatteryStatusConfig>

    fun applyConfig(config: BatteryStatusConfig)

    fun setEnabled(enabled: Boolean)

    fun toggleFavorite(themeId: Int)

    fun unlockThemeByReward(themeId: Int)
}
