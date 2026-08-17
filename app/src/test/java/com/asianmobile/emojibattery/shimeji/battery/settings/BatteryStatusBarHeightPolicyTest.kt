package com.asianmobile.emojibattery.shimeji.battery.settings

import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_BAR_HEIGHT_DP
import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryStatusBarHeightPolicyTest {
    @Test
    fun systemHeight_isTheExactCenterOfTheSliderRange() {
        val range = resolveBatteryStatusBarHeightRange(systemStatusBarHeightDp = 48f)

        assertEquals(24f, range.minimumDp)
        assertEquals(48f, range.defaultDp)
        assertEquals(72f, range.maximumDp)
        assertEquals(
            range.defaultDp - range.minimumDp,
            range.maximumDp - range.defaultDp
        )
    }

    @Test
    fun invalidSystemHeight_usesPortableFallbackAtTheCenter() {
        val range = resolveBatteryStatusBarHeightRange(Float.NaN)

        assertEquals(DEFAULT_BATTERY_BAR_HEIGHT_DP, range.defaultDp)
        assertEquals(
            range.defaultDp - range.minimumDp,
            range.maximumDp - range.defaultDp
        )
    }
}
