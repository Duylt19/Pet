package com.asianmobile.privatebrower.utils.permission

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadNotificationPermissionPolicyTest {
    @Test
    fun `notification permission is not requested before api 33`() {
        assertFalse(
            DownloadNotificationPermissionPolicy.shouldRequest(
                sdkInt = 32,
                isGranted = false,
                requestCount = 0
            )
        )
    }

    @Test
    fun `first download requests notification permission from api 33`() {
        assertTrue(
            DownloadNotificationPermissionPolicy.shouldRequest(
                sdkInt = 33,
                isGranted = false,
                requestCount = 0
            )
        )
    }

    @Test
    fun `granted notification permission is not requested again`() {
        assertFalse(
            DownloadNotificationPermissionPolicy.shouldRequest(
                sdkInt = 36,
                isGranted = true,
                requestCount = 0
            )
        )
    }

    @Test
    fun `denied notification permission does not interrupt with repeated prompts`() {
        assertFalse(
            DownloadNotificationPermissionPolicy.shouldRequest(
                sdkInt = 36,
                isGranted = false,
                requestCount = 1
            )
        )
    }
}
