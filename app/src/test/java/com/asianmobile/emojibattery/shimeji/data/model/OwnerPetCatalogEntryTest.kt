package com.asianmobile.emojibattery.shimeji.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class OwnerPetCatalogEntryTest {
    @Test
    fun `owner pack key points to current immutable conversion revision`() {
        val entry = OwnerPetCatalogEntry(
            id = 112,
            name = "Levi Ackerman",
            category = "Attack on Titan",
            author = null,
            thumbnailPath = null,
            hasLocalArchive = true
        )

        assertEquals("owner.shimeji.112@7", entry.installedPackKey)
    }

    @Test
    fun `installed pet ID is parsed only from canonical owner pack IDs`() {
        assertEquals(558, OwnerPetCatalogEntry.installedPetId("owner.shimeji.558"))
        assertEquals(null, OwnerPetCatalogEntry.installedPetId("owner.shimeji."))
        assertEquals(null, OwnerPetCatalogEntry.installedPetId("owner.shimeji.-1"))
        assertEquals(null, OwnerPetCatalogEntry.installedPetId("builtin.orange-cat"))
    }
}
