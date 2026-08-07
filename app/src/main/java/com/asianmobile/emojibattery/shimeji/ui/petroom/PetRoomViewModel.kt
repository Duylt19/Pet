package com.asianmobile.emojibattery.shimeji.ui.petroom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.data.model.PetRoomCatalogSnapshot
import com.asianmobile.emojibattery.shimeji.data.repository.OwnerPetCatalogRepository
import com.asianmobile.emojibattery.shimeji.data.repository.PetRoomCatalogRepository
import com.asianmobile.emojibattery.shimeji.data.repository.PetRoomRepository
import com.asianmobile.emojibattery.shimeji.data.repository.PetStoreRepository
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPack
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PetRoomViewModel @Inject constructor(
    private val catalogRepository: PetRoomCatalogRepository,
    private val roomRepository: PetRoomRepository,
    private val ownerCatalogRepository: OwnerPetCatalogRepository,
    private val petPackRepository: PetPackRepository,
    private val petStoreRepository: PetStoreRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PetRoomUiState())
    val uiState: StateFlow<PetRoomUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                catalogRepository.snapshot,
                roomRepository.selectedRoomId
            ) { snapshot, selectedRoomId -> snapshot to selectedRoomId }
                .collect { (snapshot, selectedRoomId) -> applyCatalog(snapshot, selectedRoomId) }
        }
        viewModelScope.launch {
            combine(
                ownerCatalogRepository.snapshot,
                petPackRepository.packs,
                petStoreRepository.customNames
            ) { catalog, packs, names ->
                PetRoomRosterPolicy.roster(
                    catalogEntries = catalog.entries.map { entry ->
                        PetRoomRosterSource(
                            petId = entry.id,
                            packKey = entry.installedPackKey,
                            catalogName = entry.name,
                            category = entry.category,
                            thumbnailPath = entry.thumbnailPath
                        )
                    },
                    installedPackKeys = packs.mapTo(mutableSetOf(), PetPack::key),
                    customNames = names
                ) to catalog.isLoading
            }.collect { (roster, isLoading) ->
                _uiState.update { it.copy(pets = roster, isRosterLoading = isLoading) }
            }
        }
    }

    fun selectTab(tab: PetRoomTab) = _uiState.update { state ->
        val (selected, expanded) = PetRoomSheetPolicy.onTabSelected(
            current = state.selectedTab,
            requested = tab,
            isExpanded = state.isSheetExpanded
        )
        state.copy(selectedTab = selected, isSheetExpanded = expanded)
    }

    fun toggleSheet() = _uiState.update { state ->
        state.copy(isSheetExpanded = PetRoomSheetPolicy.toggleExpanded(state.isSheetExpanded))
    }

    fun toggleMusic() = _uiState.update { it.copy(isMusicOn = !it.isMusicOn) }

    fun selectRoom(roomId: Int) {
        viewModelScope.launch { roomRepository.selectRoom(roomId) }
    }

    private suspend fun applyCatalog(snapshot: PetRoomCatalogSnapshot, selectedRoomId: Int) {
        val activeRoom = snapshot.resolveRoom(selectedRoomId)
        val thumbnails = snapshot.rooms.map { room ->
            PetRoomThumbnailUiState(
                id = room.id,
                name = room.name,
                thumbnailPath = catalogRepository.materializeAsset(room.thumbnailPath),
                isSelected = room.id == activeRoom?.id
            )
        }
        val backgroundPath = catalogRepository.materializeAsset(activeRoom?.backgroundPath)
        _uiState.update { state ->
            state.copy(
                backgroundPath = backgroundPath ?: state.backgroundPath,
                rooms = thumbnails,
                isRoomCatalogLoading = snapshot.isLoading,
                roomCatalogFailed = !snapshot.isLoading && snapshot.rooms.isEmpty()
            )
        }
    }
}
