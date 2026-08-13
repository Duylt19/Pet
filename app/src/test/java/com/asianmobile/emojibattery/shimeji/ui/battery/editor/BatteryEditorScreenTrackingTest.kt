package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryEditorScreenTrackingTest {

    @Test
    fun `every editor page has a unique screen event`() {
        val trackedScreens = BatteryEditorPage.entries.map(BatteryEditorPage::analyticsScreen)

        assertEquals(BatteryEditorPage.entries.size, trackedScreens.toSet().size)
    }

    @Test
    fun `picker and clock pages use their own screen events`() {
        assertEquals(
            ScreenName.BATTERY_TEMPLATE_PICKER,
            BatteryEditorPage.BATTERY_TEMPLATES.analyticsScreen()
        )
        assertEquals(
            ScreenName.BATTERY_BACKGROUND_THEME_PICKER,
            BatteryEditorPage.BACKGROUND_THEMES.analyticsScreen()
        )
        assertEquals(
            ScreenName.BATTERY_CLOCK_EDITOR,
            BatteryEditorPage.CLOCK.analyticsScreen()
        )
    }
}
