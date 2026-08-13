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
            assertTrue(
                "$screenFunction is a navigation destination but has no TrackScreenView call",
                owners.values.single().contains("TrackScreenView(")
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
    }
}
