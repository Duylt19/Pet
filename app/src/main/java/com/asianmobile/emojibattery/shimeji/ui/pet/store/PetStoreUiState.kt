package com.asianmobile.emojibattery.shimeji.ui.pet.store

import androidx.annotation.DrawableRes
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogEntry
import com.asianmobile.emojibattery.shimeji.pet.engine.PetAction
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPack

enum class PetStoreTab(val navigationValue: String) {
    PETS("pets"),
    FOOD("food");

    companion object {
        fun fromNavigationValue(value: String?): PetStoreTab? = entries.firstOrNull {
            it.navigationValue.equals(value, ignoreCase = true)
        }
    }
}

enum class PetStartBlocker {
    NO_OWNED_PETS,
    NO_ACTIVE_PETS
}

internal enum class PetUnlockActivation {
    REQUEST_OVERLAY,
    REQUEST_REMAINING_PERMISSIONS,
    START_PET
}

data class PetStoreUiState(
    val pets: List<OwnerPetCatalogEntry> = emptyList(),
    val installedPackKeys: Set<String> = emptySet(),
    val customNames: Map<Int, String> = emptyMap(),
    val selectedTab: PetStoreTab = PetStoreTab.PETS,
    val selectedCategory: String? = null,
    val isLoading: Boolean = true,
    val isPetOnScreenEnabled: Boolean = false,
    val isPetOnScreenStarting: Boolean = false,
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
    val petStartBlocker: PetStartBlocker? = null,
    val isPetCapacityDialogVisible: Boolean = false,
    val message: String? = null
)

data class PetStoreFood(
    val id: String,
    val name: String,
    /** Energy this portion restores when a pet eats it. */
    val energyValue: Int,
    @param:DrawableRes val imageRes: Int
)

/** The food the app sells. Pet Store grants portions; My Pet Room spends them. */
val PET_FOOD_CATALOG: List<PetStoreFood> = listOf(
    PetStoreFood("beef_stew", "Beef Stew", 25, R.drawable.img_pet_store_food_beef_stew),
    PetStoreFood("grilled_salmon", "Grilled Salmon", 30, R.drawable.img_pet_store_food_grilled_salmon),
    PetStoreFood("meatball_pasta", "Meatball Pasta", 25, R.drawable.img_pet_store_food_meatball_pasta),
    PetStoreFood("vegetable_rice", "Vegetable Rice", 20, R.drawable.img_pet_store_food_vegetable_rice),
    PetStoreFood("fruit_bowl", "Fruit Bowl", 15, R.drawable.img_pet_store_food_fruit_bowl),
    PetStoreFood("roast_chicken", "Roast Chicken", 30, R.drawable.img_pet_store_food_roast_chicken),
    PetStoreFood("fried_egg", "Fried Egg", 15, R.drawable.img_pet_store_food_fried_egg),
    PetStoreFood("steak", "Steak", 35, R.drawable.img_pet_store_food_steak),
    PetStoreFood("vegetables", "Vegetables", 10, R.drawable.img_pet_store_food_vegetables),
    PetStoreFood("pet_treats", "Pet Treats", 10, R.drawable.img_pet_store_food_pet_treats)
)

sealed interface PetStoreEffect {
    data object ShowRewardedAd : PetStoreEffect
    data object OpenPremium : PetStoreEffect
    data object OpenOverlaySettings : PetStoreEffect
    data object OpenGrantPermissions : PetStoreEffect
}

internal object PetStorePolicy {
    fun isUnlocked(pet: OwnerPetCatalogEntry, installedPackKeys: Set<String>): Boolean =
        pet.installedPackKey in installedPackKeys

    fun ownedPetCount(
        pets: List<OwnerPetCatalogEntry>,
        installedPackKeys: Set<String>
    ): Int = pets.count { isUnlocked(it, installedPackKeys) }

    fun startBlocker(ownedPetCount: Int, activePetCount: Int): PetStartBlocker? = when {
        ownedPetCount <= 0 -> PetStartBlocker.NO_OWNED_PETS
        activePetCount <= 0 -> PetStartBlocker.NO_ACTIVE_PETS
        else -> null
    }

    fun activationAfterUnlock(
        overlayGranted: Boolean,
        notificationGranted: Boolean
    ): PetUnlockActivation = when {
        !overlayGranted -> PetUnlockActivation.REQUEST_OVERLAY
        !notificationGranted -> PetUnlockActivation.REQUEST_REMAINING_PERMISSIONS
        else -> PetUnlockActivation.START_PET
    }

    fun normalizedName(input: String, fallback: String): String =
        input.trim().ifBlank { fallback.trim() }.take(24)

    fun categories(pets: List<OwnerPetCatalogEntry>): List<String> = pets
        .map { it.category.trim() }
        .filter(String::isNotEmpty)
        .distinctBy { it.lowercase() }

    fun selectedCategory(
        pets: List<OwnerPetCatalogEntry>,
        requestedCategory: String?
    ): String? {
        val categories = categories(pets)
        return categories.firstOrNull { it.equals(requestedCategory, ignoreCase = true) }
            ?: categories.firstOrNull()
    }

    fun petsInCategory(
        pets: List<OwnerPetCatalogEntry>,
        category: String?
    ): List<OwnerPetCatalogEntry> {
        val selected = selectedCategory(pets, category) ?: return emptyList()
        return pets.filter { it.category.trim().equals(selected, ignoreCase = true) }
    }

    fun specialSkillAction(availableActions: Set<PetAction>): PetAction? = when {
        PetAction.SPECIAL in availableActions -> PetAction.SPECIAL
        PetAction.SPECIAL_2 in availableActions -> PetAction.SPECIAL_2
        else -> null
    }
}
