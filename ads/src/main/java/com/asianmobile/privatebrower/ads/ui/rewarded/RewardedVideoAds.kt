package com.asianmobile.privatebrower.ads.ui.rewarded

import android.app.Activity
import android.content.Context
import android.util.Log
import com.asianmobile.privatebrower.ads.BuildConfig
import com.asianmobile.privatebrower.ads.R
import com.asianmobile.privatebrower.ads.tracking.Tracking
import com.asianmobile.privatebrower.ads.tracking.AdFormat
import com.asianmobile.privatebrower.ads.tracking.AdPlacement
import com.asianmobile.privatebrower.ads.utils.AdsIdLogger
import com.asianmobile.privatebrower.ads.utils.Utils
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback

class RewardedVideoAds {
    private var rewardedAd: RewardedAd? = null
    private var adCloseListener: RewardedCloseListener? = null
    var isLoading = false

    private var _isShowing = false
    val isShowing: Boolean get() = _isShowing

    private object Holder {
        val INSTANCE = RewardedVideoAds()
    }

    companion object {
        val TAG = "RewardedVideoAds"

        fun getInstance(): RewardedVideoAds {
            return Holder.INSTANCE
        }
    }

    fun loadRewardedVideo(context: Context) {
        if (isLoading) return
        isLoading = true
        if (!MobileAds.isInitialized) {
            isLoading = false
            return
        }
        if (Utils.checkLimitAd(context)) {
            isLoading = false
            return
        }
        val id: String = if (BuildConfig.DEBUG) {
            context.getString(R.string.id_private_browser_rewarded_test)
        } else {
            context.getString(R.string.id_private_browser_rewarded)
        }
        AdsIdLogger.request(format = "REWARDED", adUnitId = id, placement = "rewarded")
        val adRequest = AdRequest.Builder(id).build()
        RewardedAd.load(adRequest, object : AdLoadCallback<RewardedAd> {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                isLoading = false
                AdsIdLogger.failed(
                    format = "REWARDED",
                    adUnitId = id,
                    placement = "rewarded",
                    error = adError.message
                )
                Log.d(TAG, adError.toString())
                rewardedAd = null
            }

            override fun onAdLoaded(ad: RewardedAd) {
                isLoading = false
                AdsIdLogger.loaded(format = "REWARDED", adUnitId = id, placement = "rewarded")
                Log.d(TAG, "Ad was loaded.")
                this@RewardedVideoAds.rewardedAd = ad
            }
        })
    }

    fun isReady() = rewardedAd != null

    fun showRewardedAd(activity: Activity, listener: RewardedCloseListener) {
        adCloseListener = listener
//        if (isVipUser(activity)) {
//            adCloseListener?.onAdClosed()
//            return
//        }
        if (!MobileAds.isInitialized) {
            if (!activity.isDestroyed && !activity.isFinishing) {
                activity.runOnUiThread {
                    adCloseListener?.onAdClosed()
                }
            }
            return
        }
        if (Utils.checkLimitAd(activity)) {
            if (!activity.isDestroyed && !activity.isFinishing) {
                activity.runOnUiThread {
                    adCloseListener?.onAdClosed()
                }
            }
            return
        }
        if (rewardedAd != null) {
            rewardedAd?.adEventCallback = object : RewardedAdEventCallback {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                    Log.d(TAG, "Ad was clicked.")
                    Utils.logAdClickEvent(
                        context = activity,
                        placement = AdPlacement.REWARDED_DEFAULT,
                        adFormat = AdFormat.REWARDED
                    )
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    // Set the ad reference to null so you don't show the ad a second time.
                    Log.d(TAG, "Ad dismissed fullscreen content.")
                    rewardedAd = null
                    loadRewardedVideo(activity)
                    if (!activity.isDestroyed && !activity.isFinishing) {
                        activity.runOnUiThread {
                            adCloseListener?.onAdClosed()
                        }
                    }
                    _isShowing = false
                }

                override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                    // Called when ad fails to show.
                    Log.e(TAG, "Ad failed to show fullscreen content.")
                    rewardedAd = null
                    if (!activity.isDestroyed && !activity.isFinishing) {
                        activity.runOnUiThread {
                            adCloseListener?.onAdClosed()
                        }
                    }
                }

                override fun onAdImpression() {
                    // Called when an impression is recorded for an ad.
                    Log.d(TAG, "Ad recorded an impression.")
                }

                override fun onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                    Log.d(TAG, "Ad showed fullscreen content.")
                    _isShowing = true
                }

                override fun onAdPaid(value: AdValue) {
                    Tracking.setTrackRevenueByAdjust(value.valueMicros, value.currencyCode)
                }
            }

            rewardedAd?.setImmersiveMode(true)
            rewardedAd?.show(activity) { rewardItem ->
                val rewardAmount = rewardItem.amount
                val rewardType = rewardItem.type
                Log.d(TAG, "Earn $rewardType and $rewardAmount")
//                adCloseListener?.onAdClosed()
            }
        } else {
            Log.d(TAG, "The rewarded ad wasn't ready yet.")
            adCloseListener?.onAdClosed()
            loadRewardedVideo(activity)
        }
    }

    fun interface RewardedCloseListener {
        fun onAdClosed()
    }
}
