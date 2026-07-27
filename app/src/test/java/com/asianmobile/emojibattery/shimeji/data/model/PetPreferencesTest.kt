package com.asianmobile.emojibattery.shimeji.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PetPreferencesTest {
    @Test
    fun `slot selection falls back to first configured pet`() {
        val preferences = PetPreferences(
            petSlots = listOf(
                PetSlotPreferences(packKey = "pack.cat@1"),
                PetSlotPreferences(packKey = "pack.dog@2")
            )
        )

        assertEquals("pack.cat@1", preferences.packKeyForSlot(0))
        assertEquals("pack.dog@2", preferences.packKeyForSlot(1))
        assertEquals("pack.cat@1", preferences.packKeyForSlot(2))
    }

    @Test
    fun `empty selection falls back to built in pet`() {
        assertEquals(
            DEFAULT_SELECTED_PACK_KEY,
            PetPreferences(petSlots = emptyList()).packKeyForSlot(0)
        )
    }

    @Test
    fun `each slot keeps independent runtime customization`() {
        val preferences = PetPreferences(
            petSlots = listOf(
                PetSlotPreferences(sizePercent = 75, messagesEnabled = false),
                PetSlotPreferences(sizePercent = 150, messagesEnabled = true)
            )
        )

        assertEquals(75, preferences.slot(0).sizePercent)
        assertEquals(false, preferences.slot(0).messagesEnabled)
        assertEquals(150, preferences.slot(1).sizePercent)
        assertEquals(true, preferences.slot(1).messagesEnabled)
    }
}
