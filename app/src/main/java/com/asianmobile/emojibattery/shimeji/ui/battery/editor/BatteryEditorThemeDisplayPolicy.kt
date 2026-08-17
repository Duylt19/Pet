package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry

/** Keeps the renderer-only built-in fallback out of user-facing theme pickers. */
internal object BatteryEditorThemeDisplayPolicy {
    fun selectableThemes(
        themes: List<BatteryThemeEntry>,
        trendingThemeIds: List<Int>
    ): List<BatteryThemeEntry> {
        val selectable = themes.filterNot(BatteryThemeEntry::isBuiltIn)
        val themesById = selectable.associateBy(BatteryThemeEntry::id)
        val trending = trendingThemeIds.distinct().mapNotNull(themesById::get)
        val trendingIds = trending.mapTo(mutableSetOf(), BatteryThemeEntry::id)
        return trending + selectable.filterNot { theme -> theme.id in trendingIds }
    }

    /**
     * Resolves against the displayed order because trending themes may move ahead of catalog order.
     */
    fun selectedIndex(themes: List<BatteryThemeEntry>, selectedThemeId: Int): Int? =
        themes.indexOfFirst { it.id == selectedThemeId }.takeIf { it >= 0 }
}
