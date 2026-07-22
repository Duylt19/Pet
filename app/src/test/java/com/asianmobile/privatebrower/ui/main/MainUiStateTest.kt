package com.asianmobile.privatebrower.ui.main

import com.asianmobile.privatebrower.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Test

class MainUiStateTest {
    @Test
    fun `onboarding follows language intro default permission home order`() {
        assertEquals(Routes.LANGUAGE, MainUiState().getNextScreen())
        assertEquals(
            Routes.INTRO,
            MainUiState(isLanguageCompleted = true).getNextScreen()
        )
        assertEquals(
            Routes.SET_DEFAULT_BROWSER,
            completedState(defaultAccepted = false, permissionCompleted = false).getNextScreen()
        )
        assertEquals(
            Routes.PERMISSION,
            completedState(defaultAccepted = true, permissionCompleted = false).getNextScreen()
        )
        assertEquals(
            Routes.HOME,
            completedState(defaultAccepted = true, permissionCompleted = true).getNextScreen()
        )
    }

    @Test
    fun `default prompt returns next session after maybe later`() {
        val state = completedState(defaultAccepted = false, permissionCompleted = true)

        assertEquals(Routes.SET_DEFAULT_BROWSER, state.getNextScreen())
        assertEquals(Routes.HOME, state.getNextScreenAfterDefaultBrowser())
    }

    @Test
    fun `first session continues from default prompt to permission`() {
        val state = completedState(defaultAccepted = false, permissionCompleted = false)

        assertEquals(Routes.PERMISSION, state.getNextScreenAfterDefaultBrowser())
    }

    @Test
    fun `already default browser skips prompt but not permission`() {
        val state = completedState(defaultAccepted = false, permissionCompleted = false).copy(
            isAlreadyDefaultBrowser = true
        )

        assertEquals(Routes.PERMISSION, state.getNextScreen())
    }

    private fun completedState(
        defaultAccepted: Boolean,
        permissionCompleted: Boolean
    ) = MainUiState(
        isLanguageCompleted = true,
        isIntroCompleted = true,
        isPermissionCompleted = permissionCompleted,
        isDefaultBrowserAccepted = defaultAccepted
    )
}
