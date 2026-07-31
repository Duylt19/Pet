package com.asianmobile.emojibattery.shimeji.ui.catalog

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.ads.data.SharedPreferencesUtils
import com.asianmobile.emojibattery.shimeji.data.model.MAX_PET_SLOTS
import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogSnapshot
import com.asianmobile.emojibattery.shimeji.data.model.PetPreferences
import com.asianmobile.emojibattery.shimeji.data.repository.OwnerPetCatalogRepository
import com.asianmobile.emojibattery.shimeji.data.repository.PetSettingsRepository
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPack
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackInstallResult
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackInstaller
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
class PetCatalogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @param:ApplicationContext private val context: Context,
    private val repository: PetPackRepository,
    private val installer: PetPackInstaller,
    private val ownerCatalogRepository: OwnerPetCatalogRepository,
    private val petSettingsRepository: PetSettingsRepository
) : ViewModel() {
    private val target = savedStateHandle.get<String>("target")
        ?.let { encoded -> PetCatalogTarget.entries.firstOrNull { it.name == encoded } }
        ?: PetCatalogTarget.MIXED
    private val targetSlotIndex = (savedStateHandle.get<Int>("slotIndex") ?: 0)
        .coerceIn(0, MAX_PET_SLOTS - 1)
    private val initialPreferences = petSettingsRepository.preferences.value
    private val _uiState = MutableStateFlow(
        PetCatalogUiState(
            packs = repository.packs.value,
            selectedKey = selectedKey(initialPreferences, repository.selectedPacks.value),
            target = target,
            targetSlotIndex = targetSlotIndex,
            requiresMixedSlotReward = requiresMixedSlotReward(initialPreferences),
            localRootPath = ownerCatalogRepository.snapshot.value.localRootPath
        )
    )
    val uiState: StateFlow<PetCatalogUiState> = _uiState.asStateFlow()
    private val _effects = Channel<PetCatalogEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(
                ownerCatalogRepository.snapshot,
                repository.packs,
                repository.selectedPacks,
                petSettingsRepository.preferences
            ) { catalog, packs, selected, preferences ->
                CatalogSources(
                    catalog = catalog,
                    packs = packs,
                    selected = selected,
                    preferences = preferences
                )
            }.collect { sources ->
                _uiState.update { current ->
                    current.copy(
                        packs = sources.packs,
                        selectedKey = selectedKey(
                            sources.preferences,
                            sources.selected
                        ),
                        requiresMixedSlotReward =
                            requiresMixedSlotReward(sources.preferences),
                        pets = sources.catalog.entries,
                        visiblePets = PetCatalogFilter.apply(
                            sources.catalog.entries,
                            current.searchQuery,
                            current.selectedCategory
                        ),
                        categories = PetCatalogFilter.categories(sources.catalog.entries),
                        localRootPath = sources.catalog.localRootPath,
                        isLoading = sources.catalog.isLoading,
                        catalogError = sources.catalog.error
                    )
                }
            }
        }
    }

    fun install(uri: Uri) {
        if (_uiState.value.isInstalling || _uiState.value.requiresMixedSlotReward) return
        viewModelScope.launch {
            _uiState.update { it.copy(isInstalling = true, message = null) }
            when (val result = installer.install(uri)) {
                is PetPackInstallResult.Installed -> {
                    selectInstalledPack(result.pack.key)
                    _uiState.update {
                        it.copy(
                            isInstalling = false,
                            message = PetCatalogMessage.Installed(result.pack.manifest.name)
                        )
                    }
                }
                is PetPackInstallResult.Rejected -> _uiState.update {
                    it.copy(
                        isInstalling = false,
                        message = PetCatalogMessage.Rejected(result.reason)
                    )
                }
                is PetPackInstallResult.Failed -> _uiState.update {
                    it.copy(
                        isInstalling = false,
                        message = PetCatalogMessage.Failed(result.reason)
                    )
                }
            }
        }
    }

    fun select(key: String): Boolean {
        if (_uiState.value.requiresMixedSlotReward || !isTargetSlotSequential()) return false
        val selected = when (target) {
            PetCatalogTarget.MIXED -> repository.select(key, targetSlotIndex)
            PetCatalogTarget.SWARM -> repository.find(key)?.let {
                petSettingsRepository.updateSwarmPack(it.key)
                true
            } ?: false
        }
        if (selected) {
            if (target == PetCatalogTarget.MIXED) activateTargetSlot()
            val name = repository.find(key)?.manifest?.name ?: return true
            _uiState.update { it.copy(message = PetCatalogMessage.Selected(name)) }
            return true
        }
        return false
    }

    fun setOwnerPet(petId: Int) {
        if (_uiState.value.preparingPetId != null ||
            _uiState.value.requiresMixedSlotReward ||
            !isTargetSlotSequential()
        ) {
            return
        }
        val name = _uiState.value.pets.firstOrNull { it.id == petId }?.name ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(preparingPetId = petId, message = null) }
            when (val result = ownerCatalogRepository.preparePack(petId)) {
                is PetPackInstallResult.Installed -> {
                    selectInstalledPack(result.pack.key)
                    _uiState.update {
                        it.copy(
                            preparingPetId = null,
                            message = PetCatalogMessage.Selected(name)
                        )
                    }
                }
                is PetPackInstallResult.Rejected -> _uiState.update {
                    it.copy(
                        preparingPetId = null,
                        message = PetCatalogMessage.Rejected(result.reason)
                    )
                }
                is PetPackInstallResult.Failed -> _uiState.update {
                    it.copy(
                        preparingPetId = null,
                        message = PetCatalogMessage.Failed(result.reason)
                    )
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { current ->
            current.copy(
                searchQuery = query,
                visiblePets = PetCatalogFilter.apply(
                    current.pets,
                    query,
                    current.selectedCategory
                )
            )
        }
    }

    fun selectCategory(category: String?) {
        _uiState.update { current ->
            current.copy(
                selectedCategory = category,
                visiblePets = PetCatalogFilter.apply(
                    current.pets,
                    current.searchQuery,
                    category
                )
            )
        }
    }

    fun refreshCatalog() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, catalogError = null) }
            try {
                ownerCatalogRepository.refresh(force = true)
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun requestMixedSlotUnlock() {
        if (!_uiState.value.requiresMixedSlotReward) return
        val preferences = petSettingsRepository.preferences.value
        if (!MixedSlotUnlockPolicy.canUnlockWithReward(
                slotIndex = targetSlotIndex,
                petCount = preferences.petCount,
                rewardUnlockedSlotCount = preferences.mixedRewardUnlockedSlotCount
            )
        ) {
            _uiState.update { it.copy(message = PetCatalogMessage.PreviousSlotRequired) }
            return
        }
        emit(PetCatalogEffect.ShowMixedSlotRewardedAd)
    }

    fun onMixedSlotRewardResult(canContinue: Boolean) {
        if (!canContinue) {
            _uiState.update { it.copy(message = PetCatalogMessage.RewardNotEarned) }
            return
        }
        val preferences = petSettingsRepository.preferences.value
        if (!MixedSlotUnlockPolicy.canUnlockWithReward(
                slotIndex = targetSlotIndex,
                petCount = preferences.petCount,
                rewardUnlockedSlotCount = preferences.mixedRewardUnlockedSlotCount
            )
        ) {
            _uiState.update { it.copy(message = PetCatalogMessage.PreviousSlotRequired) }
            return
        }
        petSettingsRepository.unlockMixedSlotByReward(targetSlotIndex)
        _uiState.update {
            it.copy(requiresMixedSlotReward = false, message = null)
        }
    }

    fun refreshEntitlement() {
        _uiState.update {
            it.copy(
                requiresMixedSlotReward = requiresMixedSlotReward(
                    petSettingsRepository.preferences.value
                )
            )
        }
    }

    private fun activateTargetSlot() {
        if (target != PetCatalogTarget.MIXED) return
        val currentCount = petSettingsRepository.preferences.value.petCount
        if (targetSlotIndex == currentCount) {
            petSettingsRepository.updatePetCount(targetSlotIndex + 1)
        }
    }

    private fun selectInstalledPack(key: String) {
        when (target) {
            PetCatalogTarget.MIXED -> repository.refresh(
                preferredKey = key,
                preferredSlotIndex = targetSlotIndex
            )
            PetCatalogTarget.SWARM -> {
                repository.refresh()
                petSettingsRepository.updateSwarmPack(key)
            }
        }
        activateTargetSlot()
    }

    private fun selectedKey(
        preferences: PetPreferences,
        selected: List<PetPack>
    ): String = when (target) {
        PetCatalogTarget.MIXED -> if (targetSlotIndex < preferences.petCount) {
            selected.getOrNull(targetSlotIndex)?.key
                ?: selected.firstOrNull()?.key.orEmpty()
        } else {
            ""
        }
        PetCatalogTarget.SWARM -> preferences.swarm.packKey
    }

    private fun requiresMixedSlotReward(preferences: PetPreferences): Boolean =
        MixedSlotUnlockPolicy.requiresReward(
            target = target,
            slotIndex = targetSlotIndex,
            petCount = preferences.petCount,
            rewardUnlockedSlotCount = preferences.mixedRewardUnlockedSlotCount,
            isPremium = SharedPreferencesUtils.getIsPremium(context)
        )

    private fun isTargetSlotSequential(): Boolean {
        if (target != PetCatalogTarget.MIXED) return true
        return targetSlotIndex <= petSettingsRepository.preferences.value.petCount
    }

    private fun emit(effect: PetCatalogEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private data class CatalogSources(
        val catalog: OwnerPetCatalogSnapshot,
        val packs: List<PetPack>,
        val selected: List<PetPack>,
        val preferences: PetPreferences
    )
}
