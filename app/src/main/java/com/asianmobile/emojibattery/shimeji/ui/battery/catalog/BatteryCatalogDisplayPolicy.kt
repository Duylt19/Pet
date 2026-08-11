package com.asianmobile.emojibattery.shimeji.ui.battery.catalog

import com.asianmobile.emojibattery.shimeji.data.model.BUILT_IN_BATTERY_CATEGORY_ID
import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogCategory
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry

class BatteryCatalogDisplayPolicy {
    fun filterCategories(
        categories: List<BatteryCatalogCategory>
    ): List<BatteryCatalogCategory> = categories.filterNot { category ->
        category.id == BUILT_IN_BATTERY_CATEGORY_ID
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

    fun sections(
        categories: List<BatteryCatalogCategory>,
        themes: List<BatteryThemeEntry>
    ): List<BatteryCatalogSection> = filterCategories(categories).mapNotNull { category ->
        val categoryThemes = themes.filter { theme ->
            !theme.isBuiltIn && theme.categoryId == category.id
        }
        categoryThemes.takeIf(List<BatteryThemeEntry>::isNotEmpty)?.let { entries ->
            BatteryCatalogSection(category = category, themes = entries)
        }
    }
}

internal fun batteryCategoryDisplayName(rawName: String): String = rawName.trim()

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
