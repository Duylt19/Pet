package com.asianmobile.emojibattery.shimeji.data.model

data class BatteryStatusConfig(
    val enabled: Boolean = false,
    val selectedThemeId: Int = BUILT_IN_BATTERY_THEME_ID,
    val displayMode: BatteryStatusDisplayMode = BatteryStatusDisplayMode.COVER_SYSTEM_BAR,
    val showTime: Boolean = true,
    val showPercentage: Boolean = true,
    val barHeightDp: Float = DEFAULT_BATTERY_BAR_HEIGHT_DP,
    val horizontalPaddingDp: Float = DEFAULT_BATTERY_HORIZONTAL_PADDING_DP,
    val emojiSizeDp: Float = DEFAULT_BATTERY_EMOJI_SIZE_DP,
    val batterySizeDp: Float = DEFAULT_BATTERY_ICON_SIZE_DP,
    val backgroundColorArgb: Int = DEFAULT_BATTERY_BACKGROUND_COLOR,
    val foregroundColorArgb: Int = DEFAULT_BATTERY_FOREGROUND_COLOR,
    val privacyReserveDp: Float = DEFAULT_BATTERY_PRIVACY_RESERVE_DP,
    val favoriteThemeIds: Set<Int> = emptySet()
)

enum class BatteryStatusDisplayMode {
    BELOW_SYSTEM_BAR,
    COVER_SYSTEM_BAR
}

const val MIN_BATTERY_BAR_HEIGHT_DP = 24f
const val MAX_BATTERY_BAR_HEIGHT_DP = 48f
const val DEFAULT_BATTERY_BAR_HEIGHT_DP = 34f
const val DEFAULT_BATTERY_HORIZONTAL_PADDING_DP = 12f
const val DEFAULT_BATTERY_EMOJI_SIZE_DP = 24f
const val DEFAULT_BATTERY_ICON_SIZE_DP = 32f
const val DEFAULT_BATTERY_PRIVACY_RESERVE_DP = 72f
const val DEFAULT_BATTERY_BACKGROUND_COLOR = 0xFFE0F7F1.toInt()
const val DEFAULT_BATTERY_FOREGROUND_COLOR = 0xFF111827.toInt()
