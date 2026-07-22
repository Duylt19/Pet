package com.asianmobile.privatebrower.data.model

data class OwnerPetCatalogEntry(
    val id: Int,
    val name: String,
    val category: String,
    val author: String?,
    val thumbnailPath: String?,
    val hasLocalArchive: Boolean
) {
    val installedPackKey: String
        get() = "${installedPackId(id)}@1"

    companion object {
        private const val PACK_ID_PREFIX = "owner.shimeji."

        fun installedPackId(id: Int): String = "$PACK_ID_PREFIX$id"

        fun petIdFromPackId(packId: String): Int? = packId
            .takeIf { it.startsWith(PACK_ID_PREFIX) }
            ?.removePrefix(PACK_ID_PREFIX)
            ?.toIntOrNull()
    }
}

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
