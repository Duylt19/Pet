package com.asianmobile.privatebrower.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PetPreferencesTest {
    @Test
    fun `slot selection falls back to first configured pet`() {
        val preferences = PetPreferences(
            selectedPackKeys = listOf("pack.cat@1", "pack.dog@2")
        )

        assertEquals("pack.cat@1", preferences.packKeyForSlot(0))
        assertEquals("pack.dog@2", preferences.packKeyForSlot(1))
        assertEquals("pack.cat@1", preferences.packKeyForSlot(2))
    }

    @Test
    fun `empty selection falls back to built in pet`() {
        assertEquals(
            DEFAULT_SELECTED_PACK_KEY,
            PetPreferences(selectedPackKeys = emptyList()).packKeyForSlot(0)
        )
    }
}
