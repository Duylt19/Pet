package com.asianmobile.emojibattery.shimeji.battery.overlay

/**
 * Keeps nullable composition changes away from Lottie's non-null setComposition API.
 */
internal class BatteryLottieCompositionLifecycle<T : Any>(
    private val cancelAnimation: () -> Unit,
    private val clearComposition: () -> Unit,
    private val setComposition: (T) -> Unit,
    private val playAnimation: () -> Unit,
    private val isAnimating: () -> Boolean
) {
    private var currentComposition: T? = null

    fun update(composition: T?, shouldPlay: Boolean) {
        if (currentComposition !== composition) {
            cancelAnimation()
            if (composition == null) {
                clearComposition()
            } else {
                setComposition(composition)
            }
            currentComposition = composition
        }

        val canPlay = composition != null && shouldPlay
        when {
            canPlay && !isAnimating() -> playAnimation()
            !canPlay && isAnimating() -> cancelAnimation()
        }
    }

    fun reset() {
        cancelAnimation()
        clearComposition()
        currentComposition = null
    }
}
