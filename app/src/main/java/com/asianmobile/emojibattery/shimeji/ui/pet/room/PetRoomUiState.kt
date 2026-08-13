package com.asianmobile.emojibattery.shimeji.ui.pet.room

import com.asianmobile.emojibattery.shimeji.pet.engine.PetEngineConfig
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackVisual

enum class PetRoomTab {
    MY_PET,
    FOOD,
    ROOM
}

data class PetRoomUiState(
    val selectedTab: PetRoomTab = PetRoomTab.MY_PET,
    val isSheetExpanded: Boolean = true,
    val isMusicOn: Boolean = false,
    val backgroundPath: String? = null,
    /** Drawable of the room that ships with the app, used until a download replaces it. */
    val backgroundRes: Int? = null,
    val pets: List<PetRoomPetUiState> = emptyList(),
    val isRosterLoading: Boolean = true,
    val rooms: List<PetRoomThumbnailUiState> = emptyList(),
    val isRoomCatalogLoading: Boolean = true,
    val roomCatalogFailed: Boolean = false,
    val detail: PetRoomDetailUiState? = null,
    val foods: List<PetRoomFoodUiState> = emptyList(),
    val message: PetRoomMessage? = null,
    val petPendingRemoval: PetRoomPetUiState? = null,
    val settings: PetRoomSettingsUiState? = null
)

/** The panel that replaces the sheet body once the user taps a pet. */
data class PetRoomDetailUiState(
    val petId: Int,
    val packKey: String,
    val name: String,
    val breed: String,
    val adoptedOn: String,
    val thumbnailPath: String?,
    val energyPercent: Int,
    val isOnScreen: Boolean
)

data class PetRoomFoodUiState(
    val id: String,
    val name: String,
    val energyValue: Int,
    val imageRes: Int,
    val portions: Int
)

enum class PetRoomMessage {
    ROOM_DOWNLOAD_FAILED,
    REMOVE_FAILED,
    SELECT_A_PET_FIRST,
    OUT_OF_FOOD,
    ALREADY_FULL,
    NO_FREE_OVERLAY_SLOT
}

/** One pet the user already owns. Ownership is the installed pack, as in Pet Store. */
data class PetRoomPetUiState(
    val petId: Int,
    val packKey: String,
    val name: String,
    val breed: String,
    val thumbnailPath: String?,
    val isOnScreen: Boolean = false
)

data class PetRoomThumbnailUiState(
    val id: Int,
    val name: String,
    val thumbnailPath: String?,
    val thumbnailRes: Int?,
    val isSelected: Boolean,
    /** The background still has to be fetched before this room can be applied. */
    val needsDownload: Boolean,
    val isDownloading: Boolean
)

/** Everything the scene needs to run one pet: its sprites and its engine configuration. */
data class PetRoomSceneEntry(
    val packKey: String,
    val visual: PetPackVisual,
    val engineConfig: PetEngineConfig,
    /** Same pixel size the overlay would draw this pack at, for the same size setting. */
    val petSizePx: Float,
    val speedMultiplier: Float
)

object PetRoomRosterPolicy {
    /**
     * The room shows every pet the user owns, ordered by the catalog so the grid does not
     * reshuffle between launches. A pack without a catalog entry is skipped rather than shown
     * with a placeholder name.
     */
    fun roster(
        catalogEntries: List<PetRoomRosterSource>,
        installedPackKeys: Set<String>,
        customNames: Map<Int, String>
    ): List<PetRoomPetUiState> = catalogEntries
        .filter { it.packKey in installedPackKeys }
        .map { entry ->
            PetRoomPetUiState(
                petId = entry.petId,
                packKey = entry.packKey,
                name = customNames[entry.petId]?.trim()?.takeIf(String::isNotEmpty)
                    ?: entry.catalogName,
                breed = entry.category,
                thumbnailPath = entry.thumbnailPath
            )
        }

    /** Only Active pets walk in the room scene; Inactive pets remain available in the roster. */
    fun activePackKeys(roster: List<PetRoomPetUiState>): Set<String> = roster
        .asSequence()
        .filter(PetRoomPetUiState::isOnScreen)
        .map(PetRoomPetUiState::packKey)
        .filter(String::isNotBlank)
        .toSet()
}

/** Catalog fields the roster needs, kept free of the catalog model so it stays unit-testable. */
data class PetRoomRosterSource(
    val petId: Int,
    val packKey: String,
    val catalogName: String,
    val category: String,
    val thumbnailPath: String?
)

/**
 * The room scene keeps its background while the sheet is open, so collapsing only hides the
 * sheet body. The tab strip stays visible because it is the way back into the sheet.
 */
object PetRoomSheetPolicy {
    fun toggleExpanded(isExpanded: Boolean): Boolean = !isExpanded

    /** Tapping the active tab collapses the sheet; tapping another tab always opens it. */
    fun onTabSelected(
        current: PetRoomTab,
        requested: PetRoomTab,
        isExpanded: Boolean
    ): Pair<PetRoomTab, Boolean> = when {
        current != requested -> requested to true
        else -> current to !isExpanded
    }

    /** Pet detail belongs exclusively to My Pet and must never cover Food or Room content. */
    fun detailForTab(
        selectedTab: PetRoomTab,
        detail: PetRoomDetailUiState?
    ): PetRoomDetailUiState? = detail.takeIf { selectedTab == PetRoomTab.MY_PET }
}

/** Speed and size shared by every pet until the design gives each pet its own profile. */
data class PetRoomSettingsUiState(
    val speedPercent: Int,
    val sizePercent: Int
)

object PetRoomSettingsPolicy {
    const val DEFAULT_PERCENT = 100

    /** The steps the pet settings repository already accepts. */
    val SPEED_STEPS: List<Int> = (50..150 step 25).toList()
    val SIZE_STEPS: List<Int> = (50..150 step 10).toList()

    fun label(percent: Int): String {
        val whole = percent / 100
        val tenth = (percent % 100) / 10
        return "$whole.${tenth}x"
    }

    fun nearest(value: Int, steps: List<Int>): Int =
        steps.minByOrNull { kotlin.math.abs(it - value) } ?: DEFAULT_PERCENT
}
