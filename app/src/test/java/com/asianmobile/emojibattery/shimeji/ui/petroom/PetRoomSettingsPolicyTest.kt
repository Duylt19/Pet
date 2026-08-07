package com.asianmobile.emojibattery.shimeji.ui.petroom

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
    fun `the default sits in the middle of the track`() {
        assertEquals(0.5f, PetRoomSettingsPolicy.fraction(100, PetRoomSettingsPolicy.SPEED_STEPS), 0.001f)
        assertEquals(0f, PetRoomSettingsPolicy.fraction(50, PetRoomSettingsPolicy.SPEED_STEPS), 0.001f)
        assertEquals(1f, PetRoomSettingsPolicy.fraction(150, PetRoomSettingsPolicy.SPEED_STEPS), 0.001f)
    }

    @Test
    fun `dragging only ever lands on a step`() {
        listOf(0f, 0.13f, 0.37f, 0.5f, 0.62f, 0.88f, 1f).forEach { fraction ->
            val value = PetRoomSettingsPolicy.valueAt(fraction, PetRoomSettingsPolicy.SPEED_STEPS)
            assertTrue("$value is not a step", value in PetRoomSettingsPolicy.SPEED_STEPS)
        }
    }

    @Test
    fun `dragging past either end clamps`() {
        assertEquals(50, PetRoomSettingsPolicy.valueAt(-2f, PetRoomSettingsPolicy.SPEED_STEPS))
        assertEquals(150, PetRoomSettingsPolicy.valueAt(9f, PetRoomSettingsPolicy.SPEED_STEPS))
    }

    @Test
    fun `a stored value off the step grid snaps to the nearest step`() {
        assertEquals(100, PetRoomSettingsPolicy.nearest(97, PetRoomSettingsPolicy.SPEED_STEPS))
        assertEquals(125, PetRoomSettingsPolicy.nearest(120, PetRoomSettingsPolicy.SPEED_STEPS))
        assertEquals(120, PetRoomSettingsPolicy.nearest(120, PetRoomSettingsPolicy.SIZE_STEPS))
    }
}
