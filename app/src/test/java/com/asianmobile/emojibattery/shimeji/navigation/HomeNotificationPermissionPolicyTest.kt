package com.asianmobile.emojibattery.shimeji.navigation

import android.os.Build
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeNotificationPermissionPolicyTest {
    @Test
    fun `requests when an Android 13 user reaches Home in a fresh app session`() {
        assertTrue(
            shouldRequestHomeNotificationPermission(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                isGranted = false,
                hasRequestedThisSession = false,
                isHomeTopLevelVisible = true,
                isFullScreenAdShowing = false
            )
        )
    }

    @Test
    fun `waits until Home is visible`() {
        assertFalse(policy(isHomeTopLevelVisible = false))
    }

    @Test
    fun `never repeats within a session or asks for an existing grant`() {
        assertFalse(policy(hasRequestedThisSession = true))
        assertFalse(policy(isGranted = true))
    }

    @Test
    fun `does not compete with a full screen ad`() {
        assertFalse(policy(isFullScreenAdShowing = true))
    }

    @Test
    fun `does not request the runtime permission below Android 13`() {
        assertFalse(policy(sdkInt = Build.VERSION_CODES.S_V2))
    }

    private fun policy(
        sdkInt: Int = Build.VERSION_CODES.TIRAMISU,
        isGranted: Boolean = false,
        hasRequestedThisSession: Boolean = false,
        isHomeTopLevelVisible: Boolean = true,
        isFullScreenAdShowing: Boolean = false
    ) = shouldRequestHomeNotificationPermission(
        sdkInt = sdkInt,
        isGranted = isGranted,
        hasRequestedThisSession = hasRequestedThisSession,
        isHomeTopLevelVisible = isHomeTopLevelVisible,
        isFullScreenAdShowing = isFullScreenAdShowing
    )
}
