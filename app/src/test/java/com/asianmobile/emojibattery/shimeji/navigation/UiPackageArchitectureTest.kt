package com.asianmobile.emojibattery.shimeji.navigation

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiPackageArchitectureTest {

    @Test
    fun `all Home tab entry screens are owned by the home package`() {
        val sourceRoot = sourceRoot()
        HOME_TAB_ENTRIES.forEach { relativePath ->
            assertTrue(
                "Missing Home tab entry: $relativePath",
                sourceRoot.resolve(relativePath).isFile
            )
        }
    }

    @Test
    fun `app wide permission screens are not owned by Mine`() {
        val sourceRoot = sourceRoot()
        assertTrue(sourceRoot.resolve("ui/permissions/GrantPermissionsScreen.kt").isFile)
        assertTrue(sourceRoot.resolve("ui/permissions/AccessibilityHowToUseScreen.kt").isFile)
        assertFalse(
            sourceRoot.resolve("ui/settings/permissions")
                .walkTopDown()
                .any { it.isFile && it.extension == "kt" }
        )
    }

    private fun sourceRoot(): File =
        sequenceOf(File("src/main/java"), File("app/src/main/java"))
            .firstOrNull(File::isDirectory)
            ?.resolve("com/asianmobile/emojibattery/shimeji")
            ?: error("Cannot find app source root from ${File(".").absolutePath}")

    private companion object {
        val HOME_TAB_ENTRIES = listOf(
            "ui/home/discover/DiscoverScreen.kt",
            "ui/home/battery/BatteryHomeScreen.kt",
            "ui/home/pet/ShimejiPetsScreen.kt",
            "ui/home/mine/MineScreen.kt"
        )
    }
}
