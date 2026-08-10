package com.asianmobile.emojibattery.shimeji.pet.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PetBatteryOptimizationPolicyTest {
    @Test
    fun `granting the exemption keeps the row, so the user can still revoke it`() {
        val signals = PetBackgroundRestrictionSignals(isAlreadyIgnoringOptimization = true)

        assertTrue(PetBatteryOptimizationPolicy.isExemptionRelevant(signals))
        // Nothing left to ask for, so the row shows state rather than a reason.
        assertNull(PetBatteryOptimizationPolicy.reasonFor(signals))
    }

    @Test
    fun `a stock device with nothing wrong is never asked`() {
        val signals = PetBackgroundRestrictionSignals()

        assertFalse(PetBatteryOptimizationPolicy.isExemptionRelevant(signals))
        assertNull(PetBatteryOptimizationPolicy.reasonFor(signals))
    }

    @Test
    fun `being background restricted is enough on any device`() {
        val signals = PetBackgroundRestrictionSignals(isBackgroundRestricted = true)

        assertTrue(PetBatteryOptimizationPolicy.isExemptionRelevant(signals))
        assertEquals(
            PetExemptionReason.BACKGROUND_RESTRICTED,
            PetBatteryOptimizationPolicy.reasonFor(signals)
        )
    }

    @Test
    fun `the restricted standby bucket is enough on any device`() {
        val signals = PetBackgroundRestrictionSignals(isInRestrictedStandbyBucket = true)

        assertTrue(PetBatteryOptimizationPolicy.isExemptionRelevant(signals))
        assertEquals(
            PetExemptionReason.RESTRICTED_BUCKET,
            PetBatteryOptimizationPolicy.reasonFor(signals)
        )
    }

    @Test
    fun `a device that killed the overlay is asked even when its brand looks harmless`() {
        val signals = PetBackgroundRestrictionSignals(
            lastOverlayKill = PetProcessKillKind.SIGNALLED,
            isAggressiveVendor = false
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
    fun `evidence outranks the vendor list when both apply`() {
        val signals = PetBackgroundRestrictionSignals(
            lastOverlayKill = PetProcessKillKind.SIGNALLED,
            isAggressiveVendor = true
        )

        assertEquals(
            PetExemptionReason.PREVIOUSLY_KILLED,
            PetBatteryOptimizationPolicy.reasonFor(signals)
        )
    }

    @Test
    fun `the vendor list decides only when nothing measurable applies`() {
        val signals = PetBackgroundRestrictionSignals(isAggressiveVendor = true)

        assertTrue(PetBatteryOptimizationPolicy.isExemptionRelevant(signals))
        assertEquals(
            PetExemptionReason.AGGRESSIVE_VENDOR,
            PetBatteryOptimizationPolicy.reasonFor(signals)
        )
    }

    @Test
    fun `vendors that kill foreground services are on the list`() {
        listOf("Xiaomi", "Redmi", "POCO", "HUAWEI", "HONOR", "OPPO", "realme", "OnePlus",
            "vivo", "iQOO", "samsung", "Meizu", "TECNO", "Infinix", "itel", "asus")
            .forEach { vendor ->
                assertTrue(vendor, PetBatteryOptimizationPolicy.isAggressiveVendor(vendor))
            }
    }

    @Test
    fun `stock vendors and unknown brands are not guessed at`() {
        listOf("Google", "motorola", "Nothing", "Fairphone", "SomeNewBrand", "")
            .forEach { vendor ->
                assertFalse(vendor, PetBatteryOptimizationPolicy.isAggressiveVendor(vendor))
            }
    }

    @Test
    fun `the vendor allowlist is offered whenever the ROM has one`() {
        assertTrue(
            PetBackgroundRestrictionSignals(hasVendorPowerScreen = true)
                .shouldOfferVendorAllowlist()
        )
    }

    @Test
    fun `the vendor allowlist stays offered after the platform exemption is granted`() {
        // Granting one does not grant the other, so this must not disappear with the exemption.
        val signals = PetBackgroundRestrictionSignals(
            isAlreadyIgnoringOptimization = true,
            hasVendorPowerScreen = true
        )

        assertTrue(signals.shouldOfferVendorAllowlist())
    }

    @Test
    fun `a ROM without its own allowlist is never sent to one`() {
        assertFalse(PetBackgroundRestrictionSignals().shouldOfferVendorAllowlist())
    }

    @Test
    fun `the vendor match ignores case and stray whitespace`() {
        assertTrue(PetBatteryOptimizationPolicy.isAggressiveVendor("  XIAOMI "))
    }
}
