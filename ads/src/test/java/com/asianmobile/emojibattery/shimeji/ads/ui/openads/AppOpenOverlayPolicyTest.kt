package com.asianmobile.emojibattery.shimeji.ads.ui.openads

import org.junit.Assert.assertEquals
import org.junit.Test

class AppOpenOverlayPolicyTest {
    @Test
    fun `welcome back cover does not activate fullscreen overlay state`() {
        assertEquals(
            AppOpenOverlayDirective(
                isFullscreenAdShowing = false,
                hideActivityContent = false
            ),
            AppOpenOverlayPolicy.directive(AppOpenPresentationStage.WELCOME_BACK_COVER)
        )
    }

    @Test
    fun `fullscreen app open protects overlays while keeping host screen rendered`() {
        assertEquals(
            AppOpenOverlayDirective(
                isFullscreenAdShowing = true,
                hideActivityContent = false
            ),
            AppOpenOverlayPolicy.directive(AppOpenPresentationStage.FULLSCREEN_AD)
        )
    }

    @Test
    fun `idle restores all app owned surfaces`() {
        assertEquals(
            AppOpenOverlayDirective(
                isFullscreenAdShowing = false,
                hideActivityContent = false
            ),
            AppOpenOverlayPolicy.directive(AppOpenPresentationStage.IDLE)
        )
    }
}
