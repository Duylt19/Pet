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
        selectedTab
    ) { catalog, config, tab ->
        FavouriteRecentUiState(
            selectedTab = tab,
            favouriteThemes = favouriteThemeUiStates(
                themes = catalog.themes,
                favoriteThemeIds = config.favoriteThemeIds
            ),
            // TODO(FavouriteRecent): add ordered MRU persistence when product defines
            // which battery actions count as a recent item and the retention limit.
            recentThemes = emptyList(),
            isLoading = catalog.isLoading
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

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
