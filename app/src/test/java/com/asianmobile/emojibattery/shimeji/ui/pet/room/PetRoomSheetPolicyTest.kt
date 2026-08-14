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

    @Test
    fun `switching to food keeps selected pet detail for feeding`() {
        val detail = petDetail()
        val result = PetRoomSheetPolicy.stateAfterTabSelected(
            state = PetRoomUiState(
                selectedTab = PetRoomTab.MY_PET,
                isSheetExpanded = true,
                detail = detail
            ),
            requested = PetRoomTab.FOOD
        )

        assertEquals(PetRoomTab.FOOD, result.selectedTab)
        assertSame(detail, result.detail)
        assertNull(PetRoomSheetPolicy.detailForTab(result.selectedTab, result.detail))
    }

    @Test
    fun `room tab does not mutate selected pet detail`() {
        val detail = petDetail()
        val result = PetRoomSheetPolicy.stateAfterTabSelected(
            state = PetRoomUiState(selectedTab = PetRoomTab.FOOD, detail = detail),
            requested = PetRoomTab.ROOM
        )

        assertEquals(PetRoomTab.ROOM, result.selectedTab)
        assertSame(detail, result.detail)
    }

    @Test
    fun `food reward policy blocks duplicate request and completes local reward flow`() {
        val food = PetRoomFoodUiState(
            id = "beef_stew",
            name = "Beef Stew",
            energyValue = 25,
            imageRes = 1,
            portions = 0
        )
        val selected = PetRoomFoodRewardPolicy.select(PetRoomFoodRewardUiState(), food)
        val requesting = PetRoomFoodRewardPolicy.begin(selected)

        assertSame(requesting, PetRoomFoodRewardPolicy.begin(requesting))
        assertSame(requesting, PetRoomFoodRewardPolicy.cancel(requesting))

        val revealed = PetRoomFoodRewardPolicy.reveal(requesting, food)
        assertNull(revealed.selectedFood)
        assertSame(food, revealed.revealedFood)
        assertEquals(false, revealed.isRequesting)

        val completed = PetRoomFoodRewardPolicy.continueAfterReveal(revealed)
        assertNull(completed.revealedFood)
        assertSame(food, completed.acquiredFood)
        assertNull(PetRoomFoodRewardPolicy.dismissAcquired(completed).acquiredFood)
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
