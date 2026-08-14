package com.asianmobile.emojibattery.shimeji.ui.pet.store

import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogEntry
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/** Keeps Pet Store categories fresh between app processes and stable within one process session. */
@Singleton
class PetCategorySessionOrder @Inject constructor() {
    private val sessionSeed = Random.nextLong()

    fun arrange(pets: List<OwnerPetCatalogEntry>): List<String> =
        PetStorePolicy.randomizedCategories(pets, sessionSeed)
}
