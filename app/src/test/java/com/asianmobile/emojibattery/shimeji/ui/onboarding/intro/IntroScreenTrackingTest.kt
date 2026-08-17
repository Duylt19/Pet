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
    fun `only settled page is visible to tracking while adjacent pages are preloaded`() {
        INTRO_PAGE_INDICES.forEach { settledPage ->
            INTRO_PAGE_INDICES.forEach { composedPage ->
                assertEquals(
                    composedPage == settledPage,
                    isIntroPageVisible(
                        pageIndex = composedPage,
                        settledPage = settledPage,
                    ),
                )
            }
        }
    }

    @Test
    fun `first native stays mounted and last native starts after its first visit`() {
        assertTrue(shouldKeepIntroNativeAdMounted(pageIndex = 0, hasVisitedLastPage = false))
        assertTrue(shouldKeepIntroNativeAdMounted(pageIndex = 0, hasVisitedLastPage = true))
        assertFalse(shouldKeepIntroNativeAdMounted(pageIndex = 1, hasVisitedLastPage = false))
        assertFalse(shouldKeepIntroNativeAdMounted(pageIndex = 1, hasVisitedLastPage = true))
        assertFalse(shouldKeepIntroNativeAdMounted(pageIndex = 2, hasVisitedLastPage = false))
        assertTrue(shouldKeepIntroNativeAdMounted(pageIndex = 2, hasVisitedLastPage = true))
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

    private companion object {
        val INTRO_PAGE_INDICES = 0..2
    }
}
