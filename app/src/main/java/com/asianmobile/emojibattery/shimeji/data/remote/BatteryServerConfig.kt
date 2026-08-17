package com.asianmobile.emojibattery.shimeji.data.remote

object BatteryServerConfig {
    const val CATALOG_URL = "${PetServerConfig.BASE_URL}/json/batteries.json"
    private const val ASSET_ROOT = "${PetServerConfig.BASE_URL}/battery"

    fun resolveAsset(relativePath: String): String = "$ASSET_ROOT/$relativePath"
}
