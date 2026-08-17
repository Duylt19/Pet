package com.asianmobile.emojibattery.shimeji.data.repository.impl

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.asianmobile.emojibattery.shimeji.data.local.dataStore
import com.asianmobile.emojibattery.shimeji.data.repository.PetStoreRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

@Singleton
class DataStorePetStoreRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) : PetStoreRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val names = MutableStateFlow<Map<Int, String>>(emptyMap())
    override val customNames: StateFlow<Map<Int, String>> = names.asStateFlow()

    init {
        scope.launch {
            context.dataStore.data.collect { preferences ->
                names.value = decodeNames(preferences[CUSTOM_NAMES].orEmpty())
            }
        }
    }

    override suspend fun saveCustomName(petId: Int, name: String) {
        if (petId < 0) return
        val normalized = name.trim().take(MAX_NAME_LENGTH)
        context.dataStore.edit { preferences ->
            val updated = decodeNames(preferences[CUSTOM_NAMES].orEmpty()).toMutableMap()
            if (normalized.isEmpty()) updated.remove(petId) else updated[petId] = normalized
            preferences[CUSTOM_NAMES] = JSONObject().apply {
                updated.toSortedMap().forEach { (id, value) -> put(id.toString(), value) }
            }.toString()
        }
    }

    private fun decodeNames(raw: String): Map<Int, String> = runCatching {
        val json = JSONObject(raw.ifBlank { "{}" })
        buildMap {
            json.keys().forEach { key ->
                val id = key.toIntOrNull() ?: return@forEach
                val value = json.optString(key).trim().take(MAX_NAME_LENGTH)
                if (value.isNotEmpty()) put(id, value)
            }
        }
    }.getOrDefault(emptyMap())

    private companion object {
        val CUSTOM_NAMES = stringPreferencesKey("pet_store_custom_names")
        const val MAX_NAME_LENGTH = 24
    }
}
