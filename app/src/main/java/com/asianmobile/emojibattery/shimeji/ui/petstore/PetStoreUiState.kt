package com.asianmobile.emojibattery.shimeji.ui.petstore

import androidx.annotation.DrawableRes
import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogEntry
import com.asianmobile.emojibattery.shimeji.pet.engine.PetAction
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPack

enum class PetStoreTab { PETS, FOOD }

data class PetStoreUiState(
    val pets: List<OwnerPetCatalogEntry> = emptyList(),
    val installedPackKeys: Set<String> = emptySet(),
    val customNames: Map<Int, String> = emptyMap(),
    val selectedTab: PetStoreTab = PetStoreTab.PETS,
    val isLoading: Boolean = true,
    val isPetRunning: Boolean = false,
    val overlayGranted: Boolean = false,
    val notificationGranted: Boolean = false,
    val selectedPet: OwnerPetCatalogEntry? = null,
    val downloadingPetId: Int? = null,
    val revealedPet: OwnerPetCatalogEntry? = null,
    val revealedPetPack: PetPack? = null,
    val namingPet: OwnerPetCatalogEntry? = null,
    val joinedPetName: String? = null,
    val selectedFood: PetStoreFood? = null,
    val revealedFood: PetStoreFood? = null,
    val acquiredFood: PetStoreFood? = null,
    val message: String? = null
)

data class PetStoreFood(
    val id: String,
    val name: String,
    val coinCost: Int,
    @param:DrawableRes val imageRes: Int
)

sealed interface PetStoreEffect {
    data object ShowRewardedAd : PetStoreEffect
    data object OpenPremium : PetStoreEffect
    data object OpenOverlaySettings : PetStoreEffect
    data object RequestNotificationPermission : PetStoreEffect
}

internal object PetStorePolicy {
    fun isUnlocked(pet: OwnerPetCatalogEntry, installedPackKeys: Set<String>): Boolean =
        pet.installedPackKey in installedPackKeys

    fun normalizedName(input: String, fallback: String): String =
        input.trim().ifBlank { fallback.trim() }.take(24)

    fun specialSkillAction(availableActions: Set<PetAction>): PetAction? = when {
        PetAction.SPECIAL in availableActions -> PetAction.SPECIAL
        PetAction.SPECIAL_2 in availableActions -> PetAction.SPECIAL_2
        else -> null
    }
}
