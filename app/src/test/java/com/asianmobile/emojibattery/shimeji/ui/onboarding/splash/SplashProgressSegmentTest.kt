package com.asianmobile.emojibattery.shimeji.ui.onboarding.splash

import org.junit.Assert.assertEquals
import org.junit.Test

class SplashProgressSegmentTest {

    @Test
    fun `segment grows from the start until the track is full`() {
        assertSegment(
            expectedStart = 0f,
            expectedEnd = 0f,
            actual = calculateSplashProgressSegment(phase = 0f),
        )
        assertSegment(
            expectedStart = 0f,
            expectedEnd = 1f,
            actual = calculateSplashProgressSegment(phase = 0.65f),
        )
    }

    @Test
    fun `segment tail catches the head before the animation restarts`() {
        assertSegment(
            expectedStart = 0.5f,
            expectedEnd = 1f,
            actual = calculateSplashProgressSegment(phase = 0.825f),
        )
        assertSegment(
            expectedStart = 1f,
            expectedEnd = 1f,
            actual = calculateSplashProgressSegment(phase = 1f),
        )
    }

    private fun assertSegment(
        expectedStart: Float,
        expectedEnd: Float,
        actual: SplashProgressSegment,
    ) {
        assertEquals(expectedStart, actual.startFraction, 0.0001f)
        assertEquals(expectedEnd, actual.endFraction, 0.0001f)
    }
}
