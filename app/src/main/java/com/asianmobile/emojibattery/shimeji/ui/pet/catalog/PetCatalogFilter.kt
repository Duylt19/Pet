package com.asianmobile.emojibattery.shimeji.ui.pet.catalog

import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogEntry
import java.text.Normalizer
import java.util.Locale

object PetCatalogFilter {
    fun categories(pets: List<OwnerPetCatalogEntry>): List<PetCatalogCategory> {
        val grouped = pets.groupingBy(OwnerPetCatalogEntry::category).eachCount()
        val featured = FEATURED_CATEGORIES.mapNotNull { category ->
            grouped[category]?.let { count -> PetCatalogCategory(category, count) }
        }
        return listOf(PetCatalogCategory(name = null, count = pets.size)) +
            featured +
            grouped.entries
                .filterNot { it.key in FEATURED_CATEGORIES }
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .map { PetCatalogCategory(name = it.key, count = it.value) }
    }

    fun apply(
        pets: List<OwnerPetCatalogEntry>,
        query: String,
        category: String?
    ): List<OwnerPetCatalogEntry> {
        val queryTokens = query.toSearchText()
            .split(WHITESPACE_REGEX)
            .filter(String::isNotBlank)
        return pets.filter { pet ->
            val matchesCategory = category == null || pet.category == category
            val searchableText = buildString {
                append(pet.name)
                append(' ')
                append(pet.category)
                append(' ')
                append(pet.author.orEmpty())
                append(' ')
                append(categoryAliases(pet.category))
            }.toSearchText()
            val matchesQuery = queryTokens.all(searchableText::contains)
            matchesCategory && matchesQuery
        }
    }

    private fun categoryAliases(category: String): String = when (category) {
        WORLD_CUP_CATEGORY ->
            "wc2026 world cup fifa football soccer bong da world cup 2026 pet"
        else -> ""
    }

    private fun String.toSearchText(): String =
        Normalizer.normalize(trim(), Normalizer.Form.NFD)
            .replace(COMBINING_MARKS_REGEX, "")
            .replace('đ', 'd')
            .replace('Đ', 'D')
            .lowercase(Locale.ROOT)

    private const val WORLD_CUP_CATEGORY = "WC 2026"
    private val FEATURED_CATEGORIES = listOf(WORLD_CUP_CATEGORY)
    private val COMBINING_MARKS_REGEX = Regex("\\p{M}+")
    private val WHITESPACE_REGEX = Regex("\\s+")
}
