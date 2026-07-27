package com.asianmobile.emojibattery.shimeji.ads.utils

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global state to hide the app's ComposeView when interstitial or app open ads
 * are displaying. This prevents UI bleed-through behind the ad's transparent areas.
 *
 * Safety: auto-hide after [MAX_OVERLAY_DURATION_MS] to prevent permanent screen block.
 */
object AdOverlayState {

    private const val MAX_OVERLAY_DURATION_MS = 15_000L  // 15 seconds max

    private val _isAdShowing = MutableStateFlow(false)
    val isAdShowing: StateFlow<Boolean> = _isAdShowing.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private val autoHideRunnable = Runnable { hide() }

    fun show() {
        _isAdShowing.value = true
        handler.removeCallbacks(autoHideRunnable)
        handler.postDelayed(autoHideRunnable, MAX_OVERLAY_DURATION_MS)
    }

    fun hide() {
        _isAdShowing.value = false
        handler.removeCallbacks(autoHideRunnable)
    }
}


