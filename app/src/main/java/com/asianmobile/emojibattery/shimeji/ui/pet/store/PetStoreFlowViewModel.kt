package com.asianmobile.emojibattery.shimeji.ui.pet.store

import android.content.Context
import com.asianmobile.emojibattery.shimeji.data.repository.OwnerPetCatalogRepository
import com.asianmobile.emojibattery.shimeji.data.repository.PetFoodRepository
import com.asianmobile.emojibattery.shimeji.data.repository.PetSettingsRepository
import com.asianmobile.emojibattery.shimeji.data.repository.PetStoreRepository
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Destination-scoped owner for Pet unlock/download surfaces reused by Discover and Search. */
@HiltViewModel
class PetStoreFlowViewModel @Inject constructor(
    @ApplicationContext context: Context,
    ownerCatalogRepository: OwnerPetCatalogRepository,
    petPackRepository: PetPackRepository,
    petStoreRepository: PetStoreRepository,
    petFoodRepository: PetFoodRepository,
    petSettingsRepository: PetSettingsRepository,
    categorySessionOrder: PetCategorySessionOrder
) : PetStoreViewModel(
    context = context,
    ownerCatalogRepository = ownerCatalogRepository,
    petPackRepository = petPackRepository,
    petStoreRepository = petStoreRepository,
    petFoodRepository = petFoodRepository,
    petSettingsRepository = petSettingsRepository,
    categorySessionOrder = categorySessionOrder
)
