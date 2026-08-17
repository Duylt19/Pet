package com.asianmobile.emojibattery.shimeji.ads.ui.interstitial

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InterstitialLauncherPolicyTest {

    @Test
    fun `launcher requires consent and both remote config gates`() {
        assertTrue(
            shouldUseLauncherInterstitial(
                isLauncherEnabled = true,
                isInterstitialEligible = true,
                canRequestAds = true
            )
        )
        assertFalse(
            shouldUseLauncherInterstitial(
                isLauncherEnabled = false,
                isInterstitialEligible = true,
                canRequestAds = true
            )
        )
        assertFalse(
            shouldUseLauncherInterstitial(
                isLauncherEnabled = true,
                isInterstitialEligible = false,
                canRequestAds = true
            )
        )
        assertFalse(
            shouldUseLauncherInterstitial(
                isLauncherEnabled = true,
                isInterstitialEligible = true,
                canRequestAds = false
            )
        )
    }
}
