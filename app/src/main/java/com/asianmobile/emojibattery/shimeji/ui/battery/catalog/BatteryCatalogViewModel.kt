package com.asianmobile.emojibattery.shimeji.ui.battery.catalog

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.ads.data.SharedPreferencesUtils
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryAccessibility
import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogSnapshot
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDecorationEntry
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry
import com.asianmobile.emojibattery.shimeji.data.repository.BatteryCatalogRepository
import com.asianmobile.emojibattery.shimeji.data.repository.BatterySettingsRepository
import com.asianmobile.emojibattery.shimeji.ui.battery.editor.BatteryBackgroundAccess
import com.asianmobile.emojibattery.shimeji.ui.battery.editor.BatteryBackgroundAccessPolicy
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BatteryCatalogViewModel(
    private val context: Context,
    private val catalogRepository: BatteryCatalogRepository,
    private val settingsRepository: BatterySettingsRepository
) : ViewModel() {
    private val accessPolicy = BatteryThemeAccessPolicy()
    private val displayPolicy = BatteryCatalogDisplayPolicy()
    private val _uiState = MutableStateFlow(BatteryCatalogUiState())
    val catalogState: StateFlow<BatteryCatalogUiState> = _uiState.asStateFlow()
    private val _effects = Channel<BatteryCatalogEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()
    private var configuredBatteryEnabled = false

    init {
        viewModelScope.launch {
            combine(catalogRepository.snapshot, settingsRepository.config) { catalog, config ->
                catalog to config
            }.collect { (catalog, config) ->
                _uiState.update { current ->
                    reduce(current, catalog, config)
                }
            }
        }
    }

    fun toggleFavorite(themeId: Int) = settingsRepository.toggleFavorite(themeId)

    fun onBatteryToggle() {
        if (!BatteryAccessibility.isEnabled(context)) {
            commitBatteryEnableRequest()
            emit(BatteryCatalogEffect.RequestBatteryAccessibility)
            return
        }
        settingsRepository.setEnabled(!_uiState.value.isBatteryEnabled)
    }

    /**
     * Stored before the hand-off so the bar attaches the moment the service is bound, while the
     * user is still inside system Accessibility settings. Repeated at the disclosure hand-off,
     * because a resume in between settles pending requests.
     */
    fun commitBatteryEnableRequest() {
        settingsRepository.requestEnable(
            config = settingsRepository.config.value,
            isAccessibilityGranted = BatteryAccessibility.isEnabled(context)
        )
    }

    fun refreshAccessibility() {
        val accessibilityEnabled = BatteryAccessibility.isEnabled(context)
        settingsRepository.settleAccessibilityGrant(accessibilityEnabled)
        _uiState.update {
            it.copy(isBatteryEnabled = configuredBatteryEnabled && accessibilityEnabled)
        }
    }

    /** Back without a grant takes the optimistic enable away again. */
    fun cancelPendingBatteryEnable() {
        settingsRepository.settleAccessibilityGrant(BatteryAccessibility.isEnabled(context))
    }

    fun requestCurrentStyle() {
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
        val pendingBackground = state.backgrounds.firstOrNull {
            it.id == state.pendingUnlockBackgroundId
        }
        val canOpenPendingTheme = premium && pendingTheme?.assetsReady == true
        val canOpenPendingBackground = premium && pendingBackground != null
        _uiState.update {
            it.copy(
                isPremium = premium,
                pendingUnlockThemeId = if (premium) null else it.pendingUnlockThemeId,
                pendingUnlockBackgroundId = if (premium) null else it.pendingUnlockBackgroundId,
                isRewardInProgress = if (premium) false else it.isRewardInProgress,
                message = when {
                    canOpenPendingTheme || canOpenPendingBackground -> null
                    premium &&
                        (it.pendingUnlockThemeId != null || it.pendingUnlockBackgroundId != null) ->
                        BatteryCatalogMessage.THEME_UNAVAILABLE
                    else -> it.message
                }
            )
        }
        if (canOpenPendingTheme) {
            emit(BatteryCatalogEffect.OpenTheme(requireNotNull(pendingTheme).id))
        }
        if (canOpenPendingBackground) {
            emit(BatteryCatalogEffect.OpenBackground(requireNotNull(pendingBackground).id))
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
                    pendingUnlockBackgroundId = null,
                    isRewardInProgress = false,
                    message = null
                )
            }
            BatteryThemeAccess.UNAVAILABLE -> _uiState.update {
                it.copy(message = BatteryCatalogMessage.THEME_UNAVAILABLE)
            }
        }
    }

    fun requestBackground(background: BatteryDecorationEntry) {
        val state = _uiState.value
        val index = state.backgrounds.indexOfFirst { it.id == background.id }
        when (
            BatteryBackgroundAccessPolicy.resolve(
                background = background,
                catalogIndex = index,
                isPremium = state.isPremium,
                rewardUnlockedBackgroundIds = state.rewardUnlockedBackgroundIds
            )
        ) {
            BatteryBackgroundAccess.OPEN ->
                emit(BatteryCatalogEffect.OpenBackground(background.id))

            BatteryBackgroundAccess.REWARD_OR_PREMIUM -> _uiState.update {
                it.copy(
                    pendingUnlockThemeId = null,
                    pendingUnlockBackgroundId = background.id,
                    isRewardInProgress = false,
                    message = null
                )
            }

            BatteryBackgroundAccess.UNAVAILABLE -> _uiState.update {
                it.copy(message = BatteryCatalogMessage.THEME_UNAVAILABLE)
            }
        }
    }

    fun dismissUnlockDialog() {
        if (_uiState.value.isRewardInProgress) return
        _uiState.update {
            it.copy(
                pendingUnlockThemeId = null,
                pendingUnlockBackgroundId = null,
                message = null
            )
        }
    }

    fun requestRewardUnlock() {
        val state = _uiState.value
        if (state.isRewardInProgress) return
        val canUnlockTheme = state.themes
            .firstOrNull { it.id == state.pendingUnlockThemeId }
            ?.let { theme ->
                accessPolicy.resolve(theme, state.isPremium, state.rewardUnlockedThemeIds) ==
                    BatteryThemeAccess.REWARD_OR_PREMIUM
            } == true
        val backgroundIndex = state.backgrounds.indexOfFirst {
            it.id == state.pendingUnlockBackgroundId
        }
        val canUnlockBackground = state.backgrounds.getOrNull(backgroundIndex)?.let { background ->
            BatteryBackgroundAccessPolicy.resolve(
                background = background,
                catalogIndex = backgroundIndex,
                isPremium = state.isPremium,
                rewardUnlockedBackgroundIds = state.rewardUnlockedBackgroundIds
            ) == BatteryBackgroundAccess.REWARD_OR_PREMIUM
        } == true
        if (!canUnlockTheme && !canUnlockBackground) return
        _uiState.update { it.copy(isRewardInProgress = true, message = null) }
        emit(BatteryCatalogEffect.ShowRewardedAd)
    }

    fun onRewardResult(canContinue: Boolean) {
        val state = _uiState.value
        val themeId = state.pendingUnlockThemeId
        val backgroundId = state.pendingUnlockBackgroundId
        if (!state.isRewardInProgress || (themeId == null && backgroundId == null)) return
        if (!canContinue) {
            _uiState.update {
                it.copy(
                    isRewardInProgress = false,
                    message = BatteryCatalogMessage.REWARD_NOT_EARNED
                )
            }
            return
        }
        if (backgroundId != null) {
            val background = state.backgrounds.firstOrNull { it.id == backgroundId }
            if (background == null) {
                _uiState.update {
                    it.copy(
                        pendingUnlockBackgroundId = null,
                        isRewardInProgress = false,
                        message = BatteryCatalogMessage.THEME_UNAVAILABLE
                    )
                }
                return
            }
            settingsRepository.unlockBackgroundByReward(backgroundId)
            _uiState.update {
                it.copy(
                    rewardUnlockedBackgroundIds = it.rewardUnlockedBackgroundIds + backgroundId,
                    pendingUnlockBackgroundId = null,
                    isRewardInProgress = false,
                    message = null
                )
            }
            emit(BatteryCatalogEffect.OpenBackground(backgroundId))
            return
        }
        val resolvedThemeId = themeId ?: return
        val theme = state.themes.firstOrNull { it.id == resolvedThemeId }
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
        settingsRepository.unlockThemeByReward(resolvedThemeId)
        _uiState.update {
            it.copy(
                rewardUnlockedThemeIds = it.rewardUnlockedThemeIds + resolvedThemeId,
                pendingUnlockThemeId = null,
                isRewardInProgress = false,
                message = null
            )
        }
        emit(BatteryCatalogEffect.OpenTheme(resolvedThemeId))
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun reduce(
        current: BatteryCatalogUiState,
        catalog: BatteryCatalogSnapshot,
        config: BatteryStatusConfig
    ): BatteryCatalogUiState {
        configuredBatteryEnabled = config.enabled
        return current.copy(
            themes = catalog.themes,
            backgrounds = catalog.backgrounds,
            categories = displayPolicy.filterCategories(catalog.categories),
            sections = displayPolicy.sections(
                categories = catalog.categories,
                themes = catalog.themes,
                trendingThemeIds = catalog.trendingEmojiThemeIds
            ),
            selectedThemeId = config.selectedThemeId,
            isBatteryEnabled = config.enabled && BatteryAccessibility.isEnabled(context),
            favoriteThemeIds = config.favoriteThemeIds,
            rewardUnlockedThemeIds = config.rewardUnlockedThemeIds,
            rewardUnlockedBackgroundIds = config.rewardUnlockedBackgroundIds,
            isPremium = SharedPreferencesUtils.getIsPremium(context),
            pendingUnlockThemeId = current.pendingUnlockThemeId
                ?.takeIf { id -> catalog.themes.any { it.id == id } },
            pendingUnlockBackgroundId = current.pendingUnlockBackgroundId
                ?.takeIf { id -> catalog.backgrounds.any { it.id == id } },
            isLoading = catalog.isLoading,
            error = catalog.error
        )
    }

    private fun emit(effect: BatteryCatalogEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
