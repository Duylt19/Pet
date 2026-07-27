package com.asianmobile.emojibattery.shimeji.ui.permission

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetPermissionPolicyTest {
    @Test
    fun `notification runtime permission starts at API 33`() {
        assertFalse(PetPermissionPolicy.requiresNotificationPermission(sdkInt = 32))
        assertTrue(PetPermissionPolicy.requiresNotificationPermission(sdkInt = 33))
        assertTrue(PetPermissionPolicy.requiresNotificationPermission(sdkInt = 36))
    }
}
