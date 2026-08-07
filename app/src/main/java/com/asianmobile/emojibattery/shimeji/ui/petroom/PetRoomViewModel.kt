package com.asianmobile.emojibattery.shimeji.ui.petroom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.data.model.PetPreferences
import com.asianmobile.emojibattery.shimeji.data.model.PetRoomCatalogSnapshot
import com.asianmobile.emojibattery.shimeji.data.repository.OwnerPetCatalogRepository
import com.asianmobile.emojibattery.shimeji.data.repository.PetCareRepository
import com.asianmobile.emojibattery.shimeji.data.repository.PetEnergyRecord
import com.asianmobile.emojibattery.shimeji.data.repository.PetFoodRepository
import com.asianmobile.emojibattery.shimeji.data.repository.PetRoomCatalogRepository
import com.asianmobile.emojibattery.shimeji.data.repository.PetRoomRepository
import com.asianmobile.emojibattery.shimeji.data.repository.PetSettingsRepository
import com.asianmobile.emojibattery.shimeji.data.repository.PetStoreRepository
import com.asianmobile.emojibattery.shimeji.pet.care.PetEnergyPolicy
import com.asianmobile.emojibattery.shimeji.pet.engine.PetBehaviorProfiles
import com.asianmobile.emojibattery.shimeji.pet.engine.PetEngineConfig
import com.asianmobile.emojibattery.shimeji.pet.pack.PetBitmapCache
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPack
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackRepository
import com.asianmobile.emojibattery.shimeji.pet.pack.toEngineClips
import com.asianmobile.emojibattery.shimeji.pet.pack.toEngineSupportedActions
import com.asianmobile.emojibattery.shimeji.pet.room.PetRoomMusicPlayer
import com.asianmobile.emojibattery.shimeji.ui.petstore.PET_FOOD_CATALOG
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class PetRoomViewModel @Inject constructor(
    private val catalogRepository: PetRoomCatalogRepository,
    private val roomRepository: PetRoomRepository,
    private val ownerCatalogRepository: OwnerPetCatalogRepository,
    private val petPackRepository: PetPackRepository,
    private val petStoreRepository: PetStoreRepository,
    private val petSettingsRepository: PetSettingsRepository,
    private val careRepository: PetCareRepository,
    private val foodRepository: PetFoodRepository,
    private val bitmapCache: PetBitmapCache,
    private val musicPlayer: PetRoomMusicPlayer
) : ViewModel() {
    private val _uiState = MutableStateFlow(PetRoomUiState())
    val uiState: StateFlow<PetRoomUiState> = _uiState.asStateFlow()
    private val _scene = MutableStateFlow<List<PetRoomSceneEntry>>(emptyList())
    val scene: StateFlow<List<PetRoomSceneEntry>> = _scene.asStateFlow()

    private var selectedPetId: Int? = null
    private var isScreenResumed = false

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
                rememberAdoptions(roster)
                _uiState.update { it.copy(pets = roster, isRosterLoading = isLoading) }
                refreshDetail()
            }
        }
        viewModelScope.launch {
            petPackRepository.packs.collect { packs -> _scene.value = buildScene(packs) }
        }
        viewModelScope.launch {
            roomRepository.isMusicOn.collect { isOn ->
                _uiState.update { it.copy(isMusicOn = isOn) }
                if (isOn && isScreenResumed) musicPlayer.play() else musicPlayer.pause()
            }
        }
        viewModelScope.launch {
            foodRepository.inventory.collect { inventory ->
                _uiState.update { state -> state.copy(foods = foods(inventory)) }
            }
        }
        viewModelScope.launch {
            combine(
                careRepository.energy,
                careRepository.adoptedAtMillis,
                petSettingsRepository.preferences
            ) { _, _, _ -> Unit }.collect { refreshDetail() }
        }
    }

    fun selectTab(tab: PetRoomTab) = _uiState.update { state ->
        val (selected, expanded) = PetRoomSheetPolicy.onTabSelected(
            current = state.selectedTab,
            requested = tab,
            isExpanded = state.isSheetExpanded
        )
        state.copy(selectedTab = selected, isSheetExpanded = expanded, message = null)
    }

    fun toggleSheet() = _uiState.update { state ->
        state.copy(isSheetExpanded = PetRoomSheetPolicy.toggleExpanded(state.isSheetExpanded))
    }

    fun toggleMusic() {
        viewModelScope.launch { roomRepository.setMusicOn(!_uiState.value.isMusicOn) }
    }

    /** Music follows the screen: it must not keep playing once the user leaves the room. */
    fun onScreenResumed() {
        isScreenResumed = true
        if (_uiState.value.isMusicOn) musicPlayer.play()
    }

    fun onScreenPaused() {
        isScreenResumed = false
        musicPlayer.pause()
    }

    /** Opens the pet the user tapped inside the scene. */
    fun openPetByPackKey(packKey: String) {
        val pet = _uiState.value.pets.firstOrNull { it.packKey == packKey } ?: return
        openPet(pet.petId)
    }

    fun selectRoom(roomId: Int) {
        viewModelScope.launch { roomRepository.selectRoom(roomId) }
    }

    fun openPet(petId: Int) {
        selectedPetId = petId
        _uiState.update { it.copy(isSheetExpanded = true, message = null) }
        refreshDetail()
    }

    fun closeDetail() {
        selectedPetId = null
        _uiState.update { it.copy(detail = null, message = null) }
    }

    fun toggleOnScreen() {
        val detail = _uiState.value.detail ?: return
        val preferences = petSettingsRepository.preferences.value
        when (
            val action = PetRoomOnScreenPolicy.toggle(
                slotPackKeys = preferences.roomSlotKeys(),
                slotEnabled = preferences.petSlots.map { it.isEnabled },
                packKey = detail.packKey,
                turnOn = !detail.isOnScreen
            )
        ) {
            is PetRoomOnScreenAction.Assign -> {
                petSettingsRepository.updateSelectedPack(action.slotIndex, detail.packKey)
                petSettingsRepository.updateSlotEnabled(action.slotIndex, true)
                if (preferences.petCount <= action.slotIndex) {
                    petSettingsRepository.updatePetCount(action.slotIndex + 1)
                }
            }

            is PetRoomOnScreenAction.SetEnabled ->
                petSettingsRepository.updateSlotEnabled(action.slotIndex, action.enabled)

            PetRoomOnScreenAction.None -> if (!detail.isOnScreen) {
                showMessage(PetRoomMessage.NO_FREE_OVERLAY_SLOT)
            }
        }
        refreshDetail()
    }

    /** Feeding is why the Food tab exists, so a tap there always targets the open pet. */
    fun feed(foodId: String) {
        val detail = _uiState.value.detail
        if (detail == null) {
            showMessage(PetRoomMessage.SELECT_A_PET_FIRST)
            return
        }
        val food = PET_FOOD_CATALOG.firstOrNull { it.id == foodId } ?: return
        if (PetEnergyPolicy.isMax(detail.energyPercent)) {
            showMessage(PetRoomMessage.ALREADY_FULL)
            return
        }
        viewModelScope.launch {
            if (!foodRepository.consume(foodId)) {
                showMessage(PetRoomMessage.OUT_OF_FOOD)
                return@launch
            }
            careRepository.setEnergy(
                petId = detail.petId,
                percent = PetEnergyPolicy.afterFeeding(detail.energyPercent, food.energyValue),
                atMillis = System.currentTimeMillis()
            )
            refreshDetail()
        }
    }

    fun dismissMessage() = _uiState.update { it.copy(message = null) }

    private fun showMessage(message: PetRoomMessage) =
        _uiState.update { it.copy(message = message) }

    private suspend fun rememberAdoptions(roster: List<PetRoomPetUiState>) {
        val known = careRepository.adoptedAtMillis.value
        val now = System.currentTimeMillis()
        roster.filterNot { known.containsKey(it.petId) }.forEach { pet ->
            // A pack installed before this feature has no real date; first sighting is the
            // closest honest answer.
            careRepository.rememberAdoption(pet.petId, now)
        }
    }

    private fun refreshDetail() {
        val petId = selectedPetId
        if (petId == null) {
            _uiState.update { it.copy(detail = null) }
            return
        }
        val pet = _uiState.value.pets.firstOrNull { it.petId == petId }
        if (pet == null) {
            selectedPetId = null
            _uiState.update { it.copy(detail = null) }
            return
        }
        val now = System.currentTimeMillis()
        val record = careRepository.energy.value[petId]
            ?: PetEnergyRecord(PetEnergyPolicy.INITIAL_ENERGY, now)
        val preferences = petSettingsRepository.preferences.value
        _uiState.update { state ->
            state.copy(
                detail = PetRoomDetailUiState(
                    petId = petId,
                    packKey = pet.packKey,
                    name = pet.name,
                    breed = pet.breed,
                    adoptedOn = ADOPTED_ON_FORMAT.format(
                        Date(careRepository.adoptedAtMillis.value[petId] ?: now)
                    ),
                    thumbnailPath = pet.thumbnailPath,
                    energyPercent = PetEnergyPolicy.currentEnergy(
                        storedPercent = record.percent,
                        updatedAtMillis = record.updatedAtMillis,
                        nowMillis = now
                    ),
                    isOnScreen = PetRoomOnScreenPolicy.isOnScreen(
                        slotPackKeys = preferences.roomSlotKeys(),
                        slotEnabled = preferences.petSlots.map { it.isEnabled },
                        packKey = pet.packKey
                    )
                )
            )
        }
    }

    private fun foods(inventory: Map<String, Int>): List<PetRoomFoodUiState> =
        PET_FOOD_CATALOG.map { food ->
            PetRoomFoodUiState(
                id = food.id,
                name = food.name,
                energyValue = food.energyValue,
                imageRes = food.imageRes,
                portions = inventory[food.id] ?: 0
            )
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

    private suspend fun buildScene(packs: List<PetPack>): List<PetRoomSceneEntry> =
        withContext(Dispatchers.Default) {
            packs.take(MAX_SCENE_PETS).map { pack ->
                PetRoomSceneEntry(
                    packKey = pack.key,
                    visual = bitmapCache.prepare(pack),
                    engineConfig = PetEngineConfig(
                        clips = pack.manifest.toEngineClips(),
                        tapAction = pack.manifest.interaction.tapAction,
                        supportedActions = pack.manifest.toEngineSupportedActions(),
                        behaviorProfile = PetBehaviorProfiles.ROOM,
                        behaviorSeed = pack.manifest.id.hashCode().toLong()
                    )
                )
            }
        }

    /** Slots past the configured roster are free for a new room pet to take. */
    private fun PetPreferences.roomSlotKeys(): List<String> =
        petSlots.mapIndexed { index, slot -> if (index < petCount) slot.packKey else "" }

    override fun onCleared() {
        musicPlayer.release()
        super.onCleared()
    }

    private companion object {
        const val MAX_SCENE_PETS = 6
        val ADOPTED_ON_FORMAT = SimpleDateFormat("dd.MM.yyyy", Locale.US)
    }
}
