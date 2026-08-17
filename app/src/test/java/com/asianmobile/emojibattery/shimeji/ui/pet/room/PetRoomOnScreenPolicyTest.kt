package com.asianmobile.emojibattery.shimeji.ui.pet.room

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetRoomOnScreenPolicyTest {
    @Test
    fun `a pet holding an enabled slot is on screen`() {
        assertTrue(
            PetRoomOnScreenPolicy.isOnScreen(
                slotPackKeys = listOf("a", "b", ""),
                slotEnabled = listOf(true, true, false),
                packKey = "b"
            )
        )
    }

    @Test
    fun `a pet in a disabled slot is not on screen`() {
        assertFalse(
            PetRoomOnScreenPolicy.isOnScreen(
                slotPackKeys = listOf("a", "b", ""),
                slotEnabled = listOf(true, false, false),
                packKey = "b"
            )
        )
    }

    @Test
    fun `turning on an existing pet only enables its slot`() {
        val action = PetRoomOnScreenPolicy.toggle(
            slotPackKeys = listOf("a", "b"),
            slotEnabled = listOf(true, false),
            packKey = "b",
            turnOn = true
        )

        assertEquals(PetRoomOnScreenAction.SetEnabled(1, true), action)
    }

    @Test
    fun `turning off an existing pet keeps its slot so its settings survive`() {
        val action = PetRoomOnScreenPolicy.toggle(
            slotPackKeys = listOf("a", "b"),
            slotEnabled = listOf(true, true),
            packKey = "b",
            turnOn = false
        )

        assertEquals(PetRoomOnScreenAction.SetEnabled(1, false), action)
    }

    @Test
    fun `turning off the last active pet is blocked`() {
        val action = PetRoomOnScreenPolicy.toggle(
            slotPackKeys = listOf("a", "b", ""),
            slotEnabled = listOf(false, true, false),
            packKey = "b",
            turnOn = false
        )

        assertEquals(PetRoomOnScreenAction.KeepLastActive, action)
    }

    @Test
    fun `blank enabled slots do not count as active pets`() {
        val action = PetRoomOnScreenPolicy.toggle(
            slotPackKeys = listOf("a", ""),
            slotEnabled = listOf(true, true),
            packKey = "a",
            turnOn = false
        )

        assertEquals(PetRoomOnScreenAction.KeepLastActive, action)
    }

    @Test
    fun `a new pet takes the first free slot`() {
        val action = PetRoomOnScreenPolicy.toggle(
            slotPackKeys = listOf("a", "", ""),
            slotEnabled = listOf(true, false, false),
            packKey = "c",
            turnOn = true
        )

        assertEquals(PetRoomOnScreenAction.Assign(1), action)
    }

    @Test
    fun `a full overlay roster refuses a new pet instead of evicting one`() {
        val action = PetRoomOnScreenPolicy.toggle(
            slotPackKeys = listOf("a", "b"),
            slotEnabled = listOf(true, true),
            packKey = "c",
            turnOn = true
        )

        assertEquals(PetRoomOnScreenAction.None, action)
    }

    @Test
    fun `turning off a pet that was never on screen does nothing`() {
        val action = PetRoomOnScreenPolicy.toggle(
            slotPackKeys = listOf("a", ""),
            slotEnabled = listOf(true, false),
            packKey = "c",
            turnOn = false
        )

        assertEquals(PetRoomOnScreenAction.None, action)
    }

    @Test
    fun `a request that matches the current state does nothing`() {
        val action = PetRoomOnScreenPolicy.toggle(
            slotPackKeys = listOf("a"),
            slotEnabled = listOf(true),
            packKey = "a",
            turnOn = true
        )

        assertEquals(PetRoomOnScreenAction.None, action)
    }
}
