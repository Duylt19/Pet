package com.asianmobile.privatebrower.ads.tracking

import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.adjust.sdk.AdjustEvent
import com.asianmobile.privatebrower.ads.config.APP_AD_IMPRESSION

/**
 * Copyright © 2024 Asian Mobile Co.,Ltd
 * Created by am_Huyhn on 1/10/24
 */
object Tracking {
    internal fun setTrackEventByAdjust(tokenEvent: String) {
        val adjustEvent = AdjustEvent(tokenEvent)
        Adjust.trackEvent(adjustEvent)
    }

    internal fun setTrackRevenueByAdjust(revenue: Long, currency: String) {
        val adjustEventRevenue = AdjustAdRevenue("admob_sdk")
        adjustEventRevenue.setRevenue((revenue / 1000000f).toDouble(), currency)
        Adjust.trackAdRevenue(adjustEventRevenue)

        // Ads Impression
        val adJust = AdjustEvent(APP_AD_IMPRESSION)
        adJust.setRevenue((revenue / 1000000f).toDouble(), currency)
        Adjust.trackEvent(adJust)
    }
}

