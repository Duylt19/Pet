package com.asianmobile.privatebrower.ui.catalog

import com.asianmobile.privatebrower.pet.pack.PetPack

data class PetCatalogUiState(
    val packs: List<PetPack> = emptyList(),
    val selectedKey: String = "",
    val isInstalling: Boolean = false,
    val message: PetCatalogMessage? = null
)

sealed interface PetCatalogMessage {
    data class Installed(val name: String) : PetCatalogMessage
    data class Selected(val name: String) : PetCatalogMessage
    data class Rejected(val reason: String) : PetCatalogMessage
    data class Failed(val reason: String) : PetCatalogMessage
}
