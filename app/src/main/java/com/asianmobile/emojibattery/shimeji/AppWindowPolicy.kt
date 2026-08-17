package com.asianmobile.emojibattery.shimeji

import android.os.Build

/** Window decisions kept outside Activity callbacks so OEM/API boundaries stay testable. */
internal object AppWindowPolicy {
    fun shouldDisableNavigationBarContrast(sdkInt: Int): Boolean =
        sdkInt >= Build.VERSION_CODES.Q
}
