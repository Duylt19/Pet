package com.asianmobile.emojibattery.shimeji.pet.overlay

import org.junit.Assert.assertEquals
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
    fun `the vendors known to kill foreground services are all covered`() {
        val packages = PetVendorPowerSettings.CANDIDATES.mapTo(mutableSetOf()) { it.packageName }

        listOf(
            "com.miui.securitycenter",
            "com.huawei.systemmanager",
            "com.coloros.safecenter",
            "com.vivo.permissionmanager",
            "com.oneplus.security",
            "com.samsung.android.lool",
            "com.meizu.safe"
        ).forEach { assertTrue(it, it in packages) }
    }
}
