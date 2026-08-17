package com.asianmobile.emojibattery.shimeji.ads.ui.openads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppOpenLoadPolicyTest {

    @Test
    fun `starts only one request while an app open load is pending`() {
        assertTrue(shouldStartAppOpenLoad(isLoading = false, isAdAvailable = false))
        assertFalse(shouldStartAppOpenLoad(isLoading = true, isAdAvailable = false))
        assertFalse(shouldStartAppOpenLoad(isLoading = false, isAdAvailable = true))
    }
}
