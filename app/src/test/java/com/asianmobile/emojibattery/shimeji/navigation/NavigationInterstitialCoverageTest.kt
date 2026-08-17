package com.asianmobile.emojibattery.shimeji.navigation

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationInterstitialCoverageTest {

    @Test
    fun `every app bar back destination is wired through interstitial navigation`() {
        val navGraph = navigationSource("NavGraph.kt")

        APP_BAR_BACK_ROUTE_CONSTANTS.forEach { routeConstant ->
            val pattern = Regex(
                """safePopBackStackWithAd\(\s*context\s*=\s*context,\s*""" +
                    """currentRoute\s*=\s*Routes\.$routeConstant"""
            )
            assertTrue(
                "Routes.$routeConstant app-bar Back bypasses interstitial navigation",
                pattern.containsMatchIn(navGraph)
            )
        }

        assertTrue(
            "Premium Close bypasses its Back interstitial placement",
            Regex(
                """navigationAdPlacement\(\s*Routes\.PREMIUM,\s*""" +
                    """NavigationAdDirection\.BACK"""
            ).containsMatchIn(navGraph)
        )
    }

    @Test
    fun `home tab changes never request an interstitial`() {
        val homeGraph = navigationSource("HomeNavGraph.kt")
        val navGraph = navigationSource("NavGraph.kt")

        assertFalse(
            "Bottom navigation must switch Home tabs without an interstitial",
            homeGraph.contains("navigateWithAd(")
        )
        assertFalse(
            "A root hand-off to a Home tab must not introduce a tab interstitial",
            navGraph.contains("NavigationAdDirection.TAB")
        )
    }

    @Test
    fun `direct root navigation remains limited to documented completion paths`() {
        val navGraph = navigationSource("NavGraph.kt")

        assertEquals(
            "New user-driven forward navigation must use safeNavigateWithAd",
            EXPECTED_DIRECT_SAFE_NAVIGATIONS,
            Regex("""navController\.safeNavigate\(""").findAll(navGraph).count()
        )
        assertEquals(
            "New app-bar Back navigation must use safePopBackStackWithAd",
            EXPECTED_DIRECT_SAFE_POPS,
            Regex("""navController\.safePopBackStack\(""").findAll(navGraph).count()
        )
    }

    private fun navigationSource(fileName: String): String {
        val sourceRoot = sequenceOf(File("src/main/java"), File("app/src/main/java"))
            .firstOrNull(File::isDirectory)
            ?: error("Cannot find app source root from ${File(".").absolutePath}")
        return sourceRoot
            .resolve("com/asianmobile/emojibattery/shimeji/navigation/$fileName")
            .readText()
    }

    private companion object {
        val APP_BAR_BACK_ROUTE_CONSTANTS = setOf(
            "LANGUAGE_SETTINGS",
            "GRANT_PERMISSIONS",
            "ACCESSIBILITY_HOW_TO_USE",
            "SEARCH",
            "FAVOURITE_RECENT",
            "MY_PET",
            "BATTERY_CATEGORY",
            "BATTERY_EDITOR",
            "BATTERY_EDITOR_COMPONENT",
            "BATTERY_EDITOR_EMOTION_DETAIL",
            "BATTERY_TROLL",
            "BATTERY_TROLL_CUSTOMIZE"
        )

        // Splash routing, the already-wrapped Language transition and Premium completion paths.
        const val EXPECTED_DIRECT_SAFE_NAVIGATIONS = 4

        // Permission completion, granted Accessibility return and Premium completion/close body.
        const val EXPECTED_DIRECT_SAFE_POPS = 3
    }
}
