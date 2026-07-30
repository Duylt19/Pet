package com.asianmobile.emojibattery.shimeji.data.model

data class BatteryStatusConfig(
    val enabled: Boolean = false,
    val selectedThemeId: Int = BUILT_IN_BATTERY_THEME_ID,
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
    val dataType: BatteryDataType = BatteryDataType.G2,
    val dataSizeDp: Float = DEFAULT_BATTERY_STATUS_ICON_SIZE_DP,
    val dataColorArgb: Int = DEFAULT_BATTERY_STATUS_ICON_COLOR,
    val signalSizeDp: Float = DEFAULT_BATTERY_STATUS_ICON_SIZE_DP,
    val signalColorArgb: Int = DEFAULT_BATTERY_STATUS_ICON_COLOR,
    val airplaneSizeDp: Float = DEFAULT_BATTERY_STATUS_ICON_SIZE_DP,
    val airplaneColorArgb: Int = DEFAULT_BATTERY_STATUS_ICON_COLOR,
    val hotspotSizeDp: Float = DEFAULT_BATTERY_STATUS_ICON_SIZE_DP,
    val hotspotColorArgb: Int = DEFAULT_BATTERY_STATUS_ICON_COLOR,
    val ringerSizeDp: Float = DEFAULT_BATTERY_STATUS_ICON_SIZE_DP,
    val ringerColorArgb: Int = DEFAULT_BATTERY_STATUS_ICON_COLOR,
    val chargeSizeDp: Float = DEFAULT_BATTERY_STATUS_ICON_SIZE_DP,
    val chargeIconIndex: Int = DEFAULT_BATTERY_CHARGE_ICON_INDEX,
    val chargeColorArgb: Int = DEFAULT_BATTERY_STATUS_ICON_COLOR,
    val showDateTime: Boolean = false,
    val dateTimeColorArgb: Int = DEFAULT_BATTERY_STATUS_ICON_COLOR,
    val dateTimeSizeDp: Float = DEFAULT_BATTERY_STATUS_ICON_SIZE_DP,
    val dateFormat: BatteryDateFormat = BatteryDateFormat.WEEKDAY_MONTH_DAY,
    val dateTimeFont: BatteryDateFont = BatteryDateFont.BALOO_2,
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
    BALOO_2("Baloo 2", "baloo2_variable_wght"),
    BAKBAK_ONE("Bakbak One", "bakbak_one_normal"),
    BASIC("Basic", "basic_normal"),
    BODONI_MODA_SC("Bodoni Moda SC", "bodoni_moda_sc_bold"),
    BAUHAUS_93("Bauhaus 93", "bauhaus_93_normal"),
    BEAU_RIVAGE("Beau Rivage", "beau_rivage_normal")
}

const val MIN_BATTERY_BAR_HEIGHT_DP = 24f
const val MAX_BATTERY_BAR_HEIGHT_DP = 48f
const val DEFAULT_BATTERY_BAR_HEIGHT_DP = 34f
const val DEFAULT_BATTERY_HORIZONTAL_PADDING_DP = 12f
const val DEFAULT_BATTERY_SIDE_PADDING_DP = 16f
const val DEFAULT_BATTERY_PERCENT_SIZE_DP = 18f
const val DEFAULT_BATTERY_EMOJI_SIZE_DP = 24f
const val DEFAULT_BATTERY_ANIMATION_SIZE_DP = 25f
const val DEFAULT_BATTERY_ICON_SIZE_DP = 32f
const val DEFAULT_BATTERY_STATUS_ICON_SIZE_DP = 16f
const val DEFAULT_BATTERY_PRIVACY_RESERVE_DP = 72f
const val DEFAULT_BATTERY_BACKGROUND_ID = 17
const val DEFAULT_BATTERY_EMOTION_ID = 1
const val DEFAULT_BATTERY_ANIMATION_ASSET = "cute_1.json"
const val DEFAULT_BATTERY_CHARGE_ICON_INDEX = 1
const val DEFAULT_BATTERY_BACKGROUND_COLOR = 0xFFE0F7F1.toInt()
const val DEFAULT_BATTERY_FOREGROUND_COLOR = 0xFF111827.toInt()
const val DEFAULT_BATTERY_STATUS_ICON_COLOR = 0xFF000000.toInt()
