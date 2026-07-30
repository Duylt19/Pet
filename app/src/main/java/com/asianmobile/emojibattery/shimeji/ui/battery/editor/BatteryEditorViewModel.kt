package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.ads.data.SharedPreferencesUtils
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryEditorPreviewSession
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryStatusComponent
import com.asianmobile.emojibattery.shimeji.battery.settings.resolveBatteryStatusBarHeightRange
import com.asianmobile.emojibattery.shimeji.battery.settings.systemStatusBarHeightDp
import com.asianmobile.emojibattery.shimeji.data.model.BUILT_IN_BATTERY_THEME
import com.asianmobile.emojibattery.shimeji.data.model.BUILT_IN_BATTERY_THEME_ID
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry
import com.asianmobile.emojibattery.shimeji.data.repository.BatteryCatalogRepository
import com.asianmobile.emojibattery.shimeji.data.repository.BatterySettingsRepository
import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.BatteryThemeAccess
import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.BatteryThemeAccessPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
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
class BatteryEditorViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val catalogRepository: BatteryCatalogRepository,
    private val settingsRepository: BatterySettingsRepository,
    private val previewSession: BatteryEditorPreviewSession
) : ViewModel() {
    private val themeId = savedStateHandle.get<Int>("themeId") ?: BUILT_IN_BATTERY_THEME_ID
    private val previewOwnerId = savedStateHandle.get<String>(KEY_PREVIEW_OWNER)
        ?: UUID.randomUUID().toString().also { savedStateHandle[KEY_PREVIEW_OWNER] = it }
    private val accessPolicy = BatteryThemeAccessPolicy()
    private val selectionPolicy = BatteryThemeSelectionPolicy()
    private val barHeightRange = resolveBatteryStatusBarHeightRange(
        context.systemStatusBarHeightDp()
    )
    private val restoredDraft = BatteryDraftCodec.decode(savedStateHandle[KEY_DRAFT])
        ?.let { draft ->
            draft.copy(
                barHeightDp = draft.barHeightDp.coerceIn(
                    barHeightRange.minimumDp,
                    barHeightRange.maximumDp
                )
            )
        }
    private var hasLocalEdits = savedStateHandle.get<Boolean>(KEY_DIRTY) == true &&
        restoredDraft != null
    private var hasInitializedSelection = restoredDraft != null ||
        savedStateHandle.get<Boolean>(KEY_SELECTION_INITIALIZED) == true
    private var latestStored = BatteryStatusConfig(barHeightDp = barHeightRange.defaultDp)
    private var previewActive = false
    private var focusedComponent: BatteryStatusComponent? = null
    private val _uiState = MutableStateFlow(
        BatteryEditorUiState(
            config = restoredDraft ?: BatteryStatusConfig(barHeightDp = barHeightRange.defaultDp),
            barHeightRange = barHeightRange,
            hasUnsavedChanges = hasLocalEdits
        )
    )
    val uiState: StateFlow<BatteryEditorUiState> = _uiState.asStateFlow()
    private val _effects = Channel<BatteryEditorEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(catalogRepository.snapshot, settingsRepository.config) { catalog, stored ->
                latestStored = stored
                val theme = catalog.themes.firstOrNull { it.id == themeId }
                val selectedStyleId = theme?.id ?: BUILT_IN_BATTERY_THEME_ID
                val canInitializeSelection =
                    theme != null || themeId == BUILT_IN_BATTERY_THEME_ID
                val draft = when {
                    hasLocalEdits -> _uiState.value.config
                    !hasInitializedSelection && canInitializeSelection ->
                        selectionPolicy.initializeStyle(stored, selectedStyleId)
                            .also { initialDraft ->
                                hasInitializedSelection = true
                                savedStateHandle[KEY_SELECTION_INITIALIZED] = true
                                if (initialDraft != stored) {
                                    hasLocalEdits = true
                                    savedStateHandle[KEY_DRAFT] =
                                        BatteryDraftCodec.encode(initialDraft)
                                    savedStateHandle[KEY_DIRTY] = true
                                }
                            }
                    else -> stored
                }
                _uiState.value.copy(
                    theme = theme ?: BUILT_IN_BATTERY_THEME,
                    themes = catalog.themes,
                    categories = catalog.categories,
                    config = draft.copy(
                        rewardUnlockedThemeIds = stored.rewardUnlockedThemeIds
                    ),
                    backgrounds = catalog.backgrounds,
                    emotions = catalog.emotions,
                    animations = catalog.animations,
                    isThemeAvailable = selectedAssetsReady(catalog.themes, draft),
                    isPremium = SharedPreferencesUtils.getIsPremium(context),
                    hasUnsavedChanges = hasLocalEdits
                )
            }.collect { state ->
                _uiState.value = state
                publishPreview(state.config)
            }
        }
    }

    fun setShowTime(value: Boolean) = update { copy(showTime = value) }
    fun setShowPercentage(value: Boolean) = update { copy(showPercentage = value) }
    fun setBarHeight(value: Float) = update { copy(barHeightDp = value) }
    fun setEmojiSize(value: Float) = update { copy(emojiSizeDp = value) }
    fun setBatterySize(value: Float) = update { copy(batterySizeDp = value) }
    fun setBackgroundColor(value: Int) = update { copy(backgroundColorArgb = value) }
    fun setForegroundColor(value: Int) = update { copy(foregroundColorArgb = value) }
    fun setBackgroundDecoration(value: Int) =
        update { copy(backgroundDecorationId = value) }
    fun setShowEmotion(value: Boolean) = update { copy(showEmotion = value) }
    fun setEmotionDecoration(value: Int) =
        update { copy(emotionDecorationId = value) }
    fun setConfig(value: BatteryStatusConfig) = update { value }

    fun startPreview() {
        previewActive = true
        previewSession.start(previewOwnerId, _uiState.value.config)
        publishPreview(_uiState.value.config)
    }

    fun stopPreview() {
        previewActive = false
        previewSession.stop(previewOwnerId)
    }

    fun setPreviewComponent(component: BatteryStatusComponent?) {
        focusedComponent = component
        publishPreview(_uiState.value.config)
    }

    fun requestTheme(theme: BatteryThemeEntry, component: BatteryThemeComponent) {
        when (
            accessPolicy.resolve(
                theme,
                _uiState.value.isPremium,
                _uiState.value.config.rewardUnlockedThemeIds
            )
        ) {
            BatteryThemeAccess.OPEN -> selectTheme(theme.id, component)
            BatteryThemeAccess.REWARD_OR_PREMIUM -> _uiState.update {
                it.copy(
                    pendingSelection = BatteryEditorThemeSelection(theme.id, component),
                    isRewardInProgress = false,
                    message = null
                )
            }
            BatteryThemeAccess.UNAVAILABLE -> _uiState.update {
                it.copy(message = BatteryEditorMessage.THEME_UNAVAILABLE)
            }
        }
    }

    fun dismissUnlockDialog() {
        if (_uiState.value.isRewardInProgress) return
        _uiState.update { it.copy(pendingSelection = null, message = null) }
    }

    fun requestRewardUnlock() {
        val state = _uiState.value
        val pending = state.pendingSelection ?: return
        val theme = state.themes.firstOrNull { it.id == pending.themeId } ?: return
        if (state.isRewardInProgress ||
            accessPolicy.resolve(theme, state.isPremium, state.config.rewardUnlockedThemeIds) !=
            BatteryThemeAccess.REWARD_OR_PREMIUM
        ) {
            return
        }
        _uiState.update { it.copy(isRewardInProgress = true, message = null) }
        emit(BatteryEditorEffect.ShowRewardedAd)
    }

    fun onRewardResult(canContinue: Boolean) {
        val state = _uiState.value
        val pending = state.pendingSelection ?: return
        if (!state.isRewardInProgress) return
        val theme = state.themes.firstOrNull { it.id == pending.themeId }
        if (!canContinue) {
            _uiState.update {
                it.copy(
                    isRewardInProgress = false,
                    message = BatteryEditorMessage.REWARD_NOT_EARNED
                )
            }
            return
        }
        if (theme?.assetsReady != true) {
            _uiState.update {
                it.copy(
                    pendingSelection = null,
                    isRewardInProgress = false,
                    message = BatteryEditorMessage.THEME_UNAVAILABLE
                )
            }
            return
        }
        settingsRepository.unlockThemeByReward(theme.id)
        update {
            selectionPolicy.selectComponent(
                config = copy(rewardUnlockedThemeIds = rewardUnlockedThemeIds + theme.id),
                themeId = theme.id,
                component = pending.component
            )
        }
        _uiState.update {
            it.copy(
                pendingSelection = null,
                isRewardInProgress = false,
                message = null
            )
        }
    }

    fun refreshEntitlement() {
        val premium = SharedPreferencesUtils.getIsPremium(context)
        val state = _uiState.value
        val pending = state.pendingSelection
        _uiState.update { it.copy(isPremium = premium) }
        if (premium && pending != null) {
            val theme = state.themes.firstOrNull { it.id == pending.themeId }
            if (theme?.assetsReady == true) {
                selectTheme(theme.id, pending.component)
                _uiState.update {
                    it.copy(
                        pendingSelection = null,
                        isRewardInProgress = false,
                        message = null
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun apply() {
        val state = _uiState.value
        if (!state.isThemeAvailable) return
        settingsRepository.applyConfig(state.config.copy(enabled = true))
        clearDraft()
        _uiState.update {
            it.copy(
                config = it.config.copy(enabled = true),
                hasUnsavedChanges = false
            )
        }
    }

    fun disable() {
        clearDraft()
        stopPreview()
        settingsRepository.setEnabled(false)
        _uiState.update {
            it.copy(config = it.config.copy(enabled = false), hasUnsavedChanges = false)
        }
    }

    fun discardDraft() {
        clearDraft()
        _uiState.update { state ->
            state.copy(
                config = latestStored,
                hasUnsavedChanges = false
            )
        }
        publishPreview(latestStored)
    }

    private fun update(transform: BatteryStatusConfig.() -> BatteryStatusConfig) {
        hasLocalEdits = true
        _uiState.update {
            val config = transform(it.config)
            savedStateHandle[KEY_DRAFT] = BatteryDraftCodec.encode(config)
            savedStateHandle[KEY_DIRTY] = true
            it.copy(
                config = config,
                isThemeAvailable = selectedAssetsReady(it.themes, config),
                hasUnsavedChanges = true
            )
        }
        publishPreview(_uiState.value.config)
    }

    private fun selectTheme(themeId: Int, component: BatteryThemeComponent) {
        update { selectionPolicy.selectComponent(this, themeId, component) }
    }

    private fun selectedAssetsReady(
        themes: List<BatteryThemeEntry>,
        config: BatteryStatusConfig
    ): Boolean {
        fun isReady(id: Int): Boolean = id == BUILT_IN_BATTERY_THEME_ID ||
            themes.firstOrNull { it.id == id }?.assetsReady == true
        return isReady(config.selectedBatteryThemeId) && isReady(config.selectedEmojiThemeId)
    }

    private fun publishPreview(config: BatteryStatusConfig) {
        if (!previewActive) return
        previewSession.update(previewOwnerId, config, focusedComponent)
    }

    private fun emit(effect: BatteryEditorEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private fun clearDraft() {
        hasLocalEdits = false
        savedStateHandle[KEY_DRAFT] = null
        savedStateHandle[KEY_DIRTY] = false
    }

    private companion object {
        const val KEY_DRAFT = "battery_editor_draft"
        const val KEY_DIRTY = "battery_editor_dirty"
        const val KEY_PREVIEW_OWNER = "battery_editor_preview_owner"
        const val KEY_SELECTION_INITIALIZED = "battery_editor_selection_initialized"
    }
}
