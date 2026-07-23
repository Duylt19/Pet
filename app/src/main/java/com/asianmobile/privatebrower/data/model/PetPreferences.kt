package com.asianmobile.privatebrower.data.model

data class PetPreferences(
    val petSlots: List<PetSlotPreferences> =
        List(MAX_PET_SLOTS) { PetSlotPreferences() },
    val petCount: Int = DEFAULT_PET_COUNT,
    val soundEnabled: Boolean = false,
    val lastPositions: List<PetPositionFraction?> = List(MAX_PET_SLOTS) { null },
    val positionResetRevisions: List<Int> = List(MAX_PET_SLOTS) { 0 }
) {
    val selectedPackKeys: List<String>
        get() = petSlots.map(PetSlotPreferences::packKey)

    fun slot(slotIndex: Int): PetSlotPreferences =
        petSlots.getOrNull(slotIndex)
            ?: petSlots.firstOrNull()
            ?: PetSlotPreferences()

    fun packKeyForSlot(slotIndex: Int): String =
        slot(slotIndex).packKey.takeIf(String::isNotBlank) ?: DEFAULT_SELECTED_PACK_KEY
}

data class PetSlotPreferences(
    val packKey: String = DEFAULT_SELECTED_PACK_KEY,
    val sizePercent: Int = DEFAULT_SIZE_PERCENT,
    val speedPercent: Int = DEFAULT_SPEED_PERCENT,
    val messagesEnabled: Boolean = true,
    val customMessages: List<String> = emptyList(),
    val interactionEnabled: Boolean = true
)

data class PetPositionFraction(
    val x: Float,
    val y: Float
)

data class PetPerformanceBudget(
    val maxPets: Int,
    val targetFramesPerSecond: Int
)

const val DEFAULT_SELECTED_PACK_KEY = "builtin.orange-cat@1"
const val DEFAULT_PET_COUNT = 1
const val DEFAULT_SIZE_PERCENT = 100
const val DEFAULT_SPEED_PERCENT = 100
const val MAX_PET_SLOTS = 3
