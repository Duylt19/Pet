package com.asianmobile.emojibattery.shimeji.navigation

import com.asianmobile.emojibattery.shimeji.ui.shared.component.HomeTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeTabNavigationTest {
    @Test
    fun `top level routes map to the expected home tabs`() {
        assertEquals(HomeTab.DISCOVER, homeTabForRoute(Routes.HOME))
        assertEquals(HomeTab.BATTERY, homeTabForRoute(Routes.BATTERY_CATALOG))
        assertEquals(HomeTab.PET_STORE, homeTabForRoute(Routes.PET_STORE))
        assertEquals(HomeTab.MINE, homeTabForRoute(Routes.SETTINGS))
        assertNull(homeTabForRoute(Routes.SEARCH))
        assertNull(homeTabForRoute(Routes.FAVOURITE_RECENT))
        assertNull(homeTabForRoute("${Routes.BATTERY_CATEGORY}/{categoryId}"))
    }

    @Test
    fun `every home tab maps to its top level route`() {
        assertEquals(Routes.HOME, routeForHomeTab(HomeTab.DISCOVER))
        assertEquals(Routes.BATTERY_CATALOG, routeForHomeTab(HomeTab.BATTERY))
        assertEquals(Routes.PET_STORE, routeForHomeTab(HomeTab.PET_STORE))
        assertEquals(Routes.SETTINGS, routeForHomeTab(HomeTab.MINE))
    }

    @Test
    fun `overlay disclosure routes through the focused grant permissions screen`() {
        assertEquals(
            "${Routes.GRANT_PERMISSIONS}?" +
                "${Routes.GRANT_PERMISSIONS_REQUIRED_TARGET}=" +
                Routes.GRANT_PERMISSIONS_OVERLAY_TARGET,
            Routes.grantPermissionsForOverlay()
        )
    }

    @Test
    fun `battery category keeps the shared banner without showing as a home tab`() {
        assertEquals("${Routes.BATTERY_CATEGORY}/17", Routes.batteryCategory(17))
        assertEquals(true, showHomeBottomBanner(Routes.BATTERY_CATALOG))
        assertEquals(true, showHomeBottomBanner("${Routes.BATTERY_CATEGORY}/{categoryId}"))
        assertEquals(false, showHomeBottomBanner(Routes.SEARCH))
    }

    @Test
    fun `battery editor family keeps one editor banner placement`() {
        assertEquals(
            true,
            showBatteryEditorBottomBanner("${Routes.BATTERY_EDITOR}/{themeId}")
        )
        assertEquals(
            true,
            showBatteryEditorBottomBanner(
                "${Routes.BATTERY_EDITOR_COMPONENT}/{themeId}/{page}"
            )
        )
        assertEquals(
            true,
            showBatteryEditorBottomBanner(
                "${Routes.BATTERY_EDITOR_EMOTION_DETAIL}/{themeId}/{groupKey}"
            )
        )
        assertEquals(
            "${Routes.BATTERY_EDITOR_EMOTION_DETAIL}/3/molang",
            Routes.batteryEditorEmotionDetail(3, "molang")
        )
        assertEquals(false, showBatteryEditorBottomBanner(Routes.BATTERY_CATALOG))
    }

    @Test
    fun `status option pages replace the editor banner with one collapsible native`() {
        val route = "${Routes.BATTERY_EDITOR_COMPONENT}/{themeId}/{page}"

        assertEquals(true, showBatteryEditorCollapsibleNative(route, "AIRPLANE"))
        assertEquals(true, showBatteryEditorCollapsibleNative(route, "CLOCK"))
        assertEquals(true, showBatteryEditorCollapsibleNative(route, "ANIMATION"))
        assertEquals(true, showBatteryEditorCollapsibleNative(route, "WIFI"))
        assertEquals(true, showBatteryEditorCollapsibleNative(route, "SIGNAL"))
        assertEquals(true, showBatteryEditorCollapsibleNative(route, "DATA"))
        assertEquals(false, showBatteryEditorCollapsibleNative(route, "BACKGROUND_THEMES"))
        assertEquals(false, showBatteryEditorCollapsibleNative(Routes.BATTERY_CATALOG, "CHARGE"))
    }

    @Test
    fun `emotion list and detail keep the same collapsible native owner`() {
        val componentRoute = "${Routes.BATTERY_EDITOR_COMPONENT}/{themeId}/{page}"
        val detailRoute = "${Routes.BATTERY_EDITOR_EMOTION_DETAIL}/{themeId}/{groupKey}"

        assertEquals(true, showBatteryEditorCollapsibleNative(componentRoute, "EMOJI"))
        assertEquals(true, showBatteryEditorCollapsibleNative(detailRoute, null))
        assertEquals(false, showBatteryEditorCollapsibleNative(
            "${Routes.BATTERY_EDITOR}/{themeId}",
            null
        ))
    }
}
