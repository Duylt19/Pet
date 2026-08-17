package com.asianmobile.emojibattery.shimeji.navigation

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiPackageArchitectureTest {

    @Test
    fun `every Home tab owns its screen view model and ui state`() {
        val sourceRoot = sourceRoot()
        HOME_TAB_FEATURES.forEach { feature ->
            listOf("Screen", "ViewModel", "UiState").forEach { role ->
                val relativePath = "ui/home/${feature.packageName}/" +
                    "${feature.typePrefix}$role.kt"
                assertTrue(
                    "Missing Home ${feature.packageName} $role: $relativePath",
                    sourceRoot.resolve(relativePath).isFile
                )
            }
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
        val HOME_TAB_FEATURES = listOf(
            HomeFeature("discover", "Discover"),
            HomeFeature("battery", "BatteryHome"),
            HomeFeature("pet", "ShimejiPets"),
            HomeFeature("mine", "Mine")
        )
    }

    private data class HomeFeature(val packageName: String, val typePrefix: String)
}
