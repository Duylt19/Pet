package com.asianmobile.emojibattery.shimeji.pet.overlay

import com.asianmobile.emojibattery.shimeji.data.model.LEGACY_DEFAULT_SELECTED_PACK_KEY

/**
 * My Pet Room decides which pets float on screen through its Active/Inactive switches. A slot
 * still holding the legacy built-in key was never chosen by the user, so it does not count as a
 * pet and must not be treated as an occupied slot either.
 */
object PetOverlayRosterPolicy {
    fun isUnchosen(packKey: String): Boolean =
        packKey.isBlank() || packKey == LEGACY_DEFAULT_SELECTED_PACK_KEY

    /** True when starting the overlay would actually show a pet the user picked. */
    fun hasChosenPet(
        slotPackKeys: List<String>,
        slotEnabled: List<Boolean>,
        petCount: Int
    ): Boolean = slotPackKeys.take(petCount.coerceAtLeast(0)).withIndex().any { (index, key) ->
        !isUnchosen(key) && slotEnabled.getOrElse(index) { false }
    }

    /**
     * Slot keys as My Pet Room sees them: anything past the configured roster, and any slot that
     * still holds the legacy built-in key, is free for the next pet the user activates.
     */
    fun freeableSlotKeys(slotPackKeys: List<String>, petCount: Int): List<String> =
        slotPackKeys.mapIndexed { index, key ->
            if (index < petCount && !isUnchosen(key)) key else ""
        }
}
