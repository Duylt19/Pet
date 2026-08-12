package com.asianmobile.emojibattery.shimeji.ui.battery.troll

import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollBatteryOrientation
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollEntitlement
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryTrollAccessPolicyTest {
    private val policy = BatteryTrollAccessPolicy()

    private fun troll(
        id: Int = 1,
        entitlement: BatteryTrollEntitlement = BatteryTrollEntitlement.PREMIUM
    ): BatteryTrollEntry = BatteryTrollEntry(
        id = id,
        name = "Troll $id",
        slug = "troll_$id",
        order = id - 1,
        entitlement = entitlement,
        batteryOrientation = BatteryTrollBatteryOrientation.LANDSCAPE,
        thumbnailPath = "thumb/TROLL_$id.webp",
        emojiPaths = (1..5).map { "emoji/TROLL_${id}_$it.webp" },
        batteryPaths = (1..5).map { "battery/TROLL_${id}_$it.webp" }
    )

    @Test
    fun `a free troll opens without asking for anything`() {
        assertEquals(
            BatteryTrollAccess.OPEN,
            policy.resolve(
                troll(entitlement = BatteryTrollEntitlement.FREE),
                isPremium = false,
                rewardUnlockedTrollIds = emptySet()
            )
        )
    }

    @Test
    fun `a premium troll asks a free user to watch or subscribe`() {
        assertEquals(
            BatteryTrollAccess.REWARD_OR_PREMIUM,
            policy.resolve(troll(), isPremium = false, rewardUnlockedTrollIds = emptySet())
        )
    }

    @Test
    fun `a subscriber never sees the reward sheet`() {
        assertEquals(
            BatteryTrollAccess.OPEN,
            policy.resolve(troll(), isPremium = true, rewardUnlockedTrollIds = emptySet())
        )
    }

    @Test
    fun `a reward already earned stays earned`() {
        assertEquals(
            BatteryTrollAccess.OPEN,
            policy.resolve(troll(id = 4), isPremium = false, rewardUnlockedTrollIds = setOf(4))
        )
    }

    @Test
    fun `unlocking one troll does not unlock its neighbour`() {
        assertEquals(
            BatteryTrollAccess.REWARD_OR_PREMIUM,
            policy.resolve(troll(id = 5), isPremium = false, rewardUnlockedTrollIds = setOf(4))
        )
    }
}
