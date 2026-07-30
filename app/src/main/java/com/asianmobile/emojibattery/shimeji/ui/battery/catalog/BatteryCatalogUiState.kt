package com.asianmobile.emojibattery.shimeji.ui.battery.catalog

import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogCategory
import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogError
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry

data class BatteryCatalogUiState(
    val themes: List<BatteryThemeEntry> = emptyList(),
    val visibleThemes: List<BatteryThemeEntry> = emptyList(),
    val categories: List<BatteryCatalogCategory> = emptyList(),
    val selectedCategoryId: Int? = null,
    val searchQuery: String = "",
    val favoriteThemeIds: Set<Int> = emptySet(),
    val isPremium: Boolean = false,
    val isLoading: Boolean = true,
    val error: BatteryCatalogError? = null
)
