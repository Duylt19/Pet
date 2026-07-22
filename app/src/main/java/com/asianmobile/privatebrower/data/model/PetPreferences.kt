package com.asianmobile.privatebrower.data.model

data class PetPreferences(
    val selectedPackKey: String = DEFAULT_SELECTED_PACK_KEY,
    val petCount: Int = DEFAULT_PET_COUNT,
    val sizePercent: Int = DEFAULT_SIZE_PERCENT,
    val speedPercent: Int = DEFAULT_SPEED_PERCENT,
    val soundEnabled: Boolean = false,
    val interactionEnabled: Boolean = true,
    val lastPositions: List<PetPositionFraction> = emptyList()
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
