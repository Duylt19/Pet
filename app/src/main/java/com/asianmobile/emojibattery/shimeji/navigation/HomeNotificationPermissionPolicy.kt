package com.asianmobile.emojibattery.shimeji.navigation

import android.os.Build

/**
 * The notification runtime prompt belongs to the Home entry, not onboarding. It is shown once
 * after the user reaches a top-level Home tab and never competes with a full-screen ad.
 */
internal fun shouldRequestHomeNotificationPermission(
    sdkInt: Int,
    isGranted: Boolean,
    hasRequestedBefore: Boolean?,
    isHomeTopLevelVisible: Boolean,
    isFullScreenAdShowing: Boolean
): Boolean = sdkInt >= Build.VERSION_CODES.TIRAMISU &&
    !isGranted &&
    hasRequestedBefore == false &&
    isHomeTopLevelVisible &&
    !isFullScreenAdShowing
