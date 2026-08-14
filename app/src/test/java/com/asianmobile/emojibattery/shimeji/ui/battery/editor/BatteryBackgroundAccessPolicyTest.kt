package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import com.asianmobile.emojibattery.shimeji.data.model.BatteryDecorationEntry
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDecorationType
import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryBackgroundAccessPolicyTest {
    private val background = BatteryDecorationEntry(
        id = 12,
        name = "Cloud",
        assetPath = "background/cloud.webp",
        type = BatteryDecorationType.BACKGROUND
    )

    @Test
    fun `first five backgrounds are free`() {
        assertEquals(
            BatteryBackgroundAccess.OPEN,
            BatteryBackgroundAccessPolicy.resolve(
                background = background,
                catalogIndex = 4,
                isPremium = false,
                rewardUnlockedBackgroundIds = emptySet()
            )
        )
    }

    @Test
    fun `locked background offers reward or premium`() {
        assertEquals(
            BatteryBackgroundAccess.REWARD_OR_PREMIUM,
            BatteryBackgroundAccessPolicy.resolve(
                background = background,
                catalogIndex = 5,
                isPremium = false,
                rewardUnlockedBackgroundIds = emptySet()
            )
        )
    }

    @Test
    fun `reward or premium permanently opens locked background`() {
        assertEquals(
            BatteryBackgroundAccess.OPEN,
            BatteryBackgroundAccessPolicy.resolve(
                background = background,
                catalogIndex = 5,
                isPremium = false,
                rewardUnlockedBackgroundIds = setOf(background.id)
            )
        )
        assertEquals(
            BatteryBackgroundAccess.OPEN,
            BatteryBackgroundAccessPolicy.resolve(
                background = background,
                catalogIndex = 5,
                isPremium = true,
                rewardUnlockedBackgroundIds = emptySet()
            )
        )
    }

    @Test
    fun `background outside the active catalog is unavailable`() {
        assertEquals(
            BatteryBackgroundAccess.UNAVAILABLE,
            BatteryBackgroundAccessPolicy.resolve(
                background = background,
                catalogIndex = -1,
                isPremium = true,
                rewardUnlockedBackgroundIds = setOf(background.id)
            )
        )
    }
}
