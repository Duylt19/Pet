package com.asianmobile.privatebrower.ui.home.tabstab

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TabSelectionUiStateTest {

    @Test
    fun areAllTabsSelected_isFalseWhenThereAreNoTabs() {
        val state = TabSelectionUiState(
            tabs = emptyList(),
            selectedTabIds = emptySet()
        )

        assertFalse(state.areAllTabsSelected)
    }

    @Test
    fun areAllTabsSelected_isFalseForPartialSelection() {
        val state = TabSelectionUiState(
            tabs = listOf(tab(1), tab(2)),
            selectedTabIds = setOf(1L, 99L)
        )

        assertTrue(state.hasSelection)
        assertEquals(1, state.selectedCount)
        assertFalse(state.areAllTabsSelected)
    }

    @Test
    fun areAllTabsSelected_isTrueWhenEveryVisibleTabIsSelected() {
        val state = TabSelectionUiState(
            tabs = listOf(tab(1), tab(2)),
            selectedTabIds = setOf(1L, 2L)
        )

        assertTrue(state.areAllTabsSelected)
        assertEquals(2, state.selectedCount)
    }

    private fun tab(id: Long) = TabUi(
        id = id,
        title = "Tab $id",
        url = "https://example.com/$id",
        thumbnailPath = null,
        isActive = false
    )
}
