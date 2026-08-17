package com.asianmobile.emojibattery.shimeji.battery.overlay

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryOverlayVisibilityPolicyTest {
    @Test
    fun `fullscreen ad always hides the custom status bar`() {
        assertFalse(shouldAttachOverlay(isFullScreenAdShowing = true))
    }

    @Test
    fun `custom status bar returns after fullscreen ad closes`() {
        assertTrue(shouldAttachOverlay(isFullScreenAdShowing = false))
    }

    @Test
    fun `fullscreen ad does not override another hidden condition`() {
        assertFalse(
            shouldAttachOverlay(
                isFullScreenAdShowing = false,
                isExcludedApp = true
            )
        )
    }

    private fun shouldAttachOverlay(
        isFullScreenAdShowing: Boolean,
        isExcludedApp: Boolean = false
    ): Boolean = shouldAttachBatteryStatusOverlay(
        featureEnabled = true,
        configEnabled = true,
        isInteractive = true,
        isKeyguardLocked = false,
        isExcludedApp = isExcludedApp,
        isPortrait = true,
        isFullScreenAdShowing = isFullScreenAdShowing
    )
}
