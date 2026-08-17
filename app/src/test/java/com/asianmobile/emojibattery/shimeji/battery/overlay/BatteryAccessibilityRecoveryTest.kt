package com.asianmobile.emojibattery.shimeji.battery.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryAccessibilityRecoveryTest {
    private fun recovery(
        isBatteryConfigured: Boolean = true,
        isAccessibilityEnabled: Boolean = false,
        wasProcessEndedByUser: Boolean? = null
    ): BatteryAccessibilityRecovery = batteryAccessibilityRecovery(
        isBatteryConfigured = isBatteryConfigured,
        isAccessibilityEnabled = isAccessibilityEnabled,
        wasProcessEndedByUser = wasProcessEndedByUser
    )

    @Test
    fun `a working bar is never offered a recovery`() {
        assertEquals(
            BatteryAccessibilityRecovery.NONE,
            recovery(isAccessibilityEnabled = true)
        )
    }

    @Test
    fun `turning the bar off yourself is not a revocation`() {
        assertEquals(
            BatteryAccessibilityRecovery.NONE,
            recovery(isBatteryConfigured = false)
        )
    }

    @Test
    fun `a bar the user never turned on stays quiet even after a kill`() {
        assertEquals(
            BatteryAccessibilityRecovery.NONE,
            recovery(isBatteryConfigured = false, wasProcessEndedByUser = true)
        )
    }

    @Test
    fun `closing the app from recents is named as the cause`() {
        assertEquals(
            BatteryAccessibilityRecovery.APP_CLOSED,
            recovery(wasProcessEndedByUser = true)
        )
    }

    @Test
    fun `a kill the user did not ask for points at the device`() {
        assertEquals(
            BatteryAccessibilityRecovery.DEVICE_KILLED,
            recovery(wasProcessEndedByUser = false)
        )
    }

    @Test
    fun `an unrecorded cause still offers the fix without blaming anything`() {
        assertEquals(
            BatteryAccessibilityRecovery.UNKNOWN_CAUSE,
            recovery(wasProcessEndedByUser = null)
        )
    }
}
