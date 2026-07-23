package com.asianmobile.privatebrower.pet.pack

import org.junit.Assert.assertEquals
import org.junit.Test

class LegacyShimejiFrameSelectorTest {
    @Test
    fun `selector normalizes uppercase and renamed frames`() {
        val selected = LegacyShimejiFrameSelector.select(
            listOf("shime1.PNG", "shime2 (1).png", "notes.png~")
        )

        assertEquals("shime1.PNG", selected[1])
        assertEquals("shime2 (1).png", selected[2])
    }

    @Test
    fun `selector prefers canonical frame over alternatives`() {
        val selected = LegacyShimejiFrameSelector.select(
            listOf("shime4b.png", "shime4.PNG", "shime4.png")
        )

        assertEquals("shime4.png", selected[4])
    }

    @Test
    fun `legacy action contract preserves repeated motion frames`() {
        assertEquals(listOf(7, 5, 6, 8, 6), LegacyShimejiFrameContract.dragged)
        assertEquals(
            listOf(14, 14, 12, 13, 13, 13, 12, 14),
            LegacyShimejiFrameContract.wallClimb
        )
        assertEquals(
            listOf(25, 25, 23, 24, 24, 24, 23, 25),
            LegacyShimejiFrameContract.ceilingClimb
        )
        assertEquals(
            listOf(42, 43, 44, 45, 46, 45, 44, 43),
            LegacyShimejiFrameContract.special2
        )
    }

    @Test
    fun `extended action is available only when every required frame exists`() {
        assertEquals(
            true,
            LegacyShimejiFrameContract.isAvailable(
                LegacyShimejiFrameContract.wallClimb,
                setOf(12, 13, 14)
            )
        )
        assertEquals(
            false,
            LegacyShimejiFrameContract.isAvailable(
                LegacyShimejiFrameContract.wallClimb,
                setOf(12, 14)
            )
        )
    }
}
