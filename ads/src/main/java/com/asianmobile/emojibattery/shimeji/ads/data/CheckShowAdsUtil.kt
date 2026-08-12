package com.asianmobile.emojibattery.shimeji.ads.data

import android.app.Activity
import android.content.Context
import com.asianmobile.emojibattery.shimeji.ads.config.*
import com.asianmobile.emojibattery.shimeji.ads.utils.Utils
import com.asianmobile.emojibattery.shimeji.ads.utils.SafeRemoteConfig

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

    fun checkShowRewardedAd(context: Context): Boolean = shouldShowRewardedAd(
        isAdsEnabled = SharedPreferencesUtils.getIsEnableAds(context),
        isRewardedEnabled = isShowRewarded(),
        isAdLimitReached = Utils.checkLimitAd(context)
    )

    private fun isShowOpenAd(): Boolean = try {
        SafeRemoteConfig.getBoolean(IS_SHOW_OPEN_ADS)
    } catch (e: Exception) { true }

    private fun isShowInterstitial(): Boolean = try {
        SafeRemoteConfig.getBoolean(IS_SHOW_INTER_ADS)
    } catch (e: Exception) { true }

    private fun isShowNative(): Boolean = try {
        SafeRemoteConfig.getBoolean(IS_SHOW_NATIVE)
    } catch (e: Exception) { true }

    private fun isShowRewarded(): Boolean = try {
        SafeRemoteConfig.getBoolean(IS_SHOW_REWARDED_ADS)
    } catch (e: Exception) { true }
}

internal fun shouldShowRewardedAd(
    isAdsEnabled: Boolean,
    isRewardedEnabled: Boolean,
    isAdLimitReached: Boolean
): Boolean = isAdsEnabled && isRewardedEnabled && !isAdLimitReached
