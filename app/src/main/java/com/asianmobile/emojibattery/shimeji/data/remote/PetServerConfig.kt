package com.asianmobile.emojibattery.shimeji.data.remote

object PetServerConfig {
    const val REMOTE_CONFIG_TOKEN_KEY = "github_token_pet_server"
    const val RAW_HOST = "raw.githubusercontent.com"
    const val RAW_REPOSITORY_PATH =
        "/Asian-Mobile-Inc/Server-Emoji-Battery-Shimeji-Pet-AM/master/"
    const val BASE_URL =
        "https://$RAW_HOST/Asian-Mobile-Inc/Server-Emoji-Battery-Shimeji-Pet-AM/master"
    const val CATALOG_URL = "$BASE_URL/json/pets.json"

    fun resolve(relativePath: String): String = "$BASE_URL/$relativePath"

    fun isPetServerUrl(host: String, encodedPath: String): Boolean =
        host == RAW_HOST && encodedPath.startsWith(RAW_REPOSITORY_PATH)
}
