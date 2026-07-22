package com.asianmobile.privatebrower.data.repository

import com.asianmobile.privatebrower.data.model.PetPerformanceBudget
import com.asianmobile.privatebrower.data.model.PetPositionFraction
import com.asianmobile.privatebrower.data.model.PetPreferences
import kotlinx.coroutines.flow.StateFlow

interface PetSettingsRepository {
    val preferences: StateFlow<PetPreferences>
    val performanceBudget: PetPerformanceBudget

    fun updateSelectedPack(key: String)
    fun updatePetCount(count: Int)
    fun updateSizePercent(percent: Int)
    fun updateSpeedPercent(percent: Int)
    fun updateSoundEnabled(enabled: Boolean)
    fun updateInteractionEnabled(enabled: Boolean)
    fun updateLastPositions(positions: List<PetPositionFraction>)
}
