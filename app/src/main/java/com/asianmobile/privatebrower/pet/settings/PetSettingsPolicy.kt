package com.asianmobile.privatebrower.pet.settings

import com.asianmobile.privatebrower.data.model.PetPositionFraction
import com.asianmobile.privatebrower.data.model.DEFAULT_SELECTED_PACK_KEY
import com.asianmobile.privatebrower.data.model.MAX_PET_SLOTS

class PetSettingsPolicy {
    fun sanitizePetCount(value: Int, maxPets: Int): Int =
        value.coerceIn(MIN_PET_COUNT, maxPets.coerceAtLeast(MIN_PET_COUNT))

    fun sanitizeSizePercent(value: Int): Int =
        nearestStep(value, MIN_SIZE_PERCENT, MAX_SIZE_PERCENT, SIZE_STEP_PERCENT)

    fun sanitizeSpeedPercent(value: Int): Int =
        nearestStep(value, MIN_SPEED_PERCENT, MAX_SPEED_PERCENT, SPEED_STEP_PERCENT)

    fun targetFramesPerSecond(petCount: Int, budgetFramesPerSecond: Int): Int =
        if (petCount >= 3) {
            minOf(budgetFramesPerSecond, THREE_PET_FRAMES_PER_SECOND)
        } else {
            budgetFramesPerSecond
        }

    fun shouldPersistPositions(
        sessionResetRevision: Int,
        currentResetRevision: Int
    ): Boolean = sessionResetRevision == currentResetRevision

    private fun nearestStep(value: Int, minimum: Int, maximum: Int, step: Int): Int {
        val clamped = value.coerceIn(minimum, maximum)
        val stepsFromMinimum = ((clamped - minimum) + step / 2) / step
        return minimum + stepsFromMinimum * step
    }

    companion object {
        const val MIN_PET_COUNT = 1
        const val MIN_SIZE_PERCENT = 75
        const val MAX_SIZE_PERCENT = 150
        const val SIZE_STEP_PERCENT = 25
        const val MIN_SPEED_PERCENT = 50
        const val MAX_SPEED_PERCENT = 150
        const val SPEED_STEP_PERCENT = 25
        const val THREE_PET_FRAMES_PER_SECOND = 24
    }
}

class PetSelectionCodec {
    fun encode(packKeys: List<String>): String = packKeys
        .map(String::trim)
        .filter(String::isNotEmpty)
        .take(MAX_PET_SLOTS)
        .joinToString(SEPARATOR)

    fun decode(encoded: String): List<String> = encoded
        .split(SEPARATOR)
        .map(String::trim)
        .filter(String::isNotEmpty)
        .take(MAX_PET_SLOTS)

    fun materialize(
        packKeys: List<String>,
        fallbackKey: String = DEFAULT_SELECTED_PACK_KEY
    ): List<String> {
        val sanitized = packKeys
            .map(String::trim)
            .filter(String::isNotEmpty)
            .take(MAX_PET_SLOTS)
        val fallback = sanitized.firstOrNull() ?: fallbackKey
        return List(MAX_PET_SLOTS) { slotIndex ->
            sanitized.getOrNull(slotIndex) ?: fallback
        }
    }

    fun replace(
        packKeys: List<String>,
        slotIndex: Int,
        key: String
    ): List<String> {
        val materialized = materialize(packKeys).toMutableList()
        if (slotIndex in materialized.indices && key.isNotBlank()) {
            materialized[slotIndex] = key.trim()
        }
        return materialized
    }

    private companion object {
        const val SEPARATOR = "\n"
    }
}

class PetPositionCodec {
    fun encode(positions: List<PetPositionFraction>): String = positions
        .take(MAX_POSITIONS)
        .joinToString(separator = ";") { position ->
            "${position.x.coerceIn(0f, 1f)},${position.y.coerceIn(0f, 1f)}"
        }

    fun decode(encoded: String): List<PetPositionFraction> = encoded
        .split(';')
        .take(MAX_POSITIONS)
        .mapNotNull { item ->
            val values = item.split(',')
            if (values.size != 2) return@mapNotNull null
            val x = values[0].toFloatOrNull()?.takeIf(Float::isFinite) ?: return@mapNotNull null
            val y = values[1].toFloatOrNull()?.takeIf(Float::isFinite) ?: return@mapNotNull null
            PetPositionFraction(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
        }

    private companion object {
        const val MAX_POSITIONS = 3
    }
}
