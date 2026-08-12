package com.asianmobile.emojibattery.shimeji.data.repository.impl

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener

data class OwnerPetCatalogRecord(
    val id: Int,
    val name: String,
    val category: String,
    val author: String?,
    val thumbnail: OwnerPetCatalogAssetRecord? = null,
    val archive: OwnerPetCatalogAssetRecord? = null,
    val speechAnchor: OwnerPetCatalogSpeechAnchorRecord? = null
)

data class OwnerPetCatalogAssetRecord(
    val path: String,
    val sizeBytes: Long,
    val sha256: String
)

data class OwnerPetCatalogSpeechAnchorRecord(
    val x: Float,
    val y: Float
)

data class OwnerPetCatalogDocument(
    val catalogVersion: String?,
    val records: List<OwnerPetCatalogRecord>
)

class OwnerPetCatalogParser {
    fun parse(json: String): List<OwnerPetCatalogRecord> = parseDocument(json).records

    fun parseDocument(json: String): OwnerPetCatalogDocument = try {
        val rootValue = JSONTokener(json).nextValue()
        val catalogVersion: String?
        val root: JSONArray
        when (rootValue) {
            is JSONArray -> {
                catalogVersion = null
                root = rootValue
            }
            is JSONObject -> {
                if (rootValue.getInt("schemaVersion") != SERVER_SCHEMA_VERSION) {
                    throw OwnerPetCatalogParseException("Unsupported owner catalog schema")
                }
                catalogVersion = rootValue.getString("catalogVersion").trim()
                    .takeIf(String::isNotEmpty)
                    ?: throw OwnerPetCatalogParseException("Catalog version is missing")
                root = rootValue.getJSONArray("pets")
                if (rootValue.getInt("petCount") != root.length()) {
                    throw OwnerPetCatalogParseException("Catalog pet count does not match")
                }
            }
            else -> throw OwnerPetCatalogParseException(
                "Owner pet catalog root must be an array or object"
            )
        }
        val records = List(root.length()) { index ->
            val item = root.optJSONObject(index)
                ?: throw OwnerPetCatalogParseException("Catalog item $index must be an object")
            OwnerPetCatalogRecord(
                id = item.getInt("id"),
                name = item.getString("name").trim(),
                category = item.getString("category").trim(),
                author = item.takeUnless { it.isNull("author") }
                    ?.optString("author")
                    ?.trim()
                    ?.takeIf(String::isNotEmpty),
                thumbnail = item.optJSONObject("thumbnail")?.toAsset("thumbnail", index),
                archive = item.optJSONObject("archive")?.toAsset("archive", index),
                speechAnchor = if (item.has("speechAnchor")) {
                    item.getJSONObject("speechAnchor").toSpeechAnchor(index)
                } else {
                    null
                }
            )
        }
        validate(records)
        OwnerPetCatalogDocument(catalogVersion = catalogVersion, records = records)
    } catch (error: OwnerPetCatalogParseException) {
        throw error
    } catch (error: JSONException) {
        throw OwnerPetCatalogParseException("Malformed owner pet catalog", error)
    }

    private fun validate(records: List<OwnerPetCatalogRecord>) {
        if (records.isEmpty()) throw OwnerPetCatalogParseException("Owner pet catalog is empty")
        if (records.any { it.id < 0 || it.name.isBlank() || it.category.isBlank() }) {
            throw OwnerPetCatalogParseException("Owner pet catalog contains invalid metadata")
        }
        if (records.map(OwnerPetCatalogRecord::id).distinct().size != records.size) {
            throw OwnerPetCatalogParseException("Owner pet catalog contains duplicate IDs")
        }
        records.forEach { record ->
            if ((record.thumbnail == null) != (record.archive == null)) {
                throw OwnerPetCatalogParseException(
                    "Owner pet ${record.id} has incomplete remote assets"
                )
            }
            record.archive?.let { archive ->
                if (archive.path != "data/${record.id}.zip") {
                    throw OwnerPetCatalogParseException(
                        "Owner pet ${record.id} has a mismatched archive path"
                    )
                }
            }
            record.thumbnail?.let { thumbnail ->
                if (!THUMBNAIL_PATH.matches(thumbnail.path) ||
                    thumbnail.path.substringAfterLast('/').substringBeforeLast('.') !=
                    record.id.toString()
                ) {
                    throw OwnerPetCatalogParseException(
                        "Owner pet ${record.id} has a mismatched thumbnail path"
                    )
                }
            }
        }
    }

    private fun JSONObject.toAsset(label: String, index: Int): OwnerPetCatalogAssetRecord {
        val path = getString("path")
        val sizeBytes = getLong("sizeBytes")
        val sha256 = getString("sha256").lowercase()
        if (!isSafeRelativePath(path) || sizeBytes <= 0L || !SHA_256.matches(sha256)) {
            throw OwnerPetCatalogParseException(
                "Catalog item $index has invalid $label metadata"
            )
        }
        return OwnerPetCatalogAssetRecord(path, sizeBytes, sha256)
    }

    private fun JSONObject.toSpeechAnchor(index: Int): OwnerPetCatalogSpeechAnchorRecord {
        val xValue = get("x")
        val yValue = get("y")
        if (xValue !is Number || yValue !is Number) {
            throw OwnerPetCatalogParseException(
                "Catalog item $index has invalid speech anchor metadata"
            )
        }
        val x = xValue.toFloat()
        val y = yValue.toFloat()
        if (!x.isFinite() || x !in 0f..1f || !y.isFinite() || y !in 0f..1f) {
            throw OwnerPetCatalogParseException(
                "Catalog item $index has invalid speech anchor metadata"
            )
        }
        return OwnerPetCatalogSpeechAnchorRecord(x, y)
    }

    private fun isSafeRelativePath(path: String): Boolean {
        if (path.isBlank() || path.startsWith('/') || path.startsWith('\\') || '\\' in path) {
            return false
        }
        return path.split('/').none { it.isBlank() || it == "." || it == ".." }
    }

    private companion object {
        const val SERVER_SCHEMA_VERSION = 1
        val SHA_256 = Regex("[0-9a-f]{64}")
        val THUMBNAIL_PATH = Regex("thumb/[0-9]+\\.(?:png|webp)")
    }
}

class OwnerPetCatalogParseException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)
