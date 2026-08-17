package com.asianmobile.emojibattery.shimeji.navigation

import android.os.Build

/**
 * The notification runtime prompt belongs to the Home entry, not onboarding. It is shown at most
 * once per app session after the user reaches Home and never competes with a full-screen ad.
 */
internal fun shouldRequestHomeNotificationPermission(
    sdkInt: Int,
    isGranted: Boolean,
    hasRequestedThisSession: Boolean,
    isHomeTopLevelVisible: Boolean,
    isFullScreenAdShowing: Boolean
): Boolean = sdkInt >= Build.VERSION_CODES.TIRAMISU &&
    !isGranted &&
    !hasRequestedThisSession &&
    isHomeTopLevelVisible &&
    !isFullScreenAdShowing
