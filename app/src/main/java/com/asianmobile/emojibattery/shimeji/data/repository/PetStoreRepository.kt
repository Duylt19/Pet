package com.asianmobile.emojibattery.shimeji.data.repository

import kotlinx.coroutines.flow.StateFlow

/** Persistent Pet Store-only metadata. Installed packs remain the ownership source of truth. */
interface PetStoreRepository {
    val customNames: StateFlow<Map<Int, String>>

    suspend fun saveCustomName(petId: Int, name: String)
}
