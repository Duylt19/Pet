package com.asianmobile.emojibattery.shimeji.ui.shared.component

import org.junit.Assert.assertEquals
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

    @Test
    fun `disabled allow click reports consent requirement`() {
        var grants = 0
        var consentWarnings = 0

        handleAccessibilityAllowClick(
            isConsentGranted = false,
            onGrantPermission = { grants += 1 },
            onConsentRequired = { consentWarnings += 1 }
        )

        assertEquals(0, grants)
        assertEquals(1, consentWarnings)
    }

    @Test
    fun `enabled allow click grants without warning`() {
        var grants = 0
        var consentWarnings = 0

        handleAccessibilityAllowClick(
            isConsentGranted = true,
            onGrantPermission = { grants += 1 },
            onConsentRequired = { consentWarnings += 1 }
        )

        assertEquals(1, grants)
        assertEquals(0, consentWarnings)
    }

    @Test
    fun `disabled allow uses figma opacity`() {
        assertEquals(0.3f, ACCESSIBILITY_ALLOW_DISABLED_ALPHA, 0f)
    }
}
