package com.asianmobile.emojibattery.shimeji.ui.petroom

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
    val pets: List<PetRoomPetUiState> = emptyList(),
    val isRosterLoading: Boolean = true,
    val rooms: List<PetRoomThumbnailUiState> = emptyList(),
    val isRoomCatalogLoading: Boolean = true,
    val roomCatalogFailed: Boolean = false,
    val detail: PetRoomDetailUiState? = null,
    val foods: List<PetRoomFoodUiState> = emptyList(),
    val message: PetRoomMessage? = null,
    val petPendingRemoval: PetRoomPetUiState? = null
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
    val thumbnailPath: String?
)

data class PetRoomThumbnailUiState(
    val id: Int,
    val name: String,
    val thumbnailPath: String?,
    val isSelected: Boolean
)

/** Everything the scene needs to run one pet: its sprites and its engine configuration. */
data class PetRoomSceneEntry(
    val packKey: String,
    val visual: PetPackVisual,
    val engineConfig: PetEngineConfig
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
}
