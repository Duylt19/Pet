package com.asianmobile.emojibattery.shimeji.battery.troll

import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryTrollEmojiSizeTest {
    private fun size(
        batterySizeDp: Float = 32f,
        emojiCanvasPx: Int = 420,
        batteryCanvasPx: Int = 508,
        fallbackEmojiSizeDp: Float = 24f
    ): Float = batteryTrollEmojiSizeDp(
        batterySizeDp = batterySizeDp,
        emojiCanvasPx = emojiCanvasPx,
        batteryCanvasPx = batteryCanvasPx,
        fallbackEmojiSizeDp = fallbackEmojiSizeDp
    )

    @Test
    fun `the character is scaled by the ratio between the two canvases`() {
        assertEquals(32f * 420f / 508f, size(), 0.001f)
    }

    @Test
    fun `frames sharing one canvas are drawn at the same size`() {
        assertEquals(32f, size(emojiCanvasPx = 508), 0.001f)
    }

    /** The whole point: the offset baked into the canvas only survives a shared scale. */
    @Test
    fun `resizing the shell resizes the character with it`() {
        val small = size(batterySizeDp = 20f)
        val large = size(batterySizeDp = 40f)
        assertEquals(2f, large / small, 0.001f)
    }

    @Test
    fun `a catalog that published no canvas leaves the slider in charge`() {
        assertEquals(24f, size(emojiCanvasPx = 0), 0.001f)
        assertEquals(24f, size(batteryCanvasPx = 0), 0.001f)
    }

    @Test
    fun `an unusable battery size cannot produce a zero or infinite character`() {
        assertEquals(24f, size(batterySizeDp = 0f), 0.001f)
        assertEquals(24f, size(batterySizeDp = Float.NaN), 0.001f)
    }
}
