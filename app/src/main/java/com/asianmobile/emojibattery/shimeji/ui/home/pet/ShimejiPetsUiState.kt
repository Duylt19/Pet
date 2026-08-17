package com.asianmobile.emojibattery.shimeji.ui.home.pet

import com.asianmobile.emojibattery.shimeji.ui.pet.store.PetStoreUiState

/** Complete render state owned by the Shimeji Pets tab in the Home shell. */
data class ShimejiPetsUiState(
    val store: PetStoreUiState = PetStoreUiState()
)
