package com.asianmobile.emojibattery.shimeji.ui.battery.catalog

import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntitlement
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry

class BatteryThemeAccessPolicy {
    fun resolve(
        theme: BatteryThemeEntry,
        isPremium: Boolean,
        rewardUnlockedThemeIds: Set<Int>
    ): BatteryThemeAccess = when {
        !theme.assetsReady -> BatteryThemeAccess.UNAVAILABLE
        theme.entitlement == BatteryThemeEntitlement.FREE ->
            BatteryThemeAccess.OPEN
        isPremium || theme.id in rewardUnlockedThemeIds ->
            BatteryThemeAccess.OPEN
        else -> BatteryThemeAccess.REWARD_OR_PREMIUM
    }
}

enum class BatteryThemeAccess {
    OPEN,
    REWARD_OR_PREMIUM,
    UNAVAILABLE
}
