package com.asianmobile.emojibattery.shimeji.ui.home.discover

data class DiscoverUiState(
    val isLoading: Boolean = true,
    val isBatteryEnabled: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val trendingPets: List<DiscoverPetUiState> = emptyList(),
    val batteryThemes: List<DiscoverThemeUiState> = emptyList(),
    val statusBarThemes: List<DiscoverAssetUiState> = emptyList(),
    val emojiThemes: List<DiscoverAssetUiState> = emptyList(),
    val batteryIcons: List<DiscoverAssetUiState> = emptyList()
)

data class DiscoverPetUiState(
    val packKey: String,
    val name: String,
    val category: String,
    val thumbnailPath: String?
)

data class DiscoverThemeUiState(
    val id: Int,
    val name: String,
    val thumbnailPath: String?,
    val isFavorite: Boolean
)

data class DiscoverAssetUiState(
    val id: Int,
    val name: String,
    val assetPath: String
)

sealed interface DiscoverEffect {
    data object RequestBatteryAccessibility : DiscoverEffect
}
