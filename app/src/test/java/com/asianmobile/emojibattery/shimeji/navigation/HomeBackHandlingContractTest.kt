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
        val mainActivity = sourceFile(
            "com/asianmobile/emojibattery/shimeji/MainActivity.kt"
        ).readText()

        assertTrue(navGraph.contains("enabled = isHomeTopLevelRoute(currentRoute)"))
        assertTrue(navGraph.contains("onBack = onHomeBack"))
        assertTrue(mainActivity.contains("onHomeBack = { showExitDialog = true }"))
        assertFalse(mainActivity.contains("onBackPressedDispatcher.addCallback"))
    }

    private fun sourceFile(relativePath: String): File =
        sequenceOf(File("src/main/java/$relativePath"), File("app/src/main/java/$relativePath"))
            .firstOrNull(File::isFile)
            ?: error("Cannot find $relativePath from ${File(".").absolutePath}")
}
