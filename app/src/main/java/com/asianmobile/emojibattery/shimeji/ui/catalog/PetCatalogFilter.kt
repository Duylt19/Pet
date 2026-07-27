package com.asianmobile.emojibattery.shimeji.ui.catalog

import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogEntry

object PetCatalogFilter {
    fun categories(pets: List<OwnerPetCatalogEntry>): List<PetCatalogCategory> {
        val grouped = pets.groupingBy(OwnerPetCatalogEntry::category).eachCount()
        return listOf(PetCatalogCategory(name = null, count = pets.size)) +
            grouped.entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .map { PetCatalogCategory(name = it.key, count = it.value) }
    }

    fun apply(
        pets: List<OwnerPetCatalogEntry>,
        query: String,
        category: String?
    ): List<OwnerPetCatalogEntry> {
        val normalizedQuery = query.trim()
        return pets.filter { pet ->
            val matchesCategory = category == null || pet.category == category
            val matchesQuery = normalizedQuery.isEmpty() ||
                pet.name.contains(normalizedQuery, ignoreCase = true) ||
                pet.category.contains(normalizedQuery, ignoreCase = true) ||
                pet.author.orEmpty().contains(normalizedQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }
}
