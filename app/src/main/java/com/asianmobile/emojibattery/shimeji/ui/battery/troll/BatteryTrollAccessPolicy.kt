package com.asianmobile.emojibattery.shimeji.ui.battery.troll

import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollEntitlement
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollEntry

/**
 * Mirrors `BatteryThemeAccessPolicy` so a troll theme unlocks exactly the way a battery style
 * does — the owner asked for the same model, and two different unlock rules on two catalogs
 * would be indistinguishable from a bug.
 */
class BatteryTrollAccessPolicy {
    fun resolve(
        troll: BatteryTrollEntry,
        isPremium: Boolean,
        rewardUnlockedTrollIds: Set<Int>
    ): BatteryTrollAccess = when {
        troll.entitlement == BatteryTrollEntitlement.FREE -> BatteryTrollAccess.OPEN
        isPremium || troll.id in rewardUnlockedTrollIds -> BatteryTrollAccess.OPEN
        else -> BatteryTrollAccess.REWARD_OR_PREMIUM
    }
}

enum class BatteryTrollAccess {
    OPEN,
    REWARD_OR_PREMIUM
}
