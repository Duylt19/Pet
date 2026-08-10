package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusBarColorPickerPolicyTest {

    @Test
    fun `ARGB round trip preserves color and alpha`() {
        val source = 0x73D8F3C9

        val hsv = argbToHsvAlpha(source)
        val result = hsvAlphaToArgb(
            hue = hsv.hue,
            saturation = hsv.saturation,
            brightness = hsv.brightness,
            alpha = hsv.alpha
        )

        assertEquals(source, result)
    }

    @Test
    fun `HSV conversion clamps values to supported bounds`() {
        val result = hsvAlphaToArgb(
            hue = 400f,
            saturation = 2f,
            brightness = -1f,
            alpha = 3f
        )

        assertEquals(255, result ushr 24 and 0xFF)
        assertTrue(result and 0x00FFFFFF in 0..0x00FFFFFF)
    }
}
