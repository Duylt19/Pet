package com.asianmobile.emojibattery.shimeji.ui.permission

data class GrantPermissionsUiState(
    val isAccessibilityEnabled: Boolean = false,
    val isOverlayGranted: Boolean = false,
    val isBatteryOptimizationIgnored: Boolean = false,
    /**
     * Only devices that kill foreground services see this row: on stock Android the exemption
     * grants network and wake locks the pet overlay never uses. "Devices" rather than "vendors"
     * because a resolved vendor power manager counts too, and that is measured on the device
     * rather than guessed from its brand.
     */
    val isBatteryRowVisible: Boolean = false,
    /**
     * The ROM has its own auto-start allowlist. No API reads or writes it, so the row is an
     * action rather than a toggle and stays available even after the platform exemption.
     */
    val isAutoStartRowVisible: Boolean = false,
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
    VENDOR_AUTO_START,
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

    /**
     * The vendor's own auto-start list. Separate from the platform exemption: granting one does
     * not grant the other, and there is no API to read either the state or the result.
     */
    data object OpenVendorAutoStartSettings : GrantPermissionsEffect
    data object RequestNotificationPermission : GrantPermissionsEffect
    data object OpenAppNotificationSettings : GrantPermissionsEffect
}
