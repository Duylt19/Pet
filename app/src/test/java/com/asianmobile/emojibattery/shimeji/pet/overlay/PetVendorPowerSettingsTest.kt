package com.asianmobile.emojibattery.shimeji.pet.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetVendorPowerSettingsTest {
    @Test
    fun `every candidate names a package and a class`() {
        PetVendorPowerSettings.CANDIDATES.forEach { screen ->
            assertTrue(screen.packageName.isNotBlank())
            assertTrue(screen.className.isNotBlank())
            assertTrue(
                "${screen.className} should be a fully qualified class",
                screen.className.contains('.')
            )
        }
    }

    @Test
    fun `no candidate is listed twice`() {
        val candidates = PetVendorPowerSettings.CANDIDATES
        assertEquals(candidates.size, candidates.distinct().size)
    }

    @Test
    fun `only explicit vendor allowlist surfaces are covered`() {
        val packages = PetVendorPowerSettings.CANDIDATES.mapTo(mutableSetOf()) { it.packageName }

        listOf(
            "com.miui.securitycenter",
            "com.huawei.systemmanager",
            "com.coloros.safecenter",
            "com.vivo.permissionmanager",
            "com.oneplus.security",
            "com.meizu.safe"
        ).forEach { assertTrue(it, it in packages) }
    }

    @Test
    fun `generic battery and device manager pages are never labelled auto start`() {
        val candidates = PetVendorPowerSettings.CANDIDATES

        assertFalse(candidates.any { it.packageName == "com.samsung.android.lool" })
        assertFalse(candidates.any { it.className == "com.asus.mobilemanager.MainActivity" })
        assertFalse(candidates.any { it.packageName == "com.htc.pitroad" })
        assertFalse(candidates.any { it.packageName == "com.evenwell.powersaving.g3" })
    }
}
