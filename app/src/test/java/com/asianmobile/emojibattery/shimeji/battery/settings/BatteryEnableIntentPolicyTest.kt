package com.asianmobile.emojibattery.shimeji.battery.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryEnableIntentPolicyTest {
    @Test
    fun requestEnable_withoutPermission_storesEnabledMarkedPending() {
        val intent = BatteryEnableIntentPolicy.requestEnable(
            stored = BatteryEnableIntent(enabled = false),
            isAccessibilityGranted = false
        )

        assertEquals(BatteryEnableIntent(enabled = true, pendingAccessibilityGrant = true), intent)
    }

    @Test
    fun requestEnable_withPermission_storesEnabledWithNothingToTakeBack() {
        val intent = BatteryEnableIntentPolicy.requestEnable(
            stored = BatteryEnableIntent(enabled = false),
            isAccessibilityGranted = true
        )

        assertEquals(BatteryEnableIntent(enabled = true, pendingAccessibilityGrant = false), intent)
    }

    /** A bar the user already turned on, whose permission a force-stop stripped. */
    @Test
    fun requestEnable_whenAlreadyEnabled_doesNotMarkTheOlderIntentPending() {
        val intent = BatteryEnableIntentPolicy.requestEnable(
            stored = BatteryEnableIntent(enabled = true),
            isAccessibilityGranted = false
        )

        assertEquals(BatteryEnableIntent(enabled = true, pendingAccessibilityGrant = false), intent)
    }

    /** The toggle and the disclosure's Allow both commit; the second must not clear the first. */
    @Test
    fun requestEnable_repeatedBeforeGrant_staysPending() {
        val first = BatteryEnableIntentPolicy.requestEnable(
            stored = BatteryEnableIntent(enabled = false),
            isAccessibilityGranted = false
        )

        val second = BatteryEnableIntentPolicy.requestEnable(
            stored = first,
            isAccessibilityGranted = false
        )

        assertEquals(first, second)
    }

    @Test
    fun settle_whenGranted_keepsTheBarOnAndStopsWatching() {
        val settled = BatteryEnableIntentPolicy.settle(
            stored = BatteryEnableIntent(enabled = true, pendingAccessibilityGrant = true),
            isAccessibilityGranted = true
        )

        assertEquals(BatteryEnableIntent(enabled = true, pendingAccessibilityGrant = false), settled)
    }

    @Test
    fun settle_whenStillMissing_revertsTheOptimisticEnable() {
        val settled = BatteryEnableIntentPolicy.settle(
            stored = BatteryEnableIntent(enabled = true, pendingAccessibilityGrant = true),
            isAccessibilityGranted = false
        )

        assertEquals(
            BatteryEnableIntent(enabled = false, pendingAccessibilityGrant = false),
            settled
        )
    }

    /** Nothing optimistic was stored, so a missing permission is a revocation, not this policy's. */
    @Test
    fun settle_withoutAPendingRequest_changesNothing() {
        assertNull(
            BatteryEnableIntentPolicy.settle(
                stored = BatteryEnableIntent(enabled = true, pendingAccessibilityGrant = false),
                isAccessibilityGranted = false
            )
        )
    }

    @Test
    fun settle_afterTheUserTurnedTheBarOff_neverReEnablesIt() {
        val off = BatteryEnableIntentPolicy.setEnabled(enabled = false)

        assertEquals(BatteryEnableIntent(enabled = false, pendingAccessibilityGrant = false), off)
        assertNull(BatteryEnableIntentPolicy.settle(stored = off, isAccessibilityGranted = true))
        assertNull(BatteryEnableIntentPolicy.settle(stored = off, isAccessibilityGranted = false))
    }

    /** Settling twice — a resume and the navigation result both ask — must be a no-op. */
    @Test
    fun settle_isIdempotent() {
        val stored = BatteryEnableIntent(enabled = true, pendingAccessibilityGrant = true)

        val once = requireNotNull(
            BatteryEnableIntentPolicy.settle(stored, isAccessibilityGranted = false)
        )

        assertNull(BatteryEnableIntentPolicy.settle(once, isAccessibilityGranted = false))
        assertNull(BatteryEnableIntentPolicy.settle(once, isAccessibilityGranted = true))
    }

    /**
     * The whole point of storing the intent in DataStore: a process killed in system settings comes
     * back with the request intact, so the grant is still honoured and the decline still reverts.
     */
    @Test
    fun pendingRequest_survivesAsDataAndSettlesEitherWay() {
        val requested = BatteryEnableIntentPolicy.requestEnable(
            stored = BatteryEnableIntent(enabled = false),
            isAccessibilityGranted = false
        )
        val restoredAfterProcessDeath = requested.copy()

        assertEquals(
            BatteryEnableIntent(enabled = true, pendingAccessibilityGrant = false),
            BatteryEnableIntentPolicy.settle(
                restoredAfterProcessDeath,
                isAccessibilityGranted = true
            )
        )
        assertEquals(
            BatteryEnableIntent(enabled = false, pendingAccessibilityGrant = false),
            BatteryEnableIntentPolicy.settle(
                restoredAfterProcessDeath,
                isAccessibilityGranted = false
            )
        )
    }
}
