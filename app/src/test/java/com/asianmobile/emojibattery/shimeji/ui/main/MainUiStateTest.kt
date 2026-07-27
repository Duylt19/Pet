package com.asianmobile.emojibattery.shimeji.ui.main

import com.asianmobile.emojibattery.shimeji.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainUiStateTest {

    @Test
    fun `onboarding follows language intro permission home order`() {
        assertEquals(Routes.LANGUAGE, MainUiState().getNextScreen())
        assertEquals(
            Routes.INTRO,
            MainUiState(isLanguageCompleted = true).getNextScreen()
        )
        assertEquals(
            Routes.PERMISSION,
            completedState(permissionCompleted = false).getNextScreen()
        )
        assertEquals(
            Routes.HOME,
            completedState(permissionCompleted = true).getNextScreen()
        )
    }

    @Test
    fun `state is ready only after all onboarding values are loaded`() {
        assertFalse(MainUiState().isReady())
        assertTrue(completedState(permissionCompleted = false).isReady())
    }

    private fun completedState(permissionCompleted: Boolean) = MainUiState(
        isLanguageCompleted = true,
        isIntroCompleted = true,
        isPermissionCompleted = permissionCompleted
    )
}
