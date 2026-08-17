package com.asianmobile.emojibattery.shimeji.data.repository.impl

import com.asianmobile.emojibattery.shimeji.data.model.PetRoomEntitlement
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class RoomCatalogAssetRecord(
    val path: String,
    val sizeBytes: Long,
    val sha256: String,
    val width: Int,
    val height: Int
)

data class RoomRecord(
    val id: Int,
    val name: String,
    val slug: String,
    val entitlement: PetRoomEntitlement,
    val background: RoomCatalogAssetRecord,
    val thumbnail: RoomCatalogAssetRecord
)

data class RoomCatalogDocument(
    val catalogVersion: String,
    val capturedAt: String,
    val distributionApproved: Boolean,
    val defaultRoomId: Int,
    val rooms: List<RoomRecord>
)

class RoomCatalogParser {
    fun parse(json: String): RoomCatalogDocument = try {
        val root = JSONObject(json)
        if (root.getInt("schemaVersion") != SCHEMA_VERSION) {
            throw RoomCatalogParseException("Unsupported room catalog schema")
        }
        val catalogVersion = root.getString("catalogVersion").trim()
        val capturedAt = root.getString("capturedAt").trim()
        if (catalogVersion.isBlank() || capturedAt.isBlank()) {
            throw RoomCatalogParseException("Room catalog version is missing")
        }
        val distributionStatus = root.getJSONObject("source").getString("distributionStatus")
        if (distributionStatus !in DISTRIBUTION_STATUSES) {
            throw RoomCatalogParseException("Unknown room distribution status")
        }
        val rooms = root.getJSONArray("rooms").mapObjects { item, index ->
            val id = item.getInt("id")
            if (id <= 0) throw RoomCatalogParseException("Invalid room ID at index $index")
            val name = item.getString("name").trim()
            val slug = item.getString("slug").trim()
            if (name.isBlank() || !SLUG.matches(slug)) {
                throw RoomCatalogParseException("Invalid room metadata at index $index")
            }
            val entitlement = item.getString("entitlement").let { value ->
                PetRoomEntitlement.entries.firstOrNull { it.name == value }
                    ?: throw RoomCatalogParseException("Unknown room entitlement at index $index")
            }
            val assets = item.getJSONObject("assets")
            RoomRecord(
                id = id,
                name = name,
                slug = slug,
                entitlement = entitlement,
                background = assets.getJSONObject("background").toAsset("bg/BG_$id", index),
                thumbnail = assets.getJSONObject("thumbnail").toAsset("thumb/BG_$id", index)
            )
        }
        validate(root, rooms)
        RoomCatalogDocument(
            catalogVersion = catalogVersion,
            capturedAt = capturedAt,
            distributionApproved = distributionStatus == APPROVED_STATUS,
            defaultRoomId = root.getInt("defaultRoomId"),
            rooms = rooms
        )
    } catch (error: JSONException) {
        throw RoomCatalogParseException("Malformed room catalog", error)
    }

    private fun validate(root: JSONObject, rooms: List<RoomRecord>) {
        if (rooms.isEmpty()) {
            throw RoomCatalogParseException("Room catalog is empty")
        }
        if (rooms.map(RoomRecord::id).distinct().size != rooms.size) {
            throw RoomCatalogParseException("Duplicate room IDs")
        }
        if (rooms.map(RoomRecord::slug).distinct().size != rooms.size) {
            throw RoomCatalogParseException("Duplicate room slugs")
        }
        if (root.getInt("roomCount") != rooms.size) {
            throw RoomCatalogParseException("Room catalog count does not match")
        }
        if (rooms.none { it.id == root.getInt("defaultRoomId") }) {
            throw RoomCatalogParseException("Room defaultRoomId is missing from the catalog")
        }
        rooms.forEach { room ->
            // The server guarantees a lighter preview; refuse to trust a catalog that lost it.
            if (room.thumbnail.sizeBytes >= room.background.sizeBytes ||
                maxOf(room.thumbnail.width, room.thumbnail.height) > MAX_THUMBNAIL_EDGE
            ) {
                throw RoomCatalogParseException("Room ${room.id} preview is not preview-sized")
            }
        }
    }

    private fun JSONObject.toAsset(
        expectedPathWithoutExtension: String,
        index: Int
    ): RoomCatalogAssetRecord {
        val record = RoomCatalogAssetRecord(
            path = getString("path"),
            sizeBytes = getLong("sizeBytes"),
            sha256 = getString("sha256").lowercase(),
            width = getInt("width"),
            height = getInt("height")
        )
        val extension = record.path.substringAfterLast('.', missingDelimiterValue = "")
        val pathWithoutExtension = record.path.substringBeforeLast(
            delimiter = ".",
            missingDelimiterValue = ""
        )
        if (pathWithoutExtension != expectedPathWithoutExtension ||
            extension !in IMAGE_EXTENSIONS ||
            record.sizeBytes <= 0L ||
            !SHA_256.matches(record.sha256) ||
            record.width !in 1..MAX_IMAGE_DIMENSION ||
            record.height !in 1..MAX_IMAGE_DIMENSION
        ) {
            throw RoomCatalogParseException("Invalid room asset at index $index")
        }
        return record
    }

    private inline fun <T> JSONArray.mapObjects(
        transform: (JSONObject, Int) -> T
    ): List<T> = List(length()) { index ->
        val item = optJSONObject(index)
            ?: throw RoomCatalogParseException("Room catalog item $index must be an object")
        transform(item, index)
    }

    private companion object {
        const val SCHEMA_VERSION = 1
        const val MAX_IMAGE_DIMENSION = 4096
        const val MAX_THUMBNAIL_EDGE = 640
        const val APPROVED_STATUS = "APPROVED"
        val DISTRIBUTION_STATUSES = setOf(APPROVED_STATUS, "REVIEW_REQUIRED")
        val SHA_256 = Regex("[0-9a-f]{64}")
        val IMAGE_EXTENSIONS = setOf("png", "webp")
        val SLUG = Regex("[a-z0-9]+(?:[-_][a-z0-9]+)*")
    }
}

class RoomCatalogParseException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)
