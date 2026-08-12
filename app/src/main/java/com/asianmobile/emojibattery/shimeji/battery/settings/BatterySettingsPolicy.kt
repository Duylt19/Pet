package com.asianmobile.emojibattery.shimeji.battery.settings

import com.asianmobile.emojibattery.shimeji.data.model.BUILT_IN_BATTERY_THEME_ID
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusDisplayMode
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_BACKGROUND_COLOR
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_ANIMATION_ASSET
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_ANIMATION_SIZE_DP
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_BAR_HEIGHT_DP
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_EMOJI_SIZE_DP
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_FOREGROUND_COLOR
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_HORIZONTAL_PADDING_DP
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_SIDE_PADDING_DP
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_ICON_SIZE_DP
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_PERCENT_SIZE_DP
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_PRIVACY_RESERVE_DP
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_STATUS_ICON_COLOR
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_STATUS_ICON_SIZE_DP
import com.asianmobile.emojibattery.shimeji.data.model.MAX_BATTERY_STATUS_ICON_STYLE_INDEX
import com.asianmobile.emojibattery.shimeji.data.model.MAX_BATTERY_EMOTION_ID
import com.asianmobile.emojibattery.shimeji.data.model.MIN_BATTERY_STATUS_ICON_STYLE_INDEX

class BatterySettingsPolicy(
    private val barHeightRange: BatteryStatusBarHeightRange =
        resolveBatteryStatusBarHeightRange(DEFAULT_BATTERY_BAR_HEIGHT_DP)
) {
    fun sanitize(config: BatteryStatusConfig): BatteryStatusConfig = config.copy(
        // The shipped product deliberately supports the status-bar replacement flow only.
        // Migrating legacy BELOW values prevents an Accessibility overlay from covering app UI.
        displayMode = BatteryStatusDisplayMode.COVER_SYSTEM_BAR,
        selectedThemeId = config.selectedThemeId.coerceAtLeast(BUILT_IN_BATTERY_THEME_ID),
        selectedBatteryThemeId = config.selectedBatteryThemeId
            .coerceAtLeast(BUILT_IN_BATTERY_THEME_ID),
        selectedEmojiThemeId = config.selectedEmojiThemeId
            .coerceAtLeast(BUILT_IN_BATTERY_THEME_ID),
        backgroundDecorationId = config.backgroundDecorationId.coerceIn(0, 20),
        emotionDecorationId = config.emotionDecorationId.coerceIn(0, MAX_BATTERY_EMOTION_ID),
        animationAssetName = config.animationAssetName
            .takeIf(ANIMATION_FILE_NAME::matches)
            ?: DEFAULT_BATTERY_ANIMATION_ASSET,
        barHeightDp = config.barHeightDp.validOr(barHeightRange.defaultDp)
            .coerceIn(barHeightRange.minimumDp, barHeightRange.maximumDp),
        horizontalPaddingDp = config.horizontalPaddingDp
            .validOr(DEFAULT_BATTERY_HORIZONTAL_PADDING_DP)
            .coerceIn(0f, 24f),
        leftPaddingDp = config.leftPaddingDp.validOr(DEFAULT_BATTERY_SIDE_PADDING_DP)
            .coerceIn(0f, 32f),
        rightPaddingDp = config.rightPaddingDp.validOr(DEFAULT_BATTERY_SIDE_PADDING_DP)
            .coerceIn(0f, 32f),
        emojiSizeDp = config.emojiSizeDp.validOr(DEFAULT_BATTERY_EMOJI_SIZE_DP)
            .coerceIn(12f, 36f),
        percentSizeDp = config.percentSizeDp.validOr(DEFAULT_BATTERY_PERCENT_SIZE_DP)
            .coerceIn(10f, 32f),
        animationSizeDp = config.animationSizeDp.validOr(DEFAULT_BATTERY_ANIMATION_SIZE_DP)
            .coerceIn(12f, 48f),
        batterySizeDp = config.batterySizeDp.validOr(DEFAULT_BATTERY_ICON_SIZE_DP)
            .coerceIn(16f, 48f),
        wifiSizeDp = config.wifiSizeDp.statusSize(),
        dataSizeDp = config.dataSizeDp.statusSize(),
        signalSizeDp = config.signalSizeDp.statusSize(),
        airplaneSizeDp = config.airplaneSizeDp.statusSize(),
        hotspotSizeDp = config.hotspotSizeDp.statusSize(),
        ringerSizeDp = config.ringerSizeDp.statusSize(),
        chargeSizeDp = config.chargeSizeDp.statusSize(),
        dateTimeSizeDp = config.dateTimeSizeDp.statusSize(),
        clockSizeDp = config.clockSizeDp.statusSize(),
        wifiIconStyleIndex = config.wifiIconStyleIndex.statusIconStyle(),
        signalIconStyleIndex = config.signalIconStyleIndex.statusIconStyle(),
        airplaneIconStyleIndex = config.airplaneIconStyleIndex.statusIconStyle(),
        hotspotIconStyleIndex = config.hotspotIconStyleIndex.statusIconStyle(),
        ringerIconStyleIndex = config.ringerIconStyleIndex.statusIconStyle(),
        chargeIconIndex = config.chargeIconIndex.coerceIn(1, 12),
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
        percentColorArgb = config.percentColorArgb.statusColor(),
        wifiColorArgb = config.wifiColorArgb.statusColor(),
        dataColorArgb = config.dataColorArgb.statusColor(),
        signalColorArgb = config.signalColorArgb.statusColor(),
        airplaneColorArgb = config.airplaneColorArgb.statusColor(),
        hotspotColorArgb = config.hotspotColorArgb.statusColor(),
        ringerColorArgb = config.ringerColorArgb.statusColor(),
        chargeColorArgb = config.chargeColorArgb.statusColor(),
        dateTimeColorArgb = config.dateTimeColorArgb.statusColor(),
        clockColorArgb = config.clockColorArgb.statusColor(),
        favoriteThemeIds = config.favoriteThemeIds.filterTo(mutableSetOf()) { it >= 0 },
        rewardUnlockedThemeIds = config.rewardUnlockedThemeIds
            .filterTo(mutableSetOf()) { it > BUILT_IN_BATTERY_THEME_ID }
    )

    private fun sanitizeColor(color: Int, fallback: Int): Int =
        if ((color ushr 24) == 0) fallback else color

    private fun Float.validOr(fallback: Float): Float =
        if (isFinite()) this else fallback

    private fun Float.statusSize(): Float =
        validOr(DEFAULT_BATTERY_STATUS_ICON_SIZE_DP).coerceIn(8f, 32f)

    private fun Int.statusColor(): Int =
        sanitizeColor(this, DEFAULT_BATTERY_STATUS_ICON_COLOR)

    private fun Int.statusIconStyle(): Int =
        coerceIn(
            MIN_BATTERY_STATUS_ICON_STYLE_INDEX,
            MAX_BATTERY_STATUS_ICON_STYLE_INDEX
        )

    private companion object {
        val ANIMATION_FILE_NAME = Regex("(?:cute_[1-5]\\.json|(?:[1-9]|1[0-9]|2[01])\\.gif)")
    }
}
