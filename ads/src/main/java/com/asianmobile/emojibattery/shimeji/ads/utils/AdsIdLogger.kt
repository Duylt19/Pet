package com.asianmobile.emojibattery.shimeji.ads.utils

import android.util.Log
import com.asianmobile.emojibattery.shimeji.ads.BuildConfig

object AdsIdLogger {
    private const val TAG = "AdsIdCheck"

    fun request(format: String, adUnitId: String, placement: String) {
        log(status = "REQUEST", format = format, adUnitId = adUnitId, placement = placement)
    }

    fun loaded(format: String, adUnitId: String, placement: String) {
        log(status = "LOADED", format = format, adUnitId = adUnitId, placement = placement)
    }

    fun failed(format: String, adUnitId: String, placement: String, error: String) {
        log(
            status = "FAILED",
            format = format,
            adUnitId = adUnitId,
            placement = placement,
            error = error
        )
    }

    private fun log(
        status: String,
        format: String,
        adUnitId: String,
        placement: String,
        error: String? = null
    ) {
        val buildType = if (BuildConfig.DEBUG) "DEBUG" else "RELEASE"
        val errorSuffix = error?.let { " | error=$it" }.orEmpty()
        Log.e(
            TAG,
            "$status | build=$buildType | format=$format | placement=$placement" +
                " | adUnitId=$adUnitId$errorSuffix"
        )
    }
}
