package com.asianmobile.emojibattery.shimeji.data.remote

/** My Pet Room backgrounds are served from the same private repository as pets and batteries. */
object RoomServerConfig {
    const val CATALOG_URL = "${PetServerConfig.BASE_URL}/json/rooms.json"

    fun resolve(relativePath: String): String =
        PetServerConfig.resolve("room/${relativePath.trimStart('/')}")
}
