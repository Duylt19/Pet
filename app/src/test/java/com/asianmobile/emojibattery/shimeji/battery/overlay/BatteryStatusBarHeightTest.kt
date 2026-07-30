package com.asianmobile.emojibattery.shimeji.battery.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryStatusBarHeightTest {
    @Test
    fun configuredHeight_isConvertedDirectlyToPixels() {
        assertEquals(60, resolveBatteryStatusBarHeightPx(barHeightDp = 24f, density = 2.5f))
        assertEquals(118, resolveBatteryStatusBarHeightPx(barHeightDp = 47f, density = 2.5f))
    }

    @Test
    fun invalidInput_fallsBackToMinimumDrawableHeight() {
        assertEquals(
            1,
            resolveBatteryStatusBarHeightPx(barHeightDp = Float.NaN, density = 2.5f)
        )
        assertEquals(1, resolveBatteryStatusBarHeightPx(barHeightDp = 34f, density = 0f))
    }
}
