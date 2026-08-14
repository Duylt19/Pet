package com.asianmobile.emojibattery.shimeji.ads.ui.compose

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asianmobile.emojibattery.shimeji.ads.BuildConfig
import com.asianmobile.emojibattery.shimeji.ads.R
import com.asianmobile.emojibattery.shimeji.ads.data.CheckShowAdsUtil
import com.asianmobile.emojibattery.shimeji.ads.tracking.Tracking
import com.asianmobile.emojibattery.shimeji.ads.tracking.AdFormat
import com.asianmobile.emojibattery.shimeji.ads.utils.AdsIdLogger
import com.asianmobile.emojibattery.shimeji.ads.utils.SafeRemoteConfig
import com.asianmobile.emojibattery.shimeji.ads.utils.Utils
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView

private const val TAG = "NativeAdComposable"

// ─────────────────────────────────────────────────────────
// Internal implementation
// ─────────────────────────────────────────────────────────

enum class AdType {
    HEIGHT_150,
    HEIGHT_208,
    HEIGHT_222,
    HEIGHT_222_SMALL_CTA,
    COLLAPSE_MEDIUM,
    COLLAPSE_SMALL,
    ITEM,
    PHOTO_ITEM
}

@Composable
fun NativeAdInternal(
    screenCode: String,
    modifier: Modifier = Modifier,
    customAdId: String? = null,
    instanceKey: String? = null,
    reloadKey: Int = 0,
    adTypeOverride: AdType? = null,
    showLoadingPlaceholder: Boolean = true,
    loadResult: (Boolean) -> Unit = {}
) {
    if (!MobileAds.isInitialized) {
        loadResult(false)
        return
    }
    val context = LocalContext.current
    val view = LocalView.current
    // In dialog destinations, LocalContext may not trace back to an Activity.
    // Fall back to the View's context which always resolves to the host Activity.
    val activity = remember(context, view) {
        context.findActivity() ?: view.context.findActivity()
    }
    val adViewModel: NativeAdViewModel = viewModel(
        key = "NativeAdViewModel_${screenCode}_${instanceKey ?: customAdId.orEmpty()}"
    )
    val placement = remember(screenCode) {
        NativeAdPlacementCatalog.find(screenCode)
    }
    val adType = remember(placement, adTypeOverride) {
        adTypeOverride ?: placement?.adType ?: AdType.HEIGHT_208
    }

    val layoutRes = remember(adType) {
        when (adType) {
            AdType.HEIGHT_150 -> R.layout.layout_native_ad_150h
            AdType.HEIGHT_208 -> R.layout.layout_native_ad_208h
            AdType.HEIGHT_222 -> R.layout.layout_native_ad_222h
            AdType.HEIGHT_222_SMALL_CTA -> R.layout.layout_native_ad_222h_small_cta
            AdType.COLLAPSE_MEDIUM -> R.layout.holder_load_native_collab
            AdType.COLLAPSE_SMALL -> R.layout.holder_load_native_collab_banner
            AdType.ITEM, AdType.PHOTO_ITEM -> R.layout.layout_native_ad_item
        }
    }

    val nativeAdId = remember(customAdId, screenCode, placement) {
        customAdId ?: if (BuildConfig.DEBUG) {
            context.getString(R.string.id_emoji_battery_native_test)
        } else {
            placement?.let { context.getString(it.adUnitResId) }.orEmpty()
        }
    }

    var isCallbackCalled by remember(nativeAdId, reloadKey) { mutableStateOf(false) }
    var isAdStale by remember(reloadKey) { mutableStateOf(reloadKey > 0) }

    fun safeCallback(result: Boolean) {
        if (!isCallbackCalled) {
            isCallbackCalled = true
            loadResult(result)
        }
    }
    if (activity == null) {
        safeCallback(false)
        return
    }

    if (!CheckShowAdsUtil.checkShowNativeAd(activity)) {
        safeCallback(false)
        return
    }

    if (placement == null || !SafeRemoteConfig.getBoolean(placement.remoteConfigKey)) {
        safeCallback(false)
        return
    }

    if (nativeAdId.isBlank()) {
        safeCallback(false)
        return
    }

    LaunchedEffect(nativeAdId, reloadKey) {
        if (reloadKey > 0) {
            adViewModel.clearAds()
            isAdStale = false
            kotlinx.coroutines.delay(300L)
        }
        if (activity.isDestroyed || activity.isFinishing) {
            safeCallback(false)
            return@LaunchedEffect
        }
        if (adViewModel.nativeAd != null && reloadKey == 0) {
            return@LaunchedEffect
        }
        if (adViewModel.isLoading) {
            return@LaunchedEffect
        }

        val currentGeneration = adViewModel.loadGeneration
        adViewModel.adFailed = false
        adViewModel.isLoading = true
        Log.e(TAG, "NativeAdInternal: LOAD NATIVE ADS $screenCode")
        AdsIdLogger.request(format = "NATIVE", adUnitId = nativeAdId, placement = screenCode)
        val adRequest = NativeAdRequest
            .Builder(nativeAdId, listOf(NativeAd.NativeAdType.NATIVE))
            .build()

        val adCallback =
            object : NativeAdLoaderCallback {
                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                    super.onNativeAdLoaded(nativeAd)
                    AdsIdLogger.loaded(
                        format = "NATIVE",
                        adUnitId = nativeAdId,
                        placement = screenCode
                    )
                    if (activity.isDestroyed || activity.isFinishing) {
                        nativeAd.destroy()
                        adViewModel.isLoading = false
                        safeCallback(false)
                        return
                    }
                    if (!activity.isDestroyed && !activity.isFinishing) {
                        if (adViewModel.loadGeneration != currentGeneration) {
                            nativeAd.destroy()
                            return
                        }
                        adViewModel.nativeAd = nativeAd
                        adViewModel.isLoading = false

                        nativeAd.adEventCallback = object : NativeAdEventCallback {
                            override fun onAdPaid(value: AdValue) {
                                Tracking.setTrackRevenueByAdjust(
                                    value.valueMicros,
                                    value.currencyCode
                                )
                            }

                            override fun onAdClicked() {
                                super.onAdClicked()
                                Utils.logAdClickEvent(
                                    context = activity,
                                    placement = screenCode,
                                    adFormat = AdFormat.NATIVE
                                )
                            }
                        }
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    super.onAdFailedToLoad(adError)
                    AdsIdLogger.failed(
                        format = "NATIVE",
                        adUnitId = nativeAdId,
                        placement = screenCode,
                        error = adError.message
                    )
                    if (!activity.isDestroyed && !activity.isFinishing) {
                        Log.e(TAG, "onAdFailedToLoad: $screenCode $adError")
                        activity.runOnUiThread {
                            if (adViewModel.loadGeneration != currentGeneration) return@runOnUiThread
                            adViewModel.adFailed = true
                            adViewModel.isLoading = false
                            safeCallback(false)
                        }
                    }
                }
            }

        NativeAdLoader.load(adRequest, adCallback)
    }
    if (adViewModel.adFailed) {
        safeCallback(false)
        return
    }
    val currentAd = adViewModel.nativeAd
    if (currentAd == null && !showLoadingPlaceholder) {
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                LayoutInflater.from(ctx)
                    .inflate(layoutRes, FrameLayout(ctx), false) as ShimmerFrameLayout
            },
            update = { shimmerLayout ->
                Log.e(
                    TAG,
                    "NativeAdInternal: $screenCode  -- reloadKey=$reloadKey  hasAd=${currentAd != null}"
                )
                if (activity.isDestroyed || activity.isFinishing) return@AndroidView
                val adView =
                    shimmerLayout.findViewById<NativeAdView>(R.id.unifiedAdView)
                        ?: return@AndroidView
                if (adType == AdType.PHOTO_ITEM) {
                    shimmerLayout.findViewById<MediaView>(R.id.mediaView)?.let { mediaView ->
                        mediaView.layoutParams = mediaView.layoutParams.apply {
                            height = shimmerLayout.resources.getDimensionPixelSize(
                                com.intuit.sdp.R.dimen._103sdp
                            )
                        }
                    }
                }
                shimmerLayout.findViewById<ImageView>(R.id.ivClose)
                    ?.setOnClickListener {
                        val mediaView = shimmerLayout.findViewById<MediaView>(R.id.mediaView)
                        val ivClose = shimmerLayout.findViewById<ImageView>(R.id.ivClose)
                        // Trigger smooth collapse animation on the ConstraintLayout parent
                        (mediaView?.parent as? android.view.ViewGroup)?.let { parentGroup ->
                            val layoutTransition = android.animation.LayoutTransition()
                            layoutTransition.enableTransitionType(android.animation.LayoutTransition.CHANGING)
                            layoutTransition.setDuration(300)
                            parentGroup.layoutTransition = layoutTransition
                        }
                        mediaView?.isVisible = false
                        ivClose?.isVisible = false
                    }
                val adToShow = currentAd?.takeIf { !isAdStale }
                adToShow?.let { nativeAds ->
                    shimmerLayout.findViewById<ImageView>(R.id.ivClose)
                        ?.isVisible = true
                    shimmerLayout.stopShimmer()
                    shimmerLayout.hideShimmer()
                    shimmerLayout.isClickable = false
                    shimmerLayout.isFocusable = false

                    populateNativeAdView(nativeAds, adView)
                    adView.setNativeAdLifecycle(nativeAds)
                    safeCallback(true)
                } ?: run {
                    shimmerLayout.startShimmer()
                    shimmerLayout.showShimmer(true)
                    shimmerLayout.isClickable = true
                    clearNativeAdView(adView)
                }
            }
        )
    }
}

class NativeAdViewModel : ViewModel() {
    var nativeAd by mutableStateOf<NativeAd?>(null)
    var isLoading by mutableStateOf(false)
    var adFailed by mutableStateOf(false)
    var loadGeneration = 0
        private set

    override fun onCleared() {
        super.onCleared()
        clearAds()
    }

    internal fun clearAds() {
        nativeAd?.destroy()
        nativeAd = null
        isLoading = false
        adFailed = false
        loadGeneration++
    }
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
