package com.asianmobile.emojibattery.shimeji.pet.overlay

import com.asianmobile.emojibattery.shimeji.data.model.PetDisplayMode
import com.asianmobile.emojibattery.shimeji.data.model.PetPreferences
import com.asianmobile.emojibattery.shimeji.data.model.PetSwarmPreferences

internal enum class PetOverlaySessionUpdate {
    NONE,
    MIXED_ROSTER,
    SWARM_COUNT,
    REBUILD
}

internal data class PetOverlaySessionSignature(
    val mode: PetDisplayMode,
    val mixedPetCount: Int,
    val packKeys: List<String>,
    val swarm: PetSwarmPreferences?
)

internal object PetOverlaySessionPolicy {
    fun resolveUpdate(
        active: PetOverlaySessionSignature?,
        preferences: PetPreferences
    ): PetOverlaySessionUpdate {
        val requested = preferences.overlaySessionSignature()
        if (active == requested) return PetOverlaySessionUpdate.NONE
        if (active == null || active.mode != requested.mode) {
            return PetOverlaySessionUpdate.REBUILD
        }
        if (requested.mode == PetDisplayMode.MIXED) {
            return PetOverlaySessionUpdate.MIXED_ROSTER
        }

        val activeSwarm = active.swarm ?: return PetOverlaySessionUpdate.REBUILD
        val requestedSwarm = requested.swarm ?: return PetOverlaySessionUpdate.REBUILD
        val differsOnlyByCount =
            active.copy(swarm = activeSwarm.copy(count = requestedSwarm.count)) == requested
        return if (differsOnlyByCount) {
            PetOverlaySessionUpdate.SWARM_COUNT
        } else {
            PetOverlaySessionUpdate.REBUILD
        }
    }
}

internal object PetRosterReconciliationPolicy {
    fun retainedIndexes(
        existingPackKeys: List<String>,
        requestedPackKeys: List<String>
    ): List<Int?> {
        val unmatched = existingPackKeys.indices.toMutableList()
        return requestedPackKeys.map { requestedKey ->
            val matchPosition = unmatched.indexOfFirst { existingIndex ->
                existingPackKeys[existingIndex] == requestedKey
            }
            if (matchPosition < 0) {
                null
            } else {
                unmatched.removeAt(matchPosition)
            }
        }
    }
}

internal fun PetPreferences.overlaySessionSignature(): PetOverlaySessionSignature =
    PetOverlaySessionSignature(
        mode = displayMode,
        mixedPetCount = if (displayMode == PetDisplayMode.MIXED) petCount else 0,
        packKeys = when (displayMode) {
            PetDisplayMode.MIXED -> selectedPackKeys.take(petCount)
            PetDisplayMode.SWARM -> listOf(swarm.packKey)
        },
        swarm = swarm.takeIf { displayMode == PetDisplayMode.SWARM }
    )
