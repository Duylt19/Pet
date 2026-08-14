package com.asianmobile.emojibattery.shimeji.battery.settings

import com.asianmobile.emojibattery.shimeji.data.model.BUILT_IN_BATTERY_THEME_ID

object BatteryRecentThemePolicy {
    const val MAX_RECENT_THEMES = 30

    fun record(
        currentThemeIds: List<Int>,
        openedThemeId: Int
    ): List<Int> {
        if (openedThemeId <= BUILT_IN_BATTERY_THEME_ID) return sanitize(currentThemeIds)
        return (listOf(openedThemeId) + currentThemeIds)
            .asSequence()
            .filter { it > BUILT_IN_BATTERY_THEME_ID }
            .distinct()
            .take(MAX_RECENT_THEMES)
            .toList()
    }

    fun decode(value: String?): List<Int> = value
        .orEmpty()
        .split(DELIMITER)
        .mapNotNull(String::toIntOrNull)
        .let(::sanitize)

    fun encode(themeIds: List<Int>): String = sanitize(themeIds).joinToString(DELIMITER)

    private fun sanitize(themeIds: List<Int>): List<Int> = themeIds
        .asSequence()
        .filter { it > BUILT_IN_BATTERY_THEME_ID }
        .distinct()
        .take(MAX_RECENT_THEMES)
        .toList()

    private const val DELIMITER = ","
}
