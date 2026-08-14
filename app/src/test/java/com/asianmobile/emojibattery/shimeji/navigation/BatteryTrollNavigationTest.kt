package com.asianmobile.emojibattery.shimeji.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun `customize never renders the collapsible editor native`() {
        assertFalse(
            showBatteryEditorCollapsibleNative(Routes.batteryTrollCustomize(3), page = null)
        )
    }
}
