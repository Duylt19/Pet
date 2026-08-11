package com.asianmobile.emojibattery.shimeji.ui.pet.room

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetRoomSettingsPolicyTest {
    @Test
    fun `steps match what the pet settings accept`() {
        assertEquals(listOf(50, 75, 100, 125, 150), PetRoomSettingsPolicy.SPEED_STEPS)
        assertEquals(
            listOf(50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150),
            PetRoomSettingsPolicy.SIZE_STEPS
        )
    }

    @Test
    fun `the label reads as a multiplier`() {
        assertEquals("1.0x", PetRoomSettingsPolicy.label(100))
        assertEquals("0.5x", PetRoomSettingsPolicy.label(50))
        assertEquals("1.5x", PetRoomSettingsPolicy.label(150))
        assertEquals("0.7x", PetRoomSettingsPolicy.label(75))
    }

    @Test
    fun `a stored value off the step grid snaps to the nearest step`() {
        assertEquals(100, PetRoomSettingsPolicy.nearest(97, PetRoomSettingsPolicy.SPEED_STEPS))
        assertEquals(125, PetRoomSettingsPolicy.nearest(120, PetRoomSettingsPolicy.SPEED_STEPS))
        assertEquals(120, PetRoomSettingsPolicy.nearest(120, PetRoomSettingsPolicy.SIZE_STEPS))
    }
}
