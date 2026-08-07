package com.asianmobile.emojibattery.shimeji.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.data.repository.BatteryCatalogRepository
import com.asianmobile.emojibattery.shimeji.data.repository.BatterySettingsRepository
import com.asianmobile.emojibattery.shimeji.data.repository.OwnerPetCatalogRepository
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPack
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val catalogRepository: BatteryCatalogRepository,
    private val settingsRepository: BatterySettingsRepository,
    private val petCatalogRepository: OwnerPetCatalogRepository,
    private val petPackRepository: PetPackRepository
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val selectedTab = MutableStateFlow(SearchTab.PETS)

    val uiState: StateFlow<SearchUiState> = combine(
        catalogRepository.snapshot,
        settingsRepository.config,
        query,
        selectedTab,
        combine(petCatalogRepository.snapshot, petPackRepository.packs) { pets, packs ->
            pets to packs.mapTo(mutableSetOf(), PetPack::key)
        }
    ) { catalog, config, currentQuery, tab, (petCatalog, installedKeys) ->
        val themes = catalog.themes
            .asSequence()
            .filter { it.assetsReady }
            .map { theme ->
                SearchThemeUiState(
                    id = theme.id,
                    name = theme.name,
                    category = theme.categoryName,
                    thumbnailPath = theme.thumbnailPath,
                    isFavorite = theme.id in config.favoriteThemeIds
                )
            }
            .toList()
        val pets = petCatalog.entries.map { pet ->
            SearchPetUiState(
                id = pet.id,
                packKey = pet.installedPackKey,
                name = pet.name,
                breed = pet.category,
                thumbnailPath = pet.thumbnailPath,
                isLocked = pet.installedPackKey !in installedKeys
            )
        }
        SearchUiState(
            query = currentQuery,
            selectedTab = tab,
            pets = filterSearchPets(pets, currentQuery).take(MAX_RESULTS),
            recommendedThemes = filterSearchThemes(themes, currentQuery).take(MAX_RESULTS),
            isLoading = when (tab) {
                SearchTab.PETS -> petCatalog.isLoading
                SearchTab.BATTERY -> catalog.isLoading
            },
            hasError = catalog.error != null && themes.isEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SearchUiState()
    )

    init {
        viewModelScope.launch { catalogRepository.refresh() }
    }

    fun updateQuery(value: String) {
        query.value = value
    }

    fun selectTab(tab: SearchTab) {
        selectedTab.value = tab
    }

    fun toggleFavorite(themeId: Int) {
        settingsRepository.toggleFavorite(themeId)
    }

    private companion object {
        const val MAX_RESULTS = 30
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

internal fun filterSearchThemes(
    themes: List<SearchThemeUiState>,
    query: String
): List<SearchThemeUiState> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return themes
    return themes.filter { theme ->
        theme.name.contains(normalizedQuery, ignoreCase = true) ||
            theme.category.contains(normalizedQuery, ignoreCase = true)
    }
}
