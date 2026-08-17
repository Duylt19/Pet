package com.asianmobile.emojibattery.shimeji.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryTrollServerConfigTest {
    @Test
    fun catalogUrl_usesThePrivatePetServerContract() {
        assertEquals(
            "${PetServerConfig.BASE_URL}/json/battery-troll.json",
            BatteryTrollServerConfig.CATALOG_URL
        )
    }

    @Test
    fun resolve_mapsCatalogPathsUnderTheTrollAssetRoot() {
        assertEquals(
            "${PetServerConfig.BASE_URL}/troll/thumb/TROLL_1.webp",
            BatteryTrollServerConfig.resolve("thumb/TROLL_1.webp")
        )
        assertEquals(
            "${PetServerConfig.BASE_URL}/troll/battery/TROLL_1_5.webp",
            BatteryTrollServerConfig.resolve("battery/TROLL_1_5.webp")
        )
    }

    @Test
    fun resolve_doesNotDoubleTheSeparatorForRootedPaths() {
        assertEquals(
            "${PetServerConfig.BASE_URL}/troll/thumb/TROLL_1.webp",
            BatteryTrollServerConfig.resolve("/thumb/TROLL_1.webp")
        )
    }

    @Test
    fun resolvedAssets_stayInsideThePetServerRepository() {
        val url = BatteryTrollServerConfig.resolve("emoji/TROLL_1_1.webp")

        assertEquals(
            true,
            PetServerConfig.isPetServerUrl(
                host = PetServerConfig.RAW_HOST,
                encodedPath = url.removePrefix("https://${PetServerConfig.RAW_HOST}")
            )
        )
    }
}
