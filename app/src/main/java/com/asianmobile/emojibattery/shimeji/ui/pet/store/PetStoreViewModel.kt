package com.asianmobile.emojibattery.shimeji.ui.pet.store

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.data.repository.OwnerPetCatalogRepository
import com.asianmobile.emojibattery.shimeji.data.repository.PetFoodRepository
import com.asianmobile.emojibattery.shimeji.data.repository.PetSettingsRepository
import com.asianmobile.emojibattery.shimeji.data.repository.PetStoreRepository
import com.asianmobile.emojibattery.shimeji.ads.data.SharedPreferencesUtils
import com.asianmobile.emojibattery.shimeji.pet.overlay.PetOverlay
import com.asianmobile.emojibattery.shimeji.pet.overlay.PetOverlayRuntime
import com.asianmobile.emojibattery.shimeji.pet.overlay.PetOverlayStartResult
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackInstallResult
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackRepository
import com.asianmobile.emojibattery.shimeji.ui.pet.PetFamilyCapacityPolicy
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
    private val petFoodRepository: PetFoodRepository,
    private val petSettingsRepository: PetSettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PetStoreUiState())
    val uiState: StateFlow<PetStoreUiState> = _uiState.asStateFlow()
    private val _effects = Channel<PetStoreEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

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
                        selectedCategory = PetStorePolicy.selectedCategory(
                            pets = source.pets,
                            requestedCategory = it.selectedCategory
                        ),
                        isLoading = source.isLoading,
                        isPetRunning = source.isRunning
                    )
                }
            }
        }
        refreshPermissions()
    }

    fun selectTab(tab: PetStoreTab) = _uiState.update { it.copy(selectedTab = tab) }

    fun selectCategory(category: String) = _uiState.update { state ->
        val selected = PetStorePolicy.selectedCategory(state.pets, category)
        if (selected == null) state else state.copy(selectedCategory = selected)
    }

    fun selectPet(pet: com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogEntry) {
        if (PetStorePolicy.isUnlocked(pet, _uiState.value.installedPackKeys)) {
            _uiState.update {
                it.copy(joinedPetName = it.customNames[pet.id] ?: pet.name)
            }
        } else if (hasReachedPetCapacity()) {
            _uiState.update {
                it.copy(
                    selectedPet = null,
                    isPetCapacityDialogVisible = true,
                    message = null
                )
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
        if (!PetStorePolicy.isUnlocked(pet, _uiState.value.installedPackKeys) &&
            hasReachedPetCapacity()
        ) {
            _uiState.update {
                it.copy(selectedPet = null, isPetCapacityDialogVisible = true, message = null)
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(downloadingPetId = pet.id, message = null) }
            when (val result = ownerCatalogRepository.preparePack(pet.id)) {
                is PetPackInstallResult.Installed -> {
                    petPackRepository.refresh()
                    petSettingsRepository.enablePackInFirstFreeSlot(pet.installedPackKey)
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
            when (
                PetStorePolicy.activationAfterUnlock(
                    overlayGranted = PetOverlay.canDraw(context),
                    notificationGranted = isNotificationGranted()
                )
            ) {
                PetUnlockActivation.REQUEST_OVERLAY ->
                    _effects.send(PetStoreEffect.OpenOverlaySettings)

                PetUnlockActivation.REQUEST_REMAINING_PERMISSIONS ->
                    _effects.send(PetStoreEffect.OpenGrantPermissions)

                PetUnlockActivation.START_PET -> startOverlay()
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
        if (_uiState.value.isLoading) return
        val blocker = PetStorePolicy.startBlocker(
            ownedPetCount = PetStorePolicy.ownedPetCount(
                pets = _uiState.value.pets,
                installedPackKeys = _uiState.value.installedPackKeys
            ),
            activePetCount = petSettingsRepository.preferences.value.runtimePetCount
        )
        if (blocker != null) {
            _uiState.update { it.copy(petStartBlocker = blocker) }
            return
        }
        if (!PetOverlay.canDraw(context)) {
            viewModelScope.launch { _effects.send(PetStoreEffect.OpenOverlaySettings) }
            return
        }
        if (!isNotificationGranted()) {
            viewModelScope.launch { _effects.send(PetStoreEffect.OpenGrantPermissions) }
            return
        }
        startOverlay()
    }

    fun dismissPetStartBlocker() = _uiState.update { it.copy(petStartBlocker = null) }

    fun dismissPetCapacityDialog() =
        _uiState.update { it.copy(isPetCapacityDialogVisible = false) }

    private fun hasReachedPetCapacity(): Boolean = PetFamilyCapacityPolicy.isFull(
        PetStorePolicy.ownedPetCount(
            pets = _uiState.value.pets,
            installedPackKeys = _uiState.value.installedPackKeys
        )
    )

    private fun startOverlay() {
        if (petSettingsRepository.preferences.value.runtimePetCount == 0) return
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
