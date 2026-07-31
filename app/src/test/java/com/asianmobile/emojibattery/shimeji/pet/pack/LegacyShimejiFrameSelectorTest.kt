package com.asianmobile.emojibattery.shimeji.pet.pack

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
        assertEquals(
            listOf(1, 38, 39, 40, 41),
            LegacyShimejiFrameContract.special
        )
        assertEquals(listOf(34, 35, 34, 36), LegacyShimejiFrameContract.talk)
        assertEquals(listOf(34), LegacyShimejiFrameContract.talkStill)
        assertEquals(listOf(34, 35, 34, 36), LegacyShimejiFrameContract.talkWalk)
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

    @Test
    fun `partial special keeps real transformation frames instead of disabling the action`() {
        val leviSequence = LegacyShimejiFrameContract.availableSpecialSequence(
            sequence = LegacyShimejiFrameContract.special,
            specialFrameRange = LegacyShimejiFrameContract.specialFrameRange,
            availableFrames = setOf(1, 40)
        )
        val noSpecialSequence = LegacyShimejiFrameContract.availableSpecialSequence(
            sequence = LegacyShimejiFrameContract.special,
            specialFrameRange = LegacyShimejiFrameContract.specialFrameRange,
            availableFrames = setOf(1)
        )

        assertEquals(listOf(1, 40), leviSequence)
        assertEquals(null, noSpecialSequence)
    }

    @Test
    fun `original WC 2026 frame contract enables real movement and special actions`() {
        val wcFrames = (
            (1..9).toSet() +
                (11..14).toSet() +
                setOf(18, 19) +
                (38..46).toSet()
            )

        assertEquals(24, wcFrames.size)
        assertEquals(
            true,
            LegacyShimejiFrameContract.isAvailable(
                LegacyShimejiFrameContract.dragged,
                wcFrames
            )
        )
        assertEquals(
            true,
            LegacyShimejiFrameContract.isAvailable(
                LegacyShimejiFrameContract.wallClimb,
                wcFrames
            )
        )
        assertEquals(
            LegacyShimejiFrameContract.special,
            LegacyShimejiFrameContract.availableSpecialSequence(
                LegacyShimejiFrameContract.special,
                LegacyShimejiFrameContract.specialFrameRange,
                wcFrames
            )
        )
        assertEquals(
            LegacyShimejiFrameContract.special2,
            LegacyShimejiFrameContract.availableSpecialSequence(
                LegacyShimejiFrameContract.special2,
                LegacyShimejiFrameContract.special2FrameRange,
                wcFrames
            )
        )
        assertEquals(
            false,
            LegacyShimejiFrameContract.isAvailable(
                LegacyShimejiFrameContract.talk,
                wcFrames
            )
        )
    }
}
