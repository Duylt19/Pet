package com.asianmobile.emojibattery.shimeji.ui.onboarding.intro

import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import org.junit.Assert.assertEquals
import org.junit.Test

class IntroScreenTrackingTest {

    @Test
    fun `every settled intro page maps to its own screen event`() {
        assertEquals(ScreenName.INTRO_PAGE_1, introPageScreenName(0))
        assertEquals(ScreenName.INTRO_PAGE_2, introPageScreenName(1))
        assertEquals(ScreenName.INTRO_PAGE_3, introPageScreenName(2))
    }
}
