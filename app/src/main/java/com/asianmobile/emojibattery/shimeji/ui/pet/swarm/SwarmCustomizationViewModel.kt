package com.asianmobile.emojibattery.shimeji.ui.pet.swarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.data.model.PetSwarmMovementInsets
import com.asianmobile.emojibattery.shimeji.data.repository.PetSettingsRepository
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPack
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackRepository
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackSource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@HiltViewModel
class SwarmCustomizationViewModel @Inject constructor(
    private val petSettingsRepository: PetSettingsRepository,
    private val petPackRepository: PetPackRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SwarmCustomizationUiState(
            maxCount = petSettingsRepository.performanceBudget.maxSwarmPets
        )
    )
    val uiState: StateFlow<SwarmCustomizationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                petSettingsRepository.preferences,
                petPackRepository.packs
            ) { preferences, _ ->
                val swarm = preferences.swarm
                val pack = swarm.packKey
                    .takeIf(String::isNotBlank)
                    ?.let(petPackRepository::find)
                SwarmCustomizationUiState(
                    name = pack?.manifest?.name.orEmpty(),
                    author = pack?.manifest?.author.orEmpty(),
                    previewImagePath = pack?.previewImagePath(),
                    count = swarm.count,
                    maxCount = petSettingsRepository.performanceBudget.maxSwarmPets,
                    sizePercent = swarm.sizePercent,
                    speedPercent = swarm.speedPercent,
                    randomizeSizeAndSpeed = swarm.randomizeSizeAndSpeed,
                    constrainMovementArea = swarm.constrainMovementArea,
                    movementInsets = swarm.movementInsets
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun updateCount(count: Int) = petSettingsRepository.updateSwarmCount(count)

    fun updateSize(percent: Int) =
        petSettingsRepository.updateSwarmSizePercent(percent)

    fun updateSpeed(percent: Int) =
        petSettingsRepository.updateSwarmSpeedPercent(percent)

    fun setRandomizationEnabled(enabled: Boolean) =
        petSettingsRepository.updateSwarmRandomization(enabled)

    fun setMovementAreaEnabled(enabled: Boolean) =
        petSettingsRepository.updateSwarmMovementAreaEnabled(enabled)

    fun updateMovementInsets(insets: PetSwarmMovementInsets) =
        petSettingsRepository.updateSwarmMovementInsets(insets)

    private fun PetPack.previewImagePath(): String? {
        val installed = source as? PetPackSource.Installed ?: return null
        val firstFrame = manifest.clips.values.firstOrNull()?.frames?.firstOrNull()
            ?: return null
        return File(installed.directoryPath, firstFrame.file).absolutePath
    }
}
