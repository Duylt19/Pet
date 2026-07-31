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
            val matchesSearch = normalized.isEmpty() ||
                theme.name.contains(normalized, ignoreCase = true) ||
                batteryThemeDisplayName(theme.name).contains(normalized, ignoreCase = true)
            !theme.isBuiltIn &&
                (categoryId == null || theme.categoryId == categoryId) &&
                matchesSearch
        }
    }
}

internal fun batteryThemeDisplayName(rawName: String): String {
    val normalized = rawName
        .replace(BATTERY_CAMEL_CASE_BOUNDARY, " ")
        .replace(BATTERY_NAME_SEPARATORS, " ")
        .trim()
        .replace(BATTERY_NAME_WHITESPACE, " ")
    return normalized
        .split(' ')
        .filter(String::isNotBlank)
        .joinToString(" ") { word ->
            if (word.length > 1 && word.all { character ->
                    !character.isLetter() || character.isUpperCase()
                }
            ) {
                word
            } else {
                word.lowercase().replaceFirstChar(Char::uppercase)
            }
        }
}

private val BATTERY_CAMEL_CASE_BOUNDARY = Regex("(?<=[\\p{Ll}\\d])(?=\\p{Lu})")
private val BATTERY_NAME_SEPARATORS = Regex("[_-]+")
private val BATTERY_NAME_WHITESPACE = Regex("\\s+")
