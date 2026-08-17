package com.asianmobile.emojibattery.shimeji.ui.pet.room

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetRoomRosterPolicyTest {
    @Test
    fun `keeps only pets whose pack is installed`() {
        val roster = PetRoomRosterPolicy.roster(
            catalogEntries = listOf(entry(1), entry(2), entry(3)),
            installedPackKeys = setOf("pack-1", "pack-3"),
            customNames = emptyMap()
        )

        assertEquals(listOf(1, 3), roster.map(PetRoomPetUiState::petId))
    }

    @Test
    fun `keeps the catalog order so the grid does not reshuffle`() {
        val roster = PetRoomRosterPolicy.roster(
            catalogEntries = listOf(entry(5), entry(2), entry(9)),
            installedPackKeys = setOf("pack-9", "pack-5", "pack-2"),
            customNames = emptyMap()
        )

        assertEquals(listOf(5, 2, 9), roster.map(PetRoomPetUiState::petId))
    }

    @Test
    fun `prefers the name the user gave the pet`() {
        val roster = PetRoomRosterPolicy.roster(
            catalogEntries = listOf(entry(1, name = "Shimeji 1")),
            installedPackKeys = setOf("pack-1"),
            customNames = mapOf(1 to "  Cattey  ")
        )

        assertEquals("Cattey", roster.single().name)
    }

    @Test
    fun `falls back to the catalog name when the custom name is blank`() {
        val roster = PetRoomRosterPolicy.roster(
            catalogEntries = listOf(entry(1, name = "Shimeji 1")),
            installedPackKeys = setOf("pack-1"),
            customNames = mapOf(1 to "   ")
        )

        assertEquals("Shimeji 1", roster.single().name)
    }

    @Test
    fun `skips an installed pack the catalog does not describe`() {
        val roster = PetRoomRosterPolicy.roster(
            catalogEntries = listOf(entry(1)),
            installedPackKeys = setOf("pack-1", "some.sideloaded.pack@1"),
            customNames = emptyMap()
        )

        assertEquals(1, roster.size)
    }

    @Test
    fun `is empty before the user owns anything`() {
        assertTrue(
            PetRoomRosterPolicy.roster(
                catalogEntries = listOf(entry(1)),
                installedPackKeys = emptySet(),
                customNames = emptyMap()
            ).isEmpty()
        )
    }

    @Test
    fun `room scene includes only active pets while roster keeps inactive pets`() {
        val roster = listOf(
            pet(id = 1, isOnScreen = true),
            pet(id = 2, isOnScreen = false),
            pet(id = 3, isOnScreen = true)
        )

        assertEquals(setOf("pack-1", "pack-3"), PetRoomRosterPolicy.activePackKeys(roster))
        assertEquals(3, roster.size)
    }

    private fun entry(id: Int, name: String = "Pet $id") = PetRoomRosterSource(
        petId = id,
        packKey = "pack-$id",
        catalogName = name,
        category = "Cat",
        thumbnailPath = "https://example.invalid/$id.png"
    )

    private fun pet(id: Int, isOnScreen: Boolean) = PetRoomPetUiState(
        petId = id,
        packKey = "pack-$id",
        name = "Pet $id",
        breed = "Cat",
        thumbnailPath = null,
        isOnScreen = isOnScreen
    )
}
