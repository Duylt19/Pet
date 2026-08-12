package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun `only child screens with explicit Apply or Done use rollback transactions`() {
        listOf(
            BatteryEditorPage.ANIMATION,
            BatteryEditorPage.WIFI,
            BatteryEditorPage.SIGNAL,
            BatteryEditorPage.DATA,
            BatteryEditorPage.EMOTION_DETAIL,
            BatteryEditorPage.SIZE
        ).forEach { assertTrue(it.isTransactionalChildPage()) }

        listOf(
            BatteryEditorPage.OVERVIEW,
            BatteryEditorPage.EMOJI,
            BatteryEditorPage.BATTERY_TEMPLATES,
            BatteryEditorPage.EMOJI_TEMPLATES,
            BatteryEditorPage.BACKGROUND_THEMES
        ).forEach { assertFalse(it.isTransactionalChildPage()) }
    }
}
