package com.asianmobile.emojibattery.shimeji.ui.pet.store

import com.asianmobile.emojibattery.shimeji.pet.overlay.PetOverlayRuntimeState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetOverlayTogglePolicyTest {
    @Test
    fun `starting state ignores repeated switch taps`() {
        assertFalse(
            PetOverlayTogglePolicy.canHandle(
                runtimeState = PetOverlayRuntimeState.STARTING,
                nowMillis = 5_000L,
                lastHandledAtMillis = 1_000L
            )
        )
    }

    @Test
    fun `stable state debounces rapid taps`() {
        assertFalse(
            PetOverlayTogglePolicy.canHandle(
                runtimeState = PetOverlayRuntimeState.RUNNING,
                nowMillis = 1_500L,
                lastHandledAtMillis = 1_000L
            )
        )
        assertTrue(
            PetOverlayTogglePolicy.canHandle(
                runtimeState = PetOverlayRuntimeState.RUNNING,
                nowMillis = 1_800L,
                lastHandledAtMillis = 1_000L
            )
        )
    }

    @Test
    fun `first tap in stopped state is handled`() {
        assertTrue(
            PetOverlayTogglePolicy.canHandle(
                runtimeState = PetOverlayRuntimeState.STOPPED,
                nowMillis = 1_000L,
                lastHandledAtMillis = null
            )
        )
    }
}
