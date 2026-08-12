package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.ads.data.SharedPreferencesUtils
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryEditorPreviewSession
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryStatusComponent
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryMobileDataMonitor
import com.asianmobile.emojibattery.shimeji.battery.settings.resolveBatteryStatusBarHeightRange
import com.asianmobile.emojibattery.shimeji.battery.settings.systemStatusBarHeightDp
import com.asianmobile.emojibattery.shimeji.data.model.BUILT_IN_BATTERY_THEME
import com.asianmobile.emojibattery.shimeji.data.model.BUILT_IN_BATTERY_THEME_ID
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDecorationEntry
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry
import com.asianmobile.emojibattery.shimeji.data.repository.BatteryCatalogRepository
import com.asianmobile.emojibattery.shimeji.data.repository.BatterySettingsRepository
import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.BatteryThemeAccess
import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.BatteryThemeAccessPolicy
import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.CURRENT_BATTERY_STYLE_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val mobileDataMonitor: BatteryMobileDataMonitor,
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
    private var previewClientCount = 0
    private var previewStopJob: Job? = null
    private var focusedComponent: BatteryStatusComponent? = null
    private var assetSelectionRequestId = 0L
    private var backgroundSelectionRequestId = 0L
    private var emotionSelectionRequestId = 0L
    private val childEditCheckpoints = mutableMapOf<String, ChildEditCheckpoint>()
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
            combine(
                catalogRepository.snapshot,
                settingsRepository.config,
                mobileDataMonitor.badge
            ) { catalog, stored, mobileDataBadge ->
                latestStored = stored
                val isCurrentStyle = themeId == CURRENT_BATTERY_STYLE_ID
                val theme = if (isCurrentStyle) {
                    catalog.themes.firstOrNull { it.id == stored.selectedThemeId }
                } else {
                    catalog.themes.firstOrNull { it.id == themeId }
                }
                val selectedStyleId = theme?.id ?: BUILT_IN_BATTERY_THEME_ID
                val canInitializeSelection =
                    !isCurrentStyle &&
                        (theme != null || themeId == BUILT_IN_BATTERY_THEME_ID)
                val draft = when {
                    hasLocalEdits -> _uiState.value.config
                    isCurrentStyle -> stored
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
                    isInitialized = true,
                    theme = theme ?: BUILT_IN_BATTERY_THEME,
                    themes = catalog.themes,
                    categories = catalog.categories,
                    config = draft.copy(
                        rewardUnlockedThemeIds = stored.rewardUnlockedThemeIds
                    ),
                    backgrounds = catalog.backgrounds,
                    emotions = catalog.emotions,
                    emotionGroups = catalog.emotionGroups,
                    animations = catalog.animations,
                    mobileDataBadge = mobileDataBadge,
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
    fun selectBackground(background: BatteryDecorationEntry) {
        val state = _uiState.value
        if (state.backgroundSelectionInProgress != null) return
        if (state.config.backgroundDecorationId == background.id) return
        val requestId = ++backgroundSelectionRequestId
        _uiState.update {
            it.copy(backgroundSelectionInProgress = background.id, message = null)
        }
        viewModelScope.launch {
            val materializedPath = catalogRepository.materializeAsset(background.assetPath)
            if (requestId != backgroundSelectionRequestId) return@launch
            if (materializedPath != null) {
                update { copy(backgroundDecorationId = background.id) }
                _uiState.update {
                    it.copy(
                        backgrounds = it.backgrounds.map { entry ->
                            if (entry.id == background.id) {
                                entry.copy(assetPath = materializedPath)
                            } else entry
                        },
                        backgroundSelectionInProgress = null,
                        message = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        backgroundSelectionInProgress = null,
                        message = BatteryEditorMessage.ASSET_DOWNLOAD_FAILED
                    )
                }
            }
        }
    }
    fun setBackgroundDecoration(value: Int) {
        if (value == 0) {
            update { copy(backgroundDecorationId = 0) }
            return
        }
        _uiState.value.backgrounds.firstOrNull { it.id == value }?.let(::selectBackground)
    }
    fun setShowEmotion(value: Boolean) = update { copy(showEmotion = value) }
    fun selectEmotion(emotion: BatteryDecorationEntry) {
        val state = _uiState.value
        if (state.emotionSelectionInProgress != null) return
        if (state.config.showEmotion && state.config.emotionDecorationId == emotion.id) return
        val requestId = ++emotionSelectionRequestId
        _uiState.update {
            it.copy(emotionSelectionInProgress = emotion.id, message = null)
        }
        viewModelScope.launch {
            val materializedPath = catalogRepository.materializeAsset(emotion.assetPath)
            if (requestId != emotionSelectionRequestId) return@launch
            if (materializedPath != null) {
                update {
                    copy(showEmotion = true, emotionDecorationId = emotion.id)
                }
                _uiState.update {
                    it.copy(
                        emotions = it.emotions.map { entry ->
                            if (entry.id == emotion.id) {
                                entry.copy(assetPath = materializedPath)
                            } else entry
                        },
                        emotionSelectionInProgress = null,
                        message = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        emotionSelectionInProgress = null,
                        message = BatteryEditorMessage.ASSET_DOWNLOAD_FAILED
                    )
                }
            }
        }
    }
    fun setEmotionDecoration(value: Int) {
        _uiState.value.emotions.firstOrNull { it.id == value }?.let(::selectEmotion)
    }
    fun setConfig(value: BatteryStatusConfig) = update { value }

    internal fun beginChildEdit(page: BatteryEditorPage) {
        if (
            page == BatteryEditorPage.OVERVIEW ||
            page == BatteryEditorPage.EMOJI ||
            page.isFigmaPickerPage()
        ) return
        childEditCheckpoints.putIfAbsent(
            page.name,
            ChildEditCheckpoint(
                config = _uiState.value.config,
                hadUnsavedChanges = hasLocalEdits
            )
        )
    }

    internal fun commitChildEdit(page: BatteryEditorPage) {
        childEditCheckpoints.remove(page.name)
    }

    internal fun rollbackChildEdit(page: BatteryEditorPage) {
        val checkpoint = childEditCheckpoints.remove(page.name) ?: return
        invalidatePendingSelections()
        val restored = checkpoint.config.copy(
            rewardUnlockedThemeIds =
                checkpoint.config.rewardUnlockedThemeIds +
                    _uiState.value.config.rewardUnlockedThemeIds
        )
        hasLocalEdits = checkpoint.hadUnsavedChanges
        if (hasLocalEdits) {
            savedStateHandle[KEY_DRAFT] = BatteryDraftCodec.encode(restored)
            savedStateHandle[KEY_DIRTY] = true
        } else {
            savedStateHandle[KEY_DRAFT] = null
            savedStateHandle[KEY_DIRTY] = false
        }
        _uiState.update {
            it.copy(
                config = restored,
                isThemeAvailable = selectedAssetsReady(it.themes, restored),
                hasUnsavedChanges = hasLocalEdits,
                pendingSelection = null,
                assetSelectionInProgress = null,
                backgroundSelectionInProgress = null,
                emotionSelectionInProgress = null,
                isRewardInProgress = false,
                message = null
            )
        }
        publishPreview(restored)
    }

    fun startPreview() {
        previewClientCount += 1
        previewStopJob?.cancel()
        previewStopJob = null
        publishPreview(_uiState.value.config)
    }

    fun stopPreview() {
        previewClientCount = (previewClientCount - 1).coerceAtLeast(0)
        if (previewClientCount > 0) return
        previewStopJob?.cancel()
        previewStopJob = viewModelScope.launch {
            delay(PREVIEW_ROUTE_HANDOFF_DELAY_MS)
            if (previewClientCount == 0) {
                previewActive = false
                previewSession.stop(previewOwnerId)
            }
        }
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
            BatteryThemeAccess.OPEN -> prepareAndSelectTheme(theme, component)
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
        _uiState.update {
            it.copy(
                config = it.config.copy(
                    rewardUnlockedThemeIds = it.config.rewardUnlockedThemeIds + theme.id
                ),
                pendingSelection = null,
                isRewardInProgress = false,
                message = null
            )
        }
        prepareAndSelectTheme(theme, pending.component)
    }

    fun refreshEntitlement() {
        val premium = SharedPreferencesUtils.getIsPremium(context)
        val state = _uiState.value
        val pending = state.pendingSelection
        _uiState.update { it.copy(isPremium = premium) }
        if (premium && pending != null) {
            val theme = state.themes.firstOrNull { it.id == pending.themeId }
            if (theme?.assetsReady == true) {
                _uiState.update {
                    it.copy(
                        pendingSelection = null,
                        isRewardInProgress = false,
                        message = null
                    )
                }
                prepareAndSelectTheme(theme, pending.component)
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun apply() {
        val state = _uiState.value
        if (!state.isThemeAvailable || state.assetSelectionInProgress != null ||
            state.emotionSelectionInProgress != null
        ) return
        settingsRepository.applyConfig(state.config.copy(enabled = true, hasApplied = true))
        childEditCheckpoints.clear()
        clearDraft()
        _uiState.update {
            it.copy(
                config = it.config.copy(enabled = true, hasApplied = true),
                hasUnsavedChanges = false
            )
        }
    }

    fun disable() {
        childEditCheckpoints.clear()
        clearDraft()
        stopPreviewImmediately()
        settingsRepository.setEnabled(false)
        _uiState.update {
            it.copy(config = it.config.copy(enabled = false), hasUnsavedChanges = false)
        }
    }

    fun discardDraft() {
        childEditCheckpoints.clear()
        invalidatePendingSelections()
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

    private fun prepareAndSelectTheme(
        theme: BatteryThemeEntry,
        component: BatteryThemeComponent
    ) {
        val currentId = when (component) {
            BatteryThemeComponent.EMOJI -> _uiState.value.config.selectedEmojiThemeId
            BatteryThemeComponent.BATTERY -> _uiState.value.config.selectedBatteryThemeId
        }
        if (currentId == theme.id) return

        val selection = BatteryEditorThemeSelection(theme.id, component)
        val requestId = ++assetSelectionRequestId
        _uiState.update {
            it.copy(assetSelectionInProgress = selection, message = null)
        }
        viewModelScope.launch {
            val materializedPath = if (theme.isBuiltIn) {
                BUILT_IN_ASSET_MARKER
            } else {
                catalogRepository.materializeAsset(selectionPolicy.assetPath(theme, component))
            }
            if (requestId != assetSelectionRequestId) return@launch
            if (selectionPolicy.isMaterialized(theme, materializedPath)) {
                update { selectionPolicy.selectComponent(this, theme.id, component) }
                _uiState.update {
                    it.copy(assetSelectionInProgress = null, message = null)
                }
            } else {
                _uiState.update {
                    it.copy(
                        assetSelectionInProgress = null,
                        message = BatteryEditorMessage.ASSET_DOWNLOAD_FAILED
                    )
                }
            }
        }
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
        if (!BatteryEditorLivePreviewPolicy.shouldPublish(
                storedEnabled = latestStored.enabled,
                previewClientCount = previewClientCount
            )
        ) {
            if (previewActive) stopPreviewImmediately()
            return
        }
        if (!previewActive) {
            previewActive = true
            previewSession.start(previewOwnerId, config)
        }
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

    private fun invalidatePendingSelections() {
        assetSelectionRequestId += 1
        backgroundSelectionRequestId += 1
        emotionSelectionRequestId += 1
    }

    override fun onCleared() {
        previewStopJob?.cancel()
        previewSession.stop(previewOwnerId)
        super.onCleared()
    }

    private fun stopPreviewImmediately() {
        previewStopJob?.cancel()
        previewStopJob = null
        previewActive = false
        previewSession.stop(previewOwnerId)
    }

    private companion object {
        const val PREVIEW_ROUTE_HANDOFF_DELAY_MS = 200L
        const val KEY_DRAFT = "battery_editor_draft"
        const val KEY_DIRTY = "battery_editor_dirty"
        const val KEY_PREVIEW_OWNER = "battery_editor_preview_owner"
        const val KEY_SELECTION_INITIALIZED = "battery_editor_selection_initialized"
        const val BUILT_IN_ASSET_MARKER = "built-in"
    }

    private data class ChildEditCheckpoint(
        val config: BatteryStatusConfig,
        val hadUnsavedChanges: Boolean
    )
}
