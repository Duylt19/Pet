package com.asianmobile.emojibattery.shimeji.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryServerConfigTest {
    @Test
    fun catalogAndAssets_useThePrivatePetServerContract() {
        assertEquals(
            "${PetServerConfig.BASE_URL}/json/batteries.json",
            BatteryServerConfig.CATALOG_URL
        )
        assertEquals(
            "${PetServerConfig.BASE_URL}/battery/emoji/50.png",
            BatteryServerConfig.resolveAsset("emoji/50.png")
        )
    }
}
