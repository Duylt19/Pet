package com.asianmobile.privatebrower.data.model

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

        assertEquals("owner.shimeji.112@4", entry.installedPackKey)
    }
}
