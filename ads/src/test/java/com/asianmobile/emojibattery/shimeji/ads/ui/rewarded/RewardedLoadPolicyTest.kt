package com.asianmobile.emojibattery.shimeji.ads.ui.rewarded

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardedLoadPolicyTest {
    @Test
    fun `starts load only when no request and no ready ad exist`() {
        assertTrue(shouldStartRewardedLoad(isLoading = false, isAdReady = false))
        assertFalse(shouldStartRewardedLoad(isLoading = true, isAdReady = false))
        assertFalse(shouldStartRewardedLoad(isLoading = false, isAdReady = true))
        assertFalse(shouldStartRewardedLoad(isLoading = true, isAdReady = true))
    }
}
