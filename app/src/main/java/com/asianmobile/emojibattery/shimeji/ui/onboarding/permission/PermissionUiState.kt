package com.asianmobile.emojibattery.shimeji.ui.onboarding.permission

import android.os.Build

data class PermissionUiState(
    val overlayGranted: Boolean = false,
    val notificationGranted: Boolean = false,
    val notificationPermissionRequired: Boolean = false,
    val actionsEnabled: Boolean = true
)

object PetPermissionPolicy {
    fun requiresNotificationPermission(sdkInt: Int): Boolean =
        sdkInt >= Build.VERSION_CODES.TIRAMISU
}
