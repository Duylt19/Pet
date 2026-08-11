package com.asianmobile.emojibattery.shimeji.ui.pet.room

sealed interface PetRoomOnScreenAction {
    /** The pack has no slot yet: take a free one and switch it on. */
    data class Assign(val slotIndex: Int) : PetRoomOnScreenAction

    data class SetEnabled(val slotIndex: Int, val enabled: Boolean) : PetRoomOnScreenAction

    /** Nothing to do: already in the requested state, or every slot is taken. */
    data object None : PetRoomOnScreenAction
}

/**
 * "Pet on screen" maps a room pet onto the overlay slots the settings repository already owns,
 * so the floating session keeps one source of truth instead of gaining a second roster.
 */
object PetRoomOnScreenPolicy {
    fun isOnScreen(
        slotPackKeys: List<String>,
        slotEnabled: List<Boolean>,
        packKey: String
    ): Boolean {
        val slotIndex = slotPackKeys.indexOf(packKey)
        return slotIndex >= 0 && slotEnabled.getOrElse(slotIndex) { false }
    }

    fun toggle(
        slotPackKeys: List<String>,
        slotEnabled: List<Boolean>,
        packKey: String,
        turnOn: Boolean
    ): PetRoomOnScreenAction {
        if (packKey.isBlank()) return PetRoomOnScreenAction.None
        val slotIndex = slotPackKeys.indexOf(packKey)
        if (slotIndex >= 0) {
            val current = slotEnabled.getOrElse(slotIndex) { false }
            return if (current == turnOn) {
                PetRoomOnScreenAction.None
            } else {
                PetRoomOnScreenAction.SetEnabled(slotIndex, turnOn)
            }
        }
        if (!turnOn) return PetRoomOnScreenAction.None
        val free = slotPackKeys.indexOfFirst(String::isBlank)
        return if (free >= 0) PetRoomOnScreenAction.Assign(free) else PetRoomOnScreenAction.None
    }
}
