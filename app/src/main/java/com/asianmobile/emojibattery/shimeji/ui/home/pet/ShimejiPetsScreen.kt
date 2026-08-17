package com.asianmobile.emojibattery.shimeji.ui.home.pet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asianmobile.emojibattery.shimeji.ui.pet.store.PetStoreContent
import com.asianmobile.emojibattery.shimeji.ui.pet.store.PetStoreFlowHost
import com.asianmobile.emojibattery.shimeji.ui.pet.store.PetStoreTab
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView

/** Top-level Shimeji Pets tab owned by the Home shell. */
@Composable
fun ShimejiPetsScreen(
    requestedTab: PetStoreTab? = null,
    onRequestedTabConsumed: () -> Unit = {},
    requestedCategory: String? = null,
    onRequestedCategoryConsumed: () -> Unit = {},
    onSearch: () -> Unit,
    onPremium: () -> Unit,
    onViewPet: () -> Unit,
    onNavigateToGrantPermissions: () -> Unit,
    viewModel: ShimejiPetsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = uiState.store
    TrackScreenView(ScreenName.PET_STORE)

    LaunchedEffect(requestedTab) {
        requestedTab?.let {
            viewModel.selectTab(it)
            onRequestedTabConsumed()
        }
    }

    LaunchedEffect(requestedCategory, state.categories) {
        val category = requestedCategory ?: return@LaunchedEffect
        val isAvailable = state.categories.any { it.equals(category, ignoreCase = true) }
        if (isAvailable) {
            viewModel.selectCategory(category)
            onRequestedCategoryConsumed()
        }
    }

    PetStoreFlowHost(
        state = state,
        viewModel = viewModel,
        onPremium = onPremium,
        onViewPet = onViewPet,
        onNavigateToGrantPermissions = onNavigateToGrantPermissions
    ) {
        PetStoreContent(
            state = state,
            onSearch = onSearch,
            onPremium = onPremium,
            onOpenMyPet = onViewPet,
            onToggle = viewModel::togglePetOverlay,
            onTab = viewModel::selectTab,
            onCategory = viewModel::selectCategory,
            onPet = viewModel::selectPet,
            onFood = viewModel::selectFood,
            onRetryCatalog = viewModel::retryCatalog
        )
    }
}
