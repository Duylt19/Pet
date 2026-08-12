package com.asianmobile.emojibattery.shimeji.data.repository.impl

import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogCategory
import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogDistributionStatus
import com.asianmobile.emojibattery.shimeji.data.model.BatteryAnimationType
import com.asianmobile.emojibattery.shimeji.data.model.BATTERY_EMOTION_GROUPS
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

data class BatteryDecorationRecord(
    val id: Int,
    val name: String,
    val asset: BatteryCatalogAssetRecord,
    val preview: BatteryCatalogAssetRecord? = null,
    val groupKey: String? = null,
    val order: Int = 0
)

data class BatteryEmotionGroupRecord(
    val key: String,
    val order: Int,
    val emotionIds: List<Int>,
    val background: BatteryCatalogAssetRecord? = null
)

data class BatteryAnimationRecord(
    val id: Int,
    val name: String,
    val type: BatteryAnimationType,
    val asset: BatteryCatalogAssetRecord
)

data class BatteryCatalogDocument(
    val catalogVersion: String,
    val capturedAt: String,
    val distributionStatus: BatteryCatalogDistributionStatus,
    val categories: List<BatteryCatalogCategory>,
    val themes: List<BatteryThemeRecord>,
    val backgrounds: List<BatteryDecorationRecord>,
    val emotions: List<BatteryDecorationRecord>,
    val emotionGroups: List<BatteryEmotionGroupRecord>,
    val animations: List<BatteryAnimationRecord>
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
        val backgrounds = parseDecorations(root, "backgrounds", "background")
        val emotions = parseEmotions(root)
        val emotionGroups = parseEmotionGroups(root, emotions)
        val animations = parseAnimations(root)
        validate(root, categories, themes)
        BatteryCatalogDocument(
            catalogVersion = catalogVersion,
            capturedAt = capturedAt,
            distributionStatus = distributionStatus,
            categories = categories.sortedWith(compareBy({ it.priority }, { it.id })),
            themes = themes.sortedBy(BatteryThemeRecord::id),
            backgrounds = backgrounds,
            emotions = emotions,
            emotionGroups = emotionGroups,
            animations = animations
        )
    } catch (error: BatteryCatalogParseException) {
        throw error
    } catch (error: JSONException) {
        throw BatteryCatalogParseException("Malformed Battery catalog", error)
    }

    private fun parseDecorations(
        root: JSONObject,
        key: String,
        runtimeDirectory: String
    ): List<BatteryDecorationRecord> {
        val array = root.optJSONArray(key) ?: return emptyList()
        val records = array.mapObjects { item, index ->
            val id = item.getInt("id")
            val name = item.getString("name").trim()
            BatteryDecorationRecord(
                id = id,
                name = name,
                asset = item.getJSONObject("asset")
                    .toAsset("$runtimeDirectory/$name.png", index)
            ).also {
                if (id <= 0 || name.isBlank()) {
                    throw BatteryCatalogParseException(
                        "Invalid Battery decoration in $key at index $index"
                    )
                }
            }
        }
        if (records.map(BatteryDecorationRecord::id).distinct().size != records.size) {
            throw BatteryCatalogParseException("Duplicate Battery decoration IDs in $key")
        }
        return records.sortedBy(BatteryDecorationRecord::id)
    }

    private fun parseEmotions(root: JSONObject): List<BatteryDecorationRecord> {
        val records = root.optJSONArray("emotions")?.mapObjects { item, index ->
            val id = item.getInt("id")
            val name = item.getString("name").trim()
            val groupKey = item.optString("groupKey").trim().ifBlank { null }
            val order = if (item.has("order")) item.getInt("order") else id - 1
            BatteryDecorationRecord(
                id = id,
                name = name,
                asset = item.getJSONObject("asset")
                    .toAsset("emotion/$name.png", index),
                preview = item.optJSONObject("preview")
                    ?.toAsset("emotion_preview/$name.png", index),
                groupKey = groupKey,
                order = order
            ).also {
                if (id <= 0 || name.isBlank() || order < 0) {
                    throw BatteryCatalogParseException(
                        "Invalid Battery emotion at index $index"
                    )
                }
            }
        }.orEmpty()
        if (records.map(BatteryDecorationRecord::id).distinct().size != records.size) {
            throw BatteryCatalogParseException("Duplicate Battery emotion IDs")
        }
        return records.sortedBy(BatteryDecorationRecord::id)
    }

    private fun parseEmotionGroups(
        root: JSONObject,
        emotions: List<BatteryDecorationRecord>
    ): List<BatteryEmotionGroupRecord> {
        val array = root.optJSONArray("emotionGroups")
        if (array == null) {
            val availableIds = emotions.map(BatteryDecorationRecord::id).toSet()
            return BATTERY_EMOTION_GROUPS.mapIndexedNotNull { index, group ->
                val ids = group.emotionIds.filter(availableIds::contains)
                ids.takeIf { it.isNotEmpty() }?.let {
                    BatteryEmotionGroupRecord(
                        key = group.key,
                        order = index,
                        emotionIds = ids
                    )
                }
            }
        }
        val groups = array.mapObjects { item, index ->
            val key = item.getString("key").trim()
            val order = item.getInt("order")
            val emotionIds = item.getJSONArray("emotionIds").mapInts()
            BatteryEmotionGroupRecord(
                key = key,
                order = order,
                emotionIds = emotionIds,
                background = item.optJSONObject("background")
                    ?.toAsset("emotion_group/$key.jpg", index)
            ).also {
                if (key.isBlank() || order < 0 || emotionIds.isEmpty()) {
                    throw BatteryCatalogParseException(
                        "Invalid Battery emotion group at index $index"
                    )
                }
            }
        }.sortedBy(BatteryEmotionGroupRecord::order)
        if (groups.map(BatteryEmotionGroupRecord::key).distinct().size != groups.size ||
            groups.flatMap(BatteryEmotionGroupRecord::emotionIds).sorted() !=
            emotions.map(BatteryDecorationRecord::id).sorted()
        ) {
            throw BatteryCatalogParseException("Invalid Battery emotion group coverage")
        }
        if (root.optInt("emotionCount", emotions.size) != emotions.size ||
            root.optInt("emotionGroupCount", groups.size) != groups.size
        ) {
            throw BatteryCatalogParseException("Battery emotion count does not match")
        }
        return groups
    }

    private fun parseAnimations(root: JSONObject): List<BatteryAnimationRecord> {
        val records = root.optJSONArray("animations")?.mapObjects { item, index ->
            val id = item.getInt("id")
            val name = item.getString("name").trim()
            val type = item.getString("type").let { value ->
                BatteryAnimationType.entries.firstOrNull { it.name == value }
                    ?: throw BatteryCatalogParseException(
                        "Invalid Battery animation type at index $index"
                    )
            }
            BatteryAnimationRecord(
                id = id,
                name = name,
                type = type,
                asset = item.getJSONObject("asset")
                    .toAsset("animation/$name", index)
            ).also {
                val extensionMatches = when (type) {
                    BatteryAnimationType.GIF -> name.endsWith(".gif")
                    BatteryAnimationType.LOTTIE -> name.endsWith(".json")
                }
                if (id <= 0 || !ANIMATION_NAME.matches(name) || !extensionMatches) {
                    throw BatteryCatalogParseException(
                        "Invalid Battery animation at index $index"
                    )
                }
            }
        }.orEmpty()
        if (records.map(BatteryAnimationRecord::id).distinct().size != records.size ||
            records.map(BatteryAnimationRecord::name).distinct().size != records.size
        ) {
            throw BatteryCatalogParseException("Duplicate Battery animations")
        }
        return records.sortedBy(BatteryAnimationRecord::id)
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

    private fun JSONArray.mapInts(): List<Int> = List(length()) { index -> getInt(index) }

    private companion object {
        const val SCHEMA_VERSION = 1
        const val MAX_IMAGE_DIMENSION = 4096
        val SHA_256 = Regex("[0-9a-f]{64}")
        val SLUG = Regex("[a-z0-9]+(?:-[a-z0-9]+)*")
        val ANIMATION_NAME = Regex("(?:cute_[1-5]\\.json|(?:[1-9]|1[0-9]|2[01])\\.gif)")
    }
}

class BatteryCatalogParseException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)
