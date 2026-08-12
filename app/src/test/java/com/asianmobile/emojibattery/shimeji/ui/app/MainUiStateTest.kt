package com.asianmobile.emojibattery.shimeji.ui.app

import com.asianmobile.emojibattery.shimeji.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainUiStateTest {

    @Test
    fun `onboarding temporarily skips first permission after intro`() {
        assertEquals(Routes.LANGUAGE, MainUiState().getNextScreen())
        assertEquals(
            Routes.INTRO,
            MainUiState(isLanguageCompleted = true).getNextScreen()
        )
        assertEquals(
            Routes.HOME,
            completedState(permissionCompleted = false).getNextScreen()
        )
        assertEquals(
            Routes.HOME,
            completedState(permissionCompleted = true).getNextScreen()
        )
    }

    @Test
    fun `permission state does not block readiness while first permission is disabled`() {
        assertFalse(MainUiState().isReady())
        assertTrue(
            MainUiState(
                isLanguageCompleted = true,
                isIntroCompleted = true,
                isPermissionCompleted = null
            ).isReady()
        )
    }

    @Test
    fun `post intro destination is home while first permission is disabled`() {
        assertEquals(Routes.HOME, destinationAfterIntro())
    }

    private fun completedState(permissionCompleted: Boolean) = MainUiState(
        isLanguageCompleted = true,
        isIntroCompleted = true,
        isPermissionCompleted = permissionCompleted
    )
}
