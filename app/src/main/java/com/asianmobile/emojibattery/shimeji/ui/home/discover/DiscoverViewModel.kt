package com.asianmobile.emojibattery.shimeji.ui.home.discover

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryAccessibility
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryAccessibilityRecovery
import com.asianmobile.emojibattery.shimeji.battery.overlay.batteryAccessibilityRecovery
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry
import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogEntry
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

internal fun discoverTrendingPets(
    pets: List<OwnerPetCatalogEntry>,
    trendingIds: List<Int>
): List<OwnerPetCatalogEntry> = selectTrendingItems(
    items = pets,
    ids = trendingIds,
    idOf = OwnerPetCatalogEntry::id
)

internal fun discoverTrendingBatteryThemes(
    themes: List<BatteryThemeEntry>,
    trendingIds: List<Int>
): List<BatteryThemeEntry> = selectTrendingItems(
    items = themes.filter { theme -> theme.assetsReady && !theme.isBuiltIn },
    ids = trendingIds,
    idOf = BatteryThemeEntry::id
)

private fun <T> selectTrendingItems(
    items: List<T>,
    ids: List<Int>,
    idOf: (T) -> Int
): List<T> {
    val itemsById = items.associateBy(idOf)
    return ids.mapNotNull(itemsById::get)
}

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
    private var configuredBatteryEnabled = false

    /**
     * The stored enable is optimistic: it was written on the way *to* Accessibility settings and
     * the permission has not confirmed it yet. Recovery must stay quiet until it does, or a user
     * who simply has not granted anything yet is told the platform took their bar away.
     */
    private var isBatteryEnablePending = false

    init {
        viewModelScope.launch {
            combine(
                petCatalogRepository.snapshot,
                batteryCatalogRepository.snapshot,
                batterySettingsRepository.config
            ) { pets, batteryCatalog, batteryConfig ->
                val accessibilityEnabled = BatteryAccessibility.isEnabled(context)
                configuredBatteryEnabled = batteryConfig.enabled
                isBatteryEnablePending = batteryConfig.pendingAccessibilityGrant
                DiscoverUiState(
                    isPetCatalogLoading = pets.isLoading,
                    petCatalogLoadFailed = pets.error != null,
                    isBatteryCatalogLoading = batteryCatalog.isLoading,
                    batteryCatalogLoadFailed = batteryCatalog.error != null,
                    isBatteryEnabled = batteryConfig.enabled && accessibilityEnabled,
                    isAccessibilityEnabled = accessibilityEnabled,
                    accessibilityRecovery = resolveRecovery(
                        batteryConfig.enabled,
                        accessibilityEnabled
                    ),
                    trendingPets = discoverTrendingPets(
                        pets = pets.entries,
                        trendingIds = pets.trendingPetIds
                    ).map { pet ->
                        DiscoverPetUiState(
                            packKey = pet.installedPackKey,
                            name = pet.name,
                            category = pet.category,
                            thumbnailPath = pet.thumbnailPath
                        )
                    },
                    batteryThemes = discoverTrendingBatteryThemes(
                        themes = batteryCatalog.themes,
                        trendingIds = batteryCatalog.trendingEmojiThemeIds
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
            commitBatteryEnableRequest()
            _effects.trySend(DiscoverEffect.RequestBatteryAccessibility)
            return
        }
        batterySettingsRepository.setEnabled(!_uiState.value.isBatteryEnabled)
    }

    /**
     * Commits "the user wants the bar on" to storage. The service attaches as soon as Android binds
     * it, so committing before the hand-off is what makes the bar appear while the user is still in
     * system settings instead of after they walk back into the app.
     *
     * Called again when the disclosure hands off, because a resume in between settles pending
     * requests and this one has to survive that.
     */
    fun commitBatteryEnableRequest() {
        batterySettingsRepository.requestEnable(
            config = batterySettingsRepository.config.value,
            isAccessibilityGranted = BatteryAccessibility.isEnabled(context)
        )
    }

    fun refreshAccessibility() {
        val enabled = BatteryAccessibility.isEnabled(context)
        batterySettingsRepository.settleAccessibilityGrant(enabled)
        _uiState.update {
            it.copy(
                isBatteryEnabled = configuredBatteryEnabled && enabled,
                isAccessibilityEnabled = enabled,
                accessibilityRecovery = resolveRecovery(configuredBatteryEnabled, enabled)
            )
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
            // An enable that is still waiting for its first grant is not a revocation.
            isBatteryConfigured = batteryConfigured && !isBatteryEnablePending,
            isAccessibilityEnabled = accessibilityEnabled,
            wasProcessEndedByUser = wasProcessEndedByUser
        )
    }

    /**
     * The user came back from the hand-off, or declined it outright. Whatever was stored ahead of
     * the grant is taken back unless the permission actually arrived.
     */
    fun cancelPendingBatteryEnable() {
        batterySettingsRepository.settleAccessibilityGrant(
            BatteryAccessibility.isEnabled(context)
        )
    }

    fun toggleFavorite(themeId: Int) {
        batterySettingsRepository.toggleFavorite(themeId)
    }

    fun retryPetCatalog() {
        viewModelScope.launch { petCatalogRepository.refresh(force = true) }
    }

    fun retryBatteryCatalog() {
        viewModelScope.launch { batteryCatalogRepository.refresh() }
    }

    private companion object {
        const val MAX_DISCOVER_PREVIEW_ITEMS = 16
    }
}
