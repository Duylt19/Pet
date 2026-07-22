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
}
