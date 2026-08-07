package com.asianmobile.emojibattery.shimeji.pet.room

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetRoomSizePolicyTest {
    @Test
    fun `the default size is the overlay's own default`() {
        assertEquals(84, PetRoomSizePolicy.petSizeDp(packDefaultScale = 1f, sizePercent = 100))
    }

    @Test
    fun `size scales with the setting`() {
        assertEquals(67, PetRoomSizePolicy.petSizeDp(packDefaultScale = 1f, sizePercent = 80))
        assertEquals(126, PetRoomSizePolicy.petSizeDp(packDefaultScale = 1f, sizePercent = 150))
    }

    @Test
    fun `the smallest setting still meets the floor the overlay enforces`() {
        // 84 x 0.5 is 42, below the 48dp floor, so the two smallest steps look alike.
        assertEquals(
            PetRoomSizePolicy.MIN_PET_SIZE_DP,
            PetRoomSizePolicy.petSizeDp(packDefaultScale = 1f, sizePercent = 50)
        )
    }

    @Test
    fun `a pack scale of its own is respected`() {
        assertEquals(144, PetRoomSizePolicy.petSizeDp(packDefaultScale = 2f, sizePercent = 100))
    }

    @Test
    fun `the clamp keeps a pet usable at either extreme`() {
        assertEquals(
            PetRoomSizePolicy.MAX_PET_SIZE_DP,
            PetRoomSizePolicy.petSizeDp(packDefaultScale = 4f, sizePercent = 150)
        )
        assertEquals(
            PetRoomSizePolicy.MIN_PET_SIZE_DP,
            PetRoomSizePolicy.petSizeDp(packDefaultScale = 0.1f, sizePercent = 50)
        )
    }

    @Test
    fun `pixels follow the display density`() {
        val onePx = PetRoomSizePolicy.petSizePixels(1f, 100, density = 1f)
        val threePx = PetRoomSizePolicy.petSizePixels(1f, 100, density = 3f)

        assertEquals(onePx * 3f, threePx, 0.001f)
    }

    @Test
    fun `speed reads as a multiplier and stays in range`() {
        assertEquals(1f, PetRoomSizePolicy.speedMultiplier(100), 0.001f)
        assertEquals(0.5f, PetRoomSizePolicy.speedMultiplier(50), 0.001f)
        assertEquals(1.5f, PetRoomSizePolicy.speedMultiplier(150), 0.001f)
        assertTrue(PetRoomSizePolicy.speedMultiplier(9_000) <= 1.5f)
        assertTrue(PetRoomSizePolicy.speedMultiplier(-4) >= 0.5f)
    }
}
