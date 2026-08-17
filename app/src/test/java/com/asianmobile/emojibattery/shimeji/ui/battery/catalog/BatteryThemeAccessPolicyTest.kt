package com.asianmobile.emojibattery.shimeji.ui.battery.catalog

import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntitlement
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryThemeAccessPolicyTest {
    private val policy = BatteryThemeAccessPolicy()

    @Test
    fun freeTheme_opensWithoutPremiumOrReward() {
        assertEquals(
            BatteryThemeAccess.OPEN,
            policy.resolve(theme(entitlement = BatteryThemeEntitlement.FREE), false, emptySet())
        )
    }

    @Test
    fun premiumTheme_requiresRewardOrPremiumBeforeUnlock() {
        assertEquals(
            BatteryThemeAccess.REWARD_OR_PREMIUM,
            policy.resolve(
                theme(entitlement = BatteryThemeEntitlement.PREMIUM),
                isPremium = false,
                rewardUnlockedThemeIds = emptySet()
            )
        )
    }

    @Test
    fun premiumTheme_opensForSubscriptionOrMatchingRewardUnlock() {
        val theme = theme(id = 27, entitlement = BatteryThemeEntitlement.PREMIUM)

        assertEquals(BatteryThemeAccess.OPEN, policy.resolve(theme, true, emptySet()))
        assertEquals(BatteryThemeAccess.OPEN, policy.resolve(theme, false, setOf(27)))
        assertEquals(
            BatteryThemeAccess.REWARD_OR_PREMIUM,
            policy.resolve(theme, false, setOf(28))
        )
    }

    @Test
    fun missingAssets_neverOpenEvenWhenPremium() {
        assertEquals(
            BatteryThemeAccess.UNAVAILABLE,
            policy.resolve(
                theme(
                    entitlement = BatteryThemeEntitlement.PREMIUM,
                    assetsReady = false
                ),
                isPremium = true,
                rewardUnlockedThemeIds = setOf(7)
            )
        )
    }

    private fun theme(
        id: Int = 7,
        entitlement: BatteryThemeEntitlement,
        assetsReady: Boolean = true
    ) = BatteryThemeEntry(
        id = id,
        name = "Theme $id",
        categoryId = 1,
        categoryName = "Test",
        entitlement = entitlement,
        thumbnailPath = "thumb/$id.png",
        batteryPath = "battery/$id.png",
        emojiPath = "emoji/$id.png",
        assetsReady = assetsReady
    )
}
