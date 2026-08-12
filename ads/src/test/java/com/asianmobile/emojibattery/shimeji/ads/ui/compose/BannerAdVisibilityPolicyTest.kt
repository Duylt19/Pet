package com.asianmobile.emojibattery.shimeji.ads.ui.compose

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BannerAdVisibilityPolicyTest {

    @Test
    fun `eligible banner keeps slot while loading or loaded`() {
        assertTrue(shouldDisplayBannerSlot(isEligible = true, isAdFailed = false))
    }

    @Test
    fun `failed banner removes slot`() {
        assertFalse(shouldDisplayBannerSlot(isEligible = true, isAdFailed = true))
    }

    @Test
    fun `ineligible banner removes slot`() {
        assertFalse(shouldDisplayBannerSlot(isEligible = false, isAdFailed = false))
    }
}
