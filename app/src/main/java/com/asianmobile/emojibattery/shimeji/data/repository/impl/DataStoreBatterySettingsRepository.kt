package com.asianmobile.emojibattery.shimeji.data.repository.impl

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.asianmobile.emojibattery.shimeji.battery.settings.BatterySettingsPolicy
import com.asianmobile.emojibattery.shimeji.battery.settings.resolveBatteryStatusBarHeightRange
import com.asianmobile.emojibattery.shimeji.battery.settings.systemStatusBarHeightDp
import com.asianmobile.emojibattery.shimeji.data.local.dataStore
import com.asianmobile.emojibattery.shimeji.data.model.BUILT_IN_BATTERY_THEME_ID
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDataType
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDateFont
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDateFormat
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusDisplayMode
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_BACKGROUND_COLOR
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_BACKGROUND_ID
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_EMOJI_SIZE_DP
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_FOREGROUND_COLOR
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_HORIZONTAL_PADDING_DP
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_ICON_SIZE_DP
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_EMOTION_ID
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_PRIVACY_RESERVE_DP
import com.asianmobile.emojibattery.shimeji.data.model.normalizeSelectableBatteryThemeId
import com.asianmobile.emojibattery.shimeji.data.repository.BatterySettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Singleton
class DataStoreBatterySettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) : BatterySettingsRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val barHeightRange = resolveBatteryStatusBarHeightRange(
        context.systemStatusBarHeightDp()
    )
    private val policy = BatterySettingsPolicy(barHeightRange)

    override val config: StateFlow<BatteryStatusConfig> = context.dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map(::decode)
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = BatteryStatusConfig(barHeightDp = barHeightRange.defaultDp)
        )

    override val hiddenAppPackages: StateFlow<Set<String>> = context.dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            preferences[HIDDEN_APP_PACKAGES]
                .orEmpty()
                .map(String::trim)
                .filterTo(mutableSetOf(), String::isNotEmpty)
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptySet()
        )

    override fun applyConfig(config: BatteryStatusConfig) {
        val sanitized = policy.sanitize(config).copy(hasApplied = true)
        edit { preferences ->
            val rewardUnlockedThemeIds =
                decodeRewardUnlockedIds(preferences) + sanitized.rewardUnlockedThemeIds
            preferences[ENABLED] = sanitized.enabled
            preferences[HAS_APPLIED] = sanitized.hasApplied
            preferences[SELECTED_THEME_ID] = sanitized.selectedThemeId
            preferences[SELECTED_BATTERY_THEME_ID] = sanitized.selectedBatteryThemeId
            preferences[SELECTED_EMOJI_THEME_ID] = sanitized.selectedEmojiThemeId
            preferences[DISPLAY_MODE] = sanitized.displayMode.name
            preferences[SHOW_TIME] = sanitized.showTime
            preferences[SHOW_PERCENTAGE] = sanitized.showPercentage
            preferences[BACKGROUND_DECORATION_ID] = sanitized.backgroundDecorationId
            preferences[SHOW_EMOTION] = sanitized.showEmotion
            preferences[EMOTION_DECORATION_ID] = sanitized.emotionDecorationId
            preferences[SHOW_ANIMATION] = sanitized.showAnimation
            preferences[ANIMATION_ASSET_NAME] = sanitized.animationAssetName
            preferences[BAR_HEIGHT_DP] = sanitized.barHeightDp
            preferences[HORIZONTAL_PADDING_DP] = sanitized.horizontalPaddingDp
            preferences[LEFT_PADDING_DP] = sanitized.leftPaddingDp
            preferences[RIGHT_PADDING_DP] = sanitized.rightPaddingDp
            preferences[PERCENT_SIZE_DP] = sanitized.percentSizeDp
            preferences[EMOJI_SIZE_DP] = sanitized.emojiSizeDp
            preferences[ANIMATION_SIZE_DP] = sanitized.animationSizeDp
            preferences[BATTERY_SIZE_DP] = sanitized.batterySizeDp
            preferences[BACKGROUND_COLOR] = sanitized.backgroundColorArgb
            preferences[FOREGROUND_COLOR] = sanitized.foregroundColorArgb
            preferences[PERCENT_COLOR] = sanitized.percentColorArgb
            preferences[SHOW_WIFI] = sanitized.showWifi
            preferences[WIFI_SIZE_DP] = sanitized.wifiSizeDp
            preferences[WIFI_COLOR] = sanitized.wifiColorArgb
            preferences[WIFI_ICON_STYLE_INDEX] = sanitized.wifiIconStyleIndex
            preferences[DATA_TYPE] = sanitized.dataType.name
            preferences[SHOW_DATA] = sanitized.showData
            preferences[DATA_SIZE_DP] = sanitized.dataSizeDp
            preferences[DATA_COLOR] = sanitized.dataColorArgb
            preferences[SHOW_SIGNAL] = sanitized.showSignal
            preferences[SIGNAL_SIZE_DP] = sanitized.signalSizeDp
            preferences[SIGNAL_COLOR] = sanitized.signalColorArgb
            preferences[SIGNAL_ICON_STYLE_INDEX] = sanitized.signalIconStyleIndex
            preferences[AIRPLANE_SIZE_DP] = sanitized.airplaneSizeDp
            preferences[SHOW_AIRPLANE] = sanitized.showAirplane
            preferences[AIRPLANE_COLOR] = sanitized.airplaneColorArgb
            preferences[AIRPLANE_ICON_STYLE_INDEX] = sanitized.airplaneIconStyleIndex
            preferences[HOTSPOT_SIZE_DP] = sanitized.hotspotSizeDp
            preferences[SHOW_HOTSPOT] = sanitized.showHotspot
            preferences[HOTSPOT_COLOR] = sanitized.hotspotColorArgb
            preferences[HOTSPOT_ICON_STYLE_INDEX] = sanitized.hotspotIconStyleIndex
            preferences[RINGER_SIZE_DP] = sanitized.ringerSizeDp
            preferences[SHOW_RINGER] = sanitized.showRinger
            preferences[RINGER_COLOR] = sanitized.ringerColorArgb
            preferences[RINGER_ICON_STYLE_INDEX] = sanitized.ringerIconStyleIndex
            preferences[CHARGE_SIZE_DP] = sanitized.chargeSizeDp
            preferences[SHOW_CHARGE] = sanitized.showCharge
            preferences[CHARGE_ICON_INDEX] = sanitized.chargeIconIndex
            preferences[CHARGE_COLOR] = sanitized.chargeColorArgb
            preferences[SHOW_DATE_TIME] = sanitized.showDateTime
            preferences[DATE_TIME_COLOR] = sanitized.dateTimeColorArgb
            preferences[DATE_TIME_SIZE_DP] = sanitized.dateTimeSizeDp
            preferences[DATE_FORMAT] = sanitized.dateFormat.name
            preferences[DATE_TIME_FONT] = sanitized.dateTimeFont.name
            preferences[CLOCK_COLOR] = sanitized.clockColorArgb
            preferences[CLOCK_SIZE_DP] = sanitized.clockSizeDp
            preferences[PRIVACY_RESERVE_DP] = sanitized.privacyReserveDp
            preferences[FAVORITE_THEME_IDS] =
                sanitized.favoriteThemeIds.map(Int::toString).toSet()
            preferences[REWARD_UNLOCKED_THEME_IDS] =
                rewardUnlockedThemeIds.map(Int::toString).toSet()
        }
    }

    override fun setEnabled(enabled: Boolean) = edit { preferences ->
        preferences[ENABLED] = enabled
    }

    override fun setAppHidden(packageName: String, hidden: Boolean) {
        val normalizedPackage = packageName.trim()
        if (normalizedPackage.isEmpty()) return
        edit { preferences ->
            val packages = preferences[HIDDEN_APP_PACKAGES].orEmpty().toMutableSet()
            if (hidden) packages += normalizedPackage else packages -= normalizedPackage
            preferences[HIDDEN_APP_PACKAGES] = packages
        }
    }

    override fun toggleFavorite(themeId: Int) {
        if (themeId < 0) return
        edit { preferences ->
            val current = decodeFavoriteIds(preferences).toMutableSet()
            if (!current.add(themeId)) current.remove(themeId)
            preferences[FAVORITE_THEME_IDS] = current.map(Int::toString).toSet()
        }
    }

    override fun unlockThemeByReward(themeId: Int) {
        if (themeId <= BUILT_IN_BATTERY_THEME_ID) return
        edit { preferences ->
            val current = decodeRewardUnlockedIds(preferences).toMutableSet()
            current += themeId
            preferences[REWARD_UNLOCKED_THEME_IDS] = current.map(Int::toString).toSet()
        }
    }

    private fun decode(preferences: Preferences): BatteryStatusConfig {
        val defaults = BatteryStatusConfig()
        val enabled = preferences[ENABLED] ?: false
        val selectedThemeId = normalizeSelectableBatteryThemeId(
            preferences[SELECTED_THEME_ID] ?: defaults.selectedThemeId
        )
        return policy.sanitize(
            BatteryStatusConfig(
                enabled = enabled,
                hasApplied = resolveBatteryHasApplied(preferences[HAS_APPLIED], enabled),
                selectedThemeId = selectedThemeId,
                selectedBatteryThemeId = normalizeSelectableBatteryThemeId(
                    preferences[SELECTED_BATTERY_THEME_ID] ?: selectedThemeId
                ),
                selectedEmojiThemeId = normalizeSelectableBatteryThemeId(
                    preferences[SELECTED_EMOJI_THEME_ID] ?: selectedThemeId
                ),
                displayMode = preferences[DISPLAY_MODE]
                    ?.let { value ->
                        BatteryStatusDisplayMode.entries.firstOrNull { it.name == value }
                    }
                    ?: BatteryStatusDisplayMode.COVER_SYSTEM_BAR,
                showTime = preferences[SHOW_TIME] ?: true,
                showPercentage = preferences[SHOW_PERCENTAGE] ?: true,
                backgroundDecorationId = preferences[BACKGROUND_DECORATION_ID]
                    ?: DEFAULT_BATTERY_BACKGROUND_ID,
                showEmotion = preferences[SHOW_EMOTION] ?: defaults.showEmotion,
                emotionDecorationId = preferences[EMOTION_DECORATION_ID]
                    ?: DEFAULT_BATTERY_EMOTION_ID,
                showAnimation = preferences[SHOW_ANIMATION] ?: defaults.showAnimation,
                animationAssetName = preferences[ANIMATION_ASSET_NAME]
                    ?: defaults.animationAssetName,
                barHeightDp = preferences[BAR_HEIGHT_DP] ?: barHeightRange.defaultDp,
                horizontalPaddingDp = preferences[HORIZONTAL_PADDING_DP]
                    ?: DEFAULT_BATTERY_HORIZONTAL_PADDING_DP,
                leftPaddingDp = preferences[LEFT_PADDING_DP]
                    ?: preferences[HORIZONTAL_PADDING_DP]
                    ?: defaults.leftPaddingDp,
                rightPaddingDp = preferences[RIGHT_PADDING_DP]
                    ?: preferences[HORIZONTAL_PADDING_DP]
                    ?: defaults.rightPaddingDp,
                percentSizeDp = preferences[PERCENT_SIZE_DP] ?: defaults.percentSizeDp,
                emojiSizeDp = preferences[EMOJI_SIZE_DP] ?: DEFAULT_BATTERY_EMOJI_SIZE_DP,
                animationSizeDp = preferences[ANIMATION_SIZE_DP]
                    ?: defaults.animationSizeDp,
                batterySizeDp = preferences[BATTERY_SIZE_DP]
                    ?: DEFAULT_BATTERY_ICON_SIZE_DP,
                backgroundColorArgb = preferences[BACKGROUND_COLOR]
                    ?: DEFAULT_BATTERY_BACKGROUND_COLOR,
                foregroundColorArgb = preferences[FOREGROUND_COLOR]
                    ?: DEFAULT_BATTERY_FOREGROUND_COLOR,
                percentColorArgb = preferences[PERCENT_COLOR] ?: defaults.percentColorArgb,
                showWifi = preferences[SHOW_WIFI] ?: defaults.showWifi,
                wifiSizeDp = preferences[WIFI_SIZE_DP] ?: defaults.wifiSizeDp,
                wifiColorArgb = preferences[WIFI_COLOR] ?: defaults.wifiColorArgb,
                wifiIconStyleIndex = preferences[WIFI_ICON_STYLE_INDEX]
                    ?: defaults.wifiIconStyleIndex,
                dataType = preferences[DATA_TYPE]
                    ?.let { name -> BatteryDataType.entries.firstOrNull { it.name == name } }
                    ?: defaults.dataType,
                showData = preferences[SHOW_DATA] ?: defaults.showData,
                dataSizeDp = preferences[DATA_SIZE_DP] ?: defaults.dataSizeDp,
                dataColorArgb = preferences[DATA_COLOR] ?: defaults.dataColorArgb,
                showSignal = preferences[SHOW_SIGNAL] ?: defaults.showSignal,
                signalSizeDp = preferences[SIGNAL_SIZE_DP] ?: defaults.signalSizeDp,
                signalColorArgb = preferences[SIGNAL_COLOR] ?: defaults.signalColorArgb,
                signalIconStyleIndex = preferences[SIGNAL_ICON_STYLE_INDEX]
                    ?: defaults.signalIconStyleIndex,
                airplaneSizeDp = preferences[AIRPLANE_SIZE_DP] ?: defaults.airplaneSizeDp,
                showAirplane = preferences[SHOW_AIRPLANE] ?: defaults.showAirplane,
                airplaneColorArgb = preferences[AIRPLANE_COLOR]
                    ?: defaults.airplaneColorArgb,
                airplaneIconStyleIndex = preferences[AIRPLANE_ICON_STYLE_INDEX]
                    ?: defaults.airplaneIconStyleIndex,
                hotspotSizeDp = preferences[HOTSPOT_SIZE_DP] ?: defaults.hotspotSizeDp,
                showHotspot = preferences[SHOW_HOTSPOT] ?: defaults.showHotspot,
                hotspotColorArgb = preferences[HOTSPOT_COLOR] ?: defaults.hotspotColorArgb,
                hotspotIconStyleIndex = preferences[HOTSPOT_ICON_STYLE_INDEX]
                    ?: defaults.hotspotIconStyleIndex,
                ringerSizeDp = preferences[RINGER_SIZE_DP] ?: defaults.ringerSizeDp,
                showRinger = preferences[SHOW_RINGER] ?: defaults.showRinger,
                ringerColorArgb = preferences[RINGER_COLOR] ?: defaults.ringerColorArgb,
                ringerIconStyleIndex = preferences[RINGER_ICON_STYLE_INDEX]
                    ?: defaults.ringerIconStyleIndex,
                chargeSizeDp = preferences[CHARGE_SIZE_DP] ?: defaults.chargeSizeDp,
                showCharge = preferences[SHOW_CHARGE] ?: defaults.showCharge,
                chargeIconIndex = preferences[CHARGE_ICON_INDEX]
                    ?: defaults.chargeIconIndex,
                chargeColorArgb = preferences[CHARGE_COLOR] ?: defaults.chargeColorArgb,
                showDateTime = preferences[SHOW_DATE_TIME] ?: defaults.showDateTime,
                dateTimeColorArgb = preferences[DATE_TIME_COLOR]
                    ?: defaults.dateTimeColorArgb,
                dateTimeSizeDp = preferences[DATE_TIME_SIZE_DP] ?: defaults.dateTimeSizeDp,
                dateFormat = preferences[DATE_FORMAT]
                    ?.let { name -> BatteryDateFormat.entries.firstOrNull { it.name == name } }
                    ?: defaults.dateFormat,
                dateTimeFont = preferences[DATE_TIME_FONT]
                    ?.let { name -> BatteryDateFont.entries.firstOrNull { it.name == name } }
                    ?: defaults.dateTimeFont,
                clockColorArgb = preferences[CLOCK_COLOR]
                    ?: preferences[DATE_TIME_COLOR]
                    ?: defaults.clockColorArgb,
                clockSizeDp = preferences[CLOCK_SIZE_DP]
                    ?: preferences[DATE_TIME_SIZE_DP]
                    ?: defaults.clockSizeDp,
                privacyReserveDp = preferences[PRIVACY_RESERVE_DP]
                    ?: DEFAULT_BATTERY_PRIVACY_RESERVE_DP,
                favoriteThemeIds = decodeFavoriteIds(preferences),
                rewardUnlockedThemeIds = decodeRewardUnlockedIds(preferences)
            )
        )
    }

    private fun decodeFavoriteIds(preferences: Preferences): Set<Int> =
        preferences[FAVORITE_THEME_IDS]
            .orEmpty()
            .mapNotNull(String::toIntOrNull)
            .filterTo(mutableSetOf()) { it >= 0 }

    private fun decodeRewardUnlockedIds(preferences: Preferences): Set<Int> =
        preferences[REWARD_UNLOCKED_THEME_IDS]
            .orEmpty()
            .mapNotNull(String::toIntOrNull)
            .filterTo(mutableSetOf()) { it > BUILT_IN_BATTERY_THEME_ID }

    private fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        scope.launch {
            context.dataStore.edit { preferences -> block(preferences) }
        }
    }

    private companion object {
        val ENABLED = booleanPreferencesKey("battery_status_enabled")
        val HAS_APPLIED = booleanPreferencesKey("battery_status_has_applied")
        val SELECTED_THEME_ID = intPreferencesKey("battery_status_selected_theme_id")
        val SELECTED_BATTERY_THEME_ID =
            intPreferencesKey("battery_status_selected_battery_theme_id")
        val SELECTED_EMOJI_THEME_ID =
            intPreferencesKey("battery_status_selected_emoji_theme_id")
        val DISPLAY_MODE = stringPreferencesKey("battery_status_display_mode")
        val SHOW_TIME = booleanPreferencesKey("battery_status_show_time")
        val SHOW_PERCENTAGE = booleanPreferencesKey("battery_status_show_percentage")
        val BACKGROUND_DECORATION_ID =
            intPreferencesKey("battery_status_background_decoration_id_v2")
        val SHOW_EMOTION = booleanPreferencesKey("battery_status_show_emotion")
        val EMOTION_DECORATION_ID =
            intPreferencesKey("battery_status_emotion_decoration_id")
        val SHOW_ANIMATION = booleanPreferencesKey("battery_status_show_animation")
        val ANIMATION_ASSET_NAME =
            stringPreferencesKey("battery_status_animation_asset_name")
        val BAR_HEIGHT_DP = floatPreferencesKey("battery_status_bar_height_dp")
        val HORIZONTAL_PADDING_DP = floatPreferencesKey("battery_status_horizontal_padding_dp")
        val LEFT_PADDING_DP = floatPreferencesKey("battery_status_left_padding_dp")
        val RIGHT_PADDING_DP = floatPreferencesKey("battery_status_right_padding_dp")
        val PERCENT_SIZE_DP = floatPreferencesKey("battery_status_percent_size_dp")
        val EMOJI_SIZE_DP = floatPreferencesKey("battery_status_emoji_size_dp")
        val ANIMATION_SIZE_DP = floatPreferencesKey("battery_status_animation_size_dp")
        val BATTERY_SIZE_DP = floatPreferencesKey("battery_status_battery_size_dp")
        val BACKGROUND_COLOR = intPreferencesKey("battery_status_background_color")
        val FOREGROUND_COLOR = intPreferencesKey("battery_status_foreground_color")
        val PERCENT_COLOR = intPreferencesKey("battery_status_percent_color")
        val SHOW_WIFI = booleanPreferencesKey("battery_status_show_wifi")
        val WIFI_SIZE_DP = floatPreferencesKey("battery_status_wifi_size_dp")
        val WIFI_COLOR = intPreferencesKey("battery_status_wifi_color")
        val WIFI_ICON_STYLE_INDEX =
            intPreferencesKey("battery_status_wifi_icon_style_index")
        val DATA_TYPE = stringPreferencesKey("battery_status_data_type")
        val SHOW_DATA = booleanPreferencesKey("battery_status_show_data")
        val DATA_SIZE_DP = floatPreferencesKey("battery_status_data_size_dp")
        val DATA_COLOR = intPreferencesKey("battery_status_data_color")
        val SHOW_SIGNAL = booleanPreferencesKey("battery_status_show_signal")
        val SIGNAL_SIZE_DP = floatPreferencesKey("battery_status_signal_size_dp")
        val SIGNAL_COLOR = intPreferencesKey("battery_status_signal_color")
        val SIGNAL_ICON_STYLE_INDEX =
            intPreferencesKey("battery_status_signal_icon_style_index")
        val AIRPLANE_SIZE_DP = floatPreferencesKey("battery_status_airplane_size_dp")
        val SHOW_AIRPLANE = booleanPreferencesKey("battery_status_show_airplane")
        val AIRPLANE_COLOR = intPreferencesKey("battery_status_airplane_color")
        val AIRPLANE_ICON_STYLE_INDEX =
            intPreferencesKey("battery_status_airplane_icon_style_index")
        val HOTSPOT_SIZE_DP = floatPreferencesKey("battery_status_hotspot_size_dp")
        val SHOW_HOTSPOT = booleanPreferencesKey("battery_status_show_hotspot")
        val HOTSPOT_COLOR = intPreferencesKey("battery_status_hotspot_color")
        val HOTSPOT_ICON_STYLE_INDEX =
            intPreferencesKey("battery_status_hotspot_icon_style_index")
        val RINGER_SIZE_DP = floatPreferencesKey("battery_status_ringer_size_dp")
        val SHOW_RINGER = booleanPreferencesKey("battery_status_show_ringer")
        val RINGER_COLOR = intPreferencesKey("battery_status_ringer_color")
        val RINGER_ICON_STYLE_INDEX =
            intPreferencesKey("battery_status_ringer_icon_style_index")
        val CHARGE_SIZE_DP = floatPreferencesKey("battery_status_charge_size_dp")
        val SHOW_CHARGE = booleanPreferencesKey("battery_status_show_charge")
        val CHARGE_ICON_INDEX = intPreferencesKey("battery_status_charge_icon_index")
        val CHARGE_COLOR = intPreferencesKey("battery_status_charge_color")
        val SHOW_DATE_TIME = booleanPreferencesKey("battery_status_show_date_time")
        val DATE_TIME_COLOR = intPreferencesKey("battery_status_date_time_color")
        val DATE_TIME_SIZE_DP = floatPreferencesKey("battery_status_date_time_size_dp")
        val DATE_FORMAT = stringPreferencesKey("battery_status_date_format")
        val DATE_TIME_FONT = stringPreferencesKey("battery_status_date_time_font")
        val CLOCK_COLOR = intPreferencesKey("battery_status_clock_color")
        val CLOCK_SIZE_DP = floatPreferencesKey("battery_status_clock_size_dp")
        val PRIVACY_RESERVE_DP = floatPreferencesKey("battery_status_privacy_reserve_dp")
        val FAVORITE_THEME_IDS = stringSetPreferencesKey("battery_status_favorite_theme_ids")
        val REWARD_UNLOCKED_THEME_IDS =
            stringSetPreferencesKey("battery_status_reward_unlocked_theme_ids")
        val HIDDEN_APP_PACKAGES = stringSetPreferencesKey("battery_status_hidden_app_packages")
    }
}

internal fun resolveBatteryHasApplied(
    storedHasApplied: Boolean?,
    enabled: Boolean
): Boolean = storedHasApplied ?: enabled
