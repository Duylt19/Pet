package com.asianmobile.privatebrower.data.repository

import com.asianmobile.privatebrower.data.model.OwnerPetCatalogSnapshot
import com.asianmobile.privatebrower.pet.pack.PetPackInstallResult
import kotlinx.coroutines.flow.StateFlow

interface OwnerPetCatalogRepository {
    val snapshot: StateFlow<OwnerPetCatalogSnapshot>

    suspend fun refresh()

    suspend fun preparePack(petId: Int): PetPackInstallResult
}
