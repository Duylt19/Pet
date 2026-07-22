package com.asianmobile.privatebrower.utils

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
            "home",
            "pet_catalog",
            "pet_detail",
            "premium",
            "settings"
        )

        assertEquals(expected, ScreenName.entries.mapTo(mutableSetOf(), ScreenName::value))
    }

    private companion object {
        val SCREEN_NAME_PATTERN = Regex("^[a-z][a-z0-9]*(?:_[a-z0-9]+)*$")
    }
}
