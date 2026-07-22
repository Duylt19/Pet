package com.asianmobile.privatebrower.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.asianmobile.privatebrower.R

/**
 * Maps category names to their designated colors as defined in Figma (node 5178:8120).
 * Each category has a unique color for its tag badge.
 *
 * Usage:
 * ```kotlin
 * val color = getCategoryColor(categoryName = "Sport")
 * // Use color for text and color.copy(alpha = 0.1f) for background
 * ```
 */
@Composable
fun getCategoryColor(categoryName: String?): Color {
    if (categoryName.isNullOrBlank()) return colorResource(R.color.gray_808080)

    return when (categoryName.lowercase().trim()) {
        "sport", "sports" -> colorResource(R.color.colors_00C950)
        "kids" -> colorResource(R.color.colors_FF9000)
        "movie", "movies" -> colorResource(R.color.colors_ED3A3A)
        "news" -> colorResource(R.color.colors_3A40ED)
        "music" -> colorResource(R.color.colors_7C3AED)
        "international" -> colorResource(R.color.colors_3A55ED)
        "lifestyle" -> colorResource(R.color.colors_C33AED)
        "stream" -> colorResource(R.color.colors_3A8EED)
        "documentary" -> colorResource(R.color.colors_3AD5ED)
        "family" -> colorResource(R.color.colors_EDBA3A)
        "entertainment" -> colorResource(R.color.colors_613AED)
        "cooking", "food" -> colorResource(R.color.colors_EDDB3A)
        "animation" -> colorResource(R.color.colors_ED3A67)
        "interactive" -> colorResource(R.color.colors_3A88ED)
        "business" -> colorResource(R.color.colors_CE4C00)
        "legislative" -> colorResource(R.color.colors_00E476)
        "classic" -> colorResource(R.color.colors_9500FF)
        "culture" -> colorResource(R.color.colors_EDDB3A)
        "comedy" -> colorResource(R.color.colors_ED3AAE)
        "education" -> colorResource(R.color.colors_3A73ED)
        "outdoor" -> colorResource(R.color.colors_11CC00)
        "shop", "shopping" -> colorResource(R.color.colors_EDBA3A)
        "relax", "relaxation" -> colorResource(R.color.colors_AB00CE)
        "religious", "religion" -> colorResource(R.color.colors_3AC3ED)
        "travel" -> colorResource(R.color.colors_FF5BEF)
        "auto" -> colorResource(R.color.colors_E8383B)
        "series" -> colorResource(R.color.colors_00FFFB)
        "science" -> colorResource(R.color.colors_8BEE00)
        "public" -> colorResource(R.color.colors_9500FF)
        "weather" -> colorResource(R.color.colors_FF9D00)
        "general" -> colorResource(R.color.colors_3A8EED)
        else -> colorResource(R.color.gray_808080)
    }
}
fun String.capitalizeFirstLetter(): String {
    if (this.isBlank()) return this
    return this.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}


