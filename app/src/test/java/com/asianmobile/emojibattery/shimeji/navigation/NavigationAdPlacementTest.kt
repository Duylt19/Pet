package com.asianmobile.emojibattery.shimeji.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationAdPlacementTest {

    @Test
    fun `forward placement removes dynamic route values`() {
        assertEquals(
            "navigation_forward_battery_editor",
            navigationAdPlacement(
                route = "${Routes.BATTERY_EDITOR}/42?backgroundId=7",
                direction = NavigationAdDirection.FORWARD
            )
        )
    }

    @Test
    fun `home tab and app bar back use distinct stable placements`() {
        assertEquals(
            "navigation_tab_pet_store",
            navigationAdPlacement(Routes.PET_STORE, NavigationAdDirection.TAB)
        )
        assertEquals(
            "navigation_back_search",
            navigationAdPlacement(Routes.SEARCH, NavigationAdDirection.BACK)
        )
    }

    @Test
    fun `empty route never creates an empty analytics placement`() {
        assertEquals(
            "navigation_forward_unknown",
            navigationAdPlacement("", NavigationAdDirection.FORWARD)
        )
    }
}
