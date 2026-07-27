package com.asianmobile.emojibattery.shimeji.ads.utils

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import com.asianmobile.emojibattery.shimeji.ads.config.ADMOD_AD_CLICK
import com.asianmobile.emojibattery.shimeji.ads.config.LIMIT_AD
import com.asianmobile.emojibattery.shimeji.ads.data.SharedPreferencesUtils
import com.asianmobile.emojibattery.shimeji.ads.tracking.AdFormat
import com.asianmobile.emojibattery.shimeji.ads.tracking.AdPlacement
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Copyright © 2023 Asian Mobile Co.,Ltd
 */
object Utils {

    fun logAdClickEvent(
        context: Context,
        placement: String = AdPlacement.UNKNOWN,
        adFormat: AdFormat = AdFormat.UNKNOWN
    ) {
        SharedPreferencesUtils.increaseAdClicked(context)

        val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        val params = Bundle().apply {
            putString("ad_placement", placement.take(100))
            putString("ad_format", adFormat.value)
        }
        firebaseAnalytics.logEvent(ADMOD_AD_CLICK, params)
    }

    internal fun checkLimitAd(context: Context) =
        checkLimitAd(SharedPreferencesUtils.getTimeAdClicked(context))

    internal fun checkLimitAd(adClickCount: Int) =
        hasReachedAdClickLimit(adClickCount, getLimitClickAd())

    internal fun hasReachedAdClickLimit(adClickCount: Int, clickLimit: Long) =
        adClickCount.toLong() >= clickLimit

    private fun getLimitClickAd() =
        SafeRemoteConfig.getLong(LIMIT_AD, 100L)

    suspend fun isInternetConnected(): Boolean = withContext(Dispatchers.IO) {
        try {
            (URL("https://www.google.com").openConnection() as HttpURLConnection).run {
                connectTimeout = 2000
                readTimeout = 2000
                requestMethod = "HEAD"
                connect()
                responseCode == HttpURLConnection.HTTP_OK
            }
        } catch (_: Exception) {
            false
        }
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        return cm.getNetworkCapabilities(network) != null
    }
}
