package com.asianmobile.emojibattery.shimeji.ui.battery.catalog

import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogCategory
import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogError
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDecorationEntry
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry

data class BatteryCatalogUiState(
    val themes: List<BatteryThemeEntry> = emptyList(),
    val backgrounds: List<BatteryDecorationEntry> = emptyList(),
    val categories: List<BatteryCatalogCategory> = emptyList(),
    val favoriteThemeIds: Set<Int> = emptySet(),
    val rewardUnlockedThemeIds: Set<Int> = emptySet(),
    val rewardUnlockedBackgroundIds: Set<Int> = emptySet(),
    val isPremium: Boolean = false,
    val pendingUnlockThemeId: Int? = null,
    val pendingUnlockBackgroundId: Int? = null,
    val isRewardInProgress: Boolean = false,
    val sections: List<BatteryCatalogSection> = emptyList(),
    val selectedThemeId: Int = 0,
    val isBatteryEnabled: Boolean = false,
    val message: BatteryCatalogMessage? = null,
    val isLoading: Boolean = true,
    val error: BatteryCatalogError? = null
)

data class BatteryCatalogSection(
    val category: BatteryCatalogCategory,
    val themes: List<BatteryThemeEntry>
)

const val CURRENT_BATTERY_STYLE_ID = -1

enum class BatteryCatalogMessage {
    REWARD_NOT_EARNED,
    THEME_UNAVAILABLE
}

sealed interface BatteryCatalogEffect {
    data class OpenTheme(val themeId: Int) : BatteryCatalogEffect
    data class OpenBackground(val backgroundId: Int) : BatteryCatalogEffect
    data object ShowRewardedAd : BatteryCatalogEffect
    data object RequestBatteryAccessibility : BatteryCatalogEffect
}
