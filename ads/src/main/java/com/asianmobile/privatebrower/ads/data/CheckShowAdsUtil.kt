package com.asianmobile.privatebrower.ads.data

import android.app.Activity
import android.content.Context
import com.asianmobile.privatebrower.ads.config.*
import com.asianmobile.privatebrower.ads.utils.Utils
import com.asianmobile.privatebrower.ads.utils.SafeRemoteConfig

object CheckShowAdsUtil {

    // Open ads
    internal fun checkShowOpenAd(activity: Activity): Boolean {

        if (!SharedPreferencesUtils.getIsEnableAds(activity)) return false

        if (Utils.checkLimitAd(activity)) return false

        if (!isShowOpenAd()) return false
        return true
    }

    internal fun checkLoadOpenAd(context: Context) = !Utils.checkLimitAd(context)

    internal fun checkLoadInterAd(context: Context) =
        !Utils.checkLimitAd(context) && isShowInterstitial()

    internal fun checkShowInterAd(activity: Activity): Boolean {

        if (!SharedPreferencesUtils.getIsEnableAds(activity)) return false

        if (!isShowInterstitial()) return false

        if (Utils.checkLimitAd(activity)) return false

        return true
    }

    // Native ads
    fun checkShowNativeAd(activity: Activity): Boolean {

        if (!isShowNative()) return false

        if (!SharedPreferencesUtils.getIsEnableAds(activity)) return false

        if (activity.isDestroyed || activity.isFinishing) return false

        if (Utils.checkLimitAd(activity)) return false

        return true
    }

    private fun isShowOpenAd(): Boolean = try {
        SafeRemoteConfig.getBoolean(IS_SHOW_OPEN_ADS)
    } catch (e: Exception) { true }

    private fun isShowInterstitial(): Boolean = try {
        SafeRemoteConfig.getBoolean(IS_SHOW_INTER_ADS)
    } catch (e: Exception) { true }

    private fun isShowNative(): Boolean = try {
        SafeRemoteConfig.getBoolean(IS_SHOW_NATIVE)
    } catch (e: Exception) { true }
}

