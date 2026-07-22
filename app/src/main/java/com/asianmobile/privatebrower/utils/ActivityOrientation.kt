package com.asianmobile.privatebrower.utils

import android.app.Activity
import android.content.pm.ActivityInfo

internal fun Activity.applyAppOrientation(fullscreenLandscape: Boolean) {
    requestedOrientation = if (fullscreenLandscape) {
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    } else {
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}
