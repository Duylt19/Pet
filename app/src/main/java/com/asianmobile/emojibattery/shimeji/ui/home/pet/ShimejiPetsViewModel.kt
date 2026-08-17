package com.asianmobile.emojibattery.shimeji.ui.home.pet

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.data.repository.OwnerPetCatalogRepository
import com.asianmobile.emojibattery.shimeji.data.repository.PetFoodRepository
import com.asianmobile.emojibattery.shimeji.data.repository.PetSettingsRepository
import com.asianmobile.emojibattery.shimeji.data.repository.PetStoreRepository
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackRepository
import com.asianmobile.emojibattery.shimeji.ui.pet.store.PetCategorySessionOrder
import com.asianmobile.emojibattery.shimeji.ui.pet.store.PetStoreViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Owns state and actions for the Shimeji Pets top-level Home tab. */
@HiltViewModel
class ShimejiPetsViewModel @Inject constructor(
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
) {
    val uiState: StateFlow<ShimejiPetsUiState> = storeState
        .map(::ShimejiPetsUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ShimejiPetsUiState(storeState.value)
        )
}
