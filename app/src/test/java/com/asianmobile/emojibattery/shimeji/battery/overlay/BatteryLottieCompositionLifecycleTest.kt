package com.asianmobile.emojibattery.shimeji.battery.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryLottieCompositionLifecycleTest {
    @Test
    fun nullComposition_isClearedWithoutCallingSetComposition() {
        val events = mutableListOf<String>()
        val lifecycle = lifecycle(events = events, initiallyAnimating = true)

        lifecycle.update(composition = "composition", shouldPlay = true)
        events.clear()
        lifecycle.update(composition = null, shouldPlay = false)

        assertEquals(listOf("cancel", "clear"), events)
    }

    @Test
    fun composition_isSetAndPlayed_whenAnimationIsEnabled() {
        val events = mutableListOf<String>()
        val lifecycle = lifecycle(events)

        lifecycle.update(composition = "composition", shouldPlay = true)

        assertEquals(listOf("cancel", "set:composition", "play"), events)
    }

    @Test
    fun reset_cancelsAndClearsComposition_forDetach() {
        val events = mutableListOf<String>()
        val lifecycle = lifecycle(events)
        lifecycle.update(composition = "composition", shouldPlay = false)
        events.clear()

        lifecycle.reset()

        assertEquals(listOf("cancel", "clear"), events)
    }

    private fun lifecycle(
        events: MutableList<String>,
        initiallyAnimating: Boolean = false
    ): BatteryLottieCompositionLifecycle<String> {
        var isAnimating = initiallyAnimating
        return BatteryLottieCompositionLifecycle(
            cancelAnimation = {
                events += "cancel"
                isAnimating = false
            },
            clearComposition = { events += "clear" },
            setComposition = { events += "set:$it" },
            playAnimation = {
                events += "play"
                isAnimating = true
            },
            isAnimating = { isAnimating }
        )
    }
}
