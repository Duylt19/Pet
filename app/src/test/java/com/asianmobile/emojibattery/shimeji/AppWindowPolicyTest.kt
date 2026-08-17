package com.asianmobile.emojibattery.shimeji

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppWindowPolicyTest {
    @Test
    fun `navigation contrast enforcement is disabled from Android 10`() {
        assertFalse(AppWindowPolicy.shouldDisableNavigationBarContrast(sdkInt = 28))
        assertTrue(AppWindowPolicy.shouldDisableNavigationBarContrast(sdkInt = 29))
        assertTrue(AppWindowPolicy.shouldDisableNavigationBarContrast(sdkInt = 36))
    }
}
