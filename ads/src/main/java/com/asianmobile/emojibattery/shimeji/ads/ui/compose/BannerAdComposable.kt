package com.asianmobile.emojibattery.shimeji.ads.ui.compose

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asianmobile.emojibattery.shimeji.ads.BuildConfig
import com.asianmobile.emojibattery.shimeji.ads.R
import com.asianmobile.emojibattery.shimeji.ads.config.BOTTOM_POSITION
import com.asianmobile.emojibattery.shimeji.ads.config.IS_SHOW_BANNER_ADS
import com.asianmobile.emojibattery.shimeji.ads.config.LIMIT_AD_COUNT
import com.asianmobile.emojibattery.shimeji.ads.config.MY_PREFERENCES
import com.asianmobile.emojibattery.shimeji.ads.data.SharedPreferencesUtils
import com.asianmobile.emojibattery.shimeji.ads.tracking.Tracking
import com.asianmobile.emojibattery.shimeji.ads.tracking.AdFormat
import com.asianmobile.emojibattery.shimeji.ads.utils.AdsIdLogger
import com.asianmobile.emojibattery.shimeji.ads.utils.SafeRemoteConfig
import com.asianmobile.emojibattery.shimeji.ads.utils.Utils
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

/**
 * Composable wrapper for banner ads.
 * Usage: BannerAd(modifier = Modifier.fillMaxWidth())
 */
@Composable
fun BannerAd(
    modifier: Modifier = Modifier,
    adPosition: String = BOTTOM_POSITION
) {
    if (!MobileAds.isInitialized) return
    val context = LocalContext.current
    // In dialog destinations, LocalContext may not be an Activity.
    // Fall back to the View's context which always traces back to the host Activity.
    val activity = context.findActivity()
        ?: LocalView.current.context.findActivity()
        ?: return

    val preferences = remember(activity) {
        activity.applicationContext.getSharedPreferences(
            MY_PREFERENCES,
            Context.MODE_PRIVATE
        )
    }
    var adClickCount by remember(preferences) {
        mutableIntStateOf(SharedPreferencesUtils.getTimeAdClicked(activity))
    }
    DisposableEffect(preferences) {
        val listener =
            android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                if (key == LIMIT_AD_COUNT) {
                    adClickCount = sharedPreferences.getInt(LIMIT_AD_COUNT, 0)
                }
            }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    if (Utils.checkLimitAd(adClickCount)) return
    if (!SharedPreferencesUtils.getIsEnableAds(activity)) return
    if (!SafeRemoteConfig.getBoolean(IS_SHOW_BANNER_ADS)) return
    val adViewModel: BannerAdViewModel = viewModel(key = "BannerAdViewModel_$adPosition")

    // Actual banner ad
    LaunchedEffect(adPosition) {
        if (activity.isDestroyed || activity.isFinishing) return@LaunchedEffect
        if (adViewModel.adView != null || adViewModel.isLoading) return@LaunchedEffect

        adViewModel.isLoading = true
        val bannerAdSize = AdSize.BANNER
        val bannerId = if (BuildConfig.DEBUG) {
            activity.getString(R.string.id_private_browser_banner_test)
        } else {
            activity.getString(R.string.id_private_browser_banner)
        }
        AdsIdLogger.request(format = "BANNER", adUnitId = bannerId, placement = adPosition)
        val adRequest =
            BannerAdRequest.Builder(bannerId, bannerAdSize)
                .setGoogleExtrasBundle(Bundle()).build()

        val newAdView = AdView(activity).apply {
            loadAd(adRequest, object : AdLoadCallback<BannerAd> {
                override fun onAdLoaded(ad: BannerAd) {
                    if (!activity.isDestroyed && !activity.isFinishing) {
                        AdsIdLogger.loaded(
                            format = "BANNER",
                            adUnitId = bannerId,
                            placement = adPosition
                        )
                        adViewModel.isLoaded = true
                        adViewModel.isLoading = false

                        ad.adEventCallback = object : BannerAdEventCallback {
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
                                    placement = adPosition,
                                    adFormat = AdFormat.BANNER
                                )
                            }
                        }
                    } else {
                        ad.destroy()
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    AdsIdLogger.failed(
                        format = "BANNER",
                        adUnitId = bannerId,
                        placement = adPosition,
                        error = adError.message
                    )
                    adViewModel.isAdFailed = true
                    adViewModel.isLoading = false
                }
            })
        }

        adViewModel.adView = newAdView
    }

    if (!activity.isDestroyed && !activity.isFinishing) {
        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            factory = { ctx ->
                LayoutInflater.from(ctx)
                    .inflate(
                        R.layout.holder_load_banner,
                        FrameLayout(ctx),
                        false
                    ) as ShimmerFrameLayout
            },
            update = { shimmerLayout ->
                val bannerContainer = shimmerLayout.findViewById<FrameLayout>(R.id.bannerContainer)
                val viewShimmer = shimmerLayout.findViewById<ImageView>(R.id.ivThumbBanner)

                if (adViewModel.isLoaded && adViewModel.adView != null) {
                    shimmerLayout.stopShimmer()
                    shimmerLayout.hideShimmer()
                    viewShimmer?.visibility = View.GONE
                    bannerContainer.visibility = View.VISIBLE

                    val adView = adViewModel.adView!!
                    if (adView.parent != bannerContainer) {
                        (adView.parent as? ViewGroup)?.removeView(adView)
                        bannerContainer.removeAllViews()
                        bannerContainer.addView(
                            adView,
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                Gravity.CENTER
                            )
                        )
                    }
                } else if (adViewModel.isAdFailed) {
                    shimmerLayout.stopShimmer()
                    shimmerLayout.hideShimmer()
                    viewShimmer?.visibility = View.GONE
                    bannerContainer.visibility = View.GONE
                } else {
                    shimmerLayout.startShimmer()
                    shimmerLayout.showShimmer(true)
                    viewShimmer?.visibility = View.VISIBLE
                    bannerContainer.visibility = View.GONE
                }
            }
        )
    }
}

class BannerAdViewModel : ViewModel() {
    var adView by mutableStateOf<AdView?>(null)
    var isLoaded by mutableStateOf(false)
    var isAdFailed by mutableStateOf(false)
    var isLoading by mutableStateOf(false)

    override fun onCleared() {
        super.onCleared()
        adView?.destroy()
        adView = null
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
