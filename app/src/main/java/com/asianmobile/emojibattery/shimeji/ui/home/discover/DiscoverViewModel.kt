package com.asianmobile.emojibattery.shimeji.ui.home.discover

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryAccessibility
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryAccessibilityRecovery
import com.asianmobile.emojibattery.shimeji.battery.overlay.batteryAccessibilityRecovery
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry
import com.asianmobile.emojibattery.shimeji.data.repository.BatteryCatalogRepository
import com.asianmobile.emojibattery.shimeji.data.repository.BatterySettingsRepository
import com.asianmobile.emojibattery.shimeji.data.repository.OwnerPetCatalogRepository
import com.asianmobile.emojibattery.shimeji.pet.overlay.PetBackgroundRestrictionReader
import com.asianmobile.emojibattery.shimeji.pet.overlay.PetProcessKillKind
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

internal fun discoverPreviewBatteryThemes(
    themes: List<BatteryThemeEntry>,
    limit: Int
): List<BatteryThemeEntry> = themes
    .filter { theme -> theme.assetsReady && !theme.isBuiltIn }
    .take(limit)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val petCatalogRepository: OwnerPetCatalogRepository,
    private val batteryCatalogRepository: BatteryCatalogRepository,
    private val batterySettingsRepository: BatterySettingsRepository,
    restrictionReader: PetBackgroundRestrictionReader
) : ViewModel() {
    /**
     * Read once and kept: it describes a death that already happened, and the lookup is a binder
     * call into the system. Null whenever the platform recorded nothing usable — API 30 and below
     * has no record at all, and [PetProcessKillKind.OTHER] is a crash or an update rather than
     * anything that would have taken a permission away.
     */
    private val wasProcessEndedByUser: Boolean? =
        when (restrictionReader.lastOverlayKill()) {
            PetProcessKillKind.USER -> true
            PetProcessKillKind.SIGNALLED, PetProcessKillKind.SYSTEM_RECLAIM -> false
            PetProcessKillKind.OTHER, null -> null
        }
    private var isRecoveryDismissed = false

    private val _uiState = MutableStateFlow(
        DiscoverUiState(isAccessibilityEnabled = BatteryAccessibility.isEnabled(context))
    )
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private val _effects = Channel<DiscoverEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()
    private var enableBatteryAfterAccessibility = false
    private var configuredBatteryEnabled = false

    init {
        viewModelScope.launch {
            combine(
                petCatalogRepository.snapshot,
                batteryCatalogRepository.snapshot,
                batterySettingsRepository.config
            ) { pets, batteryCatalog, batteryConfig ->
                val accessibilityEnabled = BatteryAccessibility.isEnabled(context)
                configuredBatteryEnabled = batteryConfig.enabled
                DiscoverUiState(
                    isLoading = pets.isLoading || batteryCatalog.isLoading,
                    isBatteryEnabled = batteryConfig.enabled && accessibilityEnabled,
                    isAccessibilityEnabled = accessibilityEnabled,
                    accessibilityRecovery = resolveRecovery(
                        batteryConfig.enabled,
                        accessibilityEnabled
                    ),
                    trendingPets = pets.entries.take(MAX_DISCOVER_PREVIEW_ITEMS).map { pet ->
                        DiscoverPetUiState(
                            packKey = pet.installedPackKey,
                            name = pet.name,
                            category = pet.category,
                            thumbnailPath = pet.thumbnailPath
                        )
                    },
                    batteryThemes = discoverPreviewBatteryThemes(
                        themes = batteryCatalog.themes,
                        limit = MAX_DISCOVER_PREVIEW_ITEMS
                    )
                        .map { theme ->
                            DiscoverThemeUiState(
                                id = theme.id,
                                name = theme.name,
                                thumbnailPath = theme.thumbnailPath,
                                isFavorite = theme.id in batteryConfig.favoriteThemeIds
                            )
                        },
                    statusBarThemes = batteryCatalog.backgrounds
                        .take(MAX_DISCOVER_PREVIEW_ITEMS)
                        .map { asset ->
                            DiscoverAssetUiState(asset.id, asset.name, asset.assetPath)
                        },
                    emojiThemes = batteryCatalog.themes
                        .mapNotNull { theme ->
                            theme.emojiPath?.let { path ->
                                DiscoverAssetUiState(theme.id, theme.name, path)
                            }
                        }
                        .take(MAX_DISCOVER_PREVIEW_ITEMS),
                    batteryIcons = batteryCatalog.themes
                        .mapNotNull { theme ->
                            theme.batteryPath?.let { path ->
                                DiscoverAssetUiState(theme.id, theme.name, path)
                            }
                        }
                        .take(MAX_DISCOVER_PREVIEW_ITEMS)
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
        _uiState.update {
            it.copy(
                isBatteryEnabled = configuredBatteryEnabled && enabled,
                isAccessibilityEnabled = enabled,
                accessibilityRecovery = resolveRecovery(configuredBatteryEnabled, enabled)
            )
        }
        if (enabled && enableBatteryAfterAccessibility) {
            enableBatteryAfterAccessibility = false
            batterySettingsRepository.setEnabled(true)
        }
    }

    /** Dismissal lasts for this ViewModel only: a later revocation has to be able to say so. */
    fun dismissAccessibilityRecovery() {
        isRecoveryDismissed = true
        _uiState.update { it.copy(accessibilityRecovery = BatteryAccessibilityRecovery.NONE) }
    }

    /**
     * The kill reason is read once rather than per state change: it describes a death that already
     * happened, and `getHistoricalProcessExitReasons` is a binder call into the system.
     */
    private fun resolveRecovery(
        batteryConfigured: Boolean,
        accessibilityEnabled: Boolean
    ): BatteryAccessibilityRecovery {
        if (isRecoveryDismissed) return BatteryAccessibilityRecovery.NONE
        return batteryAccessibilityRecovery(
            isBatteryConfigured = batteryConfigured,
            isAccessibilityEnabled = accessibilityEnabled,
            wasProcessEndedByUser = wasProcessEndedByUser
        )
    }

    fun cancelPendingBatteryEnable() {
        enableBatteryAfterAccessibility = false
    }

    fun toggleFavorite(themeId: Int) {
        batterySettingsRepository.toggleFavorite(themeId)
    }

    private companion object {
        const val MAX_DISCOVER_PREVIEW_ITEMS = 16
    }
}
