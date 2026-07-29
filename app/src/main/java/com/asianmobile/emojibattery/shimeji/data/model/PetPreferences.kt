package com.asianmobile.emojibattery.shimeji.data.model

data class PetPreferences(
    val petSlots: List<PetSlotPreferences> =
        List(MAX_PET_SLOTS) { PetSlotPreferences() },
    val petCount: Int = DEFAULT_PET_COUNT,
    val displayMode: PetDisplayMode = PetDisplayMode.MIXED,
    val swarm: PetSwarmPreferences = PetSwarmPreferences(),
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

    val enabledMixedPetCount: Int
        get() = petSlots.take(petCount).count(PetSlotPreferences::isEnabled)

    val runtimePetCount: Int
        get() = when (displayMode) {
            PetDisplayMode.MIXED -> enabledMixedPetCount
            PetDisplayMode.SWARM -> if (swarm.hasSelectedPack) swarm.count else 0
        }
}

data class PetSlotPreferences(
    val packKey: String = DEFAULT_SELECTED_PACK_KEY,
    val sizePercent: Int = DEFAULT_SIZE_PERCENT,
    val speedPercent: Int = DEFAULT_SPEED_PERCENT,
    val messagesEnabled: Boolean = true,
    val customMessages: List<String> = emptyList(),
    val interactionEnabled: Boolean = true,
    val isEnabled: Boolean = true
)

enum class PetDisplayMode {
    MIXED,
    SWARM
}

data class PetSwarmPreferences(
    val packKey: String = "",
    val count: Int = DEFAULT_SWARM_COUNT,
    val unlockedByReward: Boolean = false,
    val sizePercent: Int = DEFAULT_SWARM_SIZE_PERCENT,
    val speedPercent: Int = DEFAULT_SWARM_SPEED_PERCENT,
    val randomizeSizeAndSpeed: Boolean = false,
    val constrainMovementArea: Boolean = false,
    val movementInsets: PetSwarmMovementInsets = PetSwarmMovementInsets()
) {
    val hasSelectedPack: Boolean
        get() = packKey.isNotBlank()
}

data class PetSwarmMovementInsets(
    val topPercent: Int = 0,
    val bottomPercent: Int = 0,
    val leftPercent: Int = 0,
    val rightPercent: Int = 0
)

data class PetPerformanceBudget(
    val maxPets: Int,
    val targetFramesPerSecond: Int,
    val maxSwarmPets: Int = MAX_SWARM_PETS
)

data class PetPositionFraction(
    val x: Float,
    val y: Float
)

const val DEFAULT_SELECTED_PACK_KEY = "builtin.orange-cat@1"
const val DEFAULT_PET_COUNT = 1
const val DEFAULT_SIZE_PERCENT = 100
const val DEFAULT_SPEED_PERCENT = 100
const val DEFAULT_SWARM_COUNT = 6
const val DEFAULT_SWARM_SIZE_PERCENT = 80
const val DEFAULT_SWARM_SPEED_PERCENT = 100
const val MAX_PET_SLOTS = 3
const val MIN_SWARM_PETS = 1
const val MAX_SWARM_PETS = 12
