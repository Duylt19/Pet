package com.asianmobile.emojibattery.shimeji.navigation

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeBackHandlingContractTest {

    @Test
    fun `home back is owned by the navigation composition`() {
        val navGraph = sourceFile(
            "com/asianmobile/emojibattery/shimeji/navigation/NavGraph.kt"
        ).readText()
        val homeNavGraph = sourceFile(
            "com/asianmobile/emojibattery/shimeji/navigation/HomeNavGraph.kt"
        ).readText()
        val mainActivity = sourceFile(
            "com/asianmobile/emojibattery/shimeji/MainActivity.kt"
        ).readText()

        assertTrue(navGraph.contains("composable(Routes.HOME_GRAPH)"))
        assertTrue(navGraph.contains("onHomeBack = onHomeBack"))
        assertTrue(
            homeNavGraph.contains(
                "BackHandler(enabled = canHandleHomeBack, onBack = onHomeBack)"
            )
        )
        assertTrue(homeNavGraph.contains("HOME_BACK_HANDOFF_DELAY_MS"))
        assertTrue(homeNavGraph.contains("val homeNavController = rememberNavController()"))
        assertTrue(homeNavGraph.contains("startDestination = Routes.DISCOVER"))
        listOf(
            "Routes.DISCOVER",
            "Routes.BATTERY_CATALOG",
            "Routes.PET_STORE",
            "Routes.SETTINGS"
        ).forEach { route ->
            assertTrue(homeNavGraph.contains("composable($route)"))
            assertFalse(navGraph.contains("composable($route)"))
        }
        assertTrue(mainActivity.contains("onHomeBack = { showExitDialog = true }"))
        assertFalse(mainActivity.contains("onBackPressedDispatcher.addCallback"))
    }

    @Test
    fun `root destinations do not cross fade or share a global ad footer`() {
        val navGraph = sourceFile(
            "com/asianmobile/emojibattery/shimeji/navigation/NavGraph.kt"
        ).readText()

        assertTrue(navGraph.contains("enterTransition = { EnterTransition.None }"))
        assertTrue(navGraph.contains("exitTransition = { ExitTransition.None }"))
        assertTrue(navGraph.contains("private fun IsolatedDestination("))
        assertTrue(navGraph.contains("bottomAd: @Composable () -> Unit"))
        assertFalse(navGraph.contains("if (shouldShowBatteryCategoryBottomNative)"))
        assertFalse(navGraph.contains("else if (batteryEditorNativeScreenCode != null)"))
    }

    private fun sourceFile(relativePath: String): File =
        sequenceOf(File("src/main/java/$relativePath"), File("app/src/main/java/$relativePath"))
            .firstOrNull(File::isFile)
            ?: error("Cannot find $relativePath from ${File(".").absolutePath}")
}
