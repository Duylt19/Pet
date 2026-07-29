package com.asianmobile.emojibattery.shimeji.data.repository.impl

import android.app.ActivityManager
import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.asianmobile.emojibattery.shimeji.data.local.dataStore
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_PET_COUNT
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_SELECTED_PACK_KEY
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_SIZE_PERCENT
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_SPEED_PERCENT
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_SWARM_COUNT
import com.asianmobile.emojibattery.shimeji.data.model.MAX_SWARM_PETS
import com.asianmobile.emojibattery.shimeji.data.model.MAX_PET_SLOTS
import com.asianmobile.emojibattery.shimeji.data.model.PetDisplayMode
import com.asianmobile.emojibattery.shimeji.data.model.PetPerformanceBudget
import com.asianmobile.emojibattery.shimeji.data.model.PetPositionFraction
import com.asianmobile.emojibattery.shimeji.data.model.PetPreferences
import com.asianmobile.emojibattery.shimeji.data.model.PetSlotPreferences
import com.asianmobile.emojibattery.shimeji.data.model.PetSwarmPreferences
import com.asianmobile.emojibattery.shimeji.data.repository.PetSettingsRepository
import com.asianmobile.emojibattery.shimeji.pet.settings.PetPositionCodec
import com.asianmobile.emojibattery.shimeji.pet.settings.PetSelectionCodec
import com.asianmobile.emojibattery.shimeji.pet.settings.PetSettingsPolicy
import com.asianmobile.emojibattery.shimeji.pet.settings.PetSlotValueCodec
import com.asianmobile.emojibattery.shimeji.pet.speech.PetMessageListPolicy
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
    private val slotValueCodec = PetSlotValueCodec()
    private val messageListPolicy = PetMessageListPolicy()
    private val activityManager = context.getSystemService(ActivityManager::class.java)

    override val performanceBudget = if (activityManager.isLowRamDevice) {
        PetPerformanceBudget(maxPets = 2, targetFramesPerSecond = 24, maxSwarmPets = 6)
    } else {
        PetPerformanceBudget(
            maxPets = 3,
            targetFramesPerSecond = 30,
            maxSwarmPets = MAX_SWARM_PETS
        )
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
            writeSelectedPackKeys(
                preferences,
                selectionCodec.replace(
                    packKeys = decodeSelectedPackKeys(preferences),
                    slotIndex = slotIndex,
                    key = key
                )
            )
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

    override fun updateDisplayMode(mode: PetDisplayMode) = edit { preferences ->
        preferences[DISPLAY_MODE] = mode.name
    }

    override fun removePet(slotIndex: Int) = edit { preferences ->
        val current = decode(preferences)
        if (current.petCount <= 1 || slotIndex !in 0 until current.petCount) return@edit
        val shiftedSlots = current.petSlots.toMutableList().apply {
            removeAt(slotIndex)
            add(PetSlotPreferences())
        }
        val nextPetCount = current.petCount - 1
        val visibleSlots = policy.ensureMixedPetVisible(shiftedSlots, nextPetCount)
        val shiftedPositions = current.lastPositions.toMutableList().apply {
            removeAt(slotIndex)
            add(null)
        }
        val invalidatedRevisions = current.positionResetRevisions
            .toMutableList()
            .apply {
                removeAt(slotIndex)
                add(0)
            }
            .map { it + 1 }
        writePetSlots(preferences, visibleSlots)
        preferences[LAST_POSITIONS] = positionCodec.encode(shiftedPositions)
        preferences[POSITION_RESET_REVISIONS] =
            slotValueCodec.encodeInts(invalidatedRevisions)
        preferences[PET_COUNT] = nextPetCount
    }

    override fun updateSlotEnabled(slotIndex: Int, enabled: Boolean) = edit { preferences ->
        val current = decode(preferences)
        if (slotIndex !in 0 until current.petCount) return@edit
        if (!enabled && current.enabledMixedPetCount <= 1) return@edit
        val slots = current.petSlots.toMutableList()
        slots[slotIndex] = slots[slotIndex].copy(isEnabled = enabled)
        writePetSlots(preferences, slots)
    }

    override fun updateSwarmPack(key: String) = edit { preferences ->
        if (key.isBlank()) return@edit
        preferences[SWARM_PACK_KEY] = key.trim()
    }

    override fun clearSwarmPack() = edit { preferences ->
        preferences.remove(SWARM_PACK_KEY)
    }

    override fun updateSwarmCount(count: Int) = edit { preferences ->
        preferences[SWARM_COUNT] = policy.sanitizeSwarmCount(
            count,
            performanceBudget.maxSwarmPets
        )
    }

    override fun unlockSwarmByReward() = edit { preferences ->
        preferences[SWARM_REWARD_UNLOCKED] = true
    }

    override fun updateSizePercent(slotIndex: Int, percent: Int) = edit { preferences ->
        updateSlot(preferences, slotIndex) { slot ->
            slot.copy(sizePercent = policy.sanitizeSizePercent(percent))
        }
    }

    override fun updateSpeedPercent(slotIndex: Int, percent: Int) = edit { preferences ->
        updateSlot(preferences, slotIndex) { slot ->
            slot.copy(speedPercent = policy.sanitizeSpeedPercent(percent))
        }
    }

    override fun updateSoundEnabled(enabled: Boolean) = edit { preferences ->
        preferences[SOUND_ENABLED] = enabled
    }

    override fun updateMessagesEnabled(slotIndex: Int, enabled: Boolean) = edit { preferences ->
        updateSlot(preferences, slotIndex) { slot ->
            slot.copy(messagesEnabled = enabled)
        }
    }

    override fun updateCustomMessages(
        slotIndex: Int,
        messages: List<String>
    ) = edit { preferences ->
        updateSlot(preferences, slotIndex) { slot ->
            slot.copy(customMessages = messageListPolicy.sanitize(messages))
        }
    }

    override fun updateInteractionEnabled(
        slotIndex: Int,
        enabled: Boolean
    ) = edit { preferences ->
        updateSlot(preferences, slotIndex) { slot ->
            slot.copy(interactionEnabled = enabled)
        }
    }

    override fun updateLastPositions(
        positions: List<PetPositionFraction>,
        sessionResetRevisions: List<Int>
    ) = edit { preferences ->
        val current = decode(preferences)
        val merged = current.lastPositions.toMutableList()
        positions.take(MAX_PET_SLOTS).forEachIndexed { slotIndex, position ->
            val sessionRevision = sessionResetRevisions.getOrNull(slotIndex) ?: return@forEachIndexed
            val currentRevision = current.positionResetRevisions[slotIndex]
            if (policy.shouldPersistPositions(sessionRevision, currentRevision)) {
                merged[slotIndex] = position
            }
        }
        preferences[LAST_POSITIONS] = positionCodec.encode(merged)
    }

    override fun resetLastPosition(slotIndex: Int) = edit { preferences ->
        if (slotIndex !in 0 until MAX_PET_SLOTS) return@edit
        val current = decode(preferences)
        val positions = current.lastPositions.toMutableList()
        positions[slotIndex] = null
        val revisions = current.positionResetRevisions.toMutableList()
        revisions[slotIndex] = revisions[slotIndex] + 1
        preferences[LAST_POSITIONS] = positionCodec.encode(positions)
        preferences[POSITION_RESET_REVISIONS] = slotValueCodec.encodeInts(revisions)
    }

    private fun decode(preferences: Preferences): PetPreferences {
        val packKeys = decodeSelectedPackKeys(preferences)
        val legacySize = policy.sanitizeSizePercent(
            preferences[SIZE_PERCENT] ?: DEFAULT_SIZE_PERCENT
        )
        val legacySpeed = policy.sanitizeSpeedPercent(
            preferences[SPEED_PERCENT] ?: DEFAULT_SPEED_PERCENT
        )
        val legacyMessagesEnabled = preferences[MESSAGES_ENABLED] ?: true
        val legacyCustomMessages =
            messageListPolicy.decode(preferences[CUSTOM_MESSAGES].orEmpty())
        val legacyInteraction = preferences[INTERACTION_ENABLED] ?: true
        val sizeValues = slotValueCodec.decodeInts(
            preferences[SLOT_SIZE_PERCENTS].orEmpty(),
            legacySize
        ).map(policy::sanitizeSizePercent)
        val speedValues = slotValueCodec.decodeInts(
            preferences[SLOT_SPEED_PERCENTS].orEmpty(),
            legacySpeed
        ).map(policy::sanitizeSpeedPercent)
        val messagesEnabledValues = slotValueCodec.decodeBooleans(
            preferences[SLOT_MESSAGES_ENABLED].orEmpty(),
            legacyMessagesEnabled
        )
        val customMessageValues = preferences[SLOT_CUSTOM_MESSAGES]?.let { encodedSlots ->
            slotValueCodec.decodeStrings(encodedSlots).map(messageListPolicy::decode)
        } ?: List(MAX_PET_SLOTS) { legacyCustomMessages }
        val interactionValues = slotValueCodec.decodeBooleans(
            preferences[SLOT_INTERACTION_ENABLED].orEmpty(),
            legacyInteraction
        )
        val enabledValues = slotValueCodec.decodeBooleans(
            preferences[SLOT_ENABLED].orEmpty(),
            true
        )
        val legacyResetRevision = preferences[POSITION_RESET_REVISION] ?: 0
        val resetRevisions = slotValueCodec.decodeInts(
            preferences[POSITION_RESET_REVISIONS].orEmpty(),
            legacyResetRevision
        )
        val petCount = policy.sanitizePetCount(
            preferences[PET_COUNT] ?: DEFAULT_PET_COUNT,
            performanceBudget.maxPets
        )
        val decodedSlots = List(MAX_PET_SLOTS) { slotIndex ->
            PetSlotPreferences(
                packKey = packKeys[slotIndex],
                sizePercent = sizeValues[slotIndex],
                speedPercent = speedValues[slotIndex],
                messagesEnabled = messagesEnabledValues[slotIndex],
                customMessages = customMessageValues[slotIndex],
                interactionEnabled = interactionValues[slotIndex],
                isEnabled = enabledValues[slotIndex]
            )
        }
        return PetPreferences(
            petSlots = policy.ensureMixedPetVisible(decodedSlots, petCount),
            petCount = petCount,
            displayMode = preferences[DISPLAY_MODE]
                ?.let { encoded ->
                    PetDisplayMode.entries.firstOrNull { it.name == encoded }
                }
                ?: PetDisplayMode.MIXED,
            swarm = PetSwarmPreferences(
                packKey = preferences[SWARM_PACK_KEY].orEmpty(),
                count = policy.sanitizeSwarmCount(
                    preferences[SWARM_COUNT] ?: DEFAULT_SWARM_COUNT,
                    performanceBudget.maxSwarmPets
                ),
                unlockedByReward = preferences[SWARM_REWARD_UNLOCKED] ?: false
            ),
            soundEnabled = preferences[SOUND_ENABLED] ?: false,
            lastPositions = positionCodec.decode(preferences[LAST_POSITIONS].orEmpty()),
            positionResetRevisions = resetRevisions
        )
    }

    private fun decodeSelectedPackKeys(preferences: Preferences): List<String> =
        selectionCodec.materialize(
            selectionCodec.decode(preferences[SELECTED_PACK_KEYS].orEmpty())
                .ifEmpty {
                    listOf(preferences[SELECTED_PACK_KEY] ?: DEFAULT_SELECTED_PACK_KEY)
                }
        )

    private fun writeSelectedPackKeys(
        preferences: androidx.datastore.preferences.core.MutablePreferences,
        keys: List<String>
    ) {
        val sanitized = selectionCodec.materialize(keys)
        preferences[SELECTED_PACK_KEYS] = selectionCodec.encode(sanitized)
        preferences[SELECTED_PACK_KEY] = sanitized.first()
    }

    private fun updateSlot(
        preferences: androidx.datastore.preferences.core.MutablePreferences,
        slotIndex: Int,
        transform: (PetSlotPreferences) -> PetSlotPreferences
    ) {
        if (slotIndex !in 0 until MAX_PET_SLOTS) return
        val slots = decode(preferences).petSlots.toMutableList()
        slots[slotIndex] = transform(slots[slotIndex])
        writePetSlots(preferences, slots)
    }

    private fun writePetSlots(
        preferences: androidx.datastore.preferences.core.MutablePreferences,
        slots: List<PetSlotPreferences>
    ) {
        val materialized = List(MAX_PET_SLOTS) { slotIndex ->
            slots.getOrNull(slotIndex) ?: PetSlotPreferences()
        }
        writeSelectedPackKeys(preferences, materialized.map(PetSlotPreferences::packKey))
        preferences[SLOT_SIZE_PERCENTS] =
            slotValueCodec.encodeInts(materialized.map(PetSlotPreferences::sizePercent))
        preferences[SLOT_SPEED_PERCENTS] =
            slotValueCodec.encodeInts(materialized.map(PetSlotPreferences::speedPercent))
        preferences[SLOT_MESSAGES_ENABLED] =
            slotValueCodec.encodeBooleans(materialized.map(PetSlotPreferences::messagesEnabled))
        preferences[SLOT_CUSTOM_MESSAGES] = slotValueCodec.encodeStrings(
            materialized.map { slot -> messageListPolicy.encode(slot.customMessages) }
        )
        preferences[SLOT_INTERACTION_ENABLED] =
            slotValueCodec.encodeBooleans(
                materialized.map(PetSlotPreferences::interactionEnabled)
            )
        preferences[SLOT_ENABLED] =
            slotValueCodec.encodeBooleans(materialized.map(PetSlotPreferences::isEnabled))
    }

    private fun edit(block: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        scope.launch { context.dataStore.edit(block) }
    }

    private companion object {
        val SELECTED_PACK_KEY = stringPreferencesKey("pet_selected_pack_key")
        val SELECTED_PACK_KEYS = stringPreferencesKey("pet_selected_pack_keys")
        val PET_COUNT = intPreferencesKey("pet_count")
        val DISPLAY_MODE = stringPreferencesKey("pet_display_mode")
        val SIZE_PERCENT = intPreferencesKey("pet_size_percent")
        val SPEED_PERCENT = intPreferencesKey("pet_speed_percent")
        val SLOT_SIZE_PERCENTS = stringPreferencesKey("pet_slot_size_percents")
        val SLOT_SPEED_PERCENTS = stringPreferencesKey("pet_slot_speed_percents")
        val SOUND_ENABLED = booleanPreferencesKey("pet_sound_enabled")
        val MESSAGES_ENABLED = booleanPreferencesKey("pet_messages_enabled")
        val CUSTOM_MESSAGES = stringPreferencesKey("pet_custom_messages")
        val INTERACTION_ENABLED = booleanPreferencesKey("pet_interaction_enabled")
        val SLOT_MESSAGES_ENABLED = stringPreferencesKey("pet_slot_messages_enabled")
        val SLOT_CUSTOM_MESSAGES = stringPreferencesKey("pet_slot_custom_messages")
        val SLOT_INTERACTION_ENABLED = stringPreferencesKey("pet_slot_interaction_enabled")
        val SLOT_ENABLED = stringPreferencesKey("pet_slot_enabled")
        val SWARM_PACK_KEY = stringPreferencesKey("pet_swarm_pack_key")
        val SWARM_COUNT = intPreferencesKey("pet_swarm_count")
        val SWARM_REWARD_UNLOCKED = booleanPreferencesKey("pet_swarm_reward_unlocked")
        val LAST_POSITIONS = stringPreferencesKey("pet_last_positions")
        val POSITION_RESET_REVISION = intPreferencesKey("pet_position_reset_revision")
        val POSITION_RESET_REVISIONS =
            stringPreferencesKey("pet_position_reset_revisions")
    }
}
