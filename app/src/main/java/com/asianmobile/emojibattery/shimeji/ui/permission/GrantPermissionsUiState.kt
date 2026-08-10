package com.asianmobile.emojibattery.shimeji.ui.permission

data class GrantPermissionsUiState(
    val isAccessibilityEnabled: Boolean = false,
    val isOverlayGranted: Boolean = false,
    val isBatteryOptimizationIgnored: Boolean = false,
    /**
     * Only vendors that kill foreground services see this row: on stock Android the exemption
     * grants network and wake locks the pet overlay never uses.
     */
    val isBatteryRowVisible: Boolean = false,
    val isNotificationGranted: Boolean = false,
    /** Below API 33 the notification permission does not exist, so its row is hidden. */
    val isNotificationRowVisible: Boolean = false
)

/**
 * Which permission a row on the Grant Permissions screen represents. The screen only ever hands
 * the user to a system surface; nothing here is granted in-app.
 */
enum class GrantPermissionsTarget {
    ACCESSIBILITY,
    OVERLAY,
    BATTERY_OPTIMIZATION,
    NOTIFICATION
}

sealed interface GrantPermissionsEffect {
    data object OpenAccessibilitySettings : GrantPermissionsEffect
    data object OpenOverlaySettings : GrantPermissionsEffect

    /**
     * Opens the system battery-optimisation list rather than the one-tap allow dialog: that
     * dialog needs REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, which Play restricts to a narrow set of
     * use cases this app is not in.
     */
    data object OpenBatteryOptimizationSettings : GrantPermissionsEffect
    data object RequestNotificationPermission : GrantPermissionsEffect
    data object OpenAppNotificationSettings : GrantPermissionsEffect
}
