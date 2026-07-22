package com.asianmobile.privatebrower.ui.permission

import android.Manifest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionPolicyTest {
    @Test
    fun `first permission request opens system dialog`() {
        assertEquals(
            PermissionRequestDestination.SYSTEM_DIALOG,
            PermissionPolicy.nextRequestDestination(
                isGranted = false,
                requestCount = 0,
                canShowRationale = false
            )
        )
    }

    @Test
    fun `previous denial with rationale can request again`() {
        assertEquals(
            PermissionRequestDestination.SYSTEM_DIALOG,
            PermissionPolicy.nextRequestDestination(
                isGranted = false,
                requestCount = 1,
                canShowRationale = true
            )
        )
    }

    @Test
    fun `one silent denial retries once before app settings`() {
        assertEquals(
            PermissionRequestDestination.SYSTEM_DIALOG,
            PermissionPolicy.nextRequestDestination(
                isGranted = false,
                requestCount = 1,
                canShowRationale = false
            )
        )
    }

    @Test
    fun `permanent denial opens app settings`() {
        assertEquals(
            PermissionRequestDestination.APP_SETTINGS,
            PermissionPolicy.nextRequestDestination(
                isGranted = false,
                requestCount = 2,
                canShowRationale = false
            )
        )
    }

    @Test
    fun `granted permission does not request again`() {
        assertEquals(
            PermissionRequestDestination.NONE,
            PermissionPolicy.nextRequestDestination(
                isGranted = true,
                requestCount = 2,
                canShowRationale = false
            )
        )
    }

    @Test
    fun `runtime permission uses app settings after two silent denials`() {
        assertEquals(
            true,
            PermissionPolicy.shouldOpenAppSettings(
                requestCount = 2,
                canShowRationale = false
            )
        )
    }

    @Test
    fun `runtime permission can still use system dialog before request limit`() {
        assertEquals(
            false,
            PermissionPolicy.shouldOpenAppSettings(
                requestCount = 1,
                canShowRationale = false
            )
        )
    }

    @Test
    fun `runtime permission can retry while rationale is available`() {
        assertEquals(
            false,
            PermissionPolicy.shouldOpenAppSettings(
                requestCount = 2,
                canShowRationale = true
            )
        )
    }

    @Test
    fun `onboarding requests legacy read and write storage through api 28`() {
        assertArrayEquals(
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            PermissionPolicy.onboardingStoragePermissions(28)
        )
    }

    @Test
    fun `onboarding requests broad legacy storage on api 29`() {
        assertArrayEquals(
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            PermissionPolicy.onboardingStoragePermissions(29)
        )
    }

    @Test
    fun `onboarding uses special all files settings from api 30`() {
        assertArrayEquals(
            emptyArray<String>(),
            PermissionPolicy.onboardingStoragePermissions(30)
        )
        assertArrayEquals(
            emptyArray<String>(),
            PermissionPolicy.onboardingStoragePermissions(36)
        )
    }

    @Test
    fun `all files access starts at api 30`() {
        assertFalse(PermissionPolicy.supportsAllFilesAccess(29))
        assertTrue(PermissionPolicy.supportsAllFilesAccess(30))
        assertTrue(PermissionPolicy.supportsAllFilesAccess(36))
    }
}
