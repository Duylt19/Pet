package com.asianmobile.emojibattery.shimeji.pet.overlay

import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_SELECTED_PACK_KEY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetOverlayRosterPolicyTest {
    @Test
    fun `a fresh install has no pet the user chose`() {
        assertFalse(
            PetOverlayRosterPolicy.hasChosenPet(
                slotPackKeys = listOf(DEFAULT_SELECTED_PACK_KEY, "", ""),
                slotEnabled = listOf(true, true, true),
                petCount = 1
            )
        )
    }

    @Test
    fun `an enabled owned pet counts`() {
        assertTrue(
            PetOverlayRosterPolicy.hasChosenPet(
                slotPackKeys = listOf("owner.shimeji.42@1", ""),
                slotEnabled = listOf(true, false),
                petCount = 1
            )
        )
    }

    @Test
    fun `a pet switched off does not count`() {
        assertFalse(
            PetOverlayRosterPolicy.hasChosenPet(
                slotPackKeys = listOf("owner.shimeji.42@1", ""),
                slotEnabled = listOf(false, false),
                petCount = 1
            )
        )
    }

    @Test
    fun `a pet beyond the roster does not count`() {
        assertFalse(
            PetOverlayRosterPolicy.hasChosenPet(
                slotPackKeys = listOf(DEFAULT_SELECTED_PACK_KEY, "owner.shimeji.42@1"),
                slotEnabled = listOf(true, true),
                petCount = 1
            )
        )
    }

    @Test
    fun `the built-in slot is free for the first pet turned on`() {
        val keys = PetOverlayRosterPolicy.freeableSlotKeys(
            slotPackKeys = listOf(DEFAULT_SELECTED_PACK_KEY, DEFAULT_SELECTED_PACK_KEY),
            petCount = 1
        )

        assertEquals(listOf("", ""), keys)
    }

    @Test
    fun `a chosen pet keeps its slot`() {
        val keys = PetOverlayRosterPolicy.freeableSlotKeys(
            slotPackKeys = listOf("owner.shimeji.42@1", DEFAULT_SELECTED_PACK_KEY),
            petCount = 2
        )

        assertEquals(listOf("owner.shimeji.42@1", ""), keys)
    }
}
