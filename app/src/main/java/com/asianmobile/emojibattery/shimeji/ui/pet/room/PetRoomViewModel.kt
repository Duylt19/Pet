package com.asianmobile.emojibattery.shimeji.ui.pet.room

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.data.model.MAX_PET_SLOTS
import com.asianmobile.emojibattery.shimeji.data.model.PetPreferences
import com.asianmobile.emojibattery.shimeji.data.model.PetRoomCatalogSnapshot
import com.asianmobile.emojibattery.shimeji.data.repository.OwnerPetCatalogRepository
import com.asianmobile.emojibattery.shimeji.data.repository.PetCareRepository
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
import com.asianmobile.emojibattery.shimeji.pet.overlay.PetOverlay
import com.asianmobile.emojibattery.shimeji.pet.overlay.PetOverlayRosterPolicy
import com.asianmobile.emojibattery.shimeji.pet.overlay.PetOverlayRuntime
import com.asianmobile.emojibattery.shimeji.pet.room.PetRoomBundledBackground
import com.asianmobile.emojibattery.shimeji.pet.room.PetRoomMusicPlayer
import com.asianmobile.emojibattery.shimeji.pet.room.PetRoomSizePolicy
import com.asianmobile.emojibattery.shimeji.pet.room.PetRoomSoundPlayer
import com.asianmobile.emojibattery.shimeji.ui.pet.store.PET_FOOD_CATALOG
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class PetRoomViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val catalogRepository: PetRoomCatalogRepository,
    private val roomRepository: PetRoomRepository,
    private val ownerCatalogRepository: OwnerPetCatalogRepository,
    private val petPackRepository: PetPackRepository,
    private val petStoreRepository: PetStoreRepository,
    private val petSettingsRepository: PetSettingsRepository,
    private val careRepository: PetCareRepository,
    private val foodRepository: PetFoodRepository,
    private val bitmapCache: PetBitmapCache,
    private val musicPlayer: PetRoomMusicPlayer,
    private val soundPlayer: PetRoomSoundPlayer
) : ViewModel() {
    private val _uiState = MutableStateFlow(PetRoomUiState())
    val uiState: StateFlow<PetRoomUiState> = _uiState.asStateFlow()
    private val _scene = MutableStateFlow<List<PetRoomSceneEntry>>(emptyList())
    val scene: StateFlow<List<PetRoomSceneEntry>> = _scene.asStateFlow()

    private var selectedPetId: Int? = null
    private var isScreenResumed = false
    private var restoreOverlayOnExit = false
    private var energyRefreshJob: Job? = null
    private var roomPacks: List<PetPack> = emptyList()
    private var downloadingRoomId: Int? = null

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
                petStoreRepository.customNames,
                petSettingsRepository.preferences
            ) { catalog, packs, names, preferences ->
                val roomSlotKeys = preferences.roomSlotKeys()
                val enabledSlots = preferences.petSlots.map { it.isEnabled }
                val roster = PetRoomRosterPolicy.roster(
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
                ).map { pet ->
                    pet.copy(
                        isOnScreen = PetRoomOnScreenPolicy.isOnScreen(
                            slotPackKeys = roomSlotKeys,
                            slotEnabled = enabledSlots,
                            packKey = pet.packKey
                        )
                    )
                }
                roster to packs
            }.collect { (roster, packs) ->
                rememberAdoptions(roster)
                _uiState.update {
                    it.copy(
                        pets = roster,
                        isRosterLoading = ownerCatalogRepository.snapshot.value.isLoading
                    )
                }
                refreshDetail()
                // The roster keeps every owned pet so an Inactive pet can be enabled again. The
                // scene itself follows the same Active/Inactive state as the floating overlay.
                val activePackKeys = PetRoomRosterPolicy.activePackKeys(roster)
                roomPacks = packs.filter { pack ->
                    pack.key in activePackKeys
                }
                _scene.value = buildScene(roomPacks)
            }
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
        viewModelScope.launch {
            // Size and speed are the same setting the overlay uses, so the room follows it.
            petSettingsRepository.preferences
                .map { it.slot(0).sizePercent to it.slot(0).speedPercent }
                .distinctUntilChanged()
                .collect { _scene.value = buildScene(roomPacks) }
        }
    }

    fun selectTab(tab: PetRoomTab): Unit = run {
        soundPlayer.playFlip()
        selectTabInternal(tab)
    }

    private fun selectTabInternal(tab: PetRoomTab) = _uiState.update { state ->
        val (selected, expanded) = PetRoomSheetPolicy.onTabSelected(
            current = state.selectedTab,
            requested = tab,
            isExpanded = state.isSheetExpanded
        )
        if (selected != PetRoomTab.MY_PET) selectedPetId = null
        state.copy(
            selectedTab = selected,
            isSheetExpanded = expanded,
            detail = PetRoomSheetPolicy.detailForTab(selected, state.detail),
            message = null
        )
    }

    fun toggleSheet() {
        soundPlayer.playClick()
        _uiState.update { state ->
            state.copy(isSheetExpanded = PetRoomSheetPolicy.toggleExpanded(state.isSheetExpanded))
        }
    }

    /** Taps the screen owns rather than the ViewModel, such as Back and the Pet Store shortcut. */
    fun playClick() = soundPlayer.playClick()

    fun toggleMusic() {
        soundPlayer.playClick()
        viewModelScope.launch { roomRepository.setMusicOn(!_uiState.value.isMusicOn) }
    }

    /**
     * Music and the floating overlay both follow the screen. The room already shows the pets, so
     * leaving them floating on top would show the same pet twice; the overlay comes back exactly
     * when the user leaves the room.
     */
    fun onScreenResumed() {
        isScreenResumed = true
        startEnergyRefresh()
        soundPlayer.prepare()
        if (_uiState.value.isMusicOn) musicPlayer.play()
        if (PetOverlayRuntime.isRunning.value) {
            restoreOverlayOnExit = true
            PetOverlay.stop(context)
        }
    }

    fun onScreenPaused() {
        isScreenResumed = false
        energyRefreshJob?.cancel()
        energyRefreshJob = null
        musicPlayer.pause()
        if (restoreOverlayOnExit) {
            restoreOverlayOnExit = false
            PetOverlay.start(context)
        }
    }

    /** Opens the pet the user tapped inside the scene. */
    fun openPetByPackKey(packKey: String) {
        val pet = _uiState.value.pets.firstOrNull { it.packKey == packKey } ?: return
        openPet(pet.petId)
    }

    /**
     * Applying a room may need its background first. The bundled room is always ready; anything
     * else downloads with the card showing progress, and a failure leaves the current room alone.
     */
    fun selectRoom(roomId: Int) {
        soundPlayer.playClick()
        val snapshot = catalogRepository.snapshot.value
        val room = snapshot.findRoom(roomId) ?: return
        if (PetRoomBundledBackground.isBundled(roomId) ||
            catalogRepository.cachedAssetPath(room.backgroundPath) != null
        ) {
            viewModelScope.launch { roomRepository.selectRoom(roomId) }
            return
        }
        if (downloadingRoomId != null) return
        updateDownloadingRoom(roomId)
        viewModelScope.launch {
            val path = catalogRepository.materializeAsset(room.backgroundPath)
            if (path == null) {
                updateDownloadingRoom(null)
                showMessage(PetRoomMessage.ROOM_DOWNLOAD_FAILED)
                return@launch
            }
            roomRepository.selectRoom(roomId)
            downloadingRoomId = null
            // Keep the clicked card in loading state until the downloaded background and selected
            // border are ready together, avoiding a download-icon flash before catalog mapping.
            applyCatalog(catalogRepository.snapshot.value, roomId)
        }
    }

    private fun updateDownloadingRoom(roomId: Int?) {
        downloadingRoomId = roomId
        _uiState.update { state ->
            state.copy(
                rooms = state.rooms.map { room ->
                    room.copy(isDownloading = roomId != null && room.id == roomId)
                }
            )
        }
    }

    fun openPet(petId: Int) {
        soundPlayer.playClick()
        selectedPetId = petId
        _uiState.update { it.copy(isSheetExpanded = true, message = null) }
        refreshDetail()
    }

    fun closeDetail() {
        soundPlayer.playClick()
        selectedPetId = null
        _uiState.update { it.copy(detail = null, message = null) }
    }

    fun toggleOnScreen() {
        soundPlayer.playClick()
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
                petSettingsRepository.enablePackInFirstFreeSlot(detail.packKey)
            }

            is PetRoomOnScreenAction.SetEnabled ->
                petSettingsRepository.updateSlotEnabled(action.slotIndex, action.enabled)

            PetRoomOnScreenAction.KeepLastActive -> _uiState.update {
                it.copy(lastActivePetName = detail.name)
            }

            PetRoomOnScreenAction.None -> if (!detail.isOnScreen) {
                showMessage(PetRoomMessage.NO_FREE_OVERLAY_SLOT)
            }
        }
        refreshDetail()
    }

    /** Feeding is why the Food tab exists, so a tap there always targets the open pet. */
    fun feed(foodId: String) {
        soundPlayer.playClick()
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

    /** Slot one carries the shared values; the dialog edits those and Save fans them out. */
    fun openSettings() {
        soundPlayer.playClick()
        val slot = petSettingsRepository.preferences.value.slot(0)
        _uiState.update {
            it.copy(
                settings = PetRoomSettingsUiState(
                    speedPercent = PetRoomSettingsPolicy.nearest(
                        slot.speedPercent,
                        PetRoomSettingsPolicy.SPEED_STEPS
                    ),
                    sizePercent = PetRoomSettingsPolicy.nearest(
                        slot.sizePercent,
                        PetRoomSettingsPolicy.SIZE_STEPS
                    )
                )
            )
        }
    }

    fun closeSettings() {
        soundPlayer.playClick()
        _uiState.update { it.copy(settings = null) }
    }

    fun updateSettingsSpeed(percent: Int) = _uiState.update { state ->
        state.copy(settings = state.settings?.copy(speedPercent = percent))
    }

    fun updateSettingsSize(percent: Int) = _uiState.update { state ->
        state.copy(settings = state.settings?.copy(sizePercent = percent))
    }

    fun saveSettings() {
        soundPlayer.playClick()
        val settings = _uiState.value.settings ?: return
        // One profile for every pet: the design has no per-pet settings screen yet.
        repeat(MAX_PET_SLOTS) { slotIndex ->
            petSettingsRepository.updateSpeedPercent(slotIndex, settings.speedPercent)
            petSettingsRepository.updateSizePercent(slotIndex, settings.sizePercent)
        }
        _uiState.update { it.copy(settings = null) }
    }

    fun requestRemovePet(petId: Int) {
        soundPlayer.playClick()
        val pet = _uiState.value.pets.firstOrNull { it.petId == petId } ?: return
        _uiState.update { it.copy(petPendingRemoval = pet) }
    }

    fun cancelRemovePet() = _uiState.update { it.copy(petPendingRemoval = null) }

    fun confirmRemovePet() {
        val pet = _uiState.value.petPendingRemoval ?: return
        _uiState.update { it.copy(petPendingRemoval = null) }
        if (!petPackRepository.remove(pet.packKey)) {
            showMessage(PetRoomMessage.REMOVE_FAILED)
            return
        }
        // A removed pet must not keep an overlay slot, or the floating session would still
        // show a pack the user no longer owns.
        val preferences = petSettingsRepository.preferences.value
        val slotIndex = preferences.roomSlotKeys().indexOf(pet.packKey)
        if (slotIndex >= 0) petSettingsRepository.removePet(slotIndex)
        if (selectedPetId == pet.petId) closeDetail()
    }

    fun dismissMessage() = _uiState.update { it.copy(message = null) }

    fun dismissLastActivePetDialog() = _uiState.update { it.copy(lastActivePetName = null) }

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
        val adoptedAtMillis = careRepository.adoptedAtMillis.value[petId]
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
                    energyPercent = PetEnergyPolicy.resolvedEnergy(
                        storedPercent = record?.percent,
                        updatedAtMillis = record?.updatedAtMillis,
                        adoptedAtMillis = adoptedAtMillis,
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

    private fun startEnergyRefresh() {
        energyRefreshJob?.cancel()
        energyRefreshJob = viewModelScope.launch {
            while (isActive) {
                if (selectedPetId != null) refreshDetail()
                delay(ENERGY_REFRESH_INTERVAL_MILLIS)
            }
        }
    }

    private suspend fun applyCatalog(snapshot: PetRoomCatalogSnapshot, selectedRoomId: Int) {
        val activeRoom = snapshot.resolveRoom(selectedRoomId)
        val thumbnails = snapshot.rooms.map { room ->
            val bundled = PetRoomBundledBackground.isBundled(room.id)
            PetRoomThumbnailUiState(
                id = room.id,
                name = room.name,
                // Previews are small, so they are fetched up front; backgrounds are not.
                thumbnailPath = if (bundled) {
                    null
                } else {
                    catalogRepository.materializeAsset(room.thumbnailPath)
                },
                thumbnailRes = PetRoomBundledBackground.thumbnailResOrNull(room.id),
                isSelected = room.id == activeRoom?.id,
                needsDownload = !bundled &&
                    catalogRepository.cachedAssetPath(room.backgroundPath) == null,
                isDownloading = room.id == downloadingRoomId
            )
        }
        val backgroundPath = activeRoom
            ?.takeUnless { PetRoomBundledBackground.isBundled(it.id) }
            ?.let { catalogRepository.cachedAssetPath(it.backgroundPath) }
        _uiState.update { state ->
            state.copy(
                backgroundPath = backgroundPath,
                backgroundRes = activeRoom?.id?.let(
                    PetRoomBundledBackground::backgroundResOrNull
                ) ?: PetRoomBundledBackground.backgroundRes.takeIf { backgroundPath == null },
                rooms = thumbnails,
                isRoomCatalogLoading = snapshot.isLoading,
                roomCatalogFailed = !snapshot.isLoading && snapshot.rooms.isEmpty()
            )
        }
    }

    private suspend fun buildScene(packs: List<PetPack>): List<PetRoomSceneEntry> {
        val slot = petSettingsRepository.preferences.value.slot(0)
        val density = context.resources.displayMetrics.density
        val speedMultiplier = PetRoomSizePolicy.speedMultiplier(slot.speedPercent)
        return withContext(Dispatchers.Default) {
            packs.take(MAX_SCENE_PETS).map { pack ->
                PetRoomSceneEntry(
                    packKey = pack.key,
                    visual = bitmapCache.prepare(pack),
                    engineConfig = PetEngineConfig(
                        clips = pack.manifest.toEngineClips(speedMultiplier),
                        tapAction = pack.manifest.interaction.tapAction,
                        supportedActions = pack.manifest.toEngineSupportedActions(),
                        behaviorProfile = PetBehaviorProfiles.ROOM,
                        behaviorSeed = pack.manifest.id.hashCode().toLong()
                    ),
                    petSizePx = PetRoomSizePolicy.petSizePixels(
                        packDefaultScale = pack.manifest.canvas.defaultScale,
                        sizePercent = slot.sizePercent,
                        density = density
                    ),
                    speedMultiplier = speedMultiplier
                )
            }
        }
    }

    private fun PetPreferences.roomSlotKeys(): List<String> =
        PetOverlayRosterPolicy.freeableSlotKeys(
            slotPackKeys = petSlots.map { it.packKey },
            petCount = petCount
        )

    override fun onCleared() {
        musicPlayer.release()
        soundPlayer.release()
        super.onCleared()
    }

    private companion object {
        const val MAX_SCENE_PETS = 6
        const val ENERGY_REFRESH_INTERVAL_MILLIS = 1_000L
        val ADOPTED_ON_FORMAT = SimpleDateFormat("dd.MM.yyyy", Locale.US)
    }
}
