package com.asianmobile.emojibattery.shimeji.data.repository

import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import kotlinx.coroutines.flow.StateFlow

interface BatterySettingsRepository {
    val config: StateFlow<BatteryStatusConfig>
    val hiddenAppPackages: StateFlow<Set<String>>
    val recentThemeIds: StateFlow<List<Int>>

    fun applyConfig(config: BatteryStatusConfig)

    fun setEnabled(enabled: Boolean)

    fun setAppHidden(packageName: String, hidden: Boolean)

    fun toggleFavorite(themeId: Int)

    fun recordRecentTheme(themeId: Int)

    fun unlockThemeByReward(themeId: Int)

    fun unlockTrollByReward(trollId: Int)
}
