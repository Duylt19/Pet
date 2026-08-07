package com.asianmobile.emojibattery.shimeji.data.repository.impl

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.asianmobile.emojibattery.shimeji.data.local.dataStore
import com.asianmobile.emojibattery.shimeji.data.repository.PetCareRepository
import com.asianmobile.emojibattery.shimeji.data.repository.PetEnergyRecord
import com.asianmobile.emojibattery.shimeji.pet.care.PetEnergyPolicy
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
class DataStorePetCareRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) : PetCareRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val energyState = MutableStateFlow<Map<Int, PetEnergyRecord>>(emptyMap())
    private val adoptionState = MutableStateFlow<Map<Int, Long>>(emptyMap())

    override val energy: StateFlow<Map<Int, PetEnergyRecord>> = energyState.asStateFlow()
    override val adoptedAtMillis: StateFlow<Map<Int, Long>> = adoptionState.asStateFlow()

    init {
        scope.launch {
            context.dataStore.data.collect { preferences ->
                energyState.value = decodeEnergy(preferences[ENERGY].orEmpty())
                adoptionState.value = decodeAdoptions(preferences[ADOPTED_AT].orEmpty())
            }
        }
    }

    override suspend fun setEnergy(petId: Int, percent: Int, atMillis: Long) {
        if (petId < 0) return
        val clamped = percent.coerceIn(0, PetEnergyPolicy.MAX_ENERGY)
        context.dataStore.edit { preferences ->
            val updated = decodeEnergy(preferences[ENERGY].orEmpty()).toMutableMap()
            updated[petId] = PetEnergyRecord(clamped, atMillis)
            preferences[ENERGY] = JSONObject().apply {
                updated.toSortedMap().forEach { (id, record) ->
                    put(
                        id.toString(),
                        JSONObject()
                            .put(PERCENT_KEY, record.percent)
                            .put(UPDATED_AT_KEY, record.updatedAtMillis)
                    )
                }
            }.toString()
        }
    }

    override suspend fun rememberAdoption(petId: Int, atMillis: Long) {
        if (petId < 0 || atMillis <= 0L) return
        context.dataStore.edit { preferences ->
            val updated = decodeAdoptions(preferences[ADOPTED_AT].orEmpty()).toMutableMap()
            // The first date wins; a reinstall must not reset how long a pet has been here.
            if (updated.containsKey(petId)) return@edit
            updated[petId] = atMillis
            preferences[ADOPTED_AT] = JSONObject().apply {
                updated.toSortedMap().forEach { (id, millis) -> put(id.toString(), millis) }
            }.toString()
        }
    }

    private fun decodeEnergy(raw: String): Map<Int, PetEnergyRecord> = runCatching {
        val json = JSONObject(raw.ifBlank { "{}" })
        buildMap {
            json.keys().forEach { key ->
                val id = key.toIntOrNull() ?: return@forEach
                val record = json.optJSONObject(key) ?: return@forEach
                put(
                    id,
                    PetEnergyRecord(
                        percent = record.optInt(PERCENT_KEY)
                            .coerceIn(0, PetEnergyPolicy.MAX_ENERGY),
                        updatedAtMillis = record.optLong(UPDATED_AT_KEY)
                    )
                )
            }
        }
    }.getOrDefault(emptyMap())

    private fun decodeAdoptions(raw: String): Map<Int, Long> = runCatching {
        val json = JSONObject(raw.ifBlank { "{}" })
        buildMap {
            json.keys().forEach { key ->
                val id = key.toIntOrNull() ?: return@forEach
                val millis = json.optLong(key)
                if (millis > 0L) put(id, millis)
            }
        }
    }.getOrDefault(emptyMap())

    private companion object {
        val ENERGY = stringPreferencesKey("pet_care_energy")
        val ADOPTED_AT = stringPreferencesKey("pet_care_adopted_at")
        const val PERCENT_KEY = "percent"
        const val UPDATED_AT_KEY = "updatedAt"
    }
}
