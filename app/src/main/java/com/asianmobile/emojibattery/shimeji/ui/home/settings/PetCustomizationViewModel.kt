package com.asianmobile.emojibattery.shimeji.ui.home.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.data.model.MAX_PET_SLOTS
import com.asianmobile.emojibattery.shimeji.data.repository.PetSettingsRepository
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@HiltViewModel
class PetCustomizationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val petSettingsRepository: PetSettingsRepository,
    private val petPackRepository: PetPackRepository
) : ViewModel() {
    private val slotIndex = (savedStateHandle.get<Int>("slotIndex") ?: 0)
        .coerceIn(0, MAX_PET_SLOTS - 1)
    private val _uiState = MutableStateFlow(PetCustomizationUiState(slotIndex = slotIndex))
    val uiState: StateFlow<PetCustomizationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                petSettingsRepository.preferences,
                petPackRepository.selectedPacks
            ) { preferences, selectedPacks ->
                val slot = preferences.slot(slotIndex)
                val pack = selectedPacks.getOrNull(slotIndex)
                    ?: selectedPacks.firstOrNull()
                PetCustomizationUiState(
                    slotIndex = slotIndex,
                    petCount = preferences.petCount,
                    name = pack?.manifest?.name.orEmpty(),
                    author = pack?.manifest?.author.orEmpty(),
                    previewImagePath = pack?.previewImagePath(),
                    sizePercent = slot.sizePercent,
                    speedPercent = slot.speedPercent,
                    messagesEnabled = slot.messagesEnabled,
                    customMessages = slot.customMessages,
                    interactionEnabled = slot.interactionEnabled
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun updateSize(percent: Int) =
        petSettingsRepository.updateSizePercent(slotIndex, percent)

    fun updateSpeed(percent: Int) =
        petSettingsRepository.updateSpeedPercent(slotIndex, percent)

    fun setMessagesEnabled(enabled: Boolean) =
        petSettingsRepository.updateMessagesEnabled(slotIndex, enabled)

    fun setCustomMessages(messages: List<String>) =
        petSettingsRepository.updateCustomMessages(slotIndex, messages)

    fun setInteractionEnabled(enabled: Boolean) =
        petSettingsRepository.updateInteractionEnabled(slotIndex, enabled)

    fun resetPosition() = petSettingsRepository.resetLastPosition(slotIndex)

    fun removePet(): Boolean {
        if (!_uiState.value.canRemove) return false
        petSettingsRepository.removePet(slotIndex)
        return true
    }
}
