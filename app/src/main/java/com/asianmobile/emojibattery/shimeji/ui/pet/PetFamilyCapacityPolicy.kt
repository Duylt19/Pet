package com.asianmobile.emojibattery.shimeji.ui.pet

object PetFamilyCapacityPolicy {
    const val MAX_OWNED_PETS = 5

    fun isFull(ownedPetCount: Int): Boolean = ownedPetCount >= MAX_OWNED_PETS
}
