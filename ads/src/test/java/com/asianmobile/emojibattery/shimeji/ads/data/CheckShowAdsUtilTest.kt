package com.asianmobile.emojibattery.shimeji.ads.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckShowAdsUtilTest {

    @Test
    fun `rewarded is eligible when all gates allow ads`() {
        assertTrue(
            shouldShowRewardedAd(
                isAdsEnabled = true,
                isRewardedEnabled = true,
                isAdLimitReached = false
            )
        )
    }

    @Test
    fun `rewarded is disabled for premium or ad free user`() {
        assertFalse(
            shouldShowRewardedAd(
                isAdsEnabled = false,
                isRewardedEnabled = true,
                isAdLimitReached = false
            )
        )
    }

    @Test
    fun `rewarded is disabled by remote config`() {
        assertFalse(
            shouldShowRewardedAd(
                isAdsEnabled = true,
                isRewardedEnabled = false,
                isAdLimitReached = false
            )
        )
    }

    @Test
    fun `rewarded is disabled after click limit`() {
        assertFalse(
            shouldShowRewardedAd(
                isAdsEnabled = true,
                isRewardedEnabled = true,
                isAdLimitReached = true
            )
        )
    }
}
