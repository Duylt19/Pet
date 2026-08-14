package com.asianmobile.emojibattery.shimeji.ui.settings.permissions

data class GrantPermissionsUiState(
    val isAccessibilityEnabled: Boolean = false,
    val isOverlayGranted: Boolean = false,
    val isBatteryOptimizationIgnored: Boolean = false,
    /**
     * True only when device signals indicate this ROM can benefit from the exemption. The Mine
     * dashboard and Pet flow both hide the row on ordinary stock Android devices.
     */
    val isBatteryRowVisible: Boolean = false,
    /**
     * The ROM has its own auto-start allowlist. No public API can read whether the user enabled
     * it, so this row is shown only when a matching vendor settings surface exists.
     */
    val isAutoStartRowVisible: Boolean = false,
    val isNotificationGranted: Boolean = false,
    /** Below API 33 the notification permission does not exist, so its row is hidden. */
    val isNotificationRowVisible: Boolean = false,
    /** Persisted after any runtime notification prompt, so a denial routes to App Settings. */
    val hasRequestedNotificationPermission: Boolean = false
)

internal val GrantPermissionsUiState.needsOverlayPermission: Boolean
    get() = !isOverlayGranted

internal val GrantPermissionsUiState.needsNotificationPermission: Boolean
    get() = isNotificationRowVisible && !isNotificationGranted

internal val GrantPermissionsUiState.needsBatteryOptimizationExemption: Boolean
    get() = isBatteryRowVisible && !isBatteryOptimizationIgnored

internal fun GrantPermissionsUiState.needsRequiredCard(
    requiredTarget: GrantPermissionsTarget
): Boolean = when (requiredTarget) {
    // On the Pet on Screen variant, the hero card itself is the overlay permission item.
    GrantPermissionsTarget.OVERLAY -> needsOverlayPermission
    else -> !isAccessibilityEnabled
}

internal fun GrantPermissionsUiState.hasStabilityPermissionToRequest(
    requiredTarget: GrantPermissionsTarget
): Boolean = (requiredTarget != GrantPermissionsTarget.OVERLAY && needsOverlayPermission) ||
    needsBatteryOptimizationExemption || isAutoStartRowVisible

/**
 * The dedicated Pet flow normally removes rows as they are granted. Once every relevant row is
 * complete, keep one explicit success card instead of leaving a blank screen after Settings
 * returns. Optional vendor steps still take precedence and remain visible until attempted.
 */
internal fun GrantPermissionsUiState.shouldShowOverlayCompletionCard(
    requiredTarget: GrantPermissionsTarget
): Boolean = requiredTarget == GrantPermissionsTarget.OVERLAY &&
    hasMandatoryPetPermissions &&
    !hasStabilityPermissionToRequest(requiredTarget)

/**
 * Which permission a row on the Grant Permissions screen represents. Special permissions hand
 * the user to a system surface; notification is the only runtime permission requested in-app.
 */
enum class GrantPermissionsTarget {
    ACCESSIBILITY,
    OVERLAY,
    VENDOR_AUTO_START,
    BATTERY_OPTIMIZATION,
    NOTIFICATION
}

sealed interface GrantPermissionsEffect {
    data object ShowAccessibilityDisclosure : GrantPermissionsEffect
    data object OpenAccessibilitySettings : GrantPermissionsEffect
    data object OpenOverlaySettings : GrantPermissionsEffect

    /** Opens the request dialog when missing, or the system list when already granted. */
    data object OpenBatteryOptimizationSettings : GrantPermissionsEffect

    /**
     * The vendor's own auto-start list. Separate from the platform exemption: granting one does
     * not grant the other, and there is no API to read either the state or the result.
     */
    data object OpenVendorAutoStartSettings : GrantPermissionsEffect
    data object RequestNotificationPermission : GrantPermissionsEffect
    data object OpenAppNotificationSettings : GrantPermissionsEffect
    data object PetOverlayStartFailed : GrantPermissionsEffect
}

internal val GrantPermissionsUiState.hasMandatoryPetPermissions: Boolean
    get() = isOverlayGranted && !needsNotificationPermission

/**
 * The order shown by the Pet on Screen design is also the order in which system surfaces open.
 * Optional stability steps are attempted once per run so declining one never traps the user in
 * the same settings screen.
 */
internal fun GrantPermissionsUiState.nextPetPermissionTarget(
    attempted: Set<GrantPermissionsTarget>
): GrantPermissionsTarget? = when {
    needsNotificationPermission -> GrantPermissionsTarget.NOTIFICATION
    needsOverlayPermission -> GrantPermissionsTarget.OVERLAY
    needsBatteryOptimizationExemption &&
        GrantPermissionsTarget.BATTERY_OPTIMIZATION !in attempted ->
        GrantPermissionsTarget.BATTERY_OPTIMIZATION
    isAutoStartRowVisible && GrantPermissionsTarget.VENDOR_AUTO_START !in attempted ->
        GrantPermissionsTarget.VENDOR_AUTO_START
    else -> null
}

internal fun accessibilityTargetEffect(isAccessibilityEnabled: Boolean): GrantPermissionsEffect =
    if (isAccessibilityEnabled) {
        GrantPermissionsEffect.OpenAccessibilitySettings
    } else {
        GrantPermissionsEffect.ShowAccessibilityDisclosure
    }
