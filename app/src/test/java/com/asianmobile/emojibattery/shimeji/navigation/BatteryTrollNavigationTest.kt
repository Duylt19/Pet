package com.asianmobile.emojibattery.shimeji.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryTrollNavigationTest {

    @Test
    fun `the customize route carries the troll it was opened for`() {
        assertEquals("battery_troll_customize/7", Routes.batteryTrollCustomize(7))
    }

    @Test
    fun `neither troll route is a home tab so the bottom navigation stays hidden`() {
        assertNull(homeTabForRoute(Routes.BATTERY_TROLL))
        assertNull(homeTabForRoute("${Routes.BATTERY_TROLL_CUSTOMIZE}/{trollId}"))
        assertFalse(isHomeTopLevelRoute(Routes.BATTERY_TROLL))
        assertFalse(isHomeTopLevelRoute(Routes.batteryTrollCustomize(1)))
    }

    @Test
    fun `the themes grid keeps the shared home banner`() {
        assertTrue(showHomeBottomBanner(Routes.BATTERY_TROLL))
    }

    @Test
    fun `customize uses the editor banner instead of the home one`() {
        val route = Routes.batteryTrollCustomize(3)
        assertTrue(showBatteryEditorBottomBanner(route))
        // Two banners on one screen would double-render; the grid and customize must not
        // both claim the same holder.
        assertFalse(showHomeBottomBanner(route))
    }

    @Test
    fun `customize never renders the collapsible editor native`() {
        assertFalse(
            showBatteryEditorCollapsibleNative(Routes.batteryTrollCustomize(3), page = null)
        )
    }

    @Test
    fun `the troll routes do not collide with the battery editor prefixes`() {
        assertFalse(showBatteryEditorBottomBanner(Routes.BATTERY_TROLL))
        assertFalse(showHomeBottomBanner("${Routes.BATTERY_TROLL_CUSTOMIZE}/9"))
    }
}
