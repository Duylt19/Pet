package com.asianmobile.emojibattery.shimeji.navigation

import com.asianmobile.emojibattery.shimeji.ui.home.shell.HomeTab
import com.asianmobile.emojibattery.shimeji.ui.pet.store.PET_STORE_TRENDING_CATEGORY
import com.asianmobile.emojibattery.shimeji.ui.pet.store.PetStoreTab

internal data class PetStoreHomeRequest(
    val tabValue: String,
    val category: String,
)

internal fun trendingPetsHomeRequest(): PetStoreHomeRequest = PetStoreHomeRequest(
    tabValue = PetStoreTab.PETS.navigationValue,
    category = PET_STORE_TRENDING_CATEGORY,
)

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
