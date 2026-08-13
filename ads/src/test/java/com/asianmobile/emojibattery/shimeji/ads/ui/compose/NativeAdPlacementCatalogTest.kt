package com.asianmobile.emojibattery.shimeji.ads.ui.compose

import com.asianmobile.emojibattery.shimeji.ads.config.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeAdPlacementCatalogTest {

    @Test
    fun `screen codes remote config keys and ad unit resources are unique`() {
        val placements = NativeAdPlacementCatalog.all

        assertEquals(placements.size, placements.map { it.screenCode }.toSet().size)
        assertEquals(placements.size, placements.map { it.remoteConfigKey }.toSet().size)
        assertEquals(placements.size, placements.map { it.adUnitResId }.toSet().size)
    }

    @Test
    fun `all native placements are registered`() {
        val expectedScreenCodes = setOf(
            SCREEN_LANGUAGE,
            SCREEN_LANGUAGE_SECOND,
            SCREEN_INTRO,
            SCREEN_INTRO_SECOND,
            SCREEN_PERMISSION,
            SCREEN_GRANT_PERMISSIONS,
            DIALOG_ACCESSIBILITY_DISCLOSURE,
            DIALOG_OVERLAY_PERMISSION,
            SCREEN_SEARCH,
            SCREEN_FAVOURITE_RECENT,
            SCREEN_BATTERY_CATALOG,
            SCREEN_CUSTOMIZE_STATUS_BAR,
            SCREEN_BATTERY_EDITOR,
            DIALOG_BATTERY_REWARD,
            DIALOG_BATTERY_DISCARD,
            DIALOG_PET_REWARD,
            DIALOG_FOOD_REWARD,
            DIALOG_BATTERY_TROLL_REWARD,
            DIALOG_EXIT_APP
        )

        assertEquals(
            expectedScreenCodes,
            NativeAdPlacementCatalog.all.map { it.screenCode }.toSet()
        )
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
    fun `intro pages use independently configurable string resources`() {
        val firstPage = NativeAdPlacementCatalog.find(SCREEN_INTRO)
        val secondPage = NativeAdPlacementCatalog.find(SCREEN_INTRO_SECOND)

        assertNotNull(firstPage)
        assertNotNull(secondPage)
        assertTrue(firstPage?.adUnitResId != secondPage?.adUnitResId)
    }

    @Test
    fun `feature placements keep their designed native layouts`() {
        assertEquals(AdType.HEIGHT_150, placementType(SCREEN_BATTERY_CATALOG))
        assertEquals(AdType.COLLAPSE_SMALL, placementType(SCREEN_CUSTOMIZE_STATUS_BAR))
        assertEquals(AdType.COLLAPSE_SMALL, placementType(SCREEN_BATTERY_EDITOR))

        setOf(
            SCREEN_GRANT_PERMISSIONS,
            DIALOG_ACCESSIBILITY_DISCLOSURE,
            DIALOG_OVERLAY_PERMISSION,
            SCREEN_SEARCH,
            SCREEN_FAVOURITE_RECENT,
            DIALOG_BATTERY_REWARD,
            DIALOG_BATTERY_DISCARD,
            DIALOG_PET_REWARD,
            DIALOG_FOOD_REWARD,
            DIALOG_BATTERY_TROLL_REWARD
        ).forEach { screenCode ->
            assertEquals(screenCode, AdType.HEIGHT_222, placementType(screenCode))
        }
    }

    private fun placementType(screenCode: String): AdType? =
        NativeAdPlacementCatalog.find(screenCode)?.adType
}
