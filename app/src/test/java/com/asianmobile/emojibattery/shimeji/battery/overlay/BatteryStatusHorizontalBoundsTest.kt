package com.asianmobile.emojibattery.shimeji.battery.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryStatusHorizontalBoundsTest {
    @Test
    fun backgroundAndContent_useFullOverlayWidth() {
        val bounds = resolveBatteryStatusHorizontalBounds(widthPx = 1000f)

        assertEquals(1000f, bounds.backgroundRightPx)
        assertEquals(1000f, bounds.contentRightPx)
    }

    @Test
    fun negativeWidth_isClampedToZero() {
        val bounds = resolveBatteryStatusHorizontalBounds(widthPx = -1f)

        assertEquals(0f, bounds.backgroundRightPx)
        assertEquals(0f, bounds.contentRightPx)
    }
}
