package com.asianmobile.emojibattery.shimeji.ads.ui.openads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppOpenOverlayPolicyTest {
    @Test
    fun `welcome back cover keeps the app content available behind its branded window`() {
        assertFalse(
            AppOpenOverlayPolicy.shouldHideAppContent(
                AppOpenPresentationStage.WELCOME_BACK_COVER
            )
        )
    }

    @Test
    fun `only the actual fullscreen ad hides app owned content`() {
        assertFalse(AppOpenOverlayPolicy.shouldHideAppContent(AppOpenPresentationStage.IDLE))
        assertTrue(
            AppOpenOverlayPolicy.shouldHideAppContent(AppOpenPresentationStage.FULLSCREEN_AD)
        )
    }
}
