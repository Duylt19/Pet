package com.asianmobile.emojibattery.shimeji.pet.overlay

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetOverlayRuntimeStateTest {
    @Test
    fun `switch is enabled while service is starting or running`() {
        assertTrue(PetOverlayRuntimeState.STARTING.isEnabled)
        assertTrue(PetOverlayRuntimeState.RUNNING.isEnabled)
    }

    @Test
    fun `switch is disabled when service is stopped`() {
        assertFalse(PetOverlayRuntimeState.STOPPED.isEnabled)
    }

    @Test
    fun `only enabled states allow service startup to continue`() {
        assertFalse(PetOverlayRuntimeState.STOPPED.shouldStartService())
        assertTrue(PetOverlayRuntimeState.STARTING.shouldStartService())
        assertTrue(PetOverlayRuntimeState.RUNNING.shouldStartService())
    }
}
