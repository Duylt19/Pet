package com.asianmobile.emojibattery.shimeji.ads.ui.interstitial

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.core.graphics.drawable.toDrawable
import com.asianmobile.emojibattery.shimeji.ads.BuildConfig
import com.asianmobile.emojibattery.shimeji.ads.R
import com.asianmobile.emojibattery.shimeji.ads.config.SHOW_INTER_LAUNCHER
import com.asianmobile.emojibattery.shimeji.ads.config.TIME_LOADING_ADS
import com.asianmobile.emojibattery.shimeji.ads.data.CheckShowAdsUtil
import com.asianmobile.emojibattery.shimeji.ads.databinding.DialogBeforeShowInterBinding
import com.asianmobile.emojibattery.shimeji.ads.tracking.Tracking
import com.asianmobile.emojibattery.shimeji.ads.utils.SafeRemoteConfig
import com.asianmobile.emojibattery.shimeji.ads.utils.AdOverlayState
import com.asianmobile.emojibattery.shimeji.ads.utils.AdsIdLogger
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback

class InterstitialLauncherUtil {
    private var admobFullLauncher: InterstitialAd? = null

    private var adCloseListener: AdCloseListener? = null

    private lateinit var alertDialog: Dialog
    private var _isShowing = false
    val isShowing: Boolean get() = _isShowing

    private object Holder {
        val INSTANCE = InterstitialLauncherUtil()
    }

    companion object {
        const val TAG = "InterstitialLauncherUtil"

        fun getInstance(): InterstitialLauncherUtil {
            return Holder.INSTANCE
        }
    }

    internal fun loadAdmobLauncher(
        context: Context,
        shouldLoadAds: Boolean,
        isLoadComplete: () -> Unit
    ) {
        Log.e(TAG, "loadAdmobLauncher: ${!isShowInterstitialLauncher()}  --- ${!shouldLoadAds}")
        if (
            !shouldUseLauncherInterstitial(
                isLauncherEnabled = isShowInterstitialLauncher(),
                isInterstitialEligible = CheckShowAdsUtil.checkLoadInterAd(context),
                canRequestAds = shouldLoadAds
            )
        ) {
            isLoadComplete.invoke()
            return
        }
        if (!MobileAds.isInitialized) {
            isLoadComplete.invoke()
            return
        }

        val id = if (BuildConfig.DEBUG) {
            context.getString(R.string.id_emoji_battery_inter_test)
        } else {
            context.getString(R.string.id_emoji_battery_inter_splash)
        }

        AdsIdLogger.request(
            format = "INTERSTITIAL_SPLASH",
            adUnitId = id,
            placement = "splash"
        )
        val adRequest = AdRequest.Builder(id).build()
        InterstitialAd.load(adRequest, object : AdLoadCallback<InterstitialAd> {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                AdsIdLogger.failed(
                    format = "INTERSTITIAL_SPLASH",
                    adUnitId = id,
                    placement = "splash",
                    error = adError.message
                )
                Log.e(TAG, "Ad launcher load failed: $adError")
                admobFullLauncher = null
                isLoadComplete.invoke()
            }

            override fun onAdLoaded(ad: InterstitialAd) {
                AdsIdLogger.loaded(
                    format = "INTERSTITIAL_SPLASH",
                    adUnitId = id,
                    placement = "splash"
                )
                Log.e(TAG, "Ad launcher was loaded.")
                admobFullLauncher = ad
                isLoadComplete.invoke()
            }
        })
    }

    private fun setListener(activity: Activity) {
        admobFullLauncher?.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                admobFullLauncher = null
                InterstitialUtil.getInstance().resetLastTime()
                _isShowing = false
                AdOverlayState.hide()
                if (!activity.isDestroyed && !activity.isFinishing) {
                    activity.runOnUiThread {
                        adCloseListener?.onAdClosed()
                        dismissProgressDialog(activity)
                    }
                }
            }

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                // Called when ad fails to show.
                Log.e(TAG, "Ad failed to show fullscreen content : $fullScreenContentError")
                admobFullLauncher = null
                _isShowing = false
                AdOverlayState.hide()
                if (!activity.isDestroyed && !activity.isFinishing) {
                    activity.runOnUiThread {
                        adCloseListener?.onAdClosed()
                        dismissProgressDialog(activity)
                    }
                }
            }

            override fun onAdShowedFullScreenContent() {
                // Called when ad is shown.
                Log.e(TAG, "Ad showed fullscreen content.")
                _isShowing = true
                AdOverlayState.show()
            }

            override fun onAdPaid(value: AdValue) {
                Tracking.setTrackRevenueByAdjust(value.valueMicros, value.currencyCode)
            }
        }
    }

    fun showInterstitialAdLauncher(activity: Activity, adCloseListener: AdCloseListener) {
        if (!MobileAds.isInitialized) {
            adCloseListener.onAdClosed()
            return
        }
        if (
            !isShowInterstitialLauncher() ||
            !CheckShowAdsUtil.checkShowInterAd(activity)
        ) {
            adCloseListener.onAdClosed()
            return
        }
        Log.e(TAG, "showInterstitialAdLauncher: SHOWWWWWWW")
        if (admobFullLauncher != null) {
            if (!activity.isDestroyed && !activity.isFinishing) {
                setListener(activity)
                AdOverlayState.show()
                showProgressDialog(activity)
                Handler(Looper.getMainLooper()).postDelayed({
                    this.adCloseListener = adCloseListener
                    admobFullLauncher?.setImmersiveMode(true)
                    admobFullLauncher?.show(activity)
                }, TIME_LOADING_ADS)
            }
        } else {
            Log.d(TAG, "InterstitialAd null")
            if (!activity.isDestroyed && !activity.isFinishing) {
                adCloseListener.onAdClosed()
            }
        }
    }

    private fun isShowInterstitialLauncher() =
        SafeRemoteConfig.getBoolean(SHOW_INTER_LAUNCHER)

    fun interface AdCloseListener {
        fun onAdClosed()
    }

    private fun showProgressDialog(activity: Activity) {
        alertDialog = Dialog(activity, android.R.style.Theme_Black_NoTitleBar)
        val binding = DialogBeforeShowInterBinding.inflate(activity.layoutInflater)
        alertDialog.setContentView(binding.root)
        alertDialog.setCanceledOnTouchOutside(false)
        alertDialog.setCancelable(false)
        alertDialog.window?.let { window ->
            window.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            window.setBackgroundDrawable(Color.BLACK.toDrawable())
        }
        if (activity.isFinishing) {
            return
        }
        if (activity.isDestroyed) {
            return
        }
        alertDialog.show()
    }

    private fun dismissProgressDialog(activity: Activity) {
        if (activity.isFinishing) {
            return
        }
        if (activity.isDestroyed) {
            return
        }
        if (!::alertDialog.isInitialized) {
            return
        }
        if (alertDialog.isShowing) {
            alertDialog.dismiss()
        }
    }
}

internal fun shouldUseLauncherInterstitial(
    isLauncherEnabled: Boolean,
    isInterstitialEligible: Boolean,
    canRequestAds: Boolean
): Boolean = isLauncherEnabled && isInterstitialEligible && canRequestAds
