package com.asianmobile.emojibattery.shimeji.navigation

import androidx.lifecycle.SavedStateHandle
import com.asianmobile.emojibattery.shimeji.ui.home.shell.HomeTab
import com.asianmobile.emojibattery.shimeji.ui.pet.store.PET_STORE_TRENDING_CATEGORY
import com.asianmobile.emojibattery.shimeji.ui.pet.store.PetStoreTab
import kotlinx.coroutines.flow.StateFlow

internal data class PetStoreHomeRequest(
    val tabValue: String,
    val category: String,
)

internal fun trendingPetsHomeRequest(): PetStoreHomeRequest = PetStoreHomeRequest(
    tabValue = PetStoreTab.PETS.navigationValue,
    category = PET_STORE_TRENDING_CATEGORY,
)

/**
 * Stable bridge between the root Home entry and the nested Pet Store destination.
 *
 * The nested navigation graph itself is retained by Navigation Compose, so passing current
 * request values into its builder captures the initial values. The destination must collect these
 * flows when it is composed to receive requests made later from Discover or My Pet Room.
 */
internal class PetStoreHomeRequestState(
    private val savedStateHandle: SavedStateHandle,
) {
    val requestedTab: StateFlow<String?> = savedStateHandle.getStateFlow(
        Routes.PET_STORE_TAB_REQUEST,
        null,
    )
    val requestedCategory: StateFlow<String?> = savedStateHandle.getStateFlow(
        Routes.PET_STORE_CATEGORY_REQUEST,
        null,
    )

    fun request(request: PetStoreHomeRequest) {
        savedStateHandle[Routes.PET_STORE_TAB_REQUEST] = request.tabValue
        savedStateHandle[Routes.PET_STORE_CATEGORY_REQUEST] = request.category
    }

    fun consumeTab() {
        savedStateHandle[Routes.PET_STORE_TAB_REQUEST] = null
    }

    fun consumeCategory() {
        savedStateHandle[Routes.PET_STORE_CATEGORY_REQUEST] = null
    }
}

internal fun homeTabForRoute(route: String?): HomeTab? = when (route) {
    Routes.DISCOVER -> HomeTab.DISCOVER
    Routes.BATTERY_CATALOG -> HomeTab.BATTERY
    Routes.PET_STORE -> HomeTab.PET_STORE
    Routes.SETTINGS -> HomeTab.MINE
    else -> null
}

/** All four bottom-navigation destinations are equivalent roots for system Back handling. */
internal fun isHomeTopLevelRoute(route: String?): Boolean = homeTabForRoute(route) != null

internal fun routeForHomeTab(tab: HomeTab): String = when (tab) {
    HomeTab.DISCOVER -> Routes.DISCOVER
    HomeTab.BATTERY -> Routes.BATTERY_CATALOG
    HomeTab.PET_STORE -> Routes.PET_STORE
    HomeTab.MINE -> Routes.SETTINGS
}

internal fun homeTabFromNavigationValue(value: String?): HomeTab? =
    HomeTab.entries.firstOrNull { it.name == value }
