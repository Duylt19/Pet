package com.asianmobile.emojibattery.shimeji.data.model

data class BatteryStatusConfig(
    val enabled: Boolean = false,
    val hasApplied: Boolean = false,
    val selectedThemeId: Int = BUILT_IN_BATTERY_THEME_ID,
    val selectedBatteryThemeId: Int = selectedThemeId,
    val selectedEmojiThemeId: Int = selectedThemeId,
    val displayMode: BatteryStatusDisplayMode = BatteryStatusDisplayMode.COVER_SYSTEM_BAR,
    val showTime: Boolean = true,
    val showPercentage: Boolean = true,
    val backgroundDecorationId: Int = DEFAULT_BATTERY_BACKGROUND_ID,
    val showEmotion: Boolean = true,
    val emotionDecorationId: Int = DEFAULT_BATTERY_EMOTION_ID,
    val showAnimation: Boolean = true,
    val animationAssetName: String = DEFAULT_BATTERY_ANIMATION_ASSET,
    val barHeightDp: Float = DEFAULT_BATTERY_BAR_HEIGHT_DP,
    val horizontalPaddingDp: Float = DEFAULT_BATTERY_HORIZONTAL_PADDING_DP,
    val leftPaddingDp: Float = DEFAULT_BATTERY_SIDE_PADDING_DP,
    val rightPaddingDp: Float = DEFAULT_BATTERY_SIDE_PADDING_DP,
    val percentSizeDp: Float = DEFAULT_BATTERY_PERCENT_SIZE_DP,
    val emojiSizeDp: Float = DEFAULT_BATTERY_EMOJI_SIZE_DP,
    val animationSizeDp: Float = DEFAULT_BATTERY_ANIMATION_SIZE_DP,
    val batterySizeDp: Float = DEFAULT_BATTERY_ICON_SIZE_DP,
    val backgroundColorArgb: Int = DEFAULT_BATTERY_BACKGROUND_COLOR,
    val foregroundColorArgb: Int = DEFAULT_BATTERY_FOREGROUND_COLOR,
    val percentColorArgb: Int = DEFAULT_BATTERY_STATUS_ICON_COLOR,
    val wifiSizeDp: Float = DEFAULT_BATTERY_STATUS_ICON_SIZE_DP,
    val wifiColorArgb: Int = DEFAULT_BATTERY_STATUS_ICON_COLOR,
    val wifiIconStyleIndex: Int = DEFAULT_BATTERY_WIFI_ICON_STYLE_INDEX,
    val dataType: BatteryDataType = BatteryDataType.G2,
    val dataSizeDp: Float = DEFAULT_BATTERY_STATUS_ICON_SIZE_DP,
    val dataColorArgb: Int = DEFAULT_BATTERY_STATUS_ICON_COLOR,
    val signalSizeDp: Float = DEFAULT_BATTERY_STATUS_ICON_SIZE_DP,
    val signalColorArgb: Int = DEFAULT_BATTERY_STATUS_ICON_COLOR,
    val signalIconStyleIndex: Int = DEFAULT_BATTERY_STATUS_ICON_STYLE_INDEX,
    val airplaneSizeDp: Float = DEFAULT_BATTERY_STATUS_ICON_SIZE_DP,
    val showAirplane: Boolean = true,
    val airplaneColorArgb: Int = DEFAULT_BATTERY_STATUS_ICON_COLOR,
    val airplaneIconStyleIndex: Int = DEFAULT_BATTERY_STATUS_ICON_STYLE_INDEX,
    val hotspotSizeDp: Float = DEFAULT_BATTERY_STATUS_ICON_SIZE_DP,
    val showHotspot: Boolean = true,
    val hotspotColorArgb: Int = DEFAULT_BATTERY_STATUS_ICON_COLOR,
    val hotspotIconStyleIndex: Int = DEFAULT_BATTERY_HOTSPOT_ICON_STYLE_INDEX,
    val ringerSizeDp: Float = DEFAULT_BATTERY_STATUS_ICON_SIZE_DP,
    val showRinger: Boolean = true,
    val ringerColorArgb: Int = DEFAULT_BATTERY_STATUS_ICON_COLOR,
    val ringerIconStyleIndex: Int = DEFAULT_BATTERY_STATUS_ICON_STYLE_INDEX,
    val chargeSizeDp: Float = DEFAULT_BATTERY_STATUS_ICON_SIZE_DP,
    val showCharge: Boolean = true,
    val chargeIconIndex: Int = DEFAULT_BATTERY_CHARGE_ICON_INDEX,
    val chargeColorArgb: Int = DEFAULT_BATTERY_STATUS_ICON_COLOR,
    val showDateTime: Boolean = false,
    val dateTimeColorArgb: Int = DEFAULT_BATTERY_STATUS_ICON_COLOR,
    val dateTimeSizeDp: Float = DEFAULT_BATTERY_STATUS_ICON_SIZE_DP,
    val dateFormat: BatteryDateFormat = BatteryDateFormat.WEEKDAY_MONTH_DAY,
    val dateTimeFont: BatteryDateFont = BatteryDateFont.MEDIUM,
    val clockColorArgb: Int = DEFAULT_BATTERY_STATUS_ICON_COLOR,
    val clockSizeDp: Float = DEFAULT_BATTERY_STATUS_ICON_SIZE_DP,
    val privacyReserveDp: Float = DEFAULT_BATTERY_PRIVACY_RESERVE_DP,
    val favoriteThemeIds: Set<Int> = emptySet(),
    val rewardUnlockedThemeIds: Set<Int> = emptySet()
)

enum class BatteryStatusDisplayMode {
    BELOW_SYSTEM_BAR,
    COVER_SYSTEM_BAR
}

enum class BatteryDataType(val label: String) {
    G2("2G"),
    G3("3G"),
    G4("4G"),
    G5("5G"),
    G6("6G"),
    G7("7G"),
    G8("8G"),
    G9("9G")
}

enum class BatteryDateFormat(val pattern: String) {
    WEEKDAY_MONTH_DAY("EEE, MMM dd"),
    WEEKDAY_DAY("EEE, dd"),
    MONTH_DAY("MMM dd"),
    WEEKDAY_FULL("EEEE")
}

enum class BatteryDateFont(
    val displayName: String,
    val resourceName: String
) {
    MEDIUM("Medium", "roboto_medium"),
    BOLD("Bold", "roboto_bold"),
    RUSSO_ONE("Russo One", "russo_one_regular"),
    NUNITO("Nunito", "nunito_bold"),
    COINY("Coiny", "coiny_regular"),
    DANCING_SCRIPT("Dancing Script", "dancing_script_bold")
}

const val MIN_BATTERY_BAR_HEIGHT_DP = 8f
const val MAX_BATTERY_BAR_HEIGHT_DP = 128f
// Runtime defaults to the device status-bar inset; this value is the no-inset fallback.
const val DEFAULT_BATTERY_BAR_HEIGHT_DP = 34f
const val DEFAULT_BATTERY_HORIZONTAL_PADDING_DP = 12f
const val DEFAULT_BATTERY_SIDE_PADDING_DP = 16f
const val DEFAULT_BATTERY_PERCENT_SIZE_DP = 18f
const val DEFAULT_BATTERY_EMOJI_SIZE_DP = 24f
const val DEFAULT_BATTERY_ANIMATION_SIZE_DP = 25f
const val DEFAULT_BATTERY_ICON_SIZE_DP = 32f
const val DEFAULT_BATTERY_STATUS_ICON_SIZE_DP = 16f
const val DEFAULT_BATTERY_TIME_SIZE_DP = 16f
const val DEFAULT_BATTERY_PRIVACY_RESERVE_DP = 72f
const val DEFAULT_BATTERY_BACKGROUND_ID = 17
const val DEFAULT_BATTERY_EMOTION_ID = 1
const val DEFAULT_BATTERY_ANIMATION_ASSET = "cute_1.json"
// Figma's leading Charge style maps to the simple bolt stored as charge_10.
const val DEFAULT_BATTERY_CHARGE_ICON_INDEX = 10
const val DEFAULT_BATTERY_STATUS_ICON_STYLE_INDEX = 1
const val DEFAULT_BATTERY_WIFI_ICON_STYLE_INDEX = 2
const val DEFAULT_BATTERY_HOTSPOT_ICON_STYLE_INDEX = 3
const val MIN_BATTERY_STATUS_ICON_STYLE_INDEX = 1
const val MAX_BATTERY_STATUS_ICON_STYLE_INDEX = 4
const val DEFAULT_BATTERY_BACKGROUND_COLOR = 0xFFE0F7F1.toInt()
const val DEFAULT_BATTERY_FOREGROUND_COLOR = 0xFF111827.toInt()
const val DEFAULT_BATTERY_STATUS_ICON_COLOR = 0xFF000000.toInt()
