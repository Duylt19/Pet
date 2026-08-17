package com.asianmobile.emojibattery.shimeji.ui.shared.component

import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_BAR_HEIGHT_DP
import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryStatusPreviewCardTest {

    @Test
    fun `preview height follows the edited status bar height`() {
        assertEquals(18f, resolveBatteryStatusPreviewHeightDp(18f, 24f))
        assertEquals(52f, resolveBatteryStatusPreviewHeightDp(52f, 24f))
    }

    @Test
    fun `invalid preview height falls back to the real system status bar height`() {
        assertEquals(24f, resolveBatteryStatusPreviewHeightDp(Float.NaN, 24f))
        assertEquals(24f, resolveBatteryStatusPreviewHeightDp(-1f, 24f))
    }

    @Test
    fun `invalid system height falls back to the portable default`() {
        assertEquals(
            DEFAULT_BATTERY_BAR_HEIGHT_DP,
            resolveBatteryStatusPreviewHeightDp(Float.NaN, Float.NaN)
        )
        assertEquals(
            DEFAULT_BATTERY_BAR_HEIGHT_DP,
            resolveBatteryStatusPreviewHeightDp(-1f, -1f)
        )
    }
}
