package com.asianmobile.emojibattery.shimeji.battery.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryStatusHorizontalBoundsTest {
    @Test
    fun backgroundRemainsFullWidth_whileContentReservesSystemArea() {
        val bounds = resolveBatteryStatusHorizontalBounds(
            widthPx = 1000f,
            minimumContentRightPx = 100f,
            privacyReservePx = 72f
        )

        assertEquals(1000f, bounds.backgroundRightPx)
        assertEquals(928f, bounds.contentRightPx)
    }

    @Test
    fun excessiveReserve_neverProducesInvalidContentBounds() {
        val bounds = resolveBatteryStatusHorizontalBounds(
            widthPx = 320f,
            minimumContentRightPx = 48f,
            privacyReservePx = 500f
        )

        assertEquals(320f, bounds.backgroundRightPx)
        assertEquals(48f, bounds.contentRightPx)
    }
}
