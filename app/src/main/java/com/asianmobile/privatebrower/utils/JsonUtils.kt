package com.asianmobile.privatebrower.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/**
 * Copyright © 2026 Asian Mobile Co.,Ltd
 * Created by am_viennv on 3/13/2026
 */
class JsonUtils @Inject constructor(
    @param:ApplicationContext val context: Context,
    val gson: Gson
) {

    internal inline fun <reified T> loadFromAssetsOrCache(
        fileName: String, keyTranslatedQuran: Int
    ): T? = runCatching {
        val json = if (checkFileExist("$keyTranslatedQuran$fileName")) {
            context.readCache("$keyTranslatedQuran$fileName")
        } else {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        }

        val type = object : TypeToken<T>() {}.type
        gson.fromJson<T>(json, type)
    }.getOrNull()

    internal inline fun <reified T> loadFromAssets(
        fileName: String
    ): T? = runCatching {
        val json =
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        val type = object : TypeToken<T>() {}.type
        gson.fromJson<T>(json, type)
    }.getOrNull()

    internal fun checkFileExist(fileName: String) =
        File(context.filesDir, fileName).exists()

    internal fun Context.readAsset(fileName: String): String =
        assets.open(fileName).bufferedReader().use { it.readText() }

    internal fun Context.readCache(fileName: String): String =
        File(filesDir, fileName).bufferedReader().use { it.readText() }

    private fun Context.writeCache(fileName: String, json: String) {
        File(filesDir, fileName).apply {
            parentFile?.mkdirs()
            writeText(json)
        }
    }

    /* ---------------------------------------------------
 * Object -> JSON
 * --------------------------------------------------- */
    fun toJson(any: Any): String = runCatching {
        gson.toJson(any)
    }.getOrDefault("")

    /* ---------------------------------------------------
     * JSON -> Object
     * --------------------------------------------------- */
    inline fun <reified T> fromJson(strValue: String): T? = runCatching {
        gson.fromJson(strValue, T::class.java)
    }.getOrNull()

    /* ---------------------------------------------------
     * JSON -> List<T>
     * --------------------------------------------------- */
    inline fun <reified T> fromJsonList(strValue: String): List<T>? = runCatching {
        val type = object : TypeToken<List<T>>() {}.type
        gson.fromJson<List<T>>(strValue, type)
    }.getOrNull()

    /* ---------------------------------------------------
     * JSON -> Map<K, V>
     * --------------------------------------------------- */
    inline fun <reified K, reified V> String.fromJsonMap(): Map<K, V>? = runCatching {
        val type = object : TypeToken<Map<K, V>>() {}.type
        gson.fromJson<Map<K, V>>(this, type)
    }.getOrNull()

}


