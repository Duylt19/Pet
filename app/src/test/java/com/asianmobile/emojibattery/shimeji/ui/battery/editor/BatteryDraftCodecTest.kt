package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import com.asianmobile.emojibattery.shimeji.data.model.BatteryDataType
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDateFont
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDateFormat
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusDisplayMode
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_THEME_ID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryDraftCodecTest {
    @Test
    fun codec_roundTrips_complete_editor_draft() {
        val config = BatteryStatusConfig(
            enabled = true,
            hasApplied = true,
            selectedThemeId = 42,
            selectedBatteryThemeId = 27,
            selectedEmojiThemeId = 35,
            displayMode = BatteryStatusDisplayMode.BELOW_SYSTEM_BAR,
            showTime = false,
            showPercentage = false,
            backgroundDecorationId = 9,
            showEmotion = false,
            emotionDecorationId = 8,
            showAnimation = false,
            animationAssetName = "18.gif",
            barHeightDp = 47f,
            horizontalPaddingDp = 4f,
            leftPaddingDp = 5f,
            rightPaddingDp = 6f,
            percentSizeDp = 23f,
            emojiSizeDp = 35f,
            animationSizeDp = 34f,
            batterySizeDp = 44f,
            backgroundColorArgb = 0xFF112233.toInt(),
            foregroundColorArgb = 0xFF445566.toInt(),
            percentColorArgb = 0xFF778899.toInt(),
            showWifi = false,
            wifiSizeDp = 21f,
            wifiColorArgb = 0xFF102030.toInt(),
            wifiIconStyleIndex = 2,
            dataType = BatteryDataType.G5,
            showData = false,
            dataSizeDp = 20f,
            dataColorArgb = 0xFF203040.toInt(),
            showSignal = false,
            signalSizeDp = 22f,
            signalColorArgb = 0xFF304050.toInt(),
            signalIconStyleIndex = 3,
            airplaneSizeDp = 18f,
            showAirplane = false,
            airplaneColorArgb = 0xFF405060.toInt(),
            airplaneIconStyleIndex = 4,
            hotspotSizeDp = 17f,
            showHotspot = false,
            hotspotColorArgb = 0xFF506070.toInt(),
            hotspotIconStyleIndex = 2,
            ringerSizeDp = 16f,
            showRinger = false,
            ringerColorArgb = 0xFF607080.toInt(),
            ringerIconStyleIndex = 3,
            chargeSizeDp = 15f,
            showCharge = false,
            chargeIconIndex = 12,
            chargeColorArgb = 0xFF708090.toInt(),
            showDateTime = true,
            dateTimeColorArgb = 0xFF8090A0.toInt(),
            dateTimeSizeDp = 19f,
            dateFormat = BatteryDateFormat.WEEKDAY_FULL,
            dateTimeFont = BatteryDateFont.DANCING_SCRIPT,
            clockColorArgb = 0xFFA0B0C0.toInt(),
            clockSizeDp = 21f,
            privacyReserveDp = 88f,
            favoriteThemeIds = setOf(1, 9, 42),
            rewardUnlockedThemeIds = setOf(4, 8, 42),
            rewardUnlockedBackgroundIds = setOf(9, 18, 38)
        )

        assertEquals(config, BatteryDraftCodec.decode(BatteryDraftCodec.encode(config)))
    }

    @Test
    fun codec_returns_null_for_corrupt_or_unsupported_state() {
        assertNull(BatteryDraftCodec.decode("not-json"))
        assertNull(BatteryDraftCodec.decode("""{"schema":99}"""))
    }

    @Test
    fun codec_uses_fallback_for_unknown_enum_from_current_state() {
        val fallback = BatteryStatusConfig(
            hasApplied = true,
            dataType = BatteryDataType.G4,
            wifiIconStyleIndex = 4
        )
        val restored = BatteryDraftCodec.decode(
            """{"schema":5,"dataType":"NOT_A_NETWORK"}""",
            fallback
        )

        assertEquals(BatteryDataType.G4, restored?.dataType)
        assertEquals(4, restored?.wifiIconStyleIndex)
        assertEquals(true, restored?.hasApplied)
    }

    @Test
    fun codec_rejects_legacy_state_after_background_id_refactor() {
        val restored = BatteryDraftCodec.decode(
            """{"schema":1,"selectedThemeId":42}"""
        )

        assertNull(restored)
    }

    @Test
    fun codec_migrates_rendererFallbackThemeToFirstSelectableTheme() {
        val restored = BatteryDraftCodec.decode(
            """{
                "schema":5,
                "selectedThemeId":0,
                "selectedBatteryThemeId":0,
                "selectedEmojiThemeId":0
            }""".trimIndent()
        )

        assertEquals(DEFAULT_BATTERY_THEME_ID, restored?.selectedThemeId)
        assertEquals(DEFAULT_BATTERY_THEME_ID, restored?.selectedBatteryThemeId)
        assertEquals(DEFAULT_BATTERY_THEME_ID, restored?.selectedEmojiThemeId)
    }
}
