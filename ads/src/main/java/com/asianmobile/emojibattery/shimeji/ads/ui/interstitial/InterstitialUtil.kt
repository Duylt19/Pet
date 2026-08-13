package com.asianmobile.emojibattery.shimeji.ads.ui.interstitial

import android.app.Activity
import android.app.Application
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
import com.asianmobile.emojibattery.shimeji.ads.config.APPLOVIN
import com.asianmobile.emojibattery.shimeji.ads.config.DELAY_OPEN_ADS
import com.asianmobile.emojibattery.shimeji.ads.config.DISTANCE_TIME_SHOW_OTHER_ADS
import com.asianmobile.emojibattery.shimeji.ads.config.DISTANCE_TIME_SHOW_SAME_ADS
import com.asianmobile.emojibattery.shimeji.ads.config.RULE_SHOW_INTER
import com.asianmobile.emojibattery.shimeji.ads.config.SHOW_BY_CLICK
import com.asianmobile.emojibattery.shimeji.ads.config.SHOW_BY_TIME_AND_CLICK
import com.asianmobile.emojibattery.shimeji.ads.config.SHOW_DISTANCE_TIME
import com.asianmobile.emojibattery.shimeji.ads.config.TEST_DEVICE_ADMOB
import com.asianmobile.emojibattery.shimeji.ads.config.TIME_CLICK_ACTION
import com.asianmobile.emojibattery.shimeji.ads.data.CheckShowAdsUtil
import com.asianmobile.emojibattery.shimeji.ads.data.SharedPreferencesUtils
import com.asianmobile.emojibattery.shimeji.ads.databinding.DialogBeforeShowInterBinding
import com.asianmobile.emojibattery.shimeji.ads.tracking.Tracking
import com.asianmobile.emojibattery.shimeji.ads.tracking.AdFormat
import com.asianmobile.emojibattery.shimeji.ads.tracking.AdPlacement
import com.asianmobile.emojibattery.shimeji.ads.ui.openads.AppOpenManager
import com.asianmobile.emojibattery.shimeji.ads.ui.rewarded.RewardedVideoAds
import com.asianmobile.emojibattery.shimeji.ads.utils.SafeRemoteConfig
import com.asianmobile.emojibattery.shimeji.ads.utils.AdOverlayState
import com.asianmobile.emojibattery.shimeji.ads.utils.AdsIdLogger
import com.asianmobile.emojibattery.shimeji.ads.utils.Utils
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class InterstitialUtil {
    private var _openAd: AppOpenManager? = null
    val openAd: AppOpenManager? get() = _openAd
    private lateinit var alertDialog: Dialog

    // for inters
    private var _lastTime: Long = 0
    val lastTime: Long
        get() = _lastTime

    internal fun resetLastTime() {
        _lastTime = System.currentTimeMillis()
    }

    // check for openAd
    var lastTimeOpenAd: Long = 0
    private var adCloseListener: AdCloseListener? = null
    private var _isShowing = false
    val isShowing: Boolean get() = _isShowing
    private var clickTime = 0

    private object Holder {
        val INSTANCE = InterstitialUtil()
    }

    companion object {
        const val TAG = "InterstitialUtil"

        fun getInstance(): InterstitialUtil {
            return Holder.INSTANCE
        }
    }

    private val isMobileAdsInitialized = AtomicBoolean(false) // Rename for clarity

    private fun initializeAdSdk(
        activity: Activity,
        application: Application,
        shouldLoadAds: Boolean,
        isShowDelaySplash: Boolean,
        onConsentLoaded: (Boolean) -> Unit
    ) { // Rename parameters for clarity
        if (!isMobileAdsInitialized.compareAndSet(false, true)) {
            return
        }
        if (!SharedPreferencesUtils.getIsEnableAds(application)) {
            onConsentLoaded(isShowDelaySplash)
            return
        }
        val backgroundScope = CoroutineScope(Dispatchers.IO)
        val requestConfig = RequestConfiguration.Builder()
            .setTestDeviceIds(listOf(TEST_DEVICE_ADMOB))
            .build()

        backgroundScope.launch {
            MobileAds.initialize(
                application,
                InitializationConfig.Builder(
                    application.getString(R.string.id_emoji_battery_pub)
                ).build()
            ) {
                Log.e(TAG, "initializeAdSdk: SUCCESS")
            }
            MobileAds.setRequestConfiguration(requestConfig)
            if (!activity.isDestroyed && !activity.isFinishing) {
                activity.runOnUiThread {
                    loadOpenAd(application)
                }
                loadAdmob(application)
//                    loadRewarded(application)
                InterstitialLauncherUtil.getInstance().loadAdmobLauncher(
                    application,
                    shouldLoadAds,
                    isLoadComplete = {
                        if (!activity.isFinishing && !activity.isDestroyed) {
                            activity.runOnUiThread {
                                onConsentLoaded(isShowDelaySplash)
                            }
                        }
                    })
            }
        }
        Tracking.setTrackEventByAdjust(APPLOVIN)
    }

    private fun loadRewarded(context: Context) {
        RewardedVideoAds.getInstance().loadRewardedVideo(context)
        RewardedVideoAds.getInstance().isLoading = false
    }

    fun requestConsentForm(
        activity: Activity,
        application: Application,
        onConsentLoaded: (Boolean) -> Unit
    ) {
        isMobileAdsInitialized.set(false)
        clickTime = 0
        resetLastTime()
        lastTimeOpenAd = System.currentTimeMillis()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!Utils.isInternetConnected()) {
                    withContext(Dispatchers.Main) {
                        initializeAdSdk(
                            activity = activity,
                            application = application,
                            shouldLoadAds = false,
                            isShowDelaySplash = false,
                            onConsentLoaded = onConsentLoaded
                        )
                    }
                    return@launch
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    initializeAdSdk(
                        activity = activity,
                        application = application,
                        shouldLoadAds = false,
                        isShowDelaySplash = false,
                        onConsentLoaded = onConsentLoaded
                    )
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                val debugSettings = ConsentDebugSettings.Builder(activity)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    .addTestDeviceHashedId(TEST_DEVICE_ADMOB)
                    .build()

                val params = if (BuildConfig.DEBUG) {
                    ConsentRequestParameters
                        .Builder()
                        .setConsentDebugSettings(debugSettings)
                        .setTagForUnderAgeOfConsent(false)
                        .build()
                } else {
                    ConsentRequestParameters
                        .Builder()
                        .setTagForUnderAgeOfConsent(false)
                        .build()
                }

                val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
                if (BuildConfig.DEBUG) {
//                    consentInformation.reset()
                }

                consentInformation.requestConsentInfoUpdate(activity, params, {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { loadAndShowError ->
                        if (loadAndShowError != null) {
                            // Consent gathering failed.
                            Log.w(TAG, "${loadAndShowError.errorCode}: ${loadAndShowError.message}")
                        }

                        // Consent has been gathered.
                        initializeAdSdk(
                            activity = activity,
                            application = application,
                            shouldLoadAds = consentInformation.canRequestAds(),
                            isShowDelaySplash = true,
                            onConsentLoaded = onConsentLoaded
                        )
                    }
                }, { requestConsentError ->
                    // Consent gathering failed.
                    Log.w(TAG, "${requestConsentError.errorCode}: ${requestConsentError.message}")
                    initializeAdSdk(
                        activity = activity,
                        application = application,
                        shouldLoadAds = true,
                        isShowDelaySplash = true,
                        onConsentLoaded = onConsentLoaded
                    )
                })

                if (consentInformation.canRequestAds()) {
                    initializeAdSdk(
                        activity = activity,
                        application = application,
                        shouldLoadAds = true,
                        isShowDelaySplash = true,
                        onConsentLoaded = onConsentLoaded
                    )
                }
            }
        }
    }

    private fun loadAdmob(context: Context) {
        if (!MobileAds.isInitialized) return
        if (!CheckShowAdsUtil.checkLoadInterAd(context)) return
        val id: String = if (BuildConfig.DEBUG) {
            context.getString(R.string.id_emoji_battery_inter_test)
        } else {
            Log.d(TAG, "loadAdmob: 1")
            context.getString(R.string.id_emoji_battery_inter)
        }
        AdsIdLogger.request(
            format = "INTERSTITIAL",
            adUnitId = id,
            placement = "preload"
        )
        val adRequest = AdRequest.Builder(id).build()
        val preloadConfig = PreloadConfiguration(adRequest)

        val preloadCallback =
            object : PreloadCallback {
                override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
                    AdsIdLogger.failed(
                        format = "INTERSTITIAL",
                        adUnitId = id,
                        placement = "preload",
                        error = adError.message
                    )
                    Log.e(TAG, "onAdFailedToPreload: $preloadId")
                }

                override fun onAdsExhausted(preloadId: String) {
                    Log.i(TAG, "Interstitial1 preload ad is not available.")
                }

                override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
                    AdsIdLogger.loaded(
                        format = "INTERSTITIAL",
                        adUnitId = id,
                        placement = "preload"
                    )
                    Log.i(TAG, "Interstitial 1 preload ad is available. $preloadId")
                }
            }
        InterstitialAdPreloader.start(id, preloadConfig, preloadCallback)
    }

    private fun loadOpenAd(application: Application) {
        _openAd = AppOpenManager()
        _openAd?.let {
            if (!it.isShowOpenAd()) return
            it.fetchAd(application)
        }
    }

    private fun setListener(
        admobFull: InterstitialAd?,
        activity: Activity,
        placement: String
    ) {
        admobFull?.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                Log.d(TAG, "Ad dismissed fullscreen content.")
                if (!activity.isDestroyed && !activity.isFinishing) {
                    activity.runOnUiThread {
                        dismissProgressDialog(activity)
                    }
                }
                if (!activity.isDestroyed && !activity.isFinishing) {
                    activity.runOnUiThread {
                        adCloseListener?.onAdClosed()
                    }
                }
                _isShowing = false
                resetLastTime()
                AdOverlayState.hide()
            }

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                // Called when ad fails to show.
                Log.e(TAG, "Ad failed to show fullscreen content.")
                _isShowing = false
                if (!activity.isDestroyed && !activity.isFinishing) {
                    activity.runOnUiThread {
                        dismissProgressDialog(activity)
                        adCloseListener?.onAdClosed()
                    }
                }
                AdOverlayState.hide()
            }


            override fun onAdShowedFullScreenContent() {
                // Called when ad is shown.
                Log.d(TAG, "Ad showed fullscreen content.")
                _isShowing = true
                AdOverlayState.show()
            }

            override fun onAdClicked() {
                super.onAdClicked()
                Utils.logAdClickEvent(
                    context = activity,
                    placement = placement,
                    adFormat = AdFormat.INTERSTITIAL
                )
            }

            override fun onAdPaid(value: AdValue) {
                Tracking.setTrackRevenueByAdjust(value.valueMicros, value.currencyCode)
            }
        }
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

    fun interface AdCloseListener {
        fun onAdClosed()
    }

    fun showInterstitialAd(
        activity: Activity,
        placement: String = AdPlacement.NAVIGATION,
        adCloseListener: AdCloseListener
    ) {
        if (!MobileAds.isInitialized) {
            adCloseListener.onAdClosed()
            return
        }

        if (!CheckShowAdsUtil.checkShowInterAd(activity)) {
            adCloseListener.onAdClosed()
            return
        }
        val idAds: String = if (BuildConfig.DEBUG) {
            activity.getString(R.string.id_emoji_battery_inter_test)
        } else {
            activity.getString(R.string.id_emoji_battery_inter)
        }

        val rule = getRuleShowInters()
        val currentTime = System.currentTimeMillis()
        val limitTime = getLimitTime()
        val limitTimeOther = getLimitTimeIntersAndOpen()
        val numberClick = getTimeClick()

        var canShow = false

        when (rule) {
            SHOW_DISTANCE_TIME -> {
                if (currentTime - _lastTime >= limitTime && currentTime - lastTimeOpenAd >= limitTimeOther) {
                    canShow = true
                } else {
                    Log.d(TAG, "InterstitialAd limit time")
                }
            }

            SHOW_BY_CLICK -> {
                ++clickTime
                if (clickTime >= numberClick && currentTime - lastTimeOpenAd >= limitTimeOther) {
                    canShow = true
                } else {
                    Log.d(TAG, "InterstitialAd limit by click")
                }
            }

            SHOW_BY_TIME_AND_CLICK -> {
                ++clickTime
                if (currentTime - _lastTime >= limitTime && currentTime - lastTimeOpenAd >= limitTimeOther && clickTime >= numberClick) {
                    canShow = true
                } else {
                    Log.d(TAG, "InterstitialAd limit time and click")
                }
            }

            else -> {
                canShow = false
            }
        }

        if (canShow) {
            Log.e(TAG, "showInterstitialAd: POLL id $idAds")
            val admobFull = InterstitialAdPreloader.pollAd(idAds)
            if (admobFull != null) {
                if (rule == SHOW_BY_CLICK || rule == SHOW_BY_TIME_AND_CLICK) {
                    clickTime = 0
                }
                setListener(admobFull, activity, placement)
                AdOverlayState.show()
                showProgressDialog(activity)
                Handler(Looper.getMainLooper()).postDelayed({
                    Log.d(TAG, "InterstitialAd will show")
                    this.adCloseListener = adCloseListener
                    admobFull.setImmersiveMode(true)
                    admobFull.show(activity)
                }, DELAY_OPEN_ADS)
            } else {
                Log.d(TAG, "InterstitialAd null")
                adCloseListener.onAdClosed()
            }
        } else {
            adCloseListener.onAdClosed()
        }
    }

    private fun getRuleShowInters() = SafeRemoteConfig.getLong(RULE_SHOW_INTER)

    private fun getLimitTime() = SafeRemoteConfig.getLong(DISTANCE_TIME_SHOW_SAME_ADS) * 1000

    fun getLimitTimeIntersAndOpen() = SafeRemoteConfig.getLong(DISTANCE_TIME_SHOW_OTHER_ADS) * 1000

    private fun getTimeClick() = SafeRemoteConfig.getLong(TIME_CLICK_ACTION)
}
