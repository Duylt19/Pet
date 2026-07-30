package com.asianmobile.emojibattery.shimeji.battery.settings

import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_BACKGROUND_COLOR
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_BAR_HEIGHT_DP
import org.junit.Assert.assertEquals
import org.junit.Test

class BatterySettingsPolicyTest {
    private val policy = BatterySettingsPolicy()

    @Test
    fun sanitize_clamps_geometry_and_filters_favorites() {
        val sanitized = policy.sanitize(
            BatteryStatusConfig(
                selectedThemeId = -4,
                barHeightDp = 90f,
                horizontalPaddingDp = -1f,
                emojiSizeDp = Float.NaN,
                batterySizeDp = 2f,
                privacyReserveDp = 200f,
                favoriteThemeIds = setOf(-1, 0, 7)
            )
        )

        assertEquals(0, sanitized.selectedThemeId)
        assertEquals(48f, sanitized.barHeightDp)
        assertEquals(0f, sanitized.horizontalPaddingDp)
        assertEquals(24f, sanitized.emojiSizeDp)
        assertEquals(16f, sanitized.batterySizeDp)
        assertEquals(128f, sanitized.privacyReserveDp)
        assertEquals(setOf(0, 7), sanitized.favoriteThemeIds)
    }

    @Test
    fun sanitize_replaces_non_finite_and_transparent_values() {
        val sanitized = policy.sanitize(
            BatteryStatusConfig(
                barHeightDp = Float.POSITIVE_INFINITY,
                backgroundColorArgb = 0x00112233
            )
        )

        assertEquals(DEFAULT_BATTERY_BAR_HEIGHT_DP, sanitized.barHeightDp)
        assertEquals(DEFAULT_BATTERY_BACKGROUND_COLOR, sanitized.backgroundColorArgb)
    }
}
