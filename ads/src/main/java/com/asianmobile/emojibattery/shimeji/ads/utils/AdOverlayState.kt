package com.asianmobile.emojibattery.shimeji.ads.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide source of truth while any full-screen ad owns the display.
 *
 * [isAdShowing] is consumed by app-owned overlays, including the Accessibility status bar, that
 * must never cover an ad. [shouldHideActivityContent] is separate because an App Open ad needs the
 * host screen to remain rendered behind the SDK window; otherwise a black frame can be exposed
 * while that window is being removed. Interstitial and rewarded ads keep the safer default of
 * hiding the host content.
 */
object AdOverlayState {
    private val _isAdShowing = MutableStateFlow(false)
    val isAdShowing: StateFlow<Boolean> = _isAdShowing.asStateFlow()

    private val _shouldHideActivityContent = MutableStateFlow(false)
    val shouldHideActivityContent: StateFlow<Boolean> =
        _shouldHideActivityContent.asStateFlow()

    fun show(hideActivityContent: Boolean = true) {
        _shouldHideActivityContent.value = hideActivityContent
        _isAdShowing.value = true
    }

    fun hide() {
        // Restore the Activity before app-owned overlays are allowed to return.
        _shouldHideActivityContent.value = false
        _isAdShowing.value = false
    }
}
