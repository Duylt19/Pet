package com.asianmobile.privatebrower.ui.permission

import android.Manifest
import android.os.Build

data class PermissionUiState(
    val storageGranted: Boolean = false,
    val usesAllFilesAccess: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
    val storageRequestCount: Int = 0
)

enum class PermissionRequestDestination {
    NONE,
    SYSTEM_DIALOG,
    APP_SETTINGS
}

object PermissionPolicy {
    private const val SYSTEM_REQUEST_LIMIT = 2

    fun nextRequestDestination(
        isGranted: Boolean,
        requestCount: Int,
        canShowRationale: Boolean
    ): PermissionRequestDestination = when {
        isGranted -> PermissionRequestDestination.NONE
        shouldOpenAppSettings(requestCount, canShowRationale) ->
            PermissionRequestDestination.APP_SETTINGS
        else -> PermissionRequestDestination.SYSTEM_DIALOG
    }

    fun shouldOpenAppSettings(
        requestCount: Int,
        canShowRationale: Boolean
    ): Boolean = requestCount >= SYSTEM_REQUEST_LIMIT && !canShowRationale

    // API 30+ uses All files access. API 29 opts out of scoped storage and needs both
    // legacy runtime permissions to provide equivalent broad shared-storage access.
    fun onboardingStoragePermissions(sdkInt: Int): Array<String> = when {
        supportsAllFilesAccess(sdkInt) -> emptyArray()
        else -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    fun supportsAllFilesAccess(sdkInt: Int): Boolean = sdkInt >= Build.VERSION_CODES.R
}
