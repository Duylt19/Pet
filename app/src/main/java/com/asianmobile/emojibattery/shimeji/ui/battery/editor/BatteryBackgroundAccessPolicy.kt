package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import com.asianmobile.emojibattery.shimeji.data.model.BatteryDecorationEntry

internal object BatteryBackgroundAccessPolicy {
    fun resolve(
        background: BatteryDecorationEntry,
        catalogIndex: Int,
        isPremium: Boolean,
        rewardUnlockedBackgroundIds: Set<Int>
    ): BatteryBackgroundAccess = when {
        catalogIndex < 0 || background.assetPath.isBlank() -> BatteryBackgroundAccess.UNAVAILABLE
        catalogIndex < FREE_BACKGROUND_COUNT -> BatteryBackgroundAccess.OPEN
        isPremium || background.id in rewardUnlockedBackgroundIds ->
            BatteryBackgroundAccess.OPEN
        else -> BatteryBackgroundAccess.REWARD_OR_PREMIUM
    }

    const val FREE_BACKGROUND_COUNT = 5
}

internal enum class BatteryBackgroundAccess {
    OPEN,
    REWARD_OR_PREMIUM,
    UNAVAILABLE
}
