package com.asianmobile.emojibattery.shimeji.ui.onboarding.intro

import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_INTRO
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_INTRO_SECOND
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IntroScreenTrackingTest {

    @Test
    fun `every settled intro page maps to its own screen event`() {
        assertEquals(ScreenName.INTRO_PAGE_1, introPageScreenName(0))
        assertEquals(ScreenName.INTRO_PAGE_2, introPageScreenName(1))
        assertEquals(ScreenName.INTRO_PAGE_3, introPageScreenName(2))
    }

    @Test
    fun `native ads are mounted only on their settled intro pages`() {
        assertTrue(shouldLoadIntroNativeAd(pageIndex = 0, settledPage = 0))
        assertFalse(shouldLoadIntroNativeAd(pageIndex = 0, settledPage = 1))
        assertFalse(shouldLoadIntroNativeAd(pageIndex = 1, settledPage = 1))
        assertFalse(shouldLoadIntroNativeAd(pageIndex = 2, settledPage = 1))
        assertTrue(shouldLoadIntroNativeAd(pageIndex = 2, settledPage = 2))
    }

    @Test
    fun `each ad supported intro page uses its own placement`() {
        assertEquals(SCREEN_INTRO, introNativeAdScreenCode(0))
        assertEquals(null, introNativeAdScreenCode(1))
        assertEquals(SCREEN_INTRO_SECOND, introNativeAdScreenCode(2))
    }

    @Test
    fun `compact height scale follows the available aspect ratio`() {
        assertEquals(1f, introCompactHeightScale(widthDp = 360f, heightDp = 800f), 0.001f)
        assertEquals(0.9f, introCompactHeightScale(widthDp = 360f, heightDp = 720f), 0.001f)
        assertEquals(0.8f, introCompactHeightScale(widthDp = 450f, heightDp = 800f), 0.001f)
    }
}
