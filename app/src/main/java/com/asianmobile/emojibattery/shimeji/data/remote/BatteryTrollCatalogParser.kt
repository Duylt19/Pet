package com.asianmobile.emojibattery.shimeji.data.remote

import com.asianmobile.emojibattery.shimeji.data.model.BATTERY_TROLL_LEVEL_COUNT
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollBatteryOrientation
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollDistributionStatus
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollEntitlement
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class BatteryTrollAssetRecord(
    val path: String,
    val sizeBytes: Long,
    val sha256: String,
    val width: Int,
    val height: Int
)

data class BatteryTrollRecord(
    val id: Int,
    val name: String,
    val slug: String,
    val order: Int,
    val entitlement: BatteryTrollEntitlement,
    val batteryOrientation: BatteryTrollBatteryOrientation,
    val thumbnail: BatteryTrollAssetRecord,
    val emoji: List<BatteryTrollAssetRecord>,
    val battery: List<BatteryTrollAssetRecord>
) {
    val assets: List<BatteryTrollAssetRecord>
        get() = listOf(thumbnail) + emoji + battery
}

data class BatteryTrollCatalogDocument(
    val catalogVersion: String,
    val capturedAt: String,
    val distributionStatus: BatteryTrollDistributionStatus,
    val trolls: List<BatteryTrollRecord>
)

/**
 * Parses `json/battery-troll.json` (schema v1). Unknown fields are ignored, every malformed or
 * unsupported document fails as a [BatteryTrollCatalogParseException] instead of propagating a
 * raw `JSONException`, and a troll is only accepted when it publishes exactly
 * [BATTERY_TROLL_LEVEL_COUNT] emoji and battery frames.
 */
class BatteryTrollCatalogParser {
    fun parse(json: String): BatteryTrollCatalogDocument = try {
        val root = JSONObject(json)
        if (root.getInt("schemaVersion") != SCHEMA_VERSION) {
            throw BatteryTrollCatalogParseException("Unsupported Battery Troll catalog schema")
        }
        val catalogVersion = root.getString("catalogVersion").trim()
        val capturedAt = root.getString("capturedAt").trim()
        if (catalogVersion.isBlank() || capturedAt.isBlank()) {
            throw BatteryTrollCatalogParseException("Battery Troll catalog version is missing")
        }
        val distributionStatus = root.getJSONObject("source")
            .getString("distributionStatus")
            .let { value ->
                BatteryTrollDistributionStatus.entries.firstOrNull { it.name == value }
                    ?: throw BatteryTrollCatalogParseException(
                        "Unknown Battery Troll distribution status"
                    )
            }
        val trolls = root.getJSONArray("trolls").mapObjects { item, index ->
            item.toTroll(index)
        }
        validate(root, trolls)
        BatteryTrollCatalogDocument(
            catalogVersion = catalogVersion,
            capturedAt = capturedAt,
            distributionStatus = distributionStatus,
            trolls = trolls.sortedWith(
                compareBy(BatteryTrollRecord::order, BatteryTrollRecord::id)
            )
        )
    } catch (error: BatteryTrollCatalogParseException) {
        throw error
    } catch (error: JSONException) {
        throw BatteryTrollCatalogParseException("Malformed Battery Troll catalog", error)
    }

    private fun JSONObject.toTroll(index: Int): BatteryTrollRecord {
        val id = getInt("id")
        val name = getString("name").trim()
        val slug = getString("slug").trim()
        val order = if (has("order")) getInt("order") else index
        if (id <= 0 || name.isBlank() || !SLUG.matches(slug) || order < 0) {
            throw BatteryTrollCatalogParseException("Invalid Battery Troll at index $index")
        }
        val entitlement = getString("entitlement").let { value ->
            BatteryTrollEntitlement.entries.firstOrNull { it.name == value }
                ?: throw BatteryTrollCatalogParseException(
                    "Unknown Battery Troll entitlement at index $index"
                )
        }
        val orientation = getString("batteryOrientation").let { value ->
            BatteryTrollBatteryOrientation.entries.firstOrNull { it.name == value }
                ?: throw BatteryTrollCatalogParseException(
                    "Unknown Battery Troll orientation at index $index"
                )
        }
        val assets = getJSONObject("assets")
        val thumbnail = assets.getJSONObject("thumbnail").toAsset(index)
        if (!thumbnail.path.startsWith(THUMBNAIL_DIRECTORY)) {
            throw BatteryTrollCatalogParseException(
                "Battery Troll thumbnail at index $index is outside $THUMBNAIL_DIRECTORY"
            )
        }
        return BatteryTrollRecord(
            id = id,
            name = name,
            slug = slug,
            order = order,
            entitlement = entitlement,
            batteryOrientation = orientation,
            thumbnail = thumbnail,
            emoji = assets.getJSONArray("emoji").toLevels("emoji", index),
            battery = assets.getJSONArray("battery").toLevels("battery", index)
        )
    }

    private fun JSONArray.toLevels(key: String, index: Int): List<BatteryTrollAssetRecord> {
        if (length() != BATTERY_TROLL_LEVEL_COUNT) {
            throw BatteryTrollCatalogParseException(
                "Battery Troll at index $index must publish " +
                    "$BATTERY_TROLL_LEVEL_COUNT $key frames"
            )
        }
        val levels = mapObjects { item, _ -> item.toAsset(index) }
        if (levels.map(BatteryTrollAssetRecord::path).distinct().size != levels.size) {
            throw BatteryTrollCatalogParseException(
                "Duplicate Battery Troll $key frames at index $index"
            )
        }
        return levels
    }

    private fun JSONObject.toAsset(index: Int): BatteryTrollAssetRecord {
        val record = BatteryTrollAssetRecord(
            path = getString("path").trim(),
            sizeBytes = getLong("sizeBytes"),
            sha256 = getString("sha256").lowercase(),
            width = getInt("width"),
            height = getInt("height")
        )
        if (!SAFE_PATH.matches(record.path) ||
            record.sizeBytes <= 0L ||
            !SHA_256.matches(record.sha256) ||
            record.width !in 1..MAX_IMAGE_DIMENSION ||
            record.height !in 1..MAX_IMAGE_DIMENSION
        ) {
            throw BatteryTrollCatalogParseException(
                "Invalid Battery Troll asset at index $index"
            )
        }
        return record
    }

    private fun validate(root: JSONObject, trolls: List<BatteryTrollRecord>) {
        if (trolls.isEmpty()) {
            throw BatteryTrollCatalogParseException("Battery Troll catalog is empty")
        }
        if (trolls.map(BatteryTrollRecord::id).distinct().size != trolls.size) {
            throw BatteryTrollCatalogParseException("Duplicate Battery Troll IDs")
        }
        if (trolls.map(BatteryTrollRecord::slug).distinct().size != trolls.size) {
            throw BatteryTrollCatalogParseException("Duplicate Battery Troll slugs")
        }
        if (root.optInt("trollCount", trolls.size) != trolls.size) {
            throw BatteryTrollCatalogParseException("Battery Troll count does not match")
        }
    }

    private inline fun <T> JSONArray.mapObjects(
        transform: (JSONObject, Int) -> T
    ): List<T> = List(length()) { index ->
        val item = optJSONObject(index)
            ?: throw BatteryTrollCatalogParseException(
                "Battery Troll catalog item $index must be an object"
            )
        transform(item, index)
    }

    private companion object {
        const val SCHEMA_VERSION = 1
        const val MAX_IMAGE_DIMENSION = 4096
        const val THUMBNAIL_DIRECTORY = "thumb/"
        val SHA_256 = Regex("[0-9a-f]{64}")
        val SLUG = Regex("[a-z0-9]+(?:[-_][a-z0-9]+)*")
        val SAFE_PATH = Regex("[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)*\\.(?:png|webp)")
    }
}

class BatteryTrollCatalogParseException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)
