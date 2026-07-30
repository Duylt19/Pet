package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.data.model.BUILT_IN_BATTERY_THEME
import com.asianmobile.emojibattery.shimeji.data.model.BUILT_IN_BATTERY_THEME_ID
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.repository.BatteryCatalogRepository
import com.asianmobile.emojibattery.shimeji.data.repository.BatterySettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class BatteryEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalogRepository: BatteryCatalogRepository,
    private val settingsRepository: BatterySettingsRepository
) : ViewModel() {
    private val themeId = savedStateHandle.get<Int>("themeId") ?: BUILT_IN_BATTERY_THEME_ID
    private var hasLocalEdits = false
    private val _uiState = MutableStateFlow(BatteryEditorUiState())
    val uiState: StateFlow<BatteryEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(catalogRepository.snapshot, settingsRepository.config) { catalog, stored ->
                val theme = catalog.themes.firstOrNull { it.id == themeId }
                val draft = if (hasLocalEdits) _uiState.value.config else stored
                BatteryEditorUiState(
                    theme = theme ?: BUILT_IN_BATTERY_THEME,
                    config = draft.copy(
                        selectedThemeId = theme?.id ?: BUILT_IN_BATTERY_THEME_ID
                    ),
                    backgrounds = catalog.backgrounds,
                    emotions = catalog.emotions,
                    animations = catalog.animations,
                    isThemeAvailable = theme?.assetsReady == true || themeId == BUILT_IN_BATTERY_THEME_ID
                )
            }.collect { state -> _uiState.value = state }
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

    fun apply() {
        val state = _uiState.value
        if (!state.isThemeAvailable) return
        settingsRepository.applyConfig(
            state.config.copy(enabled = true, selectedThemeId = state.theme.id)
        )
        hasLocalEdits = false
    }

    fun disable() {
        hasLocalEdits = false
        settingsRepository.setEnabled(false)
    }

    private fun update(transform: BatteryStatusConfig.() -> BatteryStatusConfig) {
        hasLocalEdits = true
        _uiState.update { it.copy(config = transform(it.config)) }
    }
}
