package com.asianmobile.emojibattery.shimeji.ads.ui.compose

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.isVisible
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asianmobile.emojibattery.shimeji.ads.BuildConfig
import com.asianmobile.emojibattery.shimeji.ads.R
import com.asianmobile.emojibattery.shimeji.ads.config.IS_SHOW_NATIVE_INTRO_FULL
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_INTRO_FULL
import com.asianmobile.emojibattery.shimeji.ads.data.SharedPreferencesUtils
import com.asianmobile.emojibattery.shimeji.ads.tracking.Tracking
import com.asianmobile.emojibattery.shimeji.ads.tracking.AdFormat
import com.asianmobile.emojibattery.shimeji.ads.utils.AdsIdLogger
import com.asianmobile.emojibattery.shimeji.ads.utils.SafeRemoteConfig
import com.asianmobile.emojibattery.shimeji.ads.utils.Utils
import com.bumptech.glide.Glide
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
import jp.wasabeef.blurry.Blurry

private const val TAG = "NativeIntroFullAd"

/**
 * Composable wrapper for full-screen intro native ads with blur effect.
 */
@Composable
fun NativeIntroFullAd(
    isReload: Boolean = false,
    modifier: Modifier = Modifier,
    onLoadComplete: () -> Unit = {}
) {
    if (!MobileAds.isInitialized) {
        onLoadComplete()
        return
    }
    val context = LocalContext.current
    val activity = context as? Activity ?: return

    if (!SharedPreferencesUtils.getIsEnableAds(activity) || Utils.checkLimitAd(activity)) {
        onLoadComplete()
        return
    }
    if (!Utils.isNetworkAvailable(activity)) {
        onLoadComplete()
        return
    }
    if (activity.isDestroyed || activity.isFinishing) {
        onLoadComplete()
        return
    }
    if (!SafeRemoteConfig.getBoolean(IS_SHOW_NATIVE_INTRO_FULL)) {
        onLoadComplete()
        return
    }

    val adViewModel: NativeAdViewModel = viewModel(key = "NativeIntroFullAdViewModel_$isReload")

    val nativeAdId = remember(isReload) {
        if (BuildConfig.DEBUG) {
            context.getString(R.string.id_emoji_battery_native_test)
        } else {
            if (isReload) {
                context.getString(R.string.id_emoji_battery_native_full_intro)
            } else {
                context.getString(R.string.id_emoji_battery_native_full_intro_2)
            }
        }
    }

    if (nativeAdId.isBlank()) {
        onLoadComplete()
        return
    }

    LaunchedEffect(nativeAdId) {
        if (activity.isDestroyed || activity.isFinishing) return@LaunchedEffect

        if (adViewModel.nativeAd != null || adViewModel.isLoading) {
            if (adViewModel.nativeAd != null) onLoadComplete()
            return@LaunchedEffect
        }

        if (adViewModel.adFailed) {
            onLoadComplete()
            return@LaunchedEffect
        }

        adViewModel.adFailed = false
        adViewModel.isLoading = true

        val placement = if (isReload) "intro_full_reload" else "intro_full"
        AdsIdLogger.request(format = "NATIVE_FULL", adUnitId = nativeAdId, placement = placement)
        val adRequest = NativeAdRequest
            .Builder(nativeAdId, listOf(NativeAd.NativeAdType.NATIVE))
            .build()

        val adCallback =
            object : NativeAdLoaderCallback {
                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                    super.onNativeAdLoaded(nativeAd)
                    AdsIdLogger.loaded(
                        format = "NATIVE_FULL",
                        adUnitId = nativeAdId,
                        placement = placement
                    )
                    if (activity.isFinishing || activity.isDestroyed) {
                        nativeAd.destroy()
                        onLoadComplete()
                        return
                    }
                    if (!activity.isDestroyed && !activity.isFinishing) {

                        adViewModel.nativeAd = nativeAd
                        adViewModel.isLoading = false
                        onLoadComplete()

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
                                    placement = SCREEN_INTRO_FULL,
                                    adFormat = AdFormat.NATIVE_FULL
                                )
                            }
                        }
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    super.onAdFailedToLoad(adError)
                    AdsIdLogger.failed(
                        format = "NATIVE_FULL",
                        adUnitId = nativeAdId,
                        placement = placement,
                        error = adError.message
                    )
                    if (!activity.isDestroyed && !activity.isFinishing) {
                        Log.e(TAG, "onAdFailedToLoad: $adError")
                        activity.runOnUiThread {
                            adViewModel.adFailed = true
                            adViewModel.isLoading = false
                            onLoadComplete()
                        }
                    }
                }
            }

        NativeAdLoader.load(adRequest, adCallback)
    }

    if (adViewModel.adFailed) return

    val currentAd = adViewModel.nativeAd

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                LayoutInflater.from(ctx)
                    .inflate(
                        R.layout.layout_native_ad_208h,
                        FrameLayout(ctx),
                        false
                    ) as ShimmerFrameLayout
            },
            update = { shimmerLayout ->
                val adView =
                    shimmerLayout.findViewById<NativeAdView>(R.id.unifiedAdView)
                        ?: return@AndroidView
                val adObj = currentAd
                if (adObj == null) {
                    shimmerLayout.startShimmer()
                    shimmerLayout.showShimmer(true)
                } else {
                    shimmerLayout.stopShimmer()
                    shimmerLayout.hideShimmer()
                    shimmerLayout.isClickable = false
                    shimmerLayout.isFocusable = false

                    populateIntroFullAdView(activity, adObj, adView)
                    adView.setNativeAdLifecycle(adObj)
                }
            }
        )
    }
}

private fun populateIntroFullAdView(activity: Activity, nativeAd: NativeAd, adView: NativeAdView) {
    val oldAd = adView.getTag(R.id.tag_native_ad) as? NativeAd
    if (oldAd === nativeAd) return

    adView.apply {
        headlineView = findViewById<TextView?>(R.id.tvTitleAds)?.apply {
            background = null
            text = nativeAd.headline
        }
        bodyView = findViewById<TextView?>(R.id.tvMessageAds)?.apply {
            background = null
            isVisible = !nativeAd.body.isNullOrEmpty()
            text = nativeAd.body.orEmpty()
        }
        callToActionView = findViewById<TextView?>(R.id.tvOpenAds)?.apply {
            isVisible = !nativeAd.callToAction.isNullOrEmpty()
            text = nativeAd.callToAction.orEmpty()
        }
        iconView = findViewById<ImageView?>(R.id.imgIcon)?.apply {
            background = null
            isVisible = nativeAd.icon != null
            nativeAd.icon?.let { setImageDrawable(it.drawable) }
        }

        isClickable = true
        isFocusable = true
    }

    val mediaView = adView.findViewById<MediaView>(R.id.mediaView)
    mediaView?.background = null
    adView.registerNativeAd(nativeAd, mediaView)

    // Blur effect for intro full
    nativeAd.mediaContent.mainImage?.let { mediaDrawable ->
        adView.findViewById<ImageView>(R.id.ivBlurContent)?.apply {
            visibility = View.VISIBLE
            if (activity.isDestroyed || activity.isFinishing) return
            Glide.with(activity).load(mediaDrawable)
                .listener(object :
                    com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                        isFirstResource: Boolean
                    ) = false

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable,
                        model: Any,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        try {
                            adView.findViewById<FrameLayout>(R.id.flBlur)?.apply {
                                Blurry.with(context).radius(25).sampling(2).onto(this)
                            }
                        } catch (e: RuntimeException) {
                            e.printStackTrace()
                        }
                        return false
                    }
                })
                .into(this)
        }
    }
}
