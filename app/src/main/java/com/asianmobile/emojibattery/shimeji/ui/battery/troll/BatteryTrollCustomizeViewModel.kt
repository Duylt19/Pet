package com.asianmobile.emojibattery.shimeji.ui.battery.troll

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryAccessibility
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryEditorSystemStateMonitor
import com.asianmobile.emojibattery.shimeji.data.model.BATTERY_TROLL_LEVEL_COUNT
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollMode
import com.asianmobile.emojibattery.shimeji.data.model.MAX_BATTERY_TROLL_FAKE_PERCENT
import com.asianmobile.emojibattery.shimeji.data.model.MIN_BATTERY_TROLL_FAKE_PERCENT
import com.asianmobile.emojibattery.shimeji.data.model.NO_BATTERY_TROLL_THEME_ID
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
 * moment the status bar is allowed to change: a user experimenting with a 999% prank must not
 * leak half-finished states onto their own status bar.
 */
@HiltViewModel
class BatteryTrollCustomizeViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val catalogRepository: BatteryTrollCatalogRepository,
    private val settingsRepository: BatterySettingsRepository,
    private val systemStateMonitor: BatteryEditorSystemStateMonitor
) : ViewModel() {
    private val trollId = savedStateHandle.get<Int>(ARG_TROLL_ID) ?: NO_BATTERY_TROLL_THEME_ID
    private val _uiState = MutableStateFlow(
        BatteryTrollCustomizeUiState(draft = BatteryTrollDraft(trollId = trollId))
    )
    val uiState: StateFlow<BatteryTrollCustomizeUiState> = _uiState.asStateFlow()
    private val _effects = Channel<BatteryTrollCustomizeEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var hasLocalEdits = false
    private var configuredBatteryEnabled = false
    private var enableBatteryAfterAccessibility = false
    private var latestConfig = BatteryStatusConfig()

    init {
        viewModelScope.launch {
            combine(
                catalogRepository.snapshot,
                settingsRepository.config,
                systemStateMonitor.state
            ) { catalog, config, systemState ->
                Triple(catalog, config, systemState)
            }.collect { (catalog, config, systemState) ->
                latestConfig = config
                configuredBatteryEnabled = config.enabled
                val applied = draftOf(config)
                _uiState.update { current ->
                    current.copy(
                        troll = catalog.findTroll(trollId),
                        draft = if (hasLocalEdits) current.draft else applied,
                        applied = applied,
                        realBatteryLevel = systemState.powerState.level,
                        isBatteryEnabled = config.enabled &&
                            BatteryAccessibility.isEnabled(context),
                        isLoading = catalog.isLoading && catalog.findTroll(trollId) == null
                    )
                }
            }
        }
        viewModelScope.launch { catalogRepository.refresh() }
    }

    fun onModeChange(mode: BatteryTrollMode) = editDraft { it.copy(mode = mode) }

    fun onShowPercentageToggle() = editDraft { it.copy(showPercentage = !it.showPercentage) }

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
        hasLocalEdits = true
        _uiState.update {
            it.copy(draft = it.draft.copy(fakePercent = clamped), isEditingFakePercent = false)
        }
    }

    fun onBatteryToggle() {
        if (!BatteryAccessibility.isEnabled(context)) {
            enableBatteryAfterAccessibility = true
            emit(BatteryTrollCustomizeEffect.RequestBatteryAccessibility)
            return
        }
        settingsRepository.setEnabled(!_uiState.value.isBatteryEnabled)
    }

    fun refreshAccessibility() {
        val accessibilityEnabled = BatteryAccessibility.isEnabled(context)
        _uiState.update {
            it.copy(isBatteryEnabled = configuredBatteryEnabled && accessibilityEnabled)
        }
        if (accessibilityEnabled && enableBatteryAfterAccessibility) {
            enableBatteryAfterAccessibility = false
            settingsRepository.setEnabled(true)
        }
    }

    fun cancelPendingBatteryEnable() {
        enableBatteryAfterAccessibility = false
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
        _uiState.update { it.copy(draft = it.applied, isDiscardVisible = false) }
        emit(BatteryTrollCustomizeEffect.Close)
    }

    /**
     * Apply is also the switch-on: a troll theme nobody can see is not what the button promises,
     * so the config is stored enabled exactly like the status-bar editor does.
     */
    fun apply() {
        val draft = _uiState.value.draft
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
                trollRandomArtwork = draft.randomArtwork
            )
        )
        hasLocalEdits = false
        _uiState.update { it.copy(applied = draft) }
    }

    private fun editDraft(transform: (BatteryTrollDraft) -> BatteryTrollDraft) {
        hasLocalEdits = true
        _uiState.update { it.copy(draft = transform(it.draft)) }
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

    private companion object {
        const val ARG_TROLL_ID = "trollId"
    }
}
