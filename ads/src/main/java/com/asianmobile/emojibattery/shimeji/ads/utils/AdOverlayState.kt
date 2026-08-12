package com.asianmobile.emojibattery.shimeji.ads.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide source of truth while any full-screen ad owns the display.
 *
 * Consumers hide app-owned surfaces that must never cover an ad, including the activity content
 * and the Accessibility status-bar overlay. Ad callbacks are authoritative: a time-based reset
 * could expose those surfaces over a long rewarded or interstitial creative.
 */
object AdOverlayState {
    private val _isAdShowing = MutableStateFlow(false)
    val isAdShowing: StateFlow<Boolean> = _isAdShowing.asStateFlow()

    fun show() {
        _isAdShowing.value = true
    }

    fun hide() {
        _isAdShowing.value = false
    }
}

