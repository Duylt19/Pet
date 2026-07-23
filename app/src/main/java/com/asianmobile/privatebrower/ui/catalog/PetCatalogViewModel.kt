package com.asianmobile.privatebrower.ui.catalog

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.privatebrower.data.model.MAX_PET_SLOTS
import com.asianmobile.privatebrower.data.repository.OwnerPetCatalogRepository
import com.asianmobile.privatebrower.pet.pack.PetPackInstallResult
import com.asianmobile.privatebrower.pet.pack.PetPackInstaller
import com.asianmobile.privatebrower.pet.pack.PetPackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PetCatalogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PetPackRepository,
    private val installer: PetPackInstaller,
    private val ownerCatalogRepository: OwnerPetCatalogRepository
) : ViewModel() {
    private val targetSlotIndex = (savedStateHandle.get<Int>("slotIndex") ?: 0)
        .coerceIn(0, MAX_PET_SLOTS - 1)
    private val _uiState = MutableStateFlow(
        PetCatalogUiState(
            packs = repository.packs.value,
            selectedKey = repository.selectedPackForSlot(targetSlotIndex).key,
            targetSlotIndex = targetSlotIndex,
            localRootPath = ownerCatalogRepository.snapshot.value.localRootPath
        )
    )
    val uiState: StateFlow<PetCatalogUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                ownerCatalogRepository.snapshot,
                repository.packs,
                repository.selectedPacks
            ) { catalog, packs, selected -> Triple(catalog, packs, selected) }
                .collect { (catalog, packs, selected) ->
                    _uiState.update { current ->
                        current.copy(
                            packs = packs,
                            selectedKey = selected.getOrNull(targetSlotIndex)?.key
                                ?: selected.firstOrNull()?.key.orEmpty(),
                            pets = catalog.entries,
                            visiblePets = PetCatalogFilter.apply(
                                catalog.entries,
                                current.searchQuery,
                                current.selectedCategory
                            ),
                            categories = PetCatalogFilter.categories(catalog.entries),
                            localRootPath = catalog.localRootPath,
                            isLoading = catalog.isLoading,
                            catalogError = catalog.error
                        )
                    }
                }
        }
    }

    fun install(uri: Uri) {
        if (_uiState.value.isInstalling) return
        viewModelScope.launch {
            _uiState.update { it.copy(isInstalling = true, message = null) }
            when (val result = installer.install(uri)) {
                is PetPackInstallResult.Installed -> {
                    repository.refresh(
                        preferredKey = result.pack.key,
                        preferredSlotIndex = targetSlotIndex
                    )
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

    fun select(key: String) {
        if (repository.select(key, targetSlotIndex)) {
            val name = repository.find(key)?.manifest?.name ?: return
            _uiState.update { it.copy(message = PetCatalogMessage.Selected(name)) }
        }
    }

    fun setOwnerPet(petId: Int) {
        if (_uiState.value.preparingPetId != null) return
        val name = _uiState.value.pets.firstOrNull { it.id == petId }?.name ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(preparingPetId = petId, message = null) }
            when (val result = ownerCatalogRepository.preparePack(petId)) {
                is PetPackInstallResult.Installed -> {
                    repository.refresh(
                        preferredKey = result.pack.key,
                        preferredSlotIndex = targetSlotIndex
                    )
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
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, catalogError = null) }
            ownerCatalogRepository.refresh()
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
