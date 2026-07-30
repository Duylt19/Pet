package com.asianmobile.emojibattery.shimeji.ui.battery.catalog

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.ads.data.SharedPreferencesUtils
import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogSnapshot
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntitlement
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry
import com.asianmobile.emojibattery.shimeji.data.repository.BatteryCatalogRepository
import com.asianmobile.emojibattery.shimeji.data.repository.BatterySettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class BatteryCatalogViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val catalogRepository: BatteryCatalogRepository,
    private val settingsRepository: BatterySettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(BatteryCatalogUiState())
    val uiState: StateFlow<BatteryCatalogUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(catalogRepository.snapshot, settingsRepository.config) { catalog, config ->
                catalog to config
            }.collect { (catalog, config) ->
                _uiState.update { current -> reduce(current, catalog, config) }
            }
        }
    }

    fun selectCategory(categoryId: Int?) {
        _uiState.update { current ->
            current.copy(
                selectedCategoryId = categoryId,
                visibleThemes = filter(current.themes, categoryId, current.searchQuery)
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { current ->
            current.copy(
                searchQuery = query,
                visibleThemes = filter(current.themes, current.selectedCategoryId, query)
            )
        }
    }

    fun toggleFavorite(themeId: Int) = settingsRepository.toggleFavorite(themeId)

    fun refresh() {
        refreshEntitlement()
        viewModelScope.launch { catalogRepository.refresh() }
    }

    fun refreshEntitlement() {
        _uiState.update { it.copy(isPremium = SharedPreferencesUtils.getIsPremium(context)) }
    }

    fun canOpen(theme: BatteryThemeEntry): Boolean =
        theme.assetsReady &&
            (theme.entitlement == BatteryThemeEntitlement.FREE || _uiState.value.isPremium)

    private fun reduce(
        current: BatteryCatalogUiState,
        catalog: BatteryCatalogSnapshot,
        config: BatteryStatusConfig
    ): BatteryCatalogUiState = current.copy(
        themes = catalog.themes,
        visibleThemes = filter(
            catalog.themes,
            current.selectedCategoryId,
            current.searchQuery
        ),
        categories = catalog.categories,
        favoriteThemeIds = config.favoriteThemeIds,
        isPremium = SharedPreferencesUtils.getIsPremium(context),
        isLoading = catalog.isLoading,
        error = catalog.error
    )

    private fun filter(
        themes: List<BatteryThemeEntry>,
        categoryId: Int?,
        query: String
    ): List<BatteryThemeEntry> {
        val normalized = query.trim()
        return themes.filter { theme ->
            (categoryId == null || theme.categoryId == categoryId) &&
                (normalized.isEmpty() || theme.name.contains(normalized, ignoreCase = true))
        }
    }
}
