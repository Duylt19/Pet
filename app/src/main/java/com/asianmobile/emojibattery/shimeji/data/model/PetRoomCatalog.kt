package com.asianmobile.emojibattery.shimeji.data.model

enum class PetRoomEntitlement {
    FREE,
    PREMIUM
}

/**
 * One selectable My Pet Room background. [thumbnailPath] is a deliberately low-quality preview
 * published beside the full background so the Room picker never downloads a full-size image.
 */
data class PetRoomEntry(
    val id: Int,
    val name: String,
    val slug: String,
    val entitlement: PetRoomEntitlement,
    val backgroundPath: String,
    val thumbnailPath: String
)

data class PetRoomCatalogSnapshot(
    val rooms: List<PetRoomEntry> = emptyList(),
    val defaultRoomId: Int = 0,
    val catalogVersion: String? = null,
    val isLoading: Boolean = true,
    val error: PetRoomCatalogError? = null
) {
    fun findRoom(roomId: Int): PetRoomEntry? = rooms.firstOrNull { it.id == roomId }

    /** The room to draw when the user has not chosen one, or their choice left the catalog. */
    fun resolveRoom(selectedRoomId: Int): PetRoomEntry? =
        findRoom(selectedRoomId) ?: findRoom(defaultRoomId) ?: rooms.firstOrNull()
}

enum class PetRoomCatalogError {
    CATALOG_UNAVAILABLE,
    CATALOG_INVALID,
    DISTRIBUTION_NOT_APPROVED
}
