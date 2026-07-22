package com.asianmobile.privatebrower.ui.home.tabstab

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TabsTabUiStateTest {

    private val tabs = listOf(
        tab(id = 1, title = "World Cup 2026", url = "https://sports.example.com"),
        tab(id = 2, title = "Private Browser", url = "https://example.com/account")
    )

    @Test
    fun displayTabs_showsAllTabsForBlankSearch() {
        val state = TabsTabUiState(
            tabs = tabs,
            isSearchActive = true,
            searchQuery = "   "
        )

        assertEquals(tabs, state.displayTabs)
    }

    @Test
    fun displayTabs_matchesTitleIgnoringCaseAndOuterWhitespace() {
        val state = TabsTabUiState(
            tabs = tabs,
            isSearchActive = true,
            searchQuery = "  world CUP  "
        )

        assertEquals(listOf(tabs.first()), state.displayTabs)
    }

    @Test
    fun displayTabs_matchesUrlIgnoringCase() {
        val state = TabsTabUiState(
            tabs = tabs,
            isSearchActive = true,
            searchQuery = "EXAMPLE.COM/ACCOUNT"
        )

        assertEquals(listOf(tabs.last()), state.displayTabs)
    }

    @Test
    fun displayTabs_returnsEmptyListWhenNothingMatches() {
        val state = TabsTabUiState(
            tabs = tabs,
            isSearchActive = true,
            searchQuery = "weather"
        )

        assertTrue(state.displayTabs.isEmpty())
    }

    private fun tab(id: Long, title: String, url: String) = TabUi(
        id = id,
        title = title,
        url = url,
        thumbnailPath = null,
        isActive = false
    )
}
