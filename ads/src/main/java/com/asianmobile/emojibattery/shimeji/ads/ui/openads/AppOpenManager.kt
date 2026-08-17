package com.asianmobile.emojibattery.shimeji.ads.ui.openads

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.asianmobile.emojibattery.shimeji.ads.BuildConfig
import com.asianmobile.emojibattery.shimeji.ads.R
import com.asianmobile.emojibattery.shimeji.ads.config.DELAY_OPEN_ADS
import com.asianmobile.emojibattery.shimeji.ads.data.CheckShowAdsUtil
import com.asianmobile.emojibattery.shimeji.ads.tracking.Tracking
import com.asianmobile.emojibattery.shimeji.ads.tracking.AdFormat
import com.asianmobile.emojibattery.shimeji.ads.tracking.AdPlacement
import com.asianmobile.emojibattery.shimeji.ads.ui.interstitial.InterstitialUtil
import com.asianmobile.emojibattery.shimeji.ads.ui.rewarded.RewardedVideoAds
import com.asianmobile.emojibattery.shimeji.ads.utils.AdsIdLogger
import com.asianmobile.emojibattery.shimeji.ads.utils.SafeRemoteConfig
import com.asianmobile.emojibattery.shimeji.ads.utils.Utils
import com.asianmobile.emojibattery.shimeji.ads.utils.AdOverlayState
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import java.util.Date

class AppOpenManager() : LifecycleObserver {

    companion object {
        val TAG = "AppOpenManager"
    }

    private var appOpenAd: AppOpenAd? = null
    private var loadCallback: AdLoadCallback<AppOpenAd>? = null

    private var isShowingAd = false
    val isShowing: Boolean get() = isShowingAd
    private var loadTime: Long = 0
    private var adCloseListener: AdCloseListener? = null
    private lateinit var alertDialog: Dialog
    var needShowOpenAds = true

    internal fun isShowOpenAd(): Boolean {
        return SafeRemoteConfig.getBoolean("is_show_open_ads")
    }

    fun interface AdCloseListener {
        fun onAdClosed()
    }

    fun showAdIfAvailable(activity: Activity, adCloseListener: AdCloseListener?) {
        if (!MobileAds.isInitialized) {
            if (!activity.isDestroyed && !activity.isFinishing) {
                activity.runOnUiThread {
                    adCloseListener?.onAdClosed()
                }
            }
            return
        }

        if (!CheckShowAdsUtil.checkShowOpenAd(activity)) {
            if (!activity.isDestroyed && !activity.isFinishing) {
                activity.runOnUiThread {
                    adCloseListener?.onAdClosed()
                }
            }
            return
        }

        this.adCloseListener = adCloseListener
        // Only show ad if there is not already an app open ad currently showing
        // and an ad is available.
        if (!isShowingAd && isAdAvailable() && !InterstitialUtil.getInstance().isShowing && !RewardedVideoAds.getInstance().isShowing) {
            // check limit time
            val currentTime = System.currentTimeMillis()
            val lastTimeInters = InterstitialUtil.getInstance().lastTime
            val limitTime = InterstitialUtil.getInstance().getLimitTimeIntersAndOpen()
            if (currentTime - lastTimeInters >= limitTime) {
                Log.d(TAG, "Open ads will show")
                val fullScreenContentCallback: AppOpenAdEventCallback =
                    object : AppOpenAdEventCallback {
                        override fun onAdDismissedFullScreenContent() {
                            // Set the reference to null so isAdAvailable() returns false.
                            appOpenAd = null
                            isShowingAd = false
                            fetchAd(activity)
                            finishAppOpenPresentation(activity) {
                                InterstitialUtil.getInstance().lastTimeOpenAd =
                                    System.currentTimeMillis()
                                adCloseListener?.onAdClosed()
                            }
                        }

                        override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                            appOpenAd = null
                            isShowingAd = false
                            finishAppOpenPresentation(activity) {
                                adCloseListener?.onAdClosed()
                            }
                        }

                        override fun onAdShowedFullScreenContent() {
                            isShowingAd = true
                            updatePresentationStage(AppOpenPresentationStage.FULLSCREEN_AD)
                        }

                        override fun onAdClicked() {
                            super.onAdClicked()
                            Utils.logAdClickEvent(
                                context = activity,
                                placement = AdPlacement.APP_OPEN,
                                adFormat = AdFormat.APP_OPEN
                            )
                        }

                        override fun onAdPaid(value: AdValue) {
                            Tracking.setTrackRevenueByAdjust(value.valueMicros, value.currencyCode)
                        }
                    }
                updatePresentationStage(AppOpenPresentationStage.WELCOME_BACK_COVER)
                isShowingAd = true
                showProgressDialog(activity)
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    appOpenAd?.adEventCallback = fullScreenContentCallback
                    appOpenAd?.setImmersiveMode(true)
                    appOpenAd?.show(activity)
                }, DELAY_OPEN_ADS)
            } else {
                Log.d(TAG, "Open ads reach limit time")
                if (!activity.isDestroyed && !activity.isFinishing) {
                    activity.runOnUiThread {
                        adCloseListener?.onAdClosed()
                    }
                }
            }
        } else {
            Log.d(TAG, "Open Ad Can not show ad.")
            fetchAd(activity)
            if (!activity.isDestroyed && !activity.isFinishing) {
                activity.runOnUiThread {
                    adCloseListener?.onAdClosed()
                }
            }
        }
    }

    /**
     * Request an ad
     */
    internal fun fetchAd(context: Context) {
        if (!MobileAds.isInitialized) return

        // Have unused ad, no need to fetch another.
        if (!CheckShowAdsUtil.checkLoadOpenAd(context)) return

        if (isAdAvailable()) {
            return
        }

        val adUnitId = if (BuildConfig.DEBUG) {
            context.getString(R.string.id_emoji_battery_open_ads_test)
        } else {
            context.getString(R.string.id_emoji_battery_open_ads)
        }
        AdsIdLogger.request(format = "APP_OPEN", adUnitId = adUnitId, placement = "app_open")

        loadCallback = object : AdLoadCallback<AppOpenAd> {
            /**
             * Called when an app open ad has loaded.
             *
             * @param ad the loaded app open ad.
             */
            override fun onAdLoaded(ad: AppOpenAd) {
                AdsIdLogger.loaded(
                    format = "APP_OPEN",
                    adUnitId = adUnitId,
                    placement = "app_open"
                )
                Log.d(TAG, "Open ads loaded")
                appOpenAd = ad
                loadTime = Date().time
            }

            /**
             * Called when an app open ad has failed to load.
             *
             * @param adError the error.
             */
            override fun onAdFailedToLoad(adError: LoadAdError) {
                // Handle the error.
                AdsIdLogger.failed(
                    format = "APP_OPEN",
                    adUnitId = adUnitId,
                    placement = "app_open",
                    error = adError.message
                )
                Log.d(TAG, "Open ads fail to load ${adError.message}")
            }
        }
        val adRequest = getAdRequest(adUnitId)
        AppOpenAd.load(
            adRequest, loadCallback as AdLoadCallback<AppOpenAd>
        )
    }

    /**
     * Creates and returns ad request.
     */
    private fun getAdRequest(idUnit: String) =
        AdRequest.Builder(idUnit).build()

    /**
     * Utility method that checks if ad exists and can be shown.
     */
    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    private fun showProgressDialog(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return

        alertDialog = Dialog(activity, R.style.Theme_CutePet_WelcomeBack)

        val activityDecorView = activity.window.decorView
        val lifecycleOwner = activityDecorView.findViewTreeLifecycleOwner()
            ?: activity as? LifecycleOwner
        if (lifecycleOwner == null) {
            Log.e(TAG, "Cannot show Welcome Back without a LifecycleOwner")
            return
        }

        val view = ComposeView(activity).apply {
            // A Compose dialog does not draw its first frame synchronously. Keep the same branded
            // cover underneath it so the host Activity never exposes its black ad background.
            setBackgroundResource(R.drawable.img_onboarding_wallpaper)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(
                activityDecorView.findViewTreeViewModelStoreOwner()
                    ?: activity as? ViewModelStoreOwner
            )
            setViewTreeSavedStateRegistryOwner(
                activityDecorView.findViewTreeSavedStateRegistryOwner()
                    ?: activity as? SavedStateRegistryOwner
            )
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool
            )
            setContent { WelcomeBackContent() }
        }

        alertDialog.setContentView(view)
        alertDialog.setCanceledOnTouchOutside(false)
        alertDialog.setCancelable(false)

        alertDialog.window?.apply {
            WindowCompat.setDecorFitsSystemWindows(this, false)
            WindowInsetsControllerCompat(this, decorView).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        if (activity.isFinishing || activity.isDestroyed) return
        alertDialog.show()
        alertDialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    private fun dismissProgressDialog(activity: Activity) {
        if (activity.isFinishing) {
            return
        }
        if (activity.isDestroyed) {
            return
        }
        if (alertDialog.isShowing) {
            alertDialog.dismiss()
        }
    }

    /** Completes the SDK lifecycle while the already-rendered host screen is ready underneath. */
    private fun finishAppOpenPresentation(
        activity: Activity,
        onRestored: () -> Unit
    ) {
        updatePresentationStage(AppOpenPresentationStage.IDLE)
        if (activity.isFinishing || activity.isDestroyed) return
        activity.runOnUiThread {
            if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
            dismissProgressDialog(activity)
            onRestored()
        }
    }

    private fun updatePresentationStage(stage: AppOpenPresentationStage) {
        val directive = AppOpenOverlayPolicy.directive(stage)
        if (directive.isFullscreenAdShowing) {
            AdOverlayState.show(hideActivityContent = directive.hideActivityContent)
        } else {
            AdOverlayState.hide()
        }
    }
}
