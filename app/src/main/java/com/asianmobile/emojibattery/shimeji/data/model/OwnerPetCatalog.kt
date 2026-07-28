package com.asianmobile.emojibattery.shimeji.data.model

data class OwnerPetCatalogEntry(
    val id: Int,
    val name: String,
    val category: String,
    val author: String?,
    val thumbnailPath: String?,
    val hasLocalArchive: Boolean,
    val archiveUrl: String? = null,
    val archiveSizeBytes: Long? = null,
    val archiveSha256: String? = null,
    val speechAnchor: OwnerPetSpeechAnchor? = null
) {
    val installedPackKey: String
        get() = "${installedPackId(id)}@$OWNER_PET_PACK_VERSION"

    companion object {
        private const val PACK_ID_PREFIX = "owner.shimeji."

        fun installedPackId(id: Int): String = "$PACK_ID_PREFIX$id"

        fun installedPetId(packId: String): Int? {
            if (!packId.startsWith(PACK_ID_PREFIX)) return null
            return packId.removePrefix(PACK_ID_PREFIX)
                .takeIf(String::isNotEmpty)
                ?.toIntOrNull()
                ?.takeIf { it >= 0 }
        }
    }
}

data class OwnerPetSpeechAnchor(
    val x: Float,
    val y: Float
)

const val OWNER_PET_PACK_VERSION = 7

data class OwnerPetCatalogSnapshot(
    val entries: List<OwnerPetCatalogEntry> = emptyList(),
    val localRootPath: String = "",
    val catalogVersion: String? = null,
    val isLoading: Boolean = true,
    val error: OwnerPetCatalogError? = null
)

enum class OwnerPetCatalogError {
    LOCAL_CATALOG_MISSING,
    LOCAL_CATALOG_INVALID,
    LOCAL_STORAGE_UNAVAILABLE,
    REMOTE_CATALOG_UNAVAILABLE,
    REMOTE_CATALOG_INVALID
}
