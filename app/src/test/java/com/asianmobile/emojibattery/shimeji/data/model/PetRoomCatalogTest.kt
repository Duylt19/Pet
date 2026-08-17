package com.asianmobile.emojibattery.shimeji.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PetRoomCatalogTest {
    @Test
    fun `resolves the selected room when it is still in the catalog`() {
        val snapshot = snapshot(selectedRooms = listOf(1, 4), defaultRoomId = 4)

        assertEquals(1, snapshot.resolveRoom(selectedRoomId = 1)?.id)
    }

    @Test
    fun `falls back to the default room when the selection is unknown`() {
        val snapshot = snapshot(selectedRooms = listOf(1, 4), defaultRoomId = 4)

        assertEquals(4, snapshot.resolveRoom(selectedRoomId = 99)?.id)
    }

    @Test
    fun `falls back to the first room when the default left the catalog`() {
        val snapshot = snapshot(selectedRooms = listOf(2, 3), defaultRoomId = 4)

        assertEquals(2, snapshot.resolveRoom(selectedRoomId = 0)?.id)
    }

    @Test
    fun `resolves nothing while the catalog is empty`() {
        assertNull(PetRoomCatalogSnapshot().resolveRoom(selectedRoomId = 1))
    }

    private fun snapshot(selectedRooms: List<Int>, defaultRoomId: Int) = PetRoomCatalogSnapshot(
        rooms = selectedRooms.map { id ->
            PetRoomEntry(
                id = id,
                name = "Room $id",
                slug = "bg_$id",
                entitlement = PetRoomEntitlement.FREE,
                backgroundPath = "bg/BG_$id.png",
                thumbnailPath = "thumb/BG_$id.png"
            )
        },
        defaultRoomId = defaultRoomId,
        isLoading = false
    )
}
