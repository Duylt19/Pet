package com.asianmobile.emojibattery.shimeji.ui.battery.troll

import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollCatalogError
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollEntry

/** State of the Battery Troll Themes grid (Figma `8315:7971` / `8315:8359` / `8326:8469`). */
data class BatteryTrollUiState(
    val trolls: List<BatteryTrollEntry> = emptyList(),
    val rewardUnlockedTrollIds: Set<Int> = emptySet(),
    val isPremium: Boolean = false,
    val selectedTrollId: Int = 0,
    val pendingUnlockTrollId: Int? = null,
    val isRewardInProgress: Boolean = false,
    val message: BatteryTrollMessage? = null,
    val isLoading: Boolean = true,
    val error: BatteryTrollCatalogError? = null
)

enum class BatteryTrollMessage {
    REWARD_NOT_EARNED
}

sealed interface BatteryTrollEffect {
    data class OpenCustomize(val trollId: Int) : BatteryTrollEffect
    data object ShowRewardedAd : BatteryTrollEffect
}
