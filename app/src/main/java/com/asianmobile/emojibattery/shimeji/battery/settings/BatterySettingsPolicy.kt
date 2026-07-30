package com.asianmobile.emojibattery.shimeji.battery.settings

import com.asianmobile.emojibattery.shimeji.data.model.BUILT_IN_BATTERY_THEME_ID
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_BACKGROUND_COLOR
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_BAR_HEIGHT_DP
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_EMOJI_SIZE_DP
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_FOREGROUND_COLOR
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_HORIZONTAL_PADDING_DP
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_ICON_SIZE_DP
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_PRIVACY_RESERVE_DP
import com.asianmobile.emojibattery.shimeji.data.model.MAX_BATTERY_BAR_HEIGHT_DP
import com.asianmobile.emojibattery.shimeji.data.model.MIN_BATTERY_BAR_HEIGHT_DP

class BatterySettingsPolicy {
    fun sanitize(config: BatteryStatusConfig): BatteryStatusConfig = config.copy(
        selectedThemeId = config.selectedThemeId.coerceAtLeast(BUILT_IN_BATTERY_THEME_ID),
        backgroundDecorationId = config.backgroundDecorationId.coerceIn(0, 20),
        emotionDecorationId = config.emotionDecorationId.coerceIn(0, 20),
        barHeightDp = config.barHeightDp.validOr(DEFAULT_BATTERY_BAR_HEIGHT_DP)
            .coerceIn(MIN_BATTERY_BAR_HEIGHT_DP, MAX_BATTERY_BAR_HEIGHT_DP),
        horizontalPaddingDp = config.horizontalPaddingDp
            .validOr(DEFAULT_BATTERY_HORIZONTAL_PADDING_DP)
            .coerceIn(0f, 24f),
        emojiSizeDp = config.emojiSizeDp.validOr(DEFAULT_BATTERY_EMOJI_SIZE_DP)
            .coerceIn(12f, 36f),
        batterySizeDp = config.batterySizeDp.validOr(DEFAULT_BATTERY_ICON_SIZE_DP)
            .coerceIn(16f, 48f),
        privacyReserveDp = config.privacyReserveDp.validOr(DEFAULT_BATTERY_PRIVACY_RESERVE_DP)
            .coerceIn(48f, 128f),
        backgroundColorArgb = sanitizeColor(
            config.backgroundColorArgb,
            DEFAULT_BATTERY_BACKGROUND_COLOR
        ),
        foregroundColorArgb = sanitizeColor(
            config.foregroundColorArgb,
            DEFAULT_BATTERY_FOREGROUND_COLOR
        ),
        favoriteThemeIds = config.favoriteThemeIds.filterTo(mutableSetOf()) { it >= 0 }
    )

    private fun sanitizeColor(color: Int, fallback: Int): Int =
        if ((color ushr 24) == 0) fallback else color

    private fun Float.validOr(fallback: Float): Float =
        if (isFinite()) this else fallback
}
