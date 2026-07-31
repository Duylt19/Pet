package com.asianmobile.emojibattery.shimeji.ui.battery.catalog

import com.asianmobile.emojibattery.shimeji.data.model.BUILT_IN_BATTERY_CATEGORY_ID
import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogCategory
import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogSnapshot
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry

class BatteryCatalogDisplayPolicy {
    fun filterCategories(
        categories: List<BatteryCatalogCategory>
    ): List<BatteryCatalogCategory> = categories.filterNot { category ->
        category.id == BUILT_IN_BATTERY_CATEGORY_ID
    }

    fun currentStyle(
        catalog: BatteryCatalogSnapshot,
        config: BatteryStatusConfig
    ): BatteryCurrentStyle? {
        if (!config.hasApplied) return null
        return BatteryCurrentStyle(
            config = config,
            batteryTheme = catalog.themes.firstOrNull {
                it.id == config.selectedBatteryThemeId
            },
            emojiTheme = catalog.themes.firstOrNull {
                it.id == config.selectedEmojiThemeId
            },
            backgroundPath = catalog.backgrounds.firstOrNull {
                it.id == config.backgroundDecorationId
            }?.assetPath
        )
    }

    fun filterThemes(
        themes: List<BatteryThemeEntry>,
        categoryId: Int?,
        query: String
    ): List<BatteryThemeEntry> {
        val normalized = query.trim()
        return themes.filter { theme ->
            !theme.isBuiltIn &&
                (categoryId == null || theme.categoryId == categoryId) &&
                (normalized.isEmpty() || theme.name.contains(normalized, ignoreCase = true))
        }
    }
}
