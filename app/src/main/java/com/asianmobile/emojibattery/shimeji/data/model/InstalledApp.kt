package com.asianmobile.emojibattery.shimeji.data.model

import android.graphics.Bitmap

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: Bitmap?
)
