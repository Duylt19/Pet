package com.asianmobile.emojibattery.shimeji.ui.search

enum class SearchTab {
    PETS,
    BATTERY
}

data class SearchUiState(
    val query: String = "",
    val selectedTab: SearchTab = SearchTab.PETS,
    val pets: List<SearchPetUiState> = emptyList(),
    val recommendedThemes: List<SearchThemeUiState> = emptyList(),
    val isLoading: Boolean = true,
    val hasError: Boolean = false
) {
    /** True when the active tab has nothing to show for the current query. */
    val isEmpty: Boolean
        get() = when (selectedTab) {
            SearchTab.PETS -> pets.isEmpty()
            SearchTab.BATTERY -> recommendedThemes.isEmpty()
        }
}

data class SearchPetUiState(
    val id: Int,
    val packKey: String,
    val name: String,
    val breed: String,
    val thumbnailPath: String?,
    /** Not unlocked yet, which is what Pet Store marks with the crown. */
    val isLocked: Boolean
)

data class SearchThemeUiState(
    val id: Int,
    val name: String,
    val category: String,
    val thumbnailPath: String?,
    val isFavorite: Boolean
)

internal fun filterSearchPets(
    pets: List<SearchPetUiState>,
    query: String
): List<SearchPetUiState> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return pets
    return pets.filter { pet ->
        pet.name.contains(normalizedQuery, ignoreCase = true) ||
            pet.breed.contains(normalizedQuery, ignoreCase = true)
    }
}
