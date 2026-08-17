package com.asianmobile.emojibattery.shimeji.navigation

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenTrackingCoverageTest {

    @Test
    fun `every navigation screen owns a screen view tracker`() {
        val sourceRoot = sourceRoot()
        val destinationScreens = NAV_GRAPH_FILES.asSequence()
            .map { relativePath -> sourceRoot.resolve(relativePath).readText() }
            .flatMap { source -> SCREEN_CALL_PATTERN.findAll(source) }
            .map { match -> match.groupValues[1] }
            .toSet()

        assertTrue("No destination screens found in NavGraph", destinationScreens.isNotEmpty())
        assertEquals(
            "Update the canonical tracker inventory when a navigation screen changes",
            EXPECTED_TRACKERS.keys,
            destinationScreens
        )

        val kotlinSources = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .associateWith(File::readText)

        destinationScreens.forEach { screenFunction ->
            val owners = kotlinSources.filterValues { source ->
                Regex("""fun\s+$screenFunction\s*\(""").containsMatchIn(source)
            }
            assertEquals(
                "Expected one source owner for $screenFunction, found ${owners.keys}",
                1,
                owners.size
            )
            val ownerSource = owners.values.single()
            assertTrue(
                "$screenFunction is a navigation destination but has no TrackScreenView call",
                ownerSource.contains("TrackScreenView(")
            )
            assertTrue(
                "$screenFunction does not track its canonical visible screen",
                EXPECTED_TRACKERS.getValue(screenFunction).containsMatchIn(ownerSource)
            )
        }
    }

    private fun sourceRoot(): File =
        sequenceOf(File("src/main/java"), File("app/src/main/java"))
            .firstOrNull(File::isDirectory)
            ?: error("Cannot find app source root from ${File(".").absolutePath}")

    private companion object {
        val NAV_GRAPH_FILES = listOf(
            "com/asianmobile/emojibattery/shimeji/navigation/NavGraph.kt",
            "com/asianmobile/emojibattery/shimeji/navigation/HomeNavGraph.kt"
        )
        val SCREEN_CALL_PATTERN = Regex("""\b([A-Z][A-Za-z0-9]*Screen)\(""")
        val EXPECTED_TRACKERS = mapOf(
            "AccessibilityHowToUseScreen" to trackerFor("ACCESSIBILITY_HOW_TO_USE"),
            "BatteryCatalogScreen" to trackerFor("BATTERY_CATALOG"),
            "BatteryCategoryScreen" to trackerFor("BATTERY_CATEGORY"),
            "BatteryEditorScreen" to Regex(
                """TrackScreenView\(\s*page\.analyticsScreen\(\)\s*\)"""
            ),
            "BatteryTrollCustomizeScreen" to trackerFor("BATTERY_TROLL_CUSTOMIZE"),
            "BatteryTrollScreen" to trackerFor("BATTERY_TROLL"),
            "DiscoverScreen" to trackerFor("DISCOVER"),
            "FavouriteRecentScreen" to trackerFor("FAVOURITE_RECENT"),
            "GrantPermissionsScreen" to trackerFor("GRANT_PERMISSIONS"),
            "IntroScreen" to Regex(
                """TrackScreenView\(\s*screen\s*=\s*introPageScreenName\(pageIndex\),"""
            ),
            "LanguageScreen" to Regex(
                """TrackScreenView\(\s*if\s*\(isSettings\)\s*""" +
                    """ScreenName\.LANGUAGE_SETTINGS\s*else\s*""" +
                    """ScreenName\.LANGUAGE_ONBOARDING\s*\)"""
            ),
            "PermissionScreen" to trackerFor("PERMISSION"),
            "PetRoomScreen" to trackerFor("MY_PET"),
            "PetStoreScreen" to trackerFor("PET_STORE"),
            "PremiumScreen" to trackerFor("PREMIUM"),
            "SearchScreen" to trackerFor("SEARCH"),
            "SettingsScreen" to trackerFor("SETTINGS"),
            "SplashScreen" to trackerFor("SPLASH")
        )

        fun trackerFor(screenName: String): Regex = Regex(
            """TrackScreenView\(\s*ScreenName\.$screenName\s*\)"""
        )
    }
}
