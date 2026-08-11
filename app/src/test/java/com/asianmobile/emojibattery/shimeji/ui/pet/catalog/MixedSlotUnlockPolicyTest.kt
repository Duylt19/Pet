package com.asianmobile.emojibattery.shimeji.ui.pet.catalog

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MixedSlotUnlockPolicyTest {
    @Test
    fun `first three mixed slots are free`() {
        repeat(3) { slotIndex ->
            assertFalse(
                MixedSlotUnlockPolicy.requiresReward(
                    target = PetCatalogTarget.MIXED,
                    slotIndex = slotIndex,
                    petCount = slotIndex,
                    rewardUnlockedSlotCount = 3,
                    isPremium = false
                )
            )
        }
    }

    @Test
    fun `each new mixed slot from fourth requires its own reward`() {
        assertTrue(
            MixedSlotUnlockPolicy.requiresReward(
                target = PetCatalogTarget.MIXED,
                slotIndex = 3,
                petCount = 3,
                rewardUnlockedSlotCount = 3,
                isPremium = false
            )
        )
        assertTrue(
            MixedSlotUnlockPolicy.canUnlockWithReward(
                slotIndex = 3,
                petCount = 3,
                rewardUnlockedSlotCount = 3
            )
        )
        assertFalse(
            MixedSlotUnlockPolicy.canUnlockWithReward(
                slotIndex = 4,
                petCount = 3,
                rewardUnlockedSlotCount = 3
            )
        )
    }

    @Test
    fun `earned capacity and premium bypass mixed reward gate`() {
        assertFalse(
            MixedSlotUnlockPolicy.requiresReward(
                target = PetCatalogTarget.MIXED,
                slotIndex = 3,
                petCount = 3,
                rewardUnlockedSlotCount = 4,
                isPremium = false
            )
        )
        assertFalse(
            MixedSlotUnlockPolicy.requiresReward(
                target = PetCatalogTarget.MIXED,
                slotIndex = 11,
                petCount = 3,
                rewardUnlockedSlotCount = 3,
                isPremium = true
            )
        )
        assertFalse(
            MixedSlotUnlockPolicy.requiresReward(
                target = PetCatalogTarget.SWARM,
                slotIndex = 11,
                petCount = 3,
                rewardUnlockedSlotCount = 3,
                isPremium = false
            )
        )
    }

    @Test
    fun `editing an existing rewarded slot never asks for another reward`() {
        assertFalse(
            MixedSlotUnlockPolicy.requiresReward(
                target = PetCatalogTarget.MIXED,
                slotIndex = 5,
                petCount = 6,
                rewardUnlockedSlotCount = 6,
                isPremium = false
            )
        )
    }
}
