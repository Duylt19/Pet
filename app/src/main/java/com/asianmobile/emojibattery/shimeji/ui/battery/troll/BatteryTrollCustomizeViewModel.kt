package com.asianmobile.emojibattery.shimeji.ui.battery.troll

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryAccessibility
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryEditorPreviewSession
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryEditorSystemStateMonitor
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryPreviewSystemState
import com.asianmobile.emojibattery.shimeji.data.model.BATTERY_TROLL_LEVEL_COUNT
import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogSnapshot
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollCatalogSnapshot
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollMode
import com.asianmobile.emojibattery.shimeji.data.model.MAX_BATTERY_TROLL_FAKE_PERCENT
import com.asianmobile.emojibattery.shimeji.data.model.MIN_BATTERY_TROLL_FAKE_PERCENT
import com.asianmobile.emojibattery.shimeji.data.model.NO_BATTERY_TROLL_THEME_ID
import com.asianmobile.emojibattery.shimeji.data.repository.BatteryCatalogRepository
import com.asianmobile.emojibattery.shimeji.data.repository.BatterySettingsRepository
import com.asianmobile.emojibattery.shimeji.data.repository.BatteryTrollCatalogRepository
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

/**
 * Drives Battery Troll Customize (Figma `8315:8232`).
 *
 * The screen edits a [BatteryTrollDraft] rather than the stored config, because Apply is the only
 * moment the status bar is allowed to change: a user experimenting with a fake percentage must not
 * leak half-finished states onto their own status bar.
 */
@HiltViewModel
class BatteryTrollCustomizeViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val catalogRepository: BatteryTrollCatalogRepository,
    // The status bar draws whatever background decoration the stored config selected, so the
    // preview has to read the same Battery catalog or it would show a bar the user does not have.
    private val batteryCatalogRepository: BatteryCatalogRepository,
    private val settingsRepository: BatterySettingsRepository,
    private val systemStateMonitor: BatteryEditorSystemStateMonitor,
    private val previewSession: BatteryEditorPreviewSession
) : ViewModel() {
    /**
     * The live status bar shows this screen's draft while it is open, through the same session the
     * status-bar editor uses — one bar, one preview channel. The id keeps the two screens from
     * overwriting each other's preview when both are on the back stack.
     */
    private val previewOwnerId = "battery_troll_customize"
    private var previewClientCount = 0
    private var previewActive = false
    private val trollId = savedStateHandle.get<Int>(ARG_TROLL_ID) ?: NO_BATTERY_TROLL_THEME_ID

    /**
     * Enabling the status bar sends the user to system Accessibility settings, which is where this
     * process is most likely to be killed. Everything the user cannot retype from memory — the
     * unapplied draft and the "switch it on when you come back" intent — therefore survives in
     * [SavedStateHandle], exactly like `BatteryEditorViewModel` does with `BatteryDraftCodec`.
     */
    private val restoredDraft = BatteryTrollDraftCodec
        .decode(savedStateHandle[KEY_DRAFT], BatteryTrollDraft(trollId = trollId))
        ?.takeIf { it.trollId == trollId }
    private val _uiState = MutableStateFlow(
        BatteryTrollCustomizeUiState(
            draft = restoredDraft ?: BatteryTrollDraft(trollId = trollId)
        )
    )
    val uiState: StateFlow<BatteryTrollCustomizeUiState> = _uiState.asStateFlow()
    private val _effects = Channel<BatteryTrollCustomizeEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var hasLocalEdits = savedStateHandle.get<Boolean>(KEY_DIRTY) == true &&
        restoredDraft != null
    private var configuredBatteryEnabled = false
    private var enableBatteryAfterAccessibility =
        savedStateHandle.get<Boolean>(KEY_PENDING_ENABLE) == true
    private var latestConfig = BatteryStatusConfig()

    init {
        viewModelScope.launch {
            combine(
                catalogRepository.snapshot,
                batteryCatalogRepository.snapshot,
                settingsRepository.config,
                systemStateMonitor.state
            ) { catalog, batteryCatalog, config, systemState ->
                Inputs(catalog, batteryCatalog, config, systemState)
            }.collect { (catalog, batteryCatalog, config, systemState) ->
                latestConfig = config
                configuredBatteryEnabled = config.enabled
                val applied = draftOf(config)
                val troll = catalog.findTroll(trollId)
                _uiState.update { current ->
                    current.copy(
                        troll = troll,
                        draft = if (hasLocalEdits) current.draft else applied,
                        applied = applied,
                        storedConfig = config,
                        systemState = systemState,
                        // Same resolution the status-bar editor's preview does. An entry the
                        // catalog has not produced yet stays null and the card falls back to
                        // `backgroundColorArgb`; nothing here waits for a download.
                        backgroundPath = batteryCatalog.backgrounds
                            .firstOrNull { it.id == config.backgroundDecorationId }
                            ?.assetPath
                            ?.takeIf(String::isNotBlank),
                        emotionPath = batteryCatalog.emotions
                            .firstOrNull { it.id == config.emotionDecorationId }
                            ?.assetPath
                            ?.takeIf(String::isNotBlank),
                        animation = batteryCatalog.animations
                            .firstOrNull { it.name == config.animationAssetName },
                        isBatteryEnabled = config.enabled &&
                            BatteryAccessibility.isEnabled(context),
                        isAccessibilityEnabled = BatteryAccessibility.isEnabled(context),
                        isLoading = troll == null && catalog.isLoading,
                        catalogError = if (troll == null) catalog.error else null
                    )
                }
                publishPreview()
            }
        }
        viewModelScope.launch { catalogRepository.refresh() }
    }

    /** The unavailable state's only affordance: ask the catalog again. */
    fun retry() {
        viewModelScope.launch { catalogRepository.refresh() }
    }

    fun onModeChange(mode: BatteryTrollMode) = editDraft { it.copy(mode = mode) }

    fun onShowPercentageToggle() = editDraft { it.copy(showPercentage = !it.showPercentage) }

    fun onShowEmojiToggle() = editDraft { it.copy(showEmoji = !it.showEmoji) }

    fun onPercentSizeChange(sizeDp: Float) = editDraft { it.copy(percentSizeDp = sizeDp) }

    fun onRandomArtworkChange(random: Boolean) = editDraft { it.copy(randomArtwork = random) }

    fun onEmojiLevelChange(index: Int) = editDraft {
        it.copy(emojiLevelIndex = index.coerceIn(0, BATTERY_TROLL_LEVEL_COUNT - 1))
    }

    fun onBatteryLevelChange(index: Int) = editDraft {
        it.copy(batteryLevelIndex = index.coerceIn(0, BATTERY_TROLL_LEVEL_COUNT - 1))
    }

    /** Real mode reads the device, so there is nothing for the dialog to change. */
    fun onEditPercentRequest() {
        if (!_uiState.value.isEditEnabled) return
        _uiState.update { it.copy(isEditingFakePercent = true) }
    }

    fun onEditPercentDismiss() {
        _uiState.update { it.copy(isEditingFakePercent = false) }
    }

    fun onEditPercentConfirm(percent: Int) {
        val clamped = percent.coerceIn(
            MIN_BATTERY_TROLL_FAKE_PERCENT,
            MAX_BATTERY_TROLL_FAKE_PERCENT
        )
        editDraft { it.copy(fakePercent = clamped) }
        _uiState.update { it.copy(isEditingFakePercent = false) }
    }

    fun onBatteryToggle() {
        if (!BatteryAccessibility.isEnabled(context)) {
            setPendingBatteryEnable(true)
            emit(BatteryTrollCustomizeEffect.RequestBatteryAccessibility)
            return
        }
        settingsRepository.setEnabled(!_uiState.value.isBatteryEnabled)
    }

    fun refreshAccessibility() {
        val accessibilityEnabled = BatteryAccessibility.isEnabled(context)
        _uiState.update {
            it.copy(
                isBatteryEnabled = configuredBatteryEnabled && accessibilityEnabled,
                isAccessibilityEnabled = accessibilityEnabled
            )
        }
        if (accessibilityEnabled && enableBatteryAfterAccessibility) {
            setPendingBatteryEnable(false)
            settingsRepository.setEnabled(true)
        }
    }

    fun cancelPendingBatteryEnable() {
        setPendingBatteryEnable(false)
    }

    fun onBackRequest() {
        if (_uiState.value.hasUnsavedChanges) {
            _uiState.update { it.copy(isDiscardVisible = true) }
        } else {
            emit(BatteryTrollCustomizeEffect.Close)
        }
    }

    fun onDiscardDismiss() {
        _uiState.update { it.copy(isDiscardVisible = false) }
    }

    fun onDiscardConfirm() {
        hasLocalEdits = false
        clearDraft()
        _uiState.update { it.copy(draft = it.applied, isDiscardVisible = false) }
        emit(BatteryTrollCustomizeEffect.Close)
    }

    /**
     * Apply is also the switch-on: a troll theme nobody can see is not what the button promises,
     * so the config is stored enabled exactly like the status-bar editor does.
     *
     * It is refused outright while the troll is unresolved. Writing `trollThemeId` for artwork the
     * catalog never produced makes `BatteryTrollAssetPolicy` fall back to the normal theme, so the
     * bar would switch on showing something the user never picked; and applying only the
     * non-artwork parts would be the same silent no-op from the user's side of the screen.
     */
    fun apply() {
        if (!_uiState.value.isApplyEnabled) return
        val draft = _uiState.value.draft
        _uiState.update { it.copy(isApplyInProgress = true) }
        settingsRepository.applyConfig(
            latestConfig.copy(
                enabled = true,
                hasApplied = true,
                showPercentage = draft.showPercentage,
                percentSizeDp = draft.percentSizeDp,
                trollMode = draft.mode,
                trollFakePercent = draft.fakePercent.coerceIn(
                    MIN_BATTERY_TROLL_FAKE_PERCENT,
                    MAX_BATTERY_TROLL_FAKE_PERCENT
                ),
                trollThemeId = trollId,
                trollEmojiLevelIndex = draft.emojiLevelIndex,
                trollBatteryLevelIndex = draft.batteryLevelIndex,
                trollRandomArtwork = draft.randomArtwork,
                trollShowEmoji = draft.showEmoji
            )
        )
        hasLocalEdits = false
        clearDraft()
        _uiState.update { it.copy(applied = draft) }
        emit(BatteryTrollCustomizeEffect.ShowApplySuccess)
    }

    fun onApplyCompletionHandled() {
        _uiState.update { it.copy(isApplyInProgress = false) }
    }

    private fun editDraft(transform: (BatteryTrollDraft) -> BatteryTrollDraft) {
        hasLocalEdits = true
        _uiState.update {
            val draft = transform(it.draft)
            savedStateHandle[KEY_DRAFT] = BatteryTrollDraftCodec.encode(draft)
            savedStateHandle[KEY_DIRTY] = true
            it.copy(draft = draft, isApplyInProgress = false)
        }
        publishPreview()
    }

    fun startPreview() {
        previewClientCount += 1
        publishPreview()
    }

    fun stopPreview() {
        previewClientCount = (previewClientCount - 1).coerceAtLeast(0)
        if (previewClientCount > 0) return
        previewActive = false
        previewSession.stop(previewOwnerId)
    }

    /**
     * Mirrors the status-bar editor: the stored activation stays authoritative, so leaving this
     * screen open can never switch a disabled bar on, and a bar the user has off is left alone.
     */
    private fun publishPreview() {
        val state = _uiState.value
        if (previewClientCount == 0 || !configuredBatteryEnabled) {
            if (previewActive) {
                previewActive = false
                previewSession.stop(previewOwnerId)
            }
            return
        }
        val config = state.previewConfig
        if (!previewActive) {
            previewActive = true
            previewSession.start(previewOwnerId, config)
        }
        previewSession.update(previewOwnerId, config, focusedComponent = null)
    }

    override fun onCleared() {
        previewSession.stop(previewOwnerId)
        super.onCleared()
    }

    /** Applied or discarded, the draft is no longer worth restoring after a process death. */
    private fun clearDraft() {
        savedStateHandle[KEY_DRAFT] = null
        savedStateHandle[KEY_DIRTY] = false
    }

    private fun setPendingBatteryEnable(pending: Boolean) {
        enableBatteryAfterAccessibility = pending
        savedStateHandle[KEY_PENDING_ENABLE] = pending
    }

    /**
     * What "already applied" means for *this* troll. A different troll being live means nothing
     * of this one is applied yet, so the baseline is the fresh draft — otherwise Back would ask
     * to discard changes the user never made.
     */
    private fun draftOf(config: BatteryStatusConfig): BatteryTrollDraft {
        val fresh = BatteryTrollDraft(trollId = trollId)
        val isThisTrollLive = config.trollThemeId == trollId &&
            trollId != NO_BATTERY_TROLL_THEME_ID
        return fresh.copy(
            mode = if (isThisTrollLive) config.trollMode else fresh.mode,
            fakePercent = if (isThisTrollLive) config.trollFakePercent else fresh.fakePercent,
            // Percentage size and visibility belong to the status bar as a whole, so they are
            // adopted from the stored config whichever troll is live.
            showPercentage = config.showPercentage,
            percentSizeDp = config.percentSizeDp,
            randomArtwork = if (isThisTrollLive) config.trollRandomArtwork else fresh.randomArtwork,
            showEmoji = if (isThisTrollLive) config.trollShowEmoji else fresh.showEmoji,
            emojiLevelIndex = if (isThisTrollLive) {
                config.trollEmojiLevelIndex.coerceIn(0, BATTERY_TROLL_LEVEL_COUNT - 1)
            } else {
                fresh.emojiLevelIndex
            },
            batteryLevelIndex = if (isThisTrollLive) {
                config.trollBatteryLevelIndex.coerceIn(0, BATTERY_TROLL_LEVEL_COUNT - 1)
            } else {
                fresh.batteryLevelIndex
            }
        )
    }

    private fun emit(effect: BatteryTrollCustomizeEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    /** Four sources feed one state, which is one too many for `Triple`. */
    private data class Inputs(
        val trollCatalog: BatteryTrollCatalogSnapshot,
        val batteryCatalog: BatteryCatalogSnapshot,
        val config: BatteryStatusConfig,
        val systemState: BatteryPreviewSystemState
    )

    private companion object {
        const val ARG_TROLL_ID = "trollId"
        const val KEY_DRAFT = "trollDraft"
        const val KEY_DIRTY = "trollDraftDirty"
        const val KEY_PENDING_ENABLE = "trollPendingBatteryEnable"
    }
}
