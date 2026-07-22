package com.asianmobile.privatebrower.ads.ui.compose

import androidx.annotation.StringRes
import com.asianmobile.privatebrower.ads.R
import com.asianmobile.privatebrower.ads.config.DIALOG_EXIT_APP
import com.asianmobile.privatebrower.ads.config.IS_SHOW_NATIVE_BOOKMARKS
import com.asianmobile.privatebrower.ads.config.IS_SHOW_NATIVE_DOWNLOADS
import com.asianmobile.privatebrower.ads.config.IS_SHOW_NATIVE_EXIT_DIALOG
import com.asianmobile.privatebrower.ads.config.IS_SHOW_NATIVE_FILES_AUDIO
import com.asianmobile.privatebrower.ads.config.IS_SHOW_NATIVE_FILES_DOCUMENTS
import com.asianmobile.privatebrower.ads.config.IS_SHOW_NATIVE_FILES_PHOTOS
import com.asianmobile.privatebrower.ads.config.IS_SHOW_NATIVE_FILES_VIDEOS
import com.asianmobile.privatebrower.ads.config.IS_SHOW_NATIVE_HOME
import com.asianmobile.privatebrower.ads.config.IS_SHOW_NATIVE_HISTORY
import com.asianmobile.privatebrower.ads.config.IS_SHOW_NATIVE_INTRO
import com.asianmobile.privatebrower.ads.config.IS_SHOW_NATIVE_INTRO_FULL
import com.asianmobile.privatebrower.ads.config.IS_SHOW_NATIVE_INTRO_SECOND
import com.asianmobile.privatebrower.ads.config.IS_SHOW_NATIVE_LANGUAGE
import com.asianmobile.privatebrower.ads.config.IS_SHOW_NATIVE_LANGUAGE_SECOND
import com.asianmobile.privatebrower.ads.config.IS_SHOW_NATIVE_PERMISSION
import com.asianmobile.privatebrower.ads.config.IS_SHOW_NATIVE_SET_DEFAULT
import com.asianmobile.privatebrower.ads.config.IS_SHOW_NATIVE_SETTING
import com.asianmobile.privatebrower.ads.config.SCREEN_DOWNLOADS
import com.asianmobile.privatebrower.ads.config.SCREEN_BOOKMARKS
import com.asianmobile.privatebrower.ads.config.SCREEN_FILES_AUDIO
import com.asianmobile.privatebrower.ads.config.SCREEN_FILES_DOCUMENTS
import com.asianmobile.privatebrower.ads.config.SCREEN_FILES_PHOTOS
import com.asianmobile.privatebrower.ads.config.SCREEN_FILES_VIDEOS
import com.asianmobile.privatebrower.ads.config.SCREEN_HOME
import com.asianmobile.privatebrower.ads.config.SCREEN_HISTORY
import com.asianmobile.privatebrower.ads.config.SCREEN_INTRO
import com.asianmobile.privatebrower.ads.config.SCREEN_INTRO_FULL
import com.asianmobile.privatebrower.ads.config.SCREEN_INTRO_SECOND
import com.asianmobile.privatebrower.ads.config.SCREEN_LANGUAGE
import com.asianmobile.privatebrower.ads.config.SCREEN_LANGUAGE_SECOND
import com.asianmobile.privatebrower.ads.config.SCREEN_PERMISSION
import com.asianmobile.privatebrower.ads.config.SCREEN_SET_DEFAULT
import com.asianmobile.privatebrower.ads.config.SCREEN_SETTING

internal data class NativeAdPlacement(
    val screenCode: String,
    val adType: AdType,
    val remoteConfigKey: String,
    @param:StringRes val adUnitResId: Int
)

internal object NativeAdPlacementCatalog {
    val all: List<NativeAdPlacement> = listOf(
        NativeAdPlacement(
            SCREEN_LANGUAGE,
            AdType.HEIGHT_222_SMALL_CTA,
            IS_SHOW_NATIVE_LANGUAGE,
            R.string.id_private_browser_native_language
        ),
        NativeAdPlacement(
            SCREEN_LANGUAGE_SECOND,
            AdType.HEIGHT_222,
            IS_SHOW_NATIVE_LANGUAGE_SECOND,
            R.string.id_private_browser_native_language_second
        ),
        NativeAdPlacement(
            SCREEN_INTRO,
            AdType.HEIGHT_222,
            IS_SHOW_NATIVE_INTRO,
            R.string.id_private_browser_native_intro
        ),
        NativeAdPlacement(
            SCREEN_INTRO_SECOND,
            AdType.HEIGHT_222,
            IS_SHOW_NATIVE_INTRO_SECOND,
            R.string.id_private_browser_native_intro
        ),
        NativeAdPlacement(
            SCREEN_INTRO_FULL,
            AdType.HEIGHT_222,
            IS_SHOW_NATIVE_INTRO_FULL,
            R.string.id_private_browser_native_full_intro
        ),
        NativeAdPlacement(
            SCREEN_PERMISSION,
            AdType.HEIGHT_222,
            IS_SHOW_NATIVE_PERMISSION,
            R.string.id_private_browser_native_permission
        ),
        NativeAdPlacement(
            SCREEN_SET_DEFAULT,
            AdType.HEIGHT_222,
            IS_SHOW_NATIVE_SET_DEFAULT,
            R.string.id_private_browser_native_set_default
        ),
        NativeAdPlacement(
            SCREEN_HOME,
            AdType.HEIGHT_222,
            IS_SHOW_NATIVE_HOME,
            R.string.id_private_browser_native_home
        ),
        NativeAdPlacement(
            SCREEN_DOWNLOADS,
            AdType.ITEM,
            IS_SHOW_NATIVE_DOWNLOADS,
            R.string.id_private_browser_native_downloads
        ),
        NativeAdPlacement(
            SCREEN_BOOKMARKS,
            AdType.ITEM,
            IS_SHOW_NATIVE_BOOKMARKS,
            R.string.id_private_browser_native_bookmarks
        ),
        NativeAdPlacement(
            SCREEN_HISTORY,
            AdType.ITEM,
            IS_SHOW_NATIVE_HISTORY,
            R.string.id_private_browser_native_history
        ),
        NativeAdPlacement(
            SCREEN_SETTING,
            AdType.HEIGHT_222,
            IS_SHOW_NATIVE_SETTING,
            R.string.id_private_browser_native_setting
        ),
        NativeAdPlacement(
            DIALOG_EXIT_APP,
            AdType.HEIGHT_222,
            IS_SHOW_NATIVE_EXIT_DIALOG,
            R.string.id_private_browser_native_exit_dialog
        ),
        NativeAdPlacement(
            SCREEN_FILES_PHOTOS,
            AdType.COLLAPSE_MEDIUM,
            IS_SHOW_NATIVE_FILES_PHOTOS,
            R.string.id_private_browser_native_files_photos
        ),
        NativeAdPlacement(
            SCREEN_FILES_VIDEOS,
            AdType.COLLAPSE_MEDIUM,
            IS_SHOW_NATIVE_FILES_VIDEOS,
            R.string.id_private_browser_native_files_videos
        ),
        NativeAdPlacement(
            SCREEN_FILES_AUDIO,
            AdType.COLLAPSE_MEDIUM,
            IS_SHOW_NATIVE_FILES_AUDIO,
            R.string.id_private_browser_native_files_audio
        ),
        NativeAdPlacement(
            SCREEN_FILES_DOCUMENTS,
            AdType.COLLAPSE_MEDIUM,
            IS_SHOW_NATIVE_FILES_DOCUMENTS,
            R.string.id_private_browser_native_files_documents
        )
    )

    private val byScreenCode = all.associateBy(NativeAdPlacement::screenCode)

    fun find(screenCode: String): NativeAdPlacement? = byScreenCode[screenCode]
}
