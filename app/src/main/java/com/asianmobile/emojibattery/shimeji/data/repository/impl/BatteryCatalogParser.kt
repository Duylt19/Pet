package com.asianmobile.emojibattery.shimeji.data.repository.impl

import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogCategory
import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogDistributionStatus
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntitlement
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class BatteryCatalogAssetRecord(
    val path: String,
    val sizeBytes: Long,
    val sha256: String,
    val width: Int,
    val height: Int
)

data class BatteryThemeRecord(
    val id: Int,
    val name: String,
    val categoryId: Int,
    val categoryName: String,
    val entitlement: BatteryThemeEntitlement,
    val thumbnail: BatteryCatalogAssetRecord,
    val battery: BatteryCatalogAssetRecord,
    val emoji: BatteryCatalogAssetRecord
)

data class BatteryCatalogDocument(
    val catalogVersion: String,
    val capturedAt: String,
    val distributionStatus: BatteryCatalogDistributionStatus,
    val categories: List<BatteryCatalogCategory>,
    val themes: List<BatteryThemeRecord>
)

class BatteryCatalogParser {
    fun parse(json: String): BatteryCatalogDocument = try {
        val root = JSONObject(json)
        if (root.getInt("schemaVersion") != SCHEMA_VERSION) {
            throw BatteryCatalogParseException("Unsupported Battery catalog schema")
        }
        val catalogVersion = root.getString("catalogVersion").trim()
        val capturedAt = root.getString("capturedAt").trim()
        val distributionStatus = root.getJSONObject("source")
            .getString("distributionStatus")
            .let { value ->
                BatteryCatalogDistributionStatus.entries.firstOrNull { it.name == value }
                    ?: throw BatteryCatalogParseException(
                        "Unknown Battery distribution status"
                    )
            }
        if (catalogVersion.isBlank() || capturedAt.isBlank()) {
            throw BatteryCatalogParseException("Battery catalog version is missing")
        }
        val categories = root.getJSONArray("categories").mapObjects { item, index ->
            BatteryCatalogCategory(
                id = item.getInt("id"),
                name = item.getString("name").trim(),
                slug = item.getString("slug").trim(),
                priority = item.getInt("priority")
            ).also { category ->
                if (category.id <= 0 || category.name.isBlank() ||
                    !SLUG.matches(category.slug)
                ) {
                    throw BatteryCatalogParseException(
                        "Invalid Battery category at index $index"
                    )
                }
            }
        }
        val themes = root.getJSONArray("themes").mapObjects { item, index ->
            val id = item.getInt("id")
            val assets = item.getJSONObject("assets")
            BatteryThemeRecord(
                id = id,
                name = item.getString("name").trim(),
                categoryId = item.getInt("categoryId"),
                categoryName = item.getString("categoryName").trim(),
                entitlement = item.getString("entitlement").let { value ->
                    BatteryThemeEntitlement.entries.firstOrNull { it.name == value }
                        ?: throw BatteryCatalogParseException(
                            "Invalid entitlement at index $index"
                        )
                },
                thumbnail = assets.getJSONObject("thumbnail")
                    .toAsset("thumb/$id.png", index),
                battery = assets.getJSONObject("battery")
                    .toAsset("battery/$id.png", index),
                emoji = assets.getJSONObject("emoji")
                    .toAsset("emoji/$id.png", index)
            ).also { theme ->
                if (theme.id <= 0 || theme.name.isBlank() || theme.categoryName.isBlank()) {
                    throw BatteryCatalogParseException(
                        "Invalid Battery theme at index $index"
                    )
                }
            }
        }
        validate(root, categories, themes)
        BatteryCatalogDocument(
            catalogVersion = catalogVersion,
            capturedAt = capturedAt,
            distributionStatus = distributionStatus,
            categories = categories.sortedWith(compareBy({ it.priority }, { it.id })),
            themes = themes.sortedBy(BatteryThemeRecord::id)
        )
    } catch (error: BatteryCatalogParseException) {
        throw error
    } catch (error: JSONException) {
        throw BatteryCatalogParseException("Malformed Battery catalog", error)
    }

    private fun validate(
        root: JSONObject,
        categories: List<BatteryCatalogCategory>,
        themes: List<BatteryThemeRecord>
    ) {
        if (categories.isEmpty() || themes.isEmpty()) {
            throw BatteryCatalogParseException("Battery catalog is empty")
        }
        if (categories.map(BatteryCatalogCategory::id).distinct().size != categories.size) {
            throw BatteryCatalogParseException("Duplicate Battery category IDs")
        }
        if (themes.map(BatteryThemeRecord::id).distinct().size != themes.size) {
            throw BatteryCatalogParseException("Duplicate Battery theme IDs")
        }
        if (root.getInt("categoryCount") != categories.size ||
            root.getInt("themeCount") != themes.size
        ) {
            throw BatteryCatalogParseException("Battery catalog count does not match")
        }
        val categoriesById = categories.associateBy(BatteryCatalogCategory::id)
        themes.forEach { theme ->
            val category = categoriesById[theme.categoryId]
                ?: throw BatteryCatalogParseException(
                    "Battery theme ${theme.id} references a missing category"
                )
            if (category.name != theme.categoryName) {
                throw BatteryCatalogParseException(
                    "Battery theme ${theme.id} has a mismatched category"
                )
            }
        }
    }

    private fun JSONObject.toAsset(
        expectedPath: String,
        index: Int
    ): BatteryCatalogAssetRecord {
        val record = BatteryCatalogAssetRecord(
            path = getString("path"),
            sizeBytes = getLong("sizeBytes"),
            sha256 = getString("sha256").lowercase(),
            width = getInt("width"),
            height = getInt("height")
        )
        if (record.path != expectedPath || record.sizeBytes <= 0L ||
            !SHA_256.matches(record.sha256) ||
            record.width !in 1..MAX_IMAGE_DIMENSION ||
            record.height !in 1..MAX_IMAGE_DIMENSION
        ) {
            throw BatteryCatalogParseException(
                "Invalid Battery asset at index $index"
            )
        }
        return record
    }

    private inline fun <T> JSONArray.mapObjects(
        transform: (JSONObject, Int) -> T
    ): List<T> = List(length()) { index ->
        val item = optJSONObject(index)
            ?: throw BatteryCatalogParseException(
                "Battery catalog item $index must be an object"
            )
        transform(item, index)
    }

    private companion object {
        const val SCHEMA_VERSION = 1
        const val MAX_IMAGE_DIMENSION = 4096
        val SHA_256 = Regex("[0-9a-f]{64}")
        val SLUG = Regex("[a-z0-9]+(?:-[a-z0-9]+)*")
    }
}

class BatteryCatalogParseException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)
