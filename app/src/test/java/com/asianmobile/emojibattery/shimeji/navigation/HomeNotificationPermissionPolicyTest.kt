package com.asianmobile.emojibattery.shimeji.navigation

import android.os.Build
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeNotificationPermissionPolicyTest {
    @Test
    fun `requests once when an Android 13 user reaches a Home tab`() {
        assertTrue(
            shouldRequestHomeNotificationPermission(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                isGranted = false,
                hasRequestedBefore = false,
                isHomeTopLevelVisible = true,
                isFullScreenAdShowing = false
            )
        )
    }

    @Test
    fun `waits until DataStore is loaded and Home is visible`() {
        assertFalse(policy(hasRequestedBefore = null))
        assertFalse(policy(isHomeTopLevelVisible = false))
    }

    @Test
    fun `never repeats a handled prompt or asks for an existing grant`() {
        assertFalse(policy(hasRequestedBefore = true))
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
        hasRequestedBefore: Boolean? = false,
        isHomeTopLevelVisible: Boolean = true,
        isFullScreenAdShowing: Boolean = false
    ) = shouldRequestHomeNotificationPermission(
        sdkInt = sdkInt,
        isGranted = isGranted,
        hasRequestedBefore = hasRequestedBefore,
        isHomeTopLevelVisible = isHomeTopLevelVisible,
        isFullScreenAdShowing = isFullScreenAdShowing
    )
}
