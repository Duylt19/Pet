package com.asianmobile.emojibattery.shimeji.ui.pet.catalog

import com.asianmobile.emojibattery.shimeji.data.model.FREE_MIXED_PET_SLOTS
import com.asianmobile.emojibattery.shimeji.data.model.MAX_PET_SLOTS
import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogEntry
import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogError
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPack

data class PetCatalogUiState(
    val packs: List<PetPack> = emptyList(),
    val selectedKey: String = "",
    val target: PetCatalogTarget = PetCatalogTarget.MIXED,
    val targetSlotIndex: Int = 0,
    val requiresMixedSlotReward: Boolean = false,
    val pets: List<OwnerPetCatalogEntry> = emptyList(),
    val visiblePets: List<OwnerPetCatalogEntry> = emptyList(),
    val categories: List<PetCatalogCategory> = emptyList(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val preparingPetId: Int? = null,
    val localRootPath: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val catalogError: OwnerPetCatalogError? = null,
    val isInstalling: Boolean = false,
    val message: PetCatalogMessage? = null
)

enum class PetCatalogTarget {
    MIXED,
    SWARM
}

data class PetCatalogCategory(
    val name: String?,
    val count: Int
)

sealed interface PetCatalogMessage {
    data class Installed(val name: String) : PetCatalogMessage
    data class Selected(val name: String) : PetCatalogMessage
    data class Rejected(val reason: String) : PetCatalogMessage
    data class Failed(val reason: String) : PetCatalogMessage
    data object RewardNotEarned : PetCatalogMessage
    data object PreviousSlotRequired : PetCatalogMessage
}

sealed interface PetCatalogEffect {
    data object ShowMixedSlotRewardedAd : PetCatalogEffect
}

object MixedSlotUnlockPolicy {
    fun requiresReward(
        target: PetCatalogTarget,
        slotIndex: Int,
        petCount: Int,
        rewardUnlockedSlotCount: Int,
        isPremium: Boolean
    ): Boolean =
        target == PetCatalogTarget.MIXED &&
            slotIndex >= petCount &&
            slotIndex >= FREE_MIXED_PET_SLOTS &&
            slotIndex >= rewardUnlockedSlotCount &&
            !isPremium

    fun canUnlockWithReward(
        slotIndex: Int,
        petCount: Int,
        rewardUnlockedSlotCount: Int
    ): Boolean =
        slotIndex in FREE_MIXED_PET_SLOTS until MAX_PET_SLOTS &&
            slotIndex == petCount &&
            slotIndex == rewardUnlockedSlotCount
}
