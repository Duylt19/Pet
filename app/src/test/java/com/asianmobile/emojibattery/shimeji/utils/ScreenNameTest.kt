package com.asianmobile.emojibattery.shimeji.utils

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenNameTest {

    @Test
    fun `screen names are unique and use canonical snake case`() {
        val values = ScreenName.entries.map(ScreenName::value)

        assertEquals(values.size, values.toSet().size)
        values.forEach { value ->
            assertTrue("Invalid screen name: $value", value.matches(SCREEN_NAME_PATTERN))
            assertTrue("Screen name is too long: $value", value.length <= 100)
        }
    }

    @Test
    fun `all retained app screens have canonical tracking names`() {
        val expected = setOf(
            "splash",
            "language_onboarding",
            "language_settings",
            "intro_page_1",
            "intro_page_2",
            "intro_page_3",
            "permission",
            "grant_permissions",
            "accessibility_how_to_use",
            "discover",
            "search",
            "favourite_recent",
            "my_pet",
            "pet_store",
            "battery_catalog",
            "battery_category",
            "battery_editor",
            "battery_template_picker",
            "battery_background_theme_picker",
            "battery_size_editor",
            "battery_appearance_editor",
            "battery_emoji_editor",
            "battery_emotion_editor",
            "battery_emotion_detail",
            "battery_icon_editor",
            "battery_animation_editor",
            "battery_wifi_editor",
            "battery_data_editor",
            "battery_signal_editor",
            "battery_airplane_editor",
            "battery_hotspot_editor",
            "battery_ringer_editor",
            "battery_charge_editor",
            "battery_date_time_editor",
            "battery_clock_editor",
            "battery_troll",
            "battery_troll_customize",
            "premium",
            "settings"
        )

        assertEquals(expected, ScreenName.entries.mapTo(mutableSetOf(), ScreenName::value))
    }

    @Test
    fun `screen tracking documentation matches canonical screen names`() {
        val trackingDocument = sequenceOf(
            File("docs/10_SCREEN_TRACKING.md"),
            File("../docs/10_SCREEN_TRACKING.md")
        ).firstOrNull(File::isFile)
            ?: error("Cannot find docs/10_SCREEN_TRACKING.md from ${File(".").absolutePath}")
        val documentedValues = SCREEN_DOCUMENT_ROW_PATTERN
            .findAll(trackingDocument.readText())
            .map { match -> match.groupValues[1] }
            .toSet()

        assertEquals(
            ScreenName.entries.mapTo(mutableSetOf(), ScreenName::value),
            documentedValues
        )
    }

    private companion object {
        val SCREEN_NAME_PATTERN = Regex("^[a-z][a-z0-9]*(?:_[a-z0-9]+)*$")
        val SCREEN_DOCUMENT_ROW_PATTERN = Regex(
            """(?m)^\|[^|]+\|\s*`([a-z][a-z0-9]*(?:_[a-z0-9]+)*)`\s*\|$"""
        )
    }
}
