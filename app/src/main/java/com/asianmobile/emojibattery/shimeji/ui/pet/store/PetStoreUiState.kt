package com.asianmobile.emojibattery.shimeji.ui.pet.store

import androidx.annotation.DrawableRes
import com.asianmobile.emojibattery.shimeji.R
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
