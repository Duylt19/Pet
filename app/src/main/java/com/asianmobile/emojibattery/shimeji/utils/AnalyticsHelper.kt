package com.asianmobile.emojibattery.shimeji.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

/**
 * Canonical screen names sent to Firebase Analytics.
 *
 * Keep values stable, lowercase, and snake_case. UI code must use this enum instead of
 * sending ad-hoc strings so reports cannot split because of spelling or casing differences.
 */
enum class ScreenName(val value: String) {
    SPLASH("splash"),
    LANGUAGE_ONBOARDING("language_onboarding"),
    LANGUAGE_SETTINGS("language_settings"),
    INTRO_PAGE_1("intro_page_1"),
    INTRO_PAGE_2("intro_page_2"),
    INTRO_PAGE_3("intro_page_3"),
    PERMISSION("permission"),
    HOME("home"),
    SEARCH("search"),
    MY_PET("my_pet"),
    PET_CATALOG("pet_catalog"),
    PET_DETAIL("pet_detail"),
    PET_CUSTOMIZATION("pet_customization"),
    SWARM_CUSTOMIZATION("swarm_customization"),
    BATTERY_CATALOG("battery_catalog"),
    BATTERY_EDITOR("battery_editor"),
    BATTERY_SIZE_EDITOR("battery_size_editor"),
    BATTERY_APPEARANCE_EDITOR("battery_appearance_editor"),
    BATTERY_EMOJI_EDITOR("battery_emoji_editor"),
    BATTERY_ICON_EDITOR("battery_icon_editor"),
    BATTERY_ANIMATION_EDITOR("battery_animation_editor"),
    BATTERY_WIFI_EDITOR("battery_wifi_editor"),
    BATTERY_DATA_EDITOR("battery_data_editor"),
    BATTERY_SIGNAL_EDITOR("battery_signal_editor"),
    BATTERY_AIRPLANE_EDITOR("battery_airplane_editor"),
    BATTERY_HOTSPOT_EDITOR("battery_hotspot_editor"),
    BATTERY_RINGER_EDITOR("battery_ringer_editor"),
    BATTERY_CHARGE_EDITOR("battery_charge_editor"),
    BATTERY_DATE_TIME_EDITOR("battery_date_time_editor"),
    PREMIUM("premium"),
    SETTINGS("settings")
}

object AnalyticsHelper {
    private const val SCREEN_CLASS = "MainActivity"

    fun logScreenView(context: Context, screen: ScreenName) {
        FirebaseAnalytics.getInstance(context.applicationContext)
            .logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                param(FirebaseAnalytics.Param.SCREEN_NAME, screen.value)
                param(FirebaseAnalytics.Param.SCREEN_CLASS, SCREEN_CLASS)
            }
    }
}

/**
 * Tracks [screen] only while this composable is the visible, resumed screen.
 *
 * [isVisible] is required for content hosted in a pager because Compose may keep adjacent pages
 * composed. A screen is logged once per resumed visibility period and again when the user returns.
 */
@Composable
fun TrackScreenView(screen: ScreenName, isVisible: Boolean = true) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, screen, isVisible) {
        if (!isVisible) return@DisposableEffect onDispose { }

        var trackedInCurrentResume = false
        fun trackIfResumed() {
            if (!trackedInCurrentResume &&
                lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            ) {
                AnalyticsHelper.logScreenView(context, screen)
                trackedInCurrentResume = true
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> trackIfResumed()
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> trackedInCurrentResume = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        trackIfResumed()

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
