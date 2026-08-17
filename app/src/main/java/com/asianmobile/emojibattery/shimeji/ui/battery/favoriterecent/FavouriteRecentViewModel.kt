package com.asianmobile.emojibattery.shimeji.ui.battery.favoriterecent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.data.repository.BatteryCatalogRepository
import com.asianmobile.emojibattery.shimeji.data.repository.BatterySettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class FavouriteRecentViewModel @Inject constructor(
    private val catalogRepository: BatteryCatalogRepository,
    private val settingsRepository: BatterySettingsRepository
) : ViewModel() {
    private val selectedTab = MutableStateFlow(FavouriteRecentTab.FAVOURITE)

    val uiState: StateFlow<FavouriteRecentUiState> = combine(
        catalogRepository.snapshot,
        settingsRepository.config,
        settingsRepository.recentThemeIds,
        selectedTab
    ) { catalog, config, recentThemeIds, tab ->
        val favourites = favouriteThemeUiStates(
            themes = catalog.themes,
            favoriteThemeIds = config.favoriteThemeIds
        )
        val recent = recentThemeUiStates(
            themes = catalog.themes,
            recentThemeIds = recentThemeIds,
            favoriteThemeIds = config.favoriteThemeIds
        )
        val requestedIds = when (tab) {
            FavouriteRecentTab.FAVOURITE -> config.favoriteThemeIds
            FavouriteRecentTab.RECENT -> recentThemeIds.toSet()
        }
        val visibleThemes = when (tab) {
            FavouriteRecentTab.FAVOURITE -> favourites
            FavouriteRecentTab.RECENT -> recent
        }
        FavouriteRecentUiState(
            selectedTab = tab,
            favouriteThemes = favourites,
            recentThemes = recent,
            isLoading = catalog.isLoading,
            catalogLoadFailed = catalog.error != null &&
                requestedIds.isNotEmpty() && visibleThemes.isEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = FavouriteRecentUiState()
    )

    init {
        viewModelScope.launch { catalogRepository.refresh() }
    }

    fun selectTab(tab: FavouriteRecentTab) {
        selectedTab.value = tab
    }

    fun toggleFavorite(themeId: Int) {
        settingsRepository.toggleFavorite(themeId)
    }

    fun retry() {
        viewModelScope.launch { catalogRepository.refresh() }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
