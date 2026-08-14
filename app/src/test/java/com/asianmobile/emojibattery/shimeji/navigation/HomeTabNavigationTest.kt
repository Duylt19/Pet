package com.asianmobile.emojibattery.shimeji.navigation

import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_BATTERY_EDITOR
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_CUSTOMIZE_STATUS_BAR
import com.asianmobile.emojibattery.shimeji.ui.home.shell.HomeTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeTabNavigationTest {
    @Test
    fun `top level routes map to the expected home tabs`() {
        assertEquals(HomeTab.DISCOVER, homeTabForRoute(Routes.DISCOVER))
        assertEquals(HomeTab.BATTERY, homeTabForRoute(Routes.BATTERY_CATALOG))
        assertEquals(HomeTab.PET_STORE, homeTabForRoute(Routes.PET_STORE))
        assertEquals(HomeTab.MINE, homeTabForRoute(Routes.SETTINGS))
        assertNull(homeTabForRoute(Routes.SEARCH))
        assertNull(homeTabForRoute(Routes.FAVOURITE_RECENT))
        assertNull(homeTabForRoute("${Routes.BATTERY_CATEGORY}/{categoryId}"))
    }

    @Test
    fun `system back treats every home tab as an equivalent app root`() {
        assertEquals(true, isHomeTopLevelRoute(Routes.DISCOVER))
        assertEquals(true, isHomeTopLevelRoute(Routes.BATTERY_CATALOG))
        assertEquals(true, isHomeTopLevelRoute(Routes.PET_STORE))
        assertEquals(true, isHomeTopLevelRoute(Routes.SETTINGS))
        assertEquals(false, isHomeTopLevelRoute(Routes.SEARCH))
        assertEquals(false, isHomeTopLevelRoute(Routes.MY_PET))
        assertEquals(false, isHomeTopLevelRoute(null))
    }

    @Test
    fun `every home tab maps to its top level route`() {
        assertEquals(Routes.DISCOVER, routeForHomeTab(HomeTab.DISCOVER))
        assertEquals(Routes.BATTERY_CATALOG, routeForHomeTab(HomeTab.BATTERY))
        assertEquals(Routes.PET_STORE, routeForHomeTab(HomeTab.PET_STORE))
        assertEquals(Routes.SETTINGS, routeForHomeTab(HomeTab.MINE))
    }

    @Test
    fun `home graph and discover destination have distinct stable routes`() {
        assertEquals("home_graph", Routes.HOME_GRAPH)
        assertEquals("discover", Routes.DISCOVER)
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
    fun `battery category replaces the home banner with its own native placement`() {
        assertEquals("${Routes.BATTERY_CATEGORY}/17", Routes.batteryCategory(17))
        assertEquals(true, showHomeBottomBanner(Routes.BATTERY_CATALOG))
        assertEquals(false, showHomeBottomBanner("${Routes.BATTERY_CATEGORY}/{categoryId}"))
        assertEquals(
            true,
            showBatteryCategoryBottomNative("${Routes.BATTERY_CATEGORY}/{categoryId}")
        )
        assertEquals(true, showBatteryCategoryBottomNative(Routes.batteryCategory(17)))
        assertEquals(false, showBatteryCategoryBottomNative(Routes.BATTERY_CATALOG))
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
    fun `customize status bar has a native placement separate from child editor pages`() {
        val editorRoute = "${Routes.BATTERY_EDITOR}/{themeId}"
        val componentRoute = "${Routes.BATTERY_EDITOR_COMPONENT}/{themeId}/{page}"
        val detailRoute = "${Routes.BATTERY_EDITOR_EMOTION_DETAIL}/{themeId}/{groupKey}"

        assertEquals(
            SCREEN_CUSTOMIZE_STATUS_BAR,
            batteryEditorCollapsibleNativeScreenCode(editorRoute, null)
        )
        assertEquals(
            "${Routes.BATTERY_EDITOR}/-1?backgroundId=23",
            Routes.batteryEditor(themeId = -1, backgroundId = 23)
        )
        assertEquals(
            SCREEN_BATTERY_EDITOR,
            batteryEditorCollapsibleNativeScreenCode(componentRoute, "EMOJI")
        )
        assertEquals(
            SCREEN_BATTERY_EDITOR,
            batteryEditorCollapsibleNativeScreenCode(detailRoute, null)
        )
    }

    @Test
    fun `each child editor back stack entry gets a fresh native reload key`() {
        val firstEntryKey = childEditorNativeReloadKey("editor-entry-1")
        val secondEntryKey = childEditorNativeReloadKey("editor-entry-2")

        assertEquals(true, firstEntryKey > 0)
        assertEquals(true, secondEntryKey > 0)
        assertEquals(false, firstEntryKey == secondEntryKey)
        assertEquals(0, childEditorNativeReloadKey(null))
    }
}
