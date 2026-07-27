package com.asianmobile.emojibattery.shimeji.data.repository

import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogSnapshot
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackInstallResult
import kotlinx.coroutines.flow.StateFlow

interface OwnerPetCatalogRepository {
    val snapshot: StateFlow<OwnerPetCatalogSnapshot>

    suspend fun refresh()

    suspend fun preparePack(petId: Int): PetPackInstallResult
}
