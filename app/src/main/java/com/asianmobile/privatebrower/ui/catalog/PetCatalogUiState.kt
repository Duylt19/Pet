package com.asianmobile.privatebrower.ui.catalog

import com.asianmobile.privatebrower.data.model.OwnerPetCatalogEntry
import com.asianmobile.privatebrower.data.model.OwnerPetCatalogError
import com.asianmobile.privatebrower.pet.pack.PetPack

data class PetCatalogUiState(
    val packs: List<PetPack> = emptyList(),
    val selectedKey: String = "",
    val targetSlotIndex: Int = 0,
    val pets: List<OwnerPetCatalogEntry> = emptyList(),
    val visiblePets: List<OwnerPetCatalogEntry> = emptyList(),
    val categories: List<PetCatalogCategory> = emptyList(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val preparingPetId: Int? = null,
    val localRootPath: String = "",
    val isLoading: Boolean = true,
    val catalogError: OwnerPetCatalogError? = null,
    val isInstalling: Boolean = false,
    val message: PetCatalogMessage? = null
)

data class PetCatalogCategory(
    val name: String?,
    val count: Int
)

sealed interface PetCatalogMessage {
    data class Installed(val name: String) : PetCatalogMessage
    data class Selected(val name: String) : PetCatalogMessage
    data class Rejected(val reason: String) : PetCatalogMessage
    data class Failed(val reason: String) : PetCatalogMessage
}
