package com.asianmobile.emojibattery.shimeji.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ads.data.SharedPreferencesUtils
import com.asianmobile.emojibattery.shimeji.data.model.PetDisplayMode
import com.asianmobile.emojibattery.shimeji.data.model.PetPreferences
import com.asianmobile.emojibattery.shimeji.data.repository.PetSettingsRepository
import com.asianmobile.emojibattery.shimeji.pet.overlay.PetOverlay
import com.asianmobile.emojibattery.shimeji.pet.overlay.PetOverlayRuntime
import com.asianmobile.emojibattery.shimeji.pet.overlay.PetOverlayStartResult
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPack
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackRepository
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val petPackRepository: PetPackRepository,
    private val petSettingsRepository: PetSettingsRepository
) : ViewModel() {
    private val notificationPermissionRequired =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    private val _uiState = MutableStateFlow(readUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()
    private var startAfterNotificationResult = false

    init {
        viewModelScope.launch {
            PetOverlayRuntime.isRunning.collect { isRunning ->
                _uiState.update {
                    it.copy(
                        isPetRunning = isRunning,
                        isStartingPet = if (isRunning) false else it.isStartingPet
                    )
                }
            }
        }
        viewModelScope.launch {
            petPackRepository.selectedPacks.collect {
                refreshProfileState()
            }
        }
        viewModelScope.launch {
            petPackRepository.packs.collect {
                refreshProfileState()
            }
        }
        viewModelScope.launch {
            petSettingsRepository.preferences.collect {
                refreshProfileState()
            }
        }
    }

    fun refreshPermissions() {
        val overlayGranted = PetOverlay.canDraw(context)
        _uiState.update {
            profileState(
                current = it,
                preferences = petSettingsRepository.preferences.value
            ).copy(
                overlayGranted = overlayGranted,
                notificationGranted = isNotificationGranted()
            )
        }
        if (!overlayGranted && PetOverlayRuntime.isRunning.value) {
            PetOverlay.stop(context)
        }
    }

    fun onPetButtonClicked() {
        val state = _uiState.value
        if (state.isStartingPet) return
        if (!state.hasRunnableSelection) {
            when {
                state.displayMode == PetDisplayMode.SWARM && !state.swarmUnlocked ->
                    requestSwarmUnlock()
                state.displayMode == PetDisplayMode.SWARM ->
                    showMessage(HomeMessage.SELECT_SWARM_PET)
                else -> showMessage(HomeMessage.KEEP_ONE_MIXED_PET_VISIBLE)
            }
            return
        }
        when (
            HomePetPolicy.nextCommand(
                overlayGranted = state.overlayGranted,
                notificationPermissionRequired = state.notificationPermissionRequired,
                notificationGranted = state.notificationGranted,
                isPetRunning = state.isPetRunning
            )
        ) {
            HomePetCommand.OPEN_OVERLAY_SETTINGS -> emit(HomeEffect.OpenOverlaySettings)
            HomePetCommand.REQUEST_NOTIFICATION_PERMISSION -> {
                startAfterNotificationResult = true
                emit(HomeEffect.RequestNotificationPermission)
            }
            HomePetCommand.START -> startPet()
            HomePetCommand.STOP -> PetOverlay.stop(context)
        }
    }

    fun selectMode(mode: PetDisplayMode) {
        if (_uiState.value.displayMode == mode) return
        val canRunTarget = when (mode) {
            PetDisplayMode.MIXED -> _uiState.value.mixedPets.any { it.isEnabled }
            PetDisplayMode.SWARM ->
                _uiState.value.swarmUnlocked && _uiState.value.swarmPackName != null
        }
        if (PetOverlayRuntime.isRunning.value && !canRunTarget) {
            PetOverlay.stop(context)
        }
        petSettingsRepository.updateDisplayMode(mode)
        _uiState.update { it.copy(displayMode = mode, message = null) }
    }

    fun toggleMixedPet(slotIndex: Int) {
        val pet = _uiState.value.mixedPets.firstOrNull { it.slotIndex == slotIndex } ?: return
        if (pet.isEnabled && _uiState.value.mixedPets.count { it.isEnabled } <= 1) {
            showMessage(HomeMessage.KEEP_ONE_MIXED_PET_VISIBLE)
            return
        }
        petSettingsRepository.updateSlotEnabled(slotIndex, !pet.isEnabled)
    }

    fun updateSwarmCount(count: Int) {
        petSettingsRepository.updateSwarmCount(count)
    }

    fun clearSwarmPet() {
        if (_uiState.value.displayMode == PetDisplayMode.SWARM &&
            PetOverlayRuntime.isRunning.value
        ) {
            PetOverlay.stop(context)
        }
        petSettingsRepository.clearSwarmPack()
    }

    fun requestSwarmUnlock() {
        if (_uiState.value.swarmUnlocked) return
        emit(HomeEffect.ShowSwarmRewardedAd)
    }

    fun onSwarmRewardResult(rewardEarned: Boolean) {
        if (rewardEarned) {
            petSettingsRepository.unlockSwarmByReward()
            _uiState.update { it.copy(swarmUnlocked = true, message = null) }
        } else {
            showMessage(HomeMessage.SWARM_REWARD_NOT_AVAILABLE)
        }
    }

    fun onNotificationPermissionResult() {
        _uiState.update { it.copy(notificationGranted = isNotificationGranted()) }
        if (startAfterNotificationResult) {
            startAfterNotificationResult = false
            startPet()
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun startPet() {
        _uiState.update { it.copy(isStartingPet = true, message = null) }
        when (PetOverlay.start(context)) {
            PetOverlayStartResult.START_REQUESTED -> {
                viewModelScope.launch {
                    delay(START_CONFIRMATION_TIMEOUT_MILLIS)
                    if (!PetOverlayRuntime.isRunning.value && _uiState.value.isStartingPet) {
                        _uiState.update {
                            it.copy(
                                isStartingPet = false,
                                message = HomeMessage.PET_START_FAILED
                            )
                        }
                    }
                }
            }
            PetOverlayStartResult.PERMISSION_REQUIRED -> {
                _uiState.update { it.copy(isStartingPet = false, overlayGranted = false) }
                emit(HomeEffect.OpenOverlaySettings)
            }
            PetOverlayStartResult.FAILED -> {
                _uiState.update {
                    it.copy(isStartingPet = false, message = HomeMessage.PET_START_FAILED)
                }
            }
        }
    }

    private fun refreshProfileState() {
        _uiState.update { current ->
            profileState(
                current = current,
                preferences = petSettingsRepository.preferences.value
            )
        }
    }

    private fun profileState(
        current: HomeUiState,
        preferences: PetPreferences
    ): HomeUiState {
        val selected = petPackRepository.selectedPacks.value
        val swarmPack = preferences.swarm.packKey
            .takeIf(String::isNotBlank)
            ?.let(petPackRepository::find)
        val isPremium = SharedPreferencesUtils.getIsPremium(context)
        return current.copy(
            displayMode = preferences.displayMode,
            mixedPets = List(preferences.petCount) { slotIndex ->
                val pack = selected.getOrNull(slotIndex)
                    ?: selected.firstOrNull()
                HomeMixedPetUiState(
                    slotIndex = slotIndex,
                    name = pack?.manifest?.name
                        ?: context.getString(R.string.home_pet_default_name),
                    previewPath = pack?.previewPath(),
                    isEnabled = preferences.slot(slotIndex).isEnabled
                )
            },
            petCount = preferences.petCount,
            maxMixedPets = petSettingsRepository.performanceBudget.maxPets,
            mixedUnlockedSlotCount = if (isPremium) {
                petSettingsRepository.performanceBudget.maxPets
            } else {
                preferences.mixedRewardUnlockedSlotCount
            },
            swarmUnlocked = isPremium || preferences.swarm.unlockedByReward,
            isPremium = isPremium,
            swarmPackName = swarmPack?.manifest?.name,
            swarmPreviewPath = swarmPack?.previewPath(),
            swarmCount = preferences.swarm.count,
            maxSwarmPets = petSettingsRepository.performanceBudget.maxSwarmPets
        )
    }

    private fun readUiState(): HomeUiState = profileState(
        current = HomeUiState(
            overlayGranted = PetOverlay.canDraw(context),
            notificationGranted = isNotificationGranted(),
            notificationPermissionRequired = notificationPermissionRequired,
            isPetRunning = PetOverlayRuntime.isRunning.value
        ),
        preferences = petSettingsRepository.preferences.value
    )

    private fun PetPack.previewPath(): String? {
        val installed = source as? PetPackSource.Installed ?: return null
        val firstFrame = manifest.clips.values.firstOrNull()?.frames?.firstOrNull()
            ?: return null
        return File(installed.directoryPath, firstFrame.file).absolutePath
    }

    private fun showMessage(message: HomeMessage) {
        _uiState.update { it.copy(message = message) }
    }

    private fun emit(effect: HomeEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private fun isNotificationGranted(): Boolean =
        !notificationPermissionRequired ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val START_CONFIRMATION_TIMEOUT_MILLIS = 1_500L
    }
}
