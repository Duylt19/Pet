package com.asianmobile.emojibattery.shimeji.ads.ui.rewarded

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardedAdResultTest {
    @Test
    fun `earned reward continues flow`() {
        assertTrue(RewardedAdResult.EARNED.shouldContinueFlow)
    }

    @Test
    fun `unavailable reward continues flow`() {
        assertTrue(RewardedAdResult.UNAVAILABLE.shouldContinueFlow)
    }

    @Test
    fun `dismissed reward stops flow`() {
        assertFalse(RewardedAdResult.DISMISSED.shouldContinueFlow)
    }
}
