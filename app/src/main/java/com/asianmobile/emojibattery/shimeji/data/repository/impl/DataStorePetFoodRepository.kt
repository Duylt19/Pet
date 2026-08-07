package com.asianmobile.emojibattery.shimeji.data.repository.impl

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.asianmobile.emojibattery.shimeji.data.local.dataStore
import com.asianmobile.emojibattery.shimeji.data.repository.PetFoodRepository
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
class DataStorePetFoodRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) : PetFoodRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val state = MutableStateFlow<Map<String, Int>>(emptyMap())
    override val inventory: StateFlow<Map<String, Int>> = state.asStateFlow()

    init {
        scope.launch {
            context.dataStore.data.collect { preferences ->
                state.value = decode(preferences[INVENTORY].orEmpty())
            }
        }
    }

    override suspend fun grant(foodId: String, portions: Int) {
        if (foodId.isBlank() || portions <= 0) return
        context.dataStore.edit { preferences ->
            val updated = decode(preferences[INVENTORY].orEmpty()).toMutableMap()
            val held = updated[foodId] ?: 0
            updated[foodId] = (held + portions).coerceAtMost(MAX_PORTIONS)
            preferences[INVENTORY] = encode(updated)
        }
    }

    override suspend fun consume(foodId: String): Boolean {
        if (foodId.isBlank()) return false
        var consumed = false
        context.dataStore.edit { preferences ->
            val updated = decode(preferences[INVENTORY].orEmpty()).toMutableMap()
            val held = updated[foodId] ?: 0
            if (held <= 0) return@edit
            if (held == 1) updated.remove(foodId) else updated[foodId] = held - 1
            preferences[INVENTORY] = encode(updated)
            consumed = true
        }
        return consumed
    }

    private fun encode(inventory: Map<String, Int>): String = JSONObject().apply {
        inventory.toSortedMap().forEach { (id, count) -> if (count > 0) put(id, count) }
    }.toString()

    private fun decode(raw: String): Map<String, Int> = runCatching {
        val json = JSONObject(raw.ifBlank { "{}" })
        buildMap {
            json.keys().forEach { key ->
                val count = json.optInt(key).coerceIn(0, MAX_PORTIONS)
                if (count > 0) put(key, count)
            }
        }
    }.getOrDefault(emptyMap())

    private companion object {
        val INVENTORY = stringPreferencesKey("pet_food_inventory")
        const val MAX_PORTIONS = 99
    }
}
