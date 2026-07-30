package com.asianmobile.emojibattery.shimeji.battery.settings

import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusDisplayMode
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
                displayMode = BatteryStatusDisplayMode.BELOW_SYSTEM_BAR,
                selectedThemeId = -4,
                selectedBatteryThemeId = -5,
                selectedEmojiThemeId = -6,
                backgroundDecorationId = 99,
                emotionDecorationId = -4,
                barHeightDp = 90f,
                horizontalPaddingDp = -1f,
                leftPaddingDp = 60f,
                rightPaddingDp = -10f,
                emojiSizeDp = Float.NaN,
                batterySizeDp = 2f,
                wifiSizeDp = 99f,
                chargeIconIndex = 20,
                animationAssetName = "../bad.json",
                privacyReserveDp = 200f,
                favoriteThemeIds = setOf(-1, 0, 7),
                rewardUnlockedThemeIds = setOf(-1, 0, 8)
            )
        )

        assertEquals(0, sanitized.selectedThemeId)
        assertEquals(0, sanitized.selectedBatteryThemeId)
        assertEquals(0, sanitized.selectedEmojiThemeId)
        assertEquals(BatteryStatusDisplayMode.COVER_SYSTEM_BAR, sanitized.displayMode)
        assertEquals(20, sanitized.backgroundDecorationId)
        assertEquals(0, sanitized.emotionDecorationId)
        assertEquals(48f, sanitized.barHeightDp)
        assertEquals(0f, sanitized.horizontalPaddingDp)
        assertEquals(32f, sanitized.leftPaddingDp)
        assertEquals(0f, sanitized.rightPaddingDp)
        assertEquals(24f, sanitized.emojiSizeDp)
        assertEquals(16f, sanitized.batterySizeDp)
        assertEquals(32f, sanitized.wifiSizeDp)
        assertEquals(12, sanitized.chargeIconIndex)
        assertEquals("cute_1.json", sanitized.animationAssetName)
        assertEquals(128f, sanitized.privacyReserveDp)
        assertEquals(setOf(0, 7), sanitized.favoriteThemeIds)
        assertEquals(setOf(8), sanitized.rewardUnlockedThemeIds)
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
