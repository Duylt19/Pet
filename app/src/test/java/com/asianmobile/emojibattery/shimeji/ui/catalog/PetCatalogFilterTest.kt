package com.asianmobile.emojibattery.shimeji.ui.catalog

import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class PetCatalogFilterTest {
    private val pets = listOf(
        pet(1, "Pikachu", "Pokemon", "Creator One"),
        pet(2, "Charmander", "Pokemon", "Creator Two"),
        pet(3, "Orange Cat", "Animals", "Cute Pet")
    )

    @Test
    fun `categories put all first and sort by count`() {
        val categories = PetCatalogFilter.categories(pets)

        assertEquals(PetCatalogCategory(null, 3), categories[0])
        assertEquals(PetCatalogCategory("Pokemon", 2), categories[1])
        assertEquals(PetCatalogCategory("Animals", 1), categories[2])
    }

    @Test
    fun `filter combines category and case insensitive search`() {
        val filtered = PetCatalogFilter.apply(pets, query = "CHAR", category = "Pokemon")

        assertEquals(listOf(2), filtered.map(OwnerPetCatalogEntry::id))
    }

    @Test
    fun `filter searches creator metadata`() {
        val filtered = PetCatalogFilter.apply(pets, query = "cute pet", category = null)

        assertEquals(listOf(3), filtered.map(OwnerPetCatalogEntry::id))
    }

    private fun pet(id: Int, name: String, category: String, author: String) =
        OwnerPetCatalogEntry(
            id = id,
            name = name,
            category = category,
            author = author,
            thumbnailPath = null,
            hasLocalArchive = true
        )
}
