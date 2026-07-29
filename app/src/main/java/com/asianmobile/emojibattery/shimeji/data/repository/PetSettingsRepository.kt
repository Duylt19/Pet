package com.asianmobile.emojibattery.shimeji.data.repository

import com.asianmobile.emojibattery.shimeji.data.model.PetPerformanceBudget
import com.asianmobile.emojibattery.shimeji.data.model.PetDisplayMode
import com.asianmobile.emojibattery.shimeji.data.model.PetPositionFraction
import com.asianmobile.emojibattery.shimeji.data.model.PetPreferences
import com.asianmobile.emojibattery.shimeji.data.model.PetSwarmMovementInsets
import kotlinx.coroutines.flow.StateFlow

interface PetSettingsRepository {
    val preferences: StateFlow<PetPreferences>
    val performanceBudget: PetPerformanceBudget

    fun updateSelectedPack(slotIndex: Int, key: String)
    fun updateSelectedPacks(keys: List<String>)
    fun updatePetCount(count: Int)
    fun unlockMixedSlotByReward(slotIndex: Int)
    fun updateDisplayMode(mode: PetDisplayMode)
    fun removePet(slotIndex: Int)
    fun updateSlotEnabled(slotIndex: Int, enabled: Boolean)
    fun updateSwarmPack(key: String)
    fun clearSwarmPack()
    fun updateSwarmCount(count: Int)
    fun updateSwarmSizePercent(percent: Int)
    fun updateSwarmSpeedPercent(percent: Int)
    fun updateSwarmRandomization(enabled: Boolean)
    fun updateSwarmMovementAreaEnabled(enabled: Boolean)
    fun updateSwarmMovementInsets(insets: PetSwarmMovementInsets)
    fun unlockSwarmByReward()
    fun updateSizePercent(slotIndex: Int, percent: Int)
    fun updateSpeedPercent(slotIndex: Int, percent: Int)
    fun updateSoundEnabled(enabled: Boolean)
    fun updateMessagesEnabled(slotIndex: Int, enabled: Boolean)
    fun updateCustomMessages(slotIndex: Int, messages: List<String>)
    fun updateInteractionEnabled(slotIndex: Int, enabled: Boolean)
    fun updateLastPositions(
        positions: List<PetPositionFraction>,
        sessionResetRevisions: List<Int>
    )
    fun resetLastPosition(slotIndex: Int)
}
