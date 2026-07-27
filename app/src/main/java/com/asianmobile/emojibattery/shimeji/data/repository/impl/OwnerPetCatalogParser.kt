package com.asianmobile.emojibattery.shimeji.data.repository.impl

import org.json.JSONArray
import org.json.JSONException

data class OwnerPetCatalogRecord(
    val id: Int,
    val name: String,
    val category: String,
    val author: String?
)

class OwnerPetCatalogParser {
    fun parse(json: String): List<OwnerPetCatalogRecord> = try {
        val root = JSONArray(json)
        val records = List(root.length()) { index ->
            val item = root.optJSONObject(index)
                ?: throw OwnerPetCatalogParseException("Catalog item $index must be an object")
            OwnerPetCatalogRecord(
                id = item.getInt("id"),
                name = item.getString("name").trim(),
                category = item.getString("category").trim(),
                author = item.optString("author").trim().takeIf(String::isNotEmpty)
            )
        }
        validate(records)
        records
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
    }
}

class OwnerPetCatalogParseException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)
