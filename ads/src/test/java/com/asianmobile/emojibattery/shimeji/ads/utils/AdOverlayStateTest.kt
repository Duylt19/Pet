package com.asianmobile.emojibattery.shimeji.ads.utils

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdOverlayStateTest {
    @After
    fun tearDown() {
        AdOverlayState.hide()
    }

    @Test
    fun `show keeps fullscreen ad state active until explicit hide`() {
        AdOverlayState.show()

        assertTrue(AdOverlayState.isAdShowing.value)
        assertTrue(AdOverlayState.shouldHideActivityContent.value)

        AdOverlayState.hide()
        assertFalse(AdOverlayState.isAdShowing.value)
        assertFalse(AdOverlayState.shouldHideActivityContent.value)
    }

    @Test
    fun `app open can protect overlays without hiding activity content`() {
        AdOverlayState.show(hideActivityContent = false)

        assertTrue(AdOverlayState.isAdShowing.value)
        assertFalse(AdOverlayState.shouldHideActivityContent.value)
    }
}
