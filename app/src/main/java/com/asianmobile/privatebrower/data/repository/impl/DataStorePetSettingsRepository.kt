package com.asianmobile.privatebrower.data.repository.impl

import android.app.ActivityManager
import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.asianmobile.privatebrower.data.local.dataStore
import com.asianmobile.privatebrower.data.model.DEFAULT_PET_COUNT
import com.asianmobile.privatebrower.data.model.DEFAULT_SELECTED_PACK_KEY
import com.asianmobile.privatebrower.data.model.DEFAULT_SIZE_PERCENT
import com.asianmobile.privatebrower.data.model.DEFAULT_SPEED_PERCENT
import com.asianmobile.privatebrower.data.model.MAX_PET_SLOTS
import com.asianmobile.privatebrower.data.model.PetPerformanceBudget
import com.asianmobile.privatebrower.data.model.PetPositionFraction
import com.asianmobile.privatebrower.data.model.PetPreferences
import com.asianmobile.privatebrower.data.repository.PetSettingsRepository
import com.asianmobile.privatebrower.pet.settings.PetPositionCodec
import com.asianmobile.privatebrower.pet.settings.PetSelectionCodec
import com.asianmobile.privatebrower.pet.settings.PetSettingsPolicy
import com.asianmobile.privatebrower.pet.speech.PetMessageListPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Singleton
class DataStorePetSettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) : PetSettingsRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val policy = PetSettingsPolicy()
    private val positionCodec = PetPositionCodec()
    private val selectionCodec = PetSelectionCodec()
    private val messageListPolicy = PetMessageListPolicy()
    private val activityManager = context.getSystemService(ActivityManager::class.java)

    override val performanceBudget = if (activityManager.isLowRamDevice) {
        PetPerformanceBudget(maxPets = 2, targetFramesPerSecond = 24)
    } else {
        PetPerformanceBudget(maxPets = 3, targetFramesPerSecond = 30)
    }

    override val preferences: StateFlow<PetPreferences> = context.dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map(::decode)
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = PetPreferences()
        )

    override fun updateSelectedPack(slotIndex: Int, key: String) {
        if (slotIndex !in 0 until MAX_PET_SLOTS || key.isBlank()) return
        edit { preferences ->
            val current = decodeSelectedPackKeys(preferences).toMutableList()
            while (current.size <= slotIndex) {
                current += current.firstOrNull() ?: DEFAULT_SELECTED_PACK_KEY
            }
            current[slotIndex] = key
            writeSelectedPackKeys(preferences, current)
        }
    }

    override fun updateSelectedPacks(keys: List<String>) = edit { preferences ->
        writeSelectedPackKeys(
            preferences,
            keys.ifEmpty { listOf(DEFAULT_SELECTED_PACK_KEY) }
        )
    }

    override fun updatePetCount(count: Int) = edit { preferences ->
        preferences[PET_COUNT] = policy.sanitizePetCount(count, performanceBudget.maxPets)
    }

    override fun updateSizePercent(percent: Int) = edit { preferences ->
        preferences[SIZE_PERCENT] = policy.sanitizeSizePercent(percent)
    }

    override fun updateSpeedPercent(percent: Int) = edit { preferences ->
        preferences[SPEED_PERCENT] = policy.sanitizeSpeedPercent(percent)
    }

    override fun updateSoundEnabled(enabled: Boolean) = edit { preferences ->
        preferences[SOUND_ENABLED] = enabled
    }

    override fun updateMessagesEnabled(enabled: Boolean) = edit { preferences ->
        preferences[MESSAGES_ENABLED] = enabled
    }

    override fun updateCustomMessages(messages: List<String>) = edit { preferences ->
        preferences[CUSTOM_MESSAGES] = messageListPolicy.encode(messages)
    }

    override fun updateInteractionEnabled(enabled: Boolean) = edit { preferences ->
        preferences[INTERACTION_ENABLED] = enabled
    }

    override fun updateLastPositions(
        positions: List<PetPositionFraction>,
        sessionResetRevision: Int
    ) = edit { preferences ->
        val currentRevision = preferences[POSITION_RESET_REVISION] ?: 0
        if (policy.shouldPersistPositions(sessionResetRevision, currentRevision)) {
            preferences[LAST_POSITIONS] = positionCodec.encode(positions)
        }
    }

    override fun resetLastPositions() = edit { preferences ->
        preferences[LAST_POSITIONS] = ""
        preferences[POSITION_RESET_REVISION] =
            (preferences[POSITION_RESET_REVISION] ?: 0) + 1
    }

    private fun decode(preferences: Preferences): PetPreferences = PetPreferences(
        selectedPackKeys = decodeSelectedPackKeys(preferences),
        petCount = policy.sanitizePetCount(
            preferences[PET_COUNT] ?: DEFAULT_PET_COUNT,
            performanceBudget.maxPets
        ),
        sizePercent = policy.sanitizeSizePercent(
            preferences[SIZE_PERCENT] ?: DEFAULT_SIZE_PERCENT
        ),
        speedPercent = policy.sanitizeSpeedPercent(
            preferences[SPEED_PERCENT] ?: DEFAULT_SPEED_PERCENT
        ),
        soundEnabled = preferences[SOUND_ENABLED] ?: false,
        messagesEnabled = preferences[MESSAGES_ENABLED] ?: true,
        customMessages = messageListPolicy.decode(preferences[CUSTOM_MESSAGES].orEmpty()),
        interactionEnabled = preferences[INTERACTION_ENABLED] ?: true,
        lastPositions = positionCodec.decode(preferences[LAST_POSITIONS].orEmpty()),
        positionResetRevision = preferences[POSITION_RESET_REVISION] ?: 0
    )

    private fun decodeSelectedPackKeys(preferences: Preferences): List<String> =
        selectionCodec.decode(preferences[SELECTED_PACK_KEYS].orEmpty())
            .ifEmpty {
                listOf(preferences[SELECTED_PACK_KEY] ?: DEFAULT_SELECTED_PACK_KEY)
            }

    private fun writeSelectedPackKeys(
        preferences: androidx.datastore.preferences.core.MutablePreferences,
        keys: List<String>
    ) {
        val sanitized = selectionCodec.decode(selectionCodec.encode(keys))
            .ifEmpty { listOf(DEFAULT_SELECTED_PACK_KEY) }
        preferences[SELECTED_PACK_KEYS] = selectionCodec.encode(sanitized)
        preferences[SELECTED_PACK_KEY] = sanitized.first()
    }

    private fun edit(block: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        scope.launch { context.dataStore.edit(block) }
    }

    private companion object {
        val SELECTED_PACK_KEY = stringPreferencesKey("pet_selected_pack_key")
        val SELECTED_PACK_KEYS = stringPreferencesKey("pet_selected_pack_keys")
        val PET_COUNT = intPreferencesKey("pet_count")
        val SIZE_PERCENT = intPreferencesKey("pet_size_percent")
        val SPEED_PERCENT = intPreferencesKey("pet_speed_percent")
        val SOUND_ENABLED = booleanPreferencesKey("pet_sound_enabled")
        val MESSAGES_ENABLED = booleanPreferencesKey("pet_messages_enabled")
        val CUSTOM_MESSAGES = stringPreferencesKey("pet_custom_messages")
        val INTERACTION_ENABLED = booleanPreferencesKey("pet_interaction_enabled")
        val LAST_POSITIONS = stringPreferencesKey("pet_last_positions")
        val POSITION_RESET_REVISION = intPreferencesKey("pet_position_reset_revision")
    }
}
