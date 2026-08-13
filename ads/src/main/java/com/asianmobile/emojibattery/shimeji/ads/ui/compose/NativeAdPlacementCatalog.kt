package com.asianmobile.emojibattery.shimeji.ads.ui.compose

import androidx.annotation.StringRes
import com.asianmobile.emojibattery.shimeji.ads.R
import com.asianmobile.emojibattery.shimeji.ads.config.*

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
            R.string.id_emoji_battery_native_language
        ),
        NativeAdPlacement(
            SCREEN_LANGUAGE_SECOND,
            AdType.HEIGHT_222,
            IS_SHOW_NATIVE_LANGUAGE_SECOND,
            R.string.id_emoji_battery_native_language_second
        ),
        NativeAdPlacement(
            SCREEN_INTRO,
            AdType.HEIGHT_222,
            IS_SHOW_NATIVE_INTRO,
            R.string.id_emoji_battery_native_intro
        ),
        NativeAdPlacement(
            SCREEN_INTRO_SECOND,
            AdType.HEIGHT_222,
            IS_SHOW_NATIVE_INTRO_SECOND,
            R.string.id_emoji_battery_native_intro_second
        ),
        NativeAdPlacement(
            SCREEN_PERMISSION,
            AdType.HEIGHT_222,
            IS_SHOW_NATIVE_PERMISSION,
            R.string.id_emoji_battery_native_permission
        ),
        NativeAdPlacement(
            SCREEN_GRANT_PERMISSIONS,
            AdType.HEIGHT_222,
            IS_SHOW_NATIVE_GRANT_PERMISSIONS,
            R.string.id_emoji_battery_native_grant_permissions
        ),
        NativeAdPlacement(
            DIALOG_ACCESSIBILITY_DISCLOSURE,
            AdType.HEIGHT_222,
            IS_SHOW_NATIVE_ACCESSIBILITY_DISCLOSURE,
            R.string.id_emoji_battery_native_accessibility_disclosure
        ),
        NativeAdPlacement(
            DIALOG_OVERLAY_PERMISSION,
            AdType.HEIGHT_222,
            IS_SHOW_NATIVE_OVERLAY_PERMISSION,
            R.string.id_emoji_battery_native_overlay_permission
        ),
        NativeAdPlacement(
            SCREEN_SEARCH,
            AdType.HEIGHT_222,
            IS_SHOW_NATIVE_SEARCH,
            R.string.id_emoji_battery_native_search
        ),
        NativeAdPlacement(
            SCREEN_FAVOURITE_RECENT,
            AdType.HEIGHT_222,
            IS_SHOW_NATIVE_FAVOURITE_RECENT,
            R.string.id_emoji_battery_native_favourite_recent
        ),
        NativeAdPlacement(
            SCREEN_BATTERY_CATALOG,
            AdType.HEIGHT_150,
            IS_SHOW_NATIVE_BATTERY_CATALOG,
            R.string.id_emoji_battery_native_battery_catalog
        ),
        NativeAdPlacement(
            SCREEN_BATTERY_EDITOR,
            AdType.COLLAPSE_SMALL,
            IS_SHOW_NATIVE_BATTERY_EDITOR,
            R.string.id_emoji_battery_native_battery_editor
        ),
        NativeAdPlacement(
            DIALOG_BATTERY_REWARD,
            AdType.HEIGHT_222,
            IS_SHOW_NATIVE_BATTERY_REWARD,
            R.string.id_emoji_battery_native_battery_reward
        ),
        NativeAdPlacement(
            DIALOG_BATTERY_DISCARD,
            AdType.HEIGHT_222,
            IS_SHOW_NATIVE_BATTERY_DISCARD,
            R.string.id_emoji_battery_native_battery_discard
        ),
        NativeAdPlacement(
            DIALOG_PET_REWARD,
            AdType.HEIGHT_222,
            IS_SHOW_NATIVE_PET_REWARD,
            R.string.id_emoji_battery_native_pet_reward
        ),
        NativeAdPlacement(
            DIALOG_FOOD_REWARD,
            AdType.HEIGHT_222,
            IS_SHOW_NATIVE_FOOD_REWARD,
            R.string.id_emoji_battery_native_food_reward
        ),
        NativeAdPlacement(
            DIALOG_BATTERY_TROLL_REWARD,
            AdType.HEIGHT_222,
            IS_SHOW_NATIVE_BATTERY_TROLL_REWARD,
            R.string.id_emoji_battery_native_battery_troll_reward
        ),
        NativeAdPlacement(
            DIALOG_EXIT_APP,
            AdType.HEIGHT_222,
            IS_SHOW_NATIVE_EXIT_DIALOG,
            R.string.id_emoji_battery_native_exit_dialog
        )
    )

    private val byScreenCode = all.associateBy(NativeAdPlacement::screenCode)

    fun find(screenCode: String): NativeAdPlacement? = byScreenCode[screenCode]
}
