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
import com.asianmobile.emojibattery.shimeji.data.local.dataStore
import com.asianmobile.emojibattery.shimeji.data.model.BUILT_IN_BATTERY_THEME_ID
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusDisplayMode
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_BACKGROUND_COLOR
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_BAR_HEIGHT_DP
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_EMOJI_SIZE_DP
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_FOREGROUND_COLOR
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_HORIZONTAL_PADDING_DP
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_ICON_SIZE_DP
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_PRIVACY_RESERVE_DP
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
    private val policy = BatterySettingsPolicy()

    override val config: StateFlow<BatteryStatusConfig> = context.dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map(::decode)
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = BatteryStatusConfig()
        )

    override fun applyConfig(config: BatteryStatusConfig) {
        val sanitized = policy.sanitize(config)
        edit { preferences ->
            preferences[ENABLED] = sanitized.enabled
            preferences[SELECTED_THEME_ID] = sanitized.selectedThemeId
            preferences[DISPLAY_MODE] = sanitized.displayMode.name
            preferences[SHOW_TIME] = sanitized.showTime
            preferences[SHOW_PERCENTAGE] = sanitized.showPercentage
            preferences[BAR_HEIGHT_DP] = sanitized.barHeightDp
            preferences[HORIZONTAL_PADDING_DP] = sanitized.horizontalPaddingDp
            preferences[EMOJI_SIZE_DP] = sanitized.emojiSizeDp
            preferences[BATTERY_SIZE_DP] = sanitized.batterySizeDp
            preferences[BACKGROUND_COLOR] = sanitized.backgroundColorArgb
            preferences[FOREGROUND_COLOR] = sanitized.foregroundColorArgb
            preferences[PRIVACY_RESERVE_DP] = sanitized.privacyReserveDp
            preferences[FAVORITE_THEME_IDS] =
                sanitized.favoriteThemeIds.map(Int::toString).toSet()
        }
    }

    override fun setEnabled(enabled: Boolean) = edit { preferences ->
        preferences[ENABLED] = enabled
    }

    override fun toggleFavorite(themeId: Int) {
        if (themeId < 0) return
        edit { preferences ->
            val current = decodeFavoriteIds(preferences).toMutableSet()
            if (!current.add(themeId)) current.remove(themeId)
            preferences[FAVORITE_THEME_IDS] = current.map(Int::toString).toSet()
        }
    }

    private fun decode(preferences: Preferences): BatteryStatusConfig =
        policy.sanitize(
            BatteryStatusConfig(
                enabled = preferences[ENABLED] ?: false,
                selectedThemeId = preferences[SELECTED_THEME_ID]
                    ?: BUILT_IN_BATTERY_THEME_ID,
                displayMode = preferences[DISPLAY_MODE]
                    ?.let { value ->
                        BatteryStatusDisplayMode.entries.firstOrNull { it.name == value }
                    }
                    ?: BatteryStatusDisplayMode.COVER_SYSTEM_BAR,
                showTime = preferences[SHOW_TIME] ?: true,
                showPercentage = preferences[SHOW_PERCENTAGE] ?: true,
                barHeightDp = preferences[BAR_HEIGHT_DP] ?: DEFAULT_BATTERY_BAR_HEIGHT_DP,
                horizontalPaddingDp = preferences[HORIZONTAL_PADDING_DP]
                    ?: DEFAULT_BATTERY_HORIZONTAL_PADDING_DP,
                emojiSizeDp = preferences[EMOJI_SIZE_DP] ?: DEFAULT_BATTERY_EMOJI_SIZE_DP,
                batterySizeDp = preferences[BATTERY_SIZE_DP]
                    ?: DEFAULT_BATTERY_ICON_SIZE_DP,
                backgroundColorArgb = preferences[BACKGROUND_COLOR]
                    ?: DEFAULT_BATTERY_BACKGROUND_COLOR,
                foregroundColorArgb = preferences[FOREGROUND_COLOR]
                    ?: DEFAULT_BATTERY_FOREGROUND_COLOR,
                privacyReserveDp = preferences[PRIVACY_RESERVE_DP]
                    ?: DEFAULT_BATTERY_PRIVACY_RESERVE_DP,
                favoriteThemeIds = decodeFavoriteIds(preferences)
            )
        )

    private fun decodeFavoriteIds(preferences: Preferences): Set<Int> =
        preferences[FAVORITE_THEME_IDS]
            .orEmpty()
            .mapNotNull(String::toIntOrNull)
            .filterTo(mutableSetOf()) { it >= 0 }

    private fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        scope.launch {
            context.dataStore.edit { preferences -> block(preferences) }
        }
    }

    private companion object {
        val ENABLED = booleanPreferencesKey("battery_status_enabled")
        val SELECTED_THEME_ID = intPreferencesKey("battery_status_selected_theme_id")
        val DISPLAY_MODE = stringPreferencesKey("battery_status_display_mode")
        val SHOW_TIME = booleanPreferencesKey("battery_status_show_time")
        val SHOW_PERCENTAGE = booleanPreferencesKey("battery_status_show_percentage")
        val BAR_HEIGHT_DP = floatPreferencesKey("battery_status_bar_height_dp")
        val HORIZONTAL_PADDING_DP = floatPreferencesKey("battery_status_horizontal_padding_dp")
        val EMOJI_SIZE_DP = floatPreferencesKey("battery_status_emoji_size_dp")
        val BATTERY_SIZE_DP = floatPreferencesKey("battery_status_battery_size_dp")
        val BACKGROUND_COLOR = intPreferencesKey("battery_status_background_color")
        val FOREGROUND_COLOR = intPreferencesKey("battery_status_foreground_color")
        val PRIVACY_RESERVE_DP = floatPreferencesKey("battery_status_privacy_reserve_dp")
        val FAVORITE_THEME_IDS = stringSetPreferencesKey("battery_status_favorite_theme_ids")
    }
}
