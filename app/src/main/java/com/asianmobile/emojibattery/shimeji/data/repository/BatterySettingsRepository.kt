package com.asianmobile.emojibattery.shimeji.data.repository

import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import kotlinx.coroutines.flow.StateFlow

interface BatterySettingsRepository {
    val config: StateFlow<BatteryStatusConfig>
    val hiddenAppPackages: StateFlow<Set<String>>

    fun applyConfig(config: BatteryStatusConfig)

    fun setEnabled(enabled: Boolean)

    /**
     * Stores [config] with `enabled = true` **before** the user is handed to system Accessibility
     * settings, so `StatusBarAccessibilityService` finds the intent already stored and attaches the
     * bar while they are still on the settings screen.
     *
     * Pass the stored config unchanged for a plain toggle, or an editor draft to apply that draft
     * at the same moment — the bar then comes up showing exactly what was pressed Apply on.
     * [settleAccessibilityGrant] takes the write back if the grant never happens.
     */
    fun requestEnable(config: BatteryStatusConfig, isAccessibilityGranted: Boolean)

    /**
     * Settles an optimistic [requestEnable] the next time the permission state can be read.
     * Granted keeps the bar on; still missing reverts it. A stored intent that was never
     * optimistic is left untouched.
     */
    fun settleAccessibilityGrant(isAccessibilityGranted: Boolean)

    fun setAppHidden(packageName: String, hidden: Boolean)

    fun toggleFavorite(themeId: Int)

    fun unlockThemeByReward(themeId: Int)

    fun unlockTrollByReward(trollId: Int)
}
