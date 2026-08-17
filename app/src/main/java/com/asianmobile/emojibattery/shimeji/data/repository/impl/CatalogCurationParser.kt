package com.asianmobile.emojibattery.shimeji.data.repository.impl

import org.json.JSONObject

/** Reads and normalizes an optional ordered ID list from the evolving schema-v1 baseline. */
internal fun JSONObject.optionalCuratedIds(
    key: String,
    fallback: List<Int>
): List<Int> {
    if (!has(key)) return fallback
    val ids = getJSONArray(key)
    return buildList(ids.length()) {
        repeat(ids.length()) { index ->
            ids.optInt(index, INVALID_CATALOG_ID)
                .takeIf { it > 0 && it !in this }
                ?.let(::add)
        }
    }
}

private const val INVALID_CATALOG_ID = Int.MIN_VALUE
