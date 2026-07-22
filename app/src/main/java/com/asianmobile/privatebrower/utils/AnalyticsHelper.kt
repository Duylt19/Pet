package com.asianmobile.privatebrower.utils

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
    SET_DEFAULT_BROWSER("set_default_browser"),
    PREMIUM("premium"),
    SETTINGS("settings"),
    PRIVACY_POLICY("privacy_policy"),

    HOME_BROWSER("home_browser"),
    TABS_NORMAL("tabs_normal"),
    TABS_PRIVATE("tabs_private"),
    TABS_SEARCH_NORMAL("tabs_search_normal"),
    TABS_SEARCH_PRIVATE("tabs_search_private"),
    TAB_SELECTION_NORMAL("tab_selection_normal"),
    TAB_SELECTION_PRIVATE("tab_selection_private"),
    DOWNLOADS_ALL("downloads_all"),
    DOWNLOADS_ACTIVE("downloads_active"),
    DOWNLOADS_COMPLETED("downloads_completed"),
    BOOKMARKS("bookmarks"),
    BOOKMARKS_SEARCH("bookmarks_search"),
    HISTORY("history"),
    HISTORY_SEARCH("history_search"),
    FILES_HOME("files_home"),

    BROWSER_NORMAL("browser_normal"),
    BROWSER_PRIVATE("browser_private"),
    FILES_IMAGES("files_images"),
    FILES_VIDEO("files_video"),
    FILES_AUDIO("files_audio"),
    FILES_DOCUMENTS("files_documents"),
    VIEWER_IMAGE("viewer_image"),
    VIEWER_VIDEO("viewer_video"),
    VIEWER_AUDIO("viewer_audio"),
    VIEWER_FILE("viewer_file"),
    HOW_TO_DOWNLOAD("how_to_download")
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
