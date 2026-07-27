package com.asianmobile.emojibattery.shimeji.ads.ui.compose

import com.asianmobile.emojibattery.shimeji.ads.config.DIALOG_EXIT_APP
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_BOOKMARKS
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_DOWNLOADS
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_FILES_AUDIO
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_FILES_DOCUMENTS
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_FILES_PHOTOS
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_FILES_VIDEOS
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_HOME
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_HISTORY
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_INTRO
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_INTRO_FULL
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_INTRO_SECOND
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_LANGUAGE
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_LANGUAGE_SECOND
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_PERMISSION
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_SET_DEFAULT
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_SETTING
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeAdPlacementCatalogTest {

    @Test
    fun `screen codes and remote config keys are unique`() {
        val placements = NativeAdPlacementCatalog.all

        assertEquals(placements.size, placements.map { it.screenCode }.toSet().size)
        assertEquals(placements.size, placements.map { it.remoteConfigKey }.toSet().size)
    }

    @Test
    fun `all private browser native placements are registered`() {
        val expectedScreenCodes = setOf(
            SCREEN_LANGUAGE,
            SCREEN_LANGUAGE_SECOND,
            SCREEN_INTRO,
            SCREEN_INTRO_SECOND,
            SCREEN_INTRO_FULL,
            SCREEN_PERMISSION,
            SCREEN_SET_DEFAULT,
            SCREEN_HOME,
            SCREEN_DOWNLOADS,
            SCREEN_BOOKMARKS,
            SCREEN_HISTORY,
            SCREEN_SETTING,
            DIALOG_EXIT_APP,
            SCREEN_FILES_PHOTOS,
            SCREEN_FILES_VIDEOS,
            SCREEN_FILES_AUDIO,
            SCREEN_FILES_DOCUMENTS
        )

        expectedScreenCodes.forEach { screenCode ->
            assertNotNull(screenCode, NativeAdPlacementCatalog.find(screenCode))
        }
    }

    @Test
    fun `catalog screen codes use canonical format`() {
        NativeAdPlacementCatalog.all.forEach { placement ->
            assertTrue(
                "Invalid screen code: ${placement.screenCode}",
                placement.screenCode.matches(Regex("^[a-z][a-z0-9]*(?:_[a-z0-9]+)*$"))
            )
        }
    }

    @Test
    fun `bookmarks placement uses inline item layout`() {
        assertEquals(
            AdType.ITEM,
            NativeAdPlacementCatalog.find(SCREEN_BOOKMARKS)?.adType
        )
    }

    @Test
    fun `history placement uses inline item layout`() {
        assertEquals(
            AdType.ITEM,
            NativeAdPlacementCatalog.find(SCREEN_HISTORY)?.adType
        )
    }
}
