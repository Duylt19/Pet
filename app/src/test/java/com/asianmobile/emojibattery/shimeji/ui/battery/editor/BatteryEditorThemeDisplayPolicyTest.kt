package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import com.asianmobile.emojibattery.shimeji.data.model.BUILT_IN_BATTERY_THEME
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntitlement
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryEditorThemeDisplayPolicyTest {
    @Test
    fun selectableThemes_omitsRendererFallbackAndPreservesCatalogOrder() {
        val first = theme(1)
        val second = theme(2)

        val result = BatteryEditorThemeDisplayPolicy.selectableThemes(
            listOf(BUILT_IN_BATTERY_THEME, second, first)
        )

        assertEquals(listOf(2, 1), result.map(BatteryThemeEntry::id))
    }

    private fun theme(id: Int) = BatteryThemeEntry(
        id = id,
        name = "Theme $id",
        categoryId = 1,
        categoryName = "Trending",
        entitlement = BatteryThemeEntitlement.FREE,
        thumbnailPath = null,
        batteryPath = null,
        emojiPath = null,
        assetsReady = true
    )
}
