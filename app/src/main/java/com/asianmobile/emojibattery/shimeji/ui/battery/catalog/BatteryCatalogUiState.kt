package com.asianmobile.emojibattery.shimeji.ui.battery.catalog

import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogCategory
import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogError
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry

data class BatteryCatalogUiState(
    val themes: List<BatteryThemeEntry> = emptyList(),
    val visibleThemes: List<BatteryThemeEntry> = emptyList(),
    val categories: List<BatteryCatalogCategory> = emptyList(),
    val selectedCategoryId: Int? = null,
    val searchQuery: String = "",
    val favoriteThemeIds: Set<Int> = emptySet(),
    val rewardUnlockedThemeIds: Set<Int> = emptySet(),
    val isPremium: Boolean = false,
    val pendingUnlockThemeId: Int? = null,
    val isRewardInProgress: Boolean = false,
    val currentStyle: BatteryCurrentStyle? = null,
    val message: BatteryCatalogMessage? = null,
    val isLoading: Boolean = true,
    val error: BatteryCatalogError? = null
) {
    val showCurrentStyle: Boolean
        get() = currentStyle != null && selectedCategoryId == null && searchQuery.isBlank()
}

data class BatteryCurrentStyle(
    val config: BatteryStatusConfig,
    val batteryTheme: BatteryThemeEntry?,
    val emojiTheme: BatteryThemeEntry?,
    val backgroundPath: String?
)

const val CURRENT_BATTERY_STYLE_ID = -1

enum class BatteryCatalogMessage {
    REWARD_NOT_EARNED,
    THEME_UNAVAILABLE
}

sealed interface BatteryCatalogEffect {
    data class OpenTheme(val themeId: Int) : BatteryCatalogEffect
    data object ShowRewardedAd : BatteryCatalogEffect
}
