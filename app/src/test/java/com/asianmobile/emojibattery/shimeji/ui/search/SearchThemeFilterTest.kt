package com.asianmobile.emojibattery.shimeji.ui.search

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchThemeFilterTest {
    private val themes = listOf(
        SearchThemeUiState(1, "Cute Battery", "Animal", null, false),
        SearchThemeUiState(2, "Pink Idol", "Kpop", null, false),
        SearchThemeUiState(3, "Anime Hero", "Anime", null, false)
    )

    @Test
    fun `blank query keeps catalog order`() {
        assertEquals(themes, filterSearchThemes(themes, "  "))
    }

    @Test
    fun `query matches theme name or category ignoring case`() {
        assertEquals(listOf(themes[0]), filterSearchThemes(themes, "animal"))
        assertEquals(listOf(themes[1]), filterSearchThemes(themes, "PINK"))
    }
}
