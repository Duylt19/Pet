package com.asianmobile.emojibattery.shimeji.ui.battery.catalog

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.ads.data.SharedPreferencesUtils
import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogSnapshot
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry
import com.asianmobile.emojibattery.shimeji.data.repository.BatteryCatalogRepository
import com.asianmobile.emojibattery.shimeji.data.repository.BatterySettingsRepository
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
class BatteryCatalogViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val catalogRepository: BatteryCatalogRepository,
    private val settingsRepository: BatterySettingsRepository
) : ViewModel() {
    private val accessPolicy = BatteryThemeAccessPolicy()
    private val displayPolicy = BatteryCatalogDisplayPolicy()
    private val _uiState = MutableStateFlow(BatteryCatalogUiState())
    val uiState: StateFlow<BatteryCatalogUiState> = _uiState.asStateFlow()
    private val _effects = Channel<BatteryCatalogEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

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
                visibleThemes = displayPolicy.filterThemes(
                    current.themes,
                    categoryId,
                    current.searchQuery
                )
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { current ->
            current.copy(
                searchQuery = query,
                visibleThemes = displayPolicy.filterThemes(
                    current.themes,
                    current.selectedCategoryId,
                    query
                )
            )
        }
    }

    fun toggleFavorite(themeId: Int) = settingsRepository.toggleFavorite(themeId)

    fun requestCurrentStyle() {
        if (_uiState.value.currentStyle == null) return
        emit(BatteryCatalogEffect.OpenTheme(CURRENT_BATTERY_STYLE_ID))
    }

    fun refresh() {
        refreshEntitlement()
        viewModelScope.launch { catalogRepository.refresh() }
    }

    fun refreshEntitlement() {
        val premium = SharedPreferencesUtils.getIsPremium(context)
        val state = _uiState.value
        val pendingTheme = state.themes.firstOrNull { it.id == state.pendingUnlockThemeId }
        val canOpenPendingTheme = premium && pendingTheme?.assetsReady == true
        _uiState.update {
            it.copy(
                isPremium = premium,
                pendingUnlockThemeId = if (premium) null else it.pendingUnlockThemeId,
                isRewardInProgress = if (premium) false else it.isRewardInProgress,
                message = when {
                    canOpenPendingTheme -> null
                    premium && it.pendingUnlockThemeId != null ->
                        BatteryCatalogMessage.THEME_UNAVAILABLE
                    else -> it.message
                }
            )
        }
        if (canOpenPendingTheme) {
            emit(BatteryCatalogEffect.OpenTheme(requireNotNull(pendingTheme).id))
        }
    }

    fun canOpen(theme: BatteryThemeEntry): Boolean =
        accessPolicy.resolve(
            theme,
            _uiState.value.isPremium,
            _uiState.value.rewardUnlockedThemeIds
        ) == BatteryThemeAccess.OPEN

    fun requestTheme(theme: BatteryThemeEntry) {
        when (
            accessPolicy.resolve(
                theme,
                _uiState.value.isPremium,
                _uiState.value.rewardUnlockedThemeIds
            )
        ) {
            BatteryThemeAccess.OPEN -> emit(BatteryCatalogEffect.OpenTheme(theme.id))
            BatteryThemeAccess.REWARD_OR_PREMIUM -> _uiState.update {
                it.copy(
                    pendingUnlockThemeId = theme.id,
                    isRewardInProgress = false,
                    message = null
                )
            }
            BatteryThemeAccess.UNAVAILABLE -> _uiState.update {
                it.copy(message = BatteryCatalogMessage.THEME_UNAVAILABLE)
            }
        }
    }

    fun dismissUnlockDialog() {
        if (_uiState.value.isRewardInProgress) return
        _uiState.update {
            it.copy(pendingUnlockThemeId = null, message = null)
        }
    }

    fun requestRewardUnlock() {
        val state = _uiState.value
        val theme = state.themes.firstOrNull { it.id == state.pendingUnlockThemeId } ?: return
        if (state.isRewardInProgress ||
            accessPolicy.resolve(theme, state.isPremium, state.rewardUnlockedThemeIds) !=
            BatteryThemeAccess.REWARD_OR_PREMIUM
        ) {
            return
        }
        _uiState.update { it.copy(isRewardInProgress = true, message = null) }
        emit(BatteryCatalogEffect.ShowRewardedAd)
    }

    fun onRewardResult(canContinue: Boolean) {
        val state = _uiState.value
        val themeId = state.pendingUnlockThemeId
        if (!state.isRewardInProgress || themeId == null) return
        if (!canContinue) {
            _uiState.update {
                it.copy(
                    isRewardInProgress = false,
                    message = BatteryCatalogMessage.REWARD_NOT_EARNED
                )
            }
            return
        }
        val theme = state.themes.firstOrNull { it.id == themeId }
        if (theme == null || !theme.assetsReady) {
            _uiState.update {
                it.copy(
                    pendingUnlockThemeId = null,
                    isRewardInProgress = false,
                    message = BatteryCatalogMessage.THEME_UNAVAILABLE
                )
            }
            return
        }
        settingsRepository.unlockThemeByReward(themeId)
        _uiState.update {
            it.copy(
                rewardUnlockedThemeIds = it.rewardUnlockedThemeIds + themeId,
                pendingUnlockThemeId = null,
                isRewardInProgress = false,
                message = null
            )
        }
        emit(BatteryCatalogEffect.OpenTheme(themeId))
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun reduce(
        current: BatteryCatalogUiState,
        catalog: BatteryCatalogSnapshot,
        config: BatteryStatusConfig
    ): BatteryCatalogUiState = current.copy(
        themes = catalog.themes,
        visibleThemes = displayPolicy.filterThemes(
            catalog.themes,
            current.selectedCategoryId,
            current.searchQuery
        ),
        categories = displayPolicy.filterCategories(catalog.categories),
        favoriteThemeIds = config.favoriteThemeIds,
        rewardUnlockedThemeIds = config.rewardUnlockedThemeIds,
        isPremium = SharedPreferencesUtils.getIsPremium(context),
        currentStyle = displayPolicy.currentStyle(catalog, config),
        pendingUnlockThemeId = current.pendingUnlockThemeId
            ?.takeIf { id -> catalog.themes.any { it.id == id } },
        isLoading = catalog.isLoading,
        error = catalog.error
    )

    private fun emit(effect: BatteryCatalogEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
