package com.asianmobile.emojibattery.shimeji.ui.permission

import org.junit.Assert.assertEquals
import org.junit.Test

class GrantPermissionsAccessibilityPolicyTest {
    @Test
    fun `missing accessibility shows disclosure before system settings`() {
        assertEquals(
            GrantPermissionsEffect.ShowAccessibilityDisclosure,
            accessibilityTargetEffect(isAccessibilityEnabled = false)
        )
    }

    @Test
    fun `enabled accessibility opens settings for permission management`() {
        assertEquals(
            GrantPermissionsEffect.OpenAccessibilitySettings,
            accessibilityTargetEffect(isAccessibilityEnabled = true)
        )
    }
}
