package com.asianmobile.emojibattery.shimeji.ui.shared.component

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityDisclosurePolicyTest {
    @Test
    fun `allow is blocked until consent is granted`() {
        assertFalse(canGrantAccessibilityPermission(isConsentGranted = false))
    }

    @Test
    fun `allow proceeds after consent is granted`() {
        assertTrue(canGrantAccessibilityPermission(isConsentGranted = true))
    }
}
