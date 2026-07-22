package com.asianmobile.privatebrower.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.privatebrower.pet.overlay.PetOverlay
import com.asianmobile.privatebrower.pet.overlay.PetOverlayRuntime
import com.asianmobile.privatebrower.pet.overlay.PetOverlayStartResult
import com.asianmobile.privatebrower.pet.pack.PetPackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val petPackRepository: PetPackRepository
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
            petPackRepository.selectedPack.collect { selected ->
                _uiState.update { it.copy(selectedPetName = selected.manifest.name) }
            }
        }
    }

    fun refreshPermissions() {
        val overlayGranted = PetOverlay.canDraw(context)
        _uiState.update {
            it.copy(
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

    private fun emit(effect: HomeEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private fun readUiState() = HomeUiState(
        overlayGranted = PetOverlay.canDraw(context),
        notificationGranted = isNotificationGranted(),
        notificationPermissionRequired = notificationPermissionRequired,
        isPetRunning = PetOverlayRuntime.isRunning.value,
        selectedPetName = petPackRepository.selectedPack.value.manifest.name
    )

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
