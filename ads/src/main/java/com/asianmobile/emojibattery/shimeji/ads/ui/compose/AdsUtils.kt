package com.asianmobile.emojibattery.shimeji.ads.ui.compose

import android.view.View
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.asianmobile.emojibattery.shimeji.ads.R
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.destroyAd
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView

/**
 * Copyright © 2026 Asian Mobile Co.,Ltd
 * Created by am_viennv on 3/16/2026
 */

fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
    val oldAd = adView.getTag(R.id.tag_native_ad) as? NativeAd
    if (oldAd === nativeAd) return

    adView.apply {
        headlineView = findViewById(R.id.tvTitleAds)
        bodyView = findViewById(R.id.tvMessageAds)
        callToActionView = findViewById(R.id.tvOpenAds)
        iconView = findViewById(R.id.imgIcon)

        mediaView?.background = null
        iconView?.background = null
        headlineView?.background = null
        bodyView?.background = null

        isClickable = true
        isFocusable = true
    }
    val mediaView = adView.findViewById<MediaView>(R.id.mediaView)
    mediaView?.background = null
    adView.registerNativeAd(nativeAd, mediaView)

    // Headline
    (adView.headlineView as? TextView)?.apply {
        text = nativeAd.headline
    }

    // Body
    nativeAd.body?.let {
        (adView.bodyView as? TextView)?.apply {
            text = it
            visibility = View.VISIBLE
        }
    } ?: run {
        adView.bodyView?.visibility = View.INVISIBLE
    }

    // Call to action
    nativeAd.callToAction?.let {
        (adView.callToActionView as? TextView)?.apply {
            text = it
            visibility = View.VISIBLE
        }
    } ?: run {
        adView.callToActionView?.visibility = View.INVISIBLE
    }

    // Icon
    nativeAd.icon?.let {
        (adView.iconView as? ImageView)?.setImageDrawable(it.drawable)
        adView.iconView?.visibility = View.VISIBLE
    } ?: run {
        adView.iconView?.visibility = View.INVISIBLE
    }

    // Price
    nativeAd.price?.let {
        (adView.priceView as? TextView)?.apply {
            text = it
            visibility = View.VISIBLE
        }
    } ?: run {
        adView.priceView?.visibility = View.GONE
    }

    // Store
    nativeAd.store?.let {
        (adView.storeView as? TextView)?.apply {
            text = it
            visibility = View.VISIBLE
        }
    } ?: run {
        adView.storeView?.visibility = View.GONE
    }

    // Rating
    nativeAd.starRating?.let {
        (adView.starRatingView as? RatingBar)?.apply {
            rating = it.toFloat()
            visibility = View.VISIBLE
        }
    } ?: run {
        adView.starRatingView?.visibility = View.GONE
    }

    // Advertiser
    nativeAd.advertiser?.let {
        (adView.advertiserView as? TextView)?.apply {
            text = it
            visibility = View.VISIBLE
        }
    } ?: run {
        adView.advertiserView?.visibility = View.GONE
    }
}

internal fun NativeAdView.setNativeAdLifecycle(ad: NativeAd) {

    val oldAd = getTag(R.id.tag_native_ad) as? NativeAd

    // Nếu cùng 1 ad object → không cần làm gì thêm
    if (oldAd === ad) return

    // Destroy ad cũ nếu khác ad mới
    oldAd?.destroy()

    setTag(R.id.tag_native_ad, ad)

    val lifecycleOwner = findViewTreeLifecycleOwner() ?: return

    val observer = object : DefaultLifecycleObserver {

        override fun onDestroy(owner: LifecycleOwner) {
            destroyAd()
            owner.lifecycle.removeObserver(this)
        }
    }

    lifecycleOwner.lifecycle.addObserver(observer)
}

internal fun NativeAdView.destroyAd() {
    (getTag(R.id.tag_native_ad) as? NativeAd)?.let {
        it.destroy()
        setTag(R.id.tag_native_ad, null)
    }
}
internal fun clearNativeAdView(adView: NativeAdView) {
    runCatching {
        adView.apply {
            val media = findViewById<View>(R.id.mediaView)
            val headline = findViewById<TextView>(R.id.tvTitleAds)
            val body = findViewById<TextView>(R.id.tvMessageAds)
            val cta = findViewById<TextView>(R.id.tvOpenAds)
            val icon = findViewById<ImageView>(R.id.imgIcon)

            headline?.text = ""
            body?.text = ""
            cta?.text = ""
            icon?.setImageDrawable(null)

            media?.setBackgroundResource(R.drawable.bg_in_loading_ads_content)
            headline?.setBackgroundResource(R.drawable.bg_in_loading_ads_content)
            body?.setBackgroundResource(R.drawable.bg_in_loading_ads_content)
            icon?.setBackgroundResource(R.drawable.bg_in_loading_ads_content)
            cta?.setBackgroundResource(R.drawable.bg_button_open_ads)

            (media as? MediaView)?.removeAllViews()

            media?.visibility = View.VISIBLE
            headline?.visibility = View.VISIBLE
            body?.visibility = View.VISIBLE
            cta?.visibility = View.VISIBLE
            icon?.visibility = View.VISIBLE

            headlineView = null
            bodyView = null
            callToActionView = null
            iconView = null
        }
    }
}


