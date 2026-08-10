package com.asianmobile.emojibattery.shimeji.pet.overlay

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetBatteryOptimizationPolicyTest {
    @Test
    fun `vendors that kill foreground services are offered the exemption`() {
        listOf("Xiaomi", "Redmi", "POCO", "HUAWEI", "HONOR", "OPPO", "realme", "OnePlus",
            "vivo", "iQOO", "samsung", "Meizu", "TECNO", "Infinix", "itel", "asus")
            .forEach { vendor ->
                assertTrue(
                    "$vendor should be offered the exemption",
                    PetBatteryOptimizationPolicy.shouldOfferExemption(vendor, false)
                )
            }
    }

    @Test
    fun `stock Android is never asked, because the exemption would change nothing`() {
        listOf("Google", "motorola", "Nothing", "Fairphone", "Sony", "")
            .forEach { vendor ->
                assertFalse(
                    "$vendor should not be asked",
                    PetBatteryOptimizationPolicy.shouldOfferExemption(vendor, false)
                )
            }
    }

    @Test
    fun `a device that already has the exemption is not asked again`() {
        assertFalse(PetBatteryOptimizationPolicy.shouldOfferExemption("Xiaomi", true))
    }

    @Test
    fun `the vendor match ignores case and stray whitespace`() {
        assertTrue(PetBatteryOptimizationPolicy.isAggressiveVendor("  XIAOMI "))
        assertTrue(PetBatteryOptimizationPolicy.isAggressiveVendor("samsung"))
    }

    @Test
    fun `an unknown vendor is treated as stock rather than guessed at`() {
        assertFalse(PetBatteryOptimizationPolicy.isAggressiveVendor("SomeNewBrand"))
    }
}
