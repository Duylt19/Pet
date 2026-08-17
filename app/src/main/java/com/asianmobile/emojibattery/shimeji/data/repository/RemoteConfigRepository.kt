package com.asianmobile.emojibattery.shimeji.data.repository

interface RemoteConfigRepository {
    fun hasPetServerToken(): Boolean

    suspend fun refresh(): Boolean
}
