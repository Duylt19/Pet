package com.asianmobile.emojibattery.shimeji.battery.overlay

/**
 * Why the battery bar is offering to turn itself back on.
 *
 * Android removes an accessibility service from `enabled_accessibility_services` whenever its
 * package is force-stopped — `AccessibilityManagerService.onPackagesForceStoppedLocked` persists
 * that removal. Nothing the app does can prevent it, and no API asks for the permission back, so
 * the only thing left is to notice and say so. Clearing the app from recents force-stops it on
 * several ROMs, which is why a bar that worked all day is simply gone after a swipe.
 */
enum class BatteryAccessibilityRecovery {
    /** Either the bar was never turned on, or the permission is still granted. */
    NONE,

    /** The platform recorded the last death as user-initiated: swiped away or force-stopped. */
    APP_CLOSED,

    /** Something other than the user ended the process — a vendor power manager, or reclaim. */
    DEVICE_KILLED,

    /**
     * The permission is gone but the cause is unknown: `ApplicationExitInfo` is API 30+, and even
     * above it the record can be missing. Still worth offering, just without blaming anything.
     */
    UNKNOWN_CAUSE
}

/**
 * [isBatteryConfigured] is the stored intent to show the bar, which survives the kill, while
 * [isAccessibilityEnabled] is what the system will actually allow right now. The two disagreeing
 * is the whole signal: a user who turned the bar off themselves clears the stored intent too, so
 * this can never mistake that for a revocation.
 *
 * [wasProcessEndedByUser] is null when the platform recorded nothing usable.
 */
fun batteryAccessibilityRecovery(
    isBatteryConfigured: Boolean,
    isAccessibilityEnabled: Boolean,
    wasProcessEndedByUser: Boolean?
): BatteryAccessibilityRecovery = when {
    !isBatteryConfigured || isAccessibilityEnabled -> BatteryAccessibilityRecovery.NONE
    wasProcessEndedByUser == true -> BatteryAccessibilityRecovery.APP_CLOSED
    wasProcessEndedByUser == false -> BatteryAccessibilityRecovery.DEVICE_KILLED
    else -> BatteryAccessibilityRecovery.UNKNOWN_CAUSE
}
