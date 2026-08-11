package com.asianmobile.emojibattery.shimeji.ui.home.discover

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryAccessibility
import com.asianmobile.emojibattery.shimeji.data.repository.BatteryCatalogRepository
import com.asianmobile.emojibattery.shimeji.data.repository.BatterySettingsRepository
import com.asianmobile.emojibattery.shimeji.data.repository.OwnerPetCatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val petCatalogRepository: OwnerPetCatalogRepository,
    private val batteryCatalogRepository: BatteryCatalogRepository,
    private val batterySettingsRepository: BatterySettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        DiscoverUiState(isAccessibilityEnabled = BatteryAccessibility.isEnabled(context))
    )
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private val _effects = Channel<DiscoverEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()
    private var enableBatteryAfterAccessibility = false

    init {
        viewModelScope.launch {
            combine(
                petCatalogRepository.snapshot,
                batteryCatalogRepository.snapshot,
                batterySettingsRepository.config
            ) { pets, batteryCatalog, batteryConfig ->
                val accessibilityEnabled = BatteryAccessibility.isEnabled(context)
                DiscoverUiState(
                    isLoading = pets.isLoading || batteryCatalog.isLoading,
                    isBatteryEnabled = batteryConfig.enabled && accessibilityEnabled,
                    isAccessibilityEnabled = accessibilityEnabled,
                    trendingPets = pets.entries.take(MAX_TRENDING_PETS).map { pet ->
                        DiscoverPetUiState(
                            packKey = pet.installedPackKey,
                            name = pet.name,
                            category = pet.category,
                            thumbnailPath = pet.thumbnailPath
                        )
                    },
                    batteryThemes = batteryCatalog.themes
                        .filter { it.assetsReady }
                        .take(MAX_BATTERY_THEMES)
                        .map { theme ->
                            DiscoverThemeUiState(
                                id = theme.id,
                                name = theme.name,
                                thumbnailPath = theme.thumbnailPath,
                                isFavorite = theme.id in batteryConfig.favoriteThemeIds
                            )
                        },
                    statusBarThemes = batteryCatalog.backgrounds
                        .take(MAX_STATUS_BAR_THEMES)
                        .map { asset ->
                            DiscoverAssetUiState(asset.id, asset.name, asset.assetPath)
                        },
                    emojiThemes = batteryCatalog.themes
                        .mapNotNull { theme ->
                            theme.emojiPath?.let { path ->
                                DiscoverAssetUiState(theme.id, theme.name, path)
                            }
                        }
                        .take(MAX_COMPONENT_ASSETS),
                    batteryIcons = batteryCatalog.themes
                        .mapNotNull { theme ->
                            theme.batteryPath?.let { path ->
                                DiscoverAssetUiState(theme.id, theme.name, path)
                            }
                        }
                        .take(MAX_COMPONENT_ASSETS)
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
        viewModelScope.launch {
            petCatalogRepository.refresh()
        }
        viewModelScope.launch {
            batteryCatalogRepository.refresh()
        }
    }

    fun onBatteryToggle() {
        if (!BatteryAccessibility.isEnabled(context)) {
            enableBatteryAfterAccessibility = true
            _effects.trySend(DiscoverEffect.RequestBatteryAccessibility)
            return
        }
        batterySettingsRepository.setEnabled(!_uiState.value.isBatteryEnabled)
    }

    fun refreshAccessibility() {
        val enabled = BatteryAccessibility.isEnabled(context)
        _uiState.update { it.copy(isAccessibilityEnabled = enabled) }
        if (enabled && enableBatteryAfterAccessibility) {
            enableBatteryAfterAccessibility = false
            batterySettingsRepository.setEnabled(true)
        }
    }

    fun cancelPendingBatteryEnable() {
        enableBatteryAfterAccessibility = false
    }

    fun toggleFavorite(themeId: Int) {
        batterySettingsRepository.toggleFavorite(themeId)
    }

    private companion object {
        const val MAX_TRENDING_PETS = 12
        const val MAX_BATTERY_THEMES = 6
        const val MAX_STATUS_BAR_THEMES = 8
        const val MAX_COMPONENT_ASSETS = 12
    }
}
