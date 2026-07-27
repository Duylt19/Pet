package com.asianmobile.emojibattery.shimeji.data.model

data class OwnerPetCatalogEntry(
    val id: Int,
    val name: String,
    val category: String,
    val author: String?,
    val thumbnailPath: String?,
    val hasLocalArchive: Boolean
) {
    val installedPackKey: String
        get() = "${installedPackId(id)}@$OWNER_PET_PACK_VERSION"

    companion object {
        private const val PACK_ID_PREFIX = "owner.shimeji."

        fun installedPackId(id: Int): String = "$PACK_ID_PREFIX$id"
    }
}

const val OWNER_PET_PACK_VERSION = 4

data class OwnerPetCatalogSnapshot(
    val entries: List<OwnerPetCatalogEntry> = emptyList(),
    val localRootPath: String = "",
    val isLoading: Boolean = true,
    val error: OwnerPetCatalogError? = null
)

enum class OwnerPetCatalogError {
    LOCAL_CATALOG_MISSING,
    LOCAL_CATALOG_INVALID,
    LOCAL_STORAGE_UNAVAILABLE
}
