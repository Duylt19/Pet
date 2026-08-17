package com.asianmobile.emojibattery.shimeji.data.repository.impl

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryAppliedStateMigrationTest {
    @Test
    fun legacyEnabledConfig_isRecognizedAsPreviouslyApplied() {
        assertTrue(resolveBatteryHasApplied(storedHasApplied = null, enabled = true))
    }

    @Test
    fun firstUseConfig_hasNoAppliedState() {
        assertFalse(resolveBatteryHasApplied(storedHasApplied = null, enabled = false))
    }

    @Test
    fun explicitAppliedState_survivesTurnOff() {
        assertTrue(resolveBatteryHasApplied(storedHasApplied = true, enabled = false))
    }
}
