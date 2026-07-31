package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryEditorPageTest {

    @Test
    fun `component routes resolve every child editor`() {
        val componentPages = BatteryEditorPage.entries - BatteryEditorPage.OVERVIEW

        componentPages.forEach { page ->
            assertEquals(page, BatteryEditorPage.fromRoute(page.name))
        }
    }

    @Test
    fun `component route rejects overview and unknown values`() {
        assertNull(BatteryEditorPage.fromRoute(BatteryEditorPage.OVERVIEW.name))
        assertNull(BatteryEditorPage.fromRoute("UNKNOWN"))
        assertNull(BatteryEditorPage.fromRoute(null))
    }
}
