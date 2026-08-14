package com.asianmobile.emojibattery.shimeji.ui.shared.policy

import org.junit.Assert.assertFalse
import org.junit.Test

class PremiumUiPolicyTest {

    @Test
    fun `v1 hides premium purchase entry points`() {
        assertFalse(PremiumUiPolicy.isPremiumEntryVisible)
    }
}
