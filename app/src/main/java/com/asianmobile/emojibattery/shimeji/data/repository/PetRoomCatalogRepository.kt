package com.asianmobile.emojibattery.shimeji.data.repository

import com.asianmobile.emojibattery.shimeji.data.model.PetRoomCatalogSnapshot
import kotlinx.coroutines.flow.StateFlow

interface PetRoomCatalogRepository {
    val snapshot: StateFlow<PetRoomCatalogSnapshot>

    suspend fun refresh()

    /** Downloads and verifies a catalog asset, returning its local path. */
    suspend fun materializeAsset(path: String?): String?
}
