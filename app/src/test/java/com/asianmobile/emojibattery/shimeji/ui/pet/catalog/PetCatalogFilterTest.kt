package com.asianmobile.emojibattery.shimeji.ui.pet.catalog

import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class PetCatalogFilterTest {
    private val pets = listOf(
        pet(1, "Pikachu", "Pokemon", "Creator One"),
        pet(2, "Charmander", "Pokemon", "Creator Two"),
        pet(3, "Orange Cat", "Animals", "Cute Pet"),
        pet(4, "ARGENTINA", "WC 2026", null),
        pet(5, "Vinícius Júnior", "WC 2026", null)
    )

    @Test
    fun `categories put all and featured world cup first`() {
        val categories = PetCatalogFilter.categories(pets)

        assertEquals(PetCatalogCategory(null, 5), categories[0])
        assertEquals(PetCatalogCategory("WC 2026", 2), categories[1])
        assertEquals(PetCatalogCategory("Pokemon", 2), categories[2])
        assertEquals(PetCatalogCategory("Animals", 1), categories[3])
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

    @Test
    fun `filter finds world cup pets using natural aliases`() {
        val football = PetCatalogFilter.apply(pets, query = "football", category = null)
        val vietnamese = PetCatalogFilter.apply(pets, query = "bóng đá", category = null)
        val worldCup = PetCatalogFilter.apply(pets, query = "world cup 2026", category = null)

        assertEquals(listOf(4, 5), football.map(OwnerPetCatalogEntry::id))
        assertEquals(listOf(4, 5), vietnamese.map(OwnerPetCatalogEntry::id))
        assertEquals(listOf(4, 5), worldCup.map(OwnerPetCatalogEntry::id))
    }

    @Test
    fun `filter ignores accents in pet names`() {
        val filtered = PetCatalogFilter.apply(pets, query = "vinicius junior", category = null)

        assertEquals(listOf(5), filtered.map(OwnerPetCatalogEntry::id))
    }

    private fun pet(id: Int, name: String, category: String, author: String?) =
        OwnerPetCatalogEntry(
            id = id,
            name = name,
            category = category,
            author = author,
            thumbnailPath = null,
            hasLocalArchive = true
        )
}
