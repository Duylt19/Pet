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
    fun `obsolete base project screen names are not reported`() {
        val values = ScreenName.entries.mapTo(mutableSetOf(), ScreenName::value)

        assertTrue("channel_list" !in values)
        assertTrue("playlist_detail" !in values)
        assertTrue("home_channel_tab" !in values)
    }

    @Test
    fun `all app screens and visible content tabs have canonical tracking names`() {
        val expected = setOf(
            "splash",
            "language_onboarding",
            "language_settings",
            "intro_page_1",
            "intro_page_2",
            "intro_page_3",
            "permission",
            "set_default_browser",
            "premium",
            "settings",
            "privacy_policy",
            "home_browser",
            "tabs_normal",
            "tabs_private",
            "tabs_search_normal",
            "tabs_search_private",
            "tab_selection_normal",
            "tab_selection_private",
            "downloads_all",
            "downloads_active",
            "downloads_completed",
            "bookmarks",
            "bookmarks_search",
            "history",
            "history_search",
            "files_home",
            "browser_normal",
            "browser_private",
            "files_images",
            "files_video",
            "files_audio",
            "files_documents",
            "viewer_image",
            "viewer_video",
            "viewer_audio",
            "viewer_file",
            "how_to_download"
        )

        assertEquals(expected, ScreenName.entries.mapTo(mutableSetOf(), ScreenName::value))
    }

    private companion object {
        val SCREEN_NAME_PATTERN = Regex("^[a-z][a-z0-9]*(?:_[a-z0-9]+)*$")
    }
}
