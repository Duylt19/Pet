package com.asianmobile.emojibattery.shimeji.pet.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PetBatteryOptimizationPolicyTest {
    @Test
    fun `a grant alone does not make a stock device relevant`() {
        val signals = PetBackgroundRestrictionSignals(isAlreadyIgnoringOptimization = true)

        assertFalse(PetBatteryOptimizationPolicy.isExemptionRelevant(signals))
        assertNull(PetBatteryOptimizationPolicy.reasonFor(signals))
    }

    @Test
    fun `a stock device with nothing wrong is never asked`() {
        val signals = PetBackgroundRestrictionSignals()

        assertFalse(PetBatteryOptimizationPolicy.isExemptionRelevant(signals))
        assertNull(PetBatteryOptimizationPolicy.reasonFor(signals))
    }

    @Test
    fun `android 9 samsung without a measured restriction is never asked`() {
        // API 28 only contributes ActivityManager.isBackgroundRestricted. Manufacturer, model
        // and API level are intentionally absent from the policy, so an unrestricted SM-J730G
        // has exactly the same no-prompt state as stock Android.
        val signalsFromUnrestrictedApi28Device = PetBackgroundRestrictionSignals()

        assertFalse(
            PetBatteryOptimizationPolicy.isExemptionRelevant(
                signalsFromUnrestrictedApi28Device
            )
        )
        assertNull(PetBatteryOptimizationPolicy.reasonFor(signalsFromUnrestrictedApi28Device))
    }

    @Test
    fun `being background restricted is enough on any supported device`() {
        val signals = PetBackgroundRestrictionSignals(isBackgroundRestricted = true)

        assertTrue(PetBatteryOptimizationPolicy.isExemptionRelevant(signals))
        assertEquals(
            PetExemptionReason.BACKGROUND_RESTRICTED,
            PetBatteryOptimizationPolicy.reasonFor(signals)
        )
    }

    @Test
    fun `the restricted standby bucket is enough on api 30 or newer`() {
        val signals = PetBackgroundRestrictionSignals(isInRestrictedStandbyBucket = true)

        assertTrue(PetBatteryOptimizationPolicy.isExemptionRelevant(signals))
        assertEquals(
            PetExemptionReason.RESTRICTED_BUCKET,
            PetBatteryOptimizationPolicy.reasonFor(signals)
        )
    }

    @Test
    fun `a device that killed the overlay is asked`() {
        val signals = PetBackgroundRestrictionSignals(
            lastOverlayKill = PetProcessKillKind.SIGNALLED
        )

        assertTrue(PetBatteryOptimizationPolicy.isExemptionRelevant(signals))
        assertEquals(
            PetExemptionReason.PREVIOUSLY_KILLED,
            PetBatteryOptimizationPolicy.reasonFor(signals)
        )
    }

    @Test
    fun `the user closing the app is not treated as a symptom`() {
        assertFalse(PetBatteryOptimizationPolicy.isUnexpectedKill(PetProcessKillKind.USER))
        assertFalse(
            PetBatteryOptimizationPolicy.isExemptionRelevant(
                PetBackgroundRestrictionSignals(lastOverlayKill = PetProcessKillKind.USER)
            )
        )
    }

    @Test
    fun `a crash is not something a battery exemption would have prevented`() {
        assertFalse(PetBatteryOptimizationPolicy.isUnexpectedKill(PetProcessKillKind.OTHER))
        assertFalse(PetBatteryOptimizationPolicy.isUnexpectedKill(null))
    }

    @Test
    fun `a system reclaim counts as an unexpected kill`() {
        assertTrue(PetBatteryOptimizationPolicy.isUnexpectedKill(PetProcessKillKind.SYSTEM_RECLAIM))
    }

    @Test
    fun `a vendor auto start screen does not imply android battery exemption`() {
        val signals = PetBackgroundRestrictionSignals(hasVendorPowerScreen = true)

        assertFalse(PetBatteryOptimizationPolicy.isExemptionRelevant(signals))
        assertNull(PetBatteryOptimizationPolicy.reasonFor(signals))
        assertTrue(signals.shouldOfferVendorAllowlist())
    }

    @Test
    fun `a grant hides a relevant battery request without hiding vendor auto start`() {
        val signals = PetBackgroundRestrictionSignals(
            isAlreadyIgnoringOptimization = true,
            isBackgroundRestricted = true,
            hasVendorPowerScreen = true
        )

        assertTrue(PetBatteryOptimizationPolicy.isExemptionRelevant(signals))
        assertNull(PetBatteryOptimizationPolicy.reasonFor(signals))
        assertTrue(signals.shouldOfferVendorAllowlist())
    }

    @Test
    fun `a ROM without its own allowlist is never sent to one`() {
        assertFalse(PetBackgroundRestrictionSignals().shouldOfferVendorAllowlist())
    }
}
