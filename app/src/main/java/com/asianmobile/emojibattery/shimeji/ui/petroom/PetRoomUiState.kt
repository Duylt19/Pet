package com.asianmobile.emojibattery.shimeji.ui.petroom

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
    val rooms: List<PetRoomThumbnailUiState> = emptyList(),
    val isRoomCatalogLoading: Boolean = true,
    val roomCatalogFailed: Boolean = false
)

data class PetRoomThumbnailUiState(
    val id: Int,
    val name: String,
    val thumbnailPath: String?,
    val isSelected: Boolean
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
