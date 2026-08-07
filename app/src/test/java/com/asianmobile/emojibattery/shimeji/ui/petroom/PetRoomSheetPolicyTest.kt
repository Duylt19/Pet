package com.asianmobile.emojibattery.shimeji.ui.petroom

import org.junit.Assert.assertEquals
import org.junit.Test

class PetRoomSheetPolicyTest {
    @Test
    fun `selecting another tab always opens the sheet`() {
        val result = PetRoomSheetPolicy.onTabSelected(
            current = PetRoomTab.MY_PET,
            requested = PetRoomTab.ROOM,
            isExpanded = false
        )

        assertEquals(PetRoomTab.ROOM to true, result)
    }

    @Test
    fun `selecting another tab keeps an open sheet open`() {
        val result = PetRoomSheetPolicy.onTabSelected(
            current = PetRoomTab.FOOD,
            requested = PetRoomTab.MY_PET,
            isExpanded = true
        )

        assertEquals(PetRoomTab.MY_PET to true, result)
    }

    @Test
    fun `tapping the active tab collapses the sheet`() {
        val result = PetRoomSheetPolicy.onTabSelected(
            current = PetRoomTab.ROOM,
            requested = PetRoomTab.ROOM,
            isExpanded = true
        )

        assertEquals(PetRoomTab.ROOM to false, result)
    }

    @Test
    fun `tapping the active tab reopens a collapsed sheet`() {
        val result = PetRoomSheetPolicy.onTabSelected(
            current = PetRoomTab.ROOM,
            requested = PetRoomTab.ROOM,
            isExpanded = false
        )

        assertEquals(PetRoomTab.ROOM to true, result)
    }

    @Test
    fun `the chevron toggles the sheet without changing the tab`() {
        assertEquals(false, PetRoomSheetPolicy.toggleExpanded(isExpanded = true))
        assertEquals(true, PetRoomSheetPolicy.toggleExpanded(isExpanded = false))
    }
}
