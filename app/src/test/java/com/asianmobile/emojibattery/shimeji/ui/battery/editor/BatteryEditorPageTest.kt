package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryEditorPageTest {

    @Test
    fun `component routes resolve every child editor`() {
        val componentPages = BatteryEditorPage.entries -
            setOf(BatteryEditorPage.OVERVIEW, BatteryEditorPage.EMOTION_DETAIL)

        componentPages.forEach { page ->
            assertEquals(page, BatteryEditorPage.fromRoute(page.name))
        }
    }

    @Test
    fun `component route rejects overview and unknown values`() {
        assertNull(BatteryEditorPage.fromRoute(BatteryEditorPage.OVERVIEW.name))
        assertNull(BatteryEditorPage.fromRoute(BatteryEditorPage.EMOTION_DETAIL.name))
        assertNull(BatteryEditorPage.fromRoute("UNKNOWN"))
        assertNull(BatteryEditorPage.fromRoute(null))
    }
}
