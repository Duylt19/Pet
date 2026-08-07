package com.asianmobile.emojibattery.shimeji.ui.petstore

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.data.repository.OwnerPetCatalogRepository
import com.asianmobile.emojibattery.shimeji.data.repository.PetFoodRepository
import com.asianmobile.emojibattery.shimeji.data.repository.PetStoreRepository
import com.asianmobile.emojibattery.shimeji.ads.data.SharedPreferencesUtils
import com.asianmobile.emojibattery.shimeji.pet.overlay.PetOverlay
import com.asianmobile.emojibattery.shimeji.pet.overlay.PetOverlayRuntime
import com.asianmobile.emojibattery.shimeji.pet.overlay.PetOverlayStartResult
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackInstallResult
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackRepository
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

@HiltViewModel
class PetStoreViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val ownerCatalogRepository: OwnerPetCatalogRepository,
    private val petPackRepository: PetPackRepository,
    private val petStoreRepository: PetStoreRepository,
    private val petFoodRepository: PetFoodRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PetStoreUiState())
    val uiState: StateFlow<PetStoreUiState> = _uiState.asStateFlow()
    private val _effects = Channel<PetStoreEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()
    private var startAfterNotification = false

    init {
        viewModelScope.launch {
            combine(
                ownerCatalogRepository.snapshot,
                petPackRepository.packs,
                petStoreRepository.customNames,
                PetOverlayRuntime.isRunning
            ) { catalog, packs, names, isRunning ->
                CatalogState(
                    pets = catalog.entries,
                    installedKeys = packs.mapTo(mutableSetOf()) { it.key },
                    names = names,
                    isLoading = catalog.isLoading,
                    isRunning = isRunning
                )
            }.collect { source ->
                _uiState.update {
                    it.copy(
                        pets = source.pets,
                        installedPackKeys = source.installedKeys,
                        customNames = source.names,
                        isLoading = source.isLoading,
                        isPetRunning = source.isRunning
                    )
                }
            }
        }
        refreshPermissions()
    }

    fun selectTab(tab: PetStoreTab) = _uiState.update { it.copy(selectedTab = tab) }

    fun selectPet(pet: com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogEntry) {
        if (PetStorePolicy.isUnlocked(pet, _uiState.value.installedPackKeys)) {
            _uiState.update {
                it.copy(joinedPetName = it.customNames[pet.id] ?: pet.name)
            }
        } else {
            _uiState.update { it.copy(selectedPet = pet, message = null) }
        }
    }

    fun dismissRewardSheet() = _uiState.update {
        if (it.downloadingPetId == null) it.copy(selectedPet = null, selectedFood = null) else it
    }

    fun requestPetReward() {
        if (_uiState.value.selectedPet == null || _uiState.value.downloadingPetId != null) return
        viewModelScope.launch { _effects.send(PetStoreEffect.ShowRewardedAd) }
    }

    fun requestUnlimited() {
        val pet = _uiState.value.selectedPet ?: return
        if (SharedPreferencesUtils.getIsPremium(context)) {
            downloadPet(pet)
        } else {
            viewModelScope.launch { _effects.send(PetStoreEffect.OpenPremium) }
        }
    }

    fun onRewardResult(canContinue: Boolean) {
        val pet = _uiState.value.selectedPet ?: return
        if (!canContinue) {
            _uiState.update { it.copy(message = "Watch the full video to unlock this pet.") }
            return
        }
        downloadPet(pet)
    }

    private fun downloadPet(pet: com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogEntry) {
        if (_uiState.value.downloadingPetId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(downloadingPetId = pet.id, message = null) }
            when (val result = ownerCatalogRepository.preparePack(pet.id)) {
                is PetPackInstallResult.Installed -> {
                    // Refresh without preferredKey: Store unlock never changes a Mixed/Swarm slot.
                    petPackRepository.refresh()
                    val installedPack = petPackRepository.find(pet.installedPackKey)
                    _uiState.update {
                        it.copy(
                            selectedPet = null,
                            downloadingPetId = null,
                            revealedPet = pet,
                            revealedPetPack = installedPack
                        )
                    }
                }
                is PetPackInstallResult.Rejected -> showDownloadFailure(result.reason)
                is PetPackInstallResult.Failed -> showDownloadFailure(result.reason)
            }
        }
    }

    private fun showDownloadFailure(reason: String) = _uiState.update {
        it.copy(downloadingPetId = null, message = reason.ifBlank { "Unable to download this pet." })
    }

    fun continueAfterReveal() {
        val pet = _uiState.value.revealedPet ?: return
        _uiState.update {
            it.copy(
                revealedPet = null,
                revealedPetPack = null,
                namingPet = pet
            )
        }
    }

    fun savePetName(name: String) {
        val pet = _uiState.value.namingPet ?: return
        val normalized = PetStorePolicy.normalizedName(name, pet.name)
        viewModelScope.launch {
            petStoreRepository.saveCustomName(pet.id, normalized)
            _uiState.update {
                it.copy(namingPet = null, joinedPetName = normalized)
            }
        }
    }

    fun dismissJoinedToast() = _uiState.update { it.copy(joinedPetName = null) }
    fun dismissMessage() = _uiState.update { it.copy(message = null) }

    fun selectFood(food: PetStoreFood) = _uiState.update { it.copy(selectedFood = food) }

    fun acquireFoodPreview() {
        val food = _uiState.value.selectedFood ?: return
        viewModelScope.launch { petFoodRepository.grant(food.id) }
        _uiState.update { it.copy(selectedFood = null, revealedFood = food) }
    }

    fun continueAfterFoodReveal() {
        val food = _uiState.value.revealedFood ?: return
        _uiState.update { it.copy(revealedFood = null, acquiredFood = food) }
    }

    fun dismissFoodToast() = _uiState.update { it.copy(acquiredFood = null) }

    fun refreshPermissions() {
        val overlayGranted = PetOverlay.canDraw(context)
        _uiState.update {
            it.copy(
                overlayGranted = overlayGranted,
                notificationGranted = isNotificationGranted()
            )
        }
        if (!overlayGranted && PetOverlayRuntime.isRunning.value) PetOverlay.stop(context)
        val selected = _uiState.value.selectedPet
        if (selected != null && _uiState.value.downloadingPetId == null &&
            SharedPreferencesUtils.getIsPremium(context)
        ) {
            downloadPet(selected)
        }
    }

    fun togglePetOverlay() {
        if (PetOverlayRuntime.isRunning.value) {
            PetOverlay.stop(context)
            return
        }
        if (!PetOverlay.canDraw(context)) {
            viewModelScope.launch { _effects.send(PetStoreEffect.OpenOverlaySettings) }
            return
        }
        if (!isNotificationGranted()) {
            startAfterNotification = true
            viewModelScope.launch { _effects.send(PetStoreEffect.RequestNotificationPermission) }
            return
        }
        startOverlay()
    }

    fun onNotificationPermissionResult() {
        refreshPermissions()
        if (startAfterNotification) {
            startAfterNotification = false
            startOverlay()
        }
    }

    private fun startOverlay() {
        when (PetOverlay.start(context)) {
            PetOverlayStartResult.START_REQUESTED -> Unit
            PetOverlayStartResult.PERMISSION_REQUIRED -> viewModelScope.launch {
                _effects.send(PetStoreEffect.OpenOverlaySettings)
            }
            PetOverlayStartResult.FAILED -> _uiState.update {
                it.copy(message = "Unable to start your pet. Please try again.")
            }
        }
    }

    private fun isNotificationGranted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private data class CatalogState(
        val pets: List<com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogEntry>,
        val installedKeys: Set<String>,
        val names: Map<Int, String>,
        val isLoading: Boolean,
        val isRunning: Boolean
    )
}
