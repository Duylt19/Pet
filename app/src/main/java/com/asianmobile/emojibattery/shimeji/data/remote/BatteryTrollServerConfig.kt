package com.asianmobile.emojibattery.shimeji.data.remote

/** Battery Troll art is served from the same private repository as pets, batteries and rooms. */
object BatteryTrollServerConfig {
    const val CATALOG_URL = "${PetServerConfig.BASE_URL}/json/battery-troll.json"

    fun resolve(relativePath: String): String =
        PetServerConfig.resolve("troll/${relativePath.trimStart('/')}")
}
