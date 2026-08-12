package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry

/** Keeps the renderer-only built-in fallback out of user-facing theme pickers. */
internal object BatteryEditorThemeDisplayPolicy {
    fun selectableThemes(themes: List<BatteryThemeEntry>): List<BatteryThemeEntry> =
        themes.filterNot(BatteryThemeEntry::isBuiltIn)
}
