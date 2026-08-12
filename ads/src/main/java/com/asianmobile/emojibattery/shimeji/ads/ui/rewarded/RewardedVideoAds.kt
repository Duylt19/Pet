package com.asianmobile.emojibattery.shimeji.ads.ui.rewarded

import android.app.Activity
import android.content.Context
import android.util.Log
import com.asianmobile.emojibattery.shimeji.ads.BuildConfig
import com.asianmobile.emojibattery.shimeji.ads.R
import com.asianmobile.emojibattery.shimeji.ads.tracking.Tracking
import com.asianmobile.emojibattery.shimeji.ads.tracking.AdFormat
import com.asianmobile.emojibattery.shimeji.ads.tracking.AdPlacement
import com.asianmobile.emojibattery.shimeji.ads.utils.AdsIdLogger
import com.asianmobile.emojibattery.shimeji.ads.utils.AdOverlayState
import com.asianmobile.emojibattery.shimeji.ads.utils.Utils
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
    private var resultListener: RewardedResultListener? = null
    private var rewardEarned = false
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

    fun showRewardedAd(activity: Activity, listener: RewardedResultListener) {
        if (_isShowing) return
        resultListener = listener
        rewardEarned = false
        if (!MobileAds.isInitialized) {
            completeResult(activity, RewardedAdResult.UNAVAILABLE)
            return
        }
        if (Utils.checkLimitAd(activity)) {
            completeResult(activity, RewardedAdResult.UNAVAILABLE)
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
                    _isShowing = false
                    AdOverlayState.hide()
                    completeResult(
                        activity,
                        if (rewardEarned) {
                            RewardedAdResult.EARNED
                        } else {
                            RewardedAdResult.DISMISSED
                        }
                    )
                }

                override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                    // Called when ad fails to show.
                    Log.e(TAG, "Ad failed to show fullscreen content.")
                    rewardedAd = null
                    _isShowing = false
                    AdOverlayState.hide()
                    completeResult(activity, RewardedAdResult.UNAVAILABLE)
                    loadRewardedVideo(activity)
                }

                override fun onAdImpression() {
                    // Called when an impression is recorded for an ad.
                    Log.d(TAG, "Ad recorded an impression.")
                }

                override fun onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                    Log.d(TAG, "Ad showed fullscreen content.")
                    _isShowing = true
                    AdOverlayState.show()
                }

                override fun onAdPaid(value: AdValue) {
                    Tracking.setTrackRevenueByAdjust(value.valueMicros, value.currencyCode)
                }
            }

            rewardedAd?.setImmersiveMode(true)
            _isShowing = true
            AdOverlayState.show()
            rewardedAd?.show(activity) { rewardItem ->
                val rewardAmount = rewardItem.amount
                val rewardType = rewardItem.type
                rewardEarned = true
                Log.d(TAG, "Earn $rewardType and $rewardAmount")
            }
        } else {
            Log.d(TAG, "The rewarded ad wasn't ready yet.")
            completeResult(activity, RewardedAdResult.UNAVAILABLE)
            loadRewardedVideo(activity)
        }
    }

    private fun completeResult(activity: Activity, result: RewardedAdResult) {
        val listener = resultListener ?: return
        resultListener = null
        if (!activity.isDestroyed && !activity.isFinishing) {
            activity.runOnUiThread {
                listener.onAdClosed(result)
            }
        }
    }

    fun interface RewardedResultListener {
        fun onAdClosed(result: RewardedAdResult)
    }
}

enum class RewardedAdResult {
    EARNED,
    DISMISSED,
    UNAVAILABLE;

    val shouldContinueFlow: Boolean
        get() = this != DISMISSED
}
