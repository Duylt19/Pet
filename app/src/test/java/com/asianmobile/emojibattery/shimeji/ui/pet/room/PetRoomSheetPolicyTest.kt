package com.asianmobile.emojibattery.shimeji.ui.pet.room

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
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

    @Test
    fun `pet detail is visible only on my pet tab`() {
        val detail = petDetail()

        assertSame(detail, PetRoomSheetPolicy.detailForTab(PetRoomTab.MY_PET, detail))
        assertNull(PetRoomSheetPolicy.detailForTab(PetRoomTab.FOOD, detail))
        assertNull(PetRoomSheetPolicy.detailForTab(PetRoomTab.ROOM, detail))
    }

    private fun petDetail() = PetRoomDetailUiState(
        petId = 1,
        packKey = "ampharos",
        name = "Ampharos",
        breed = "Pokemon",
        adoptedOn = "13.08.2026",
        thumbnailPath = null,
        energyPercent = 100,
        isOnScreen = true
    )
}
