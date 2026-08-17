package com.asianmobile.emojibattery.shimeji.data.model

/**
 * Backward-compatible rankings used until a catalog explicitly supplies its Discover curation.
 *
 * Keep these defaults when adding an optional ranking field to an existing catalog. Once the
 * field is present, including when it is an empty array, the catalog is the source of truth.
 */
val DEFAULT_DISCOVER_TRENDING_PET_IDS: List<Int> = listOf(
    8, 30, 40, 42, 44, 73, 112, 126, 144, 146, 163, 166, 224, 346, 393, 427, 447,
    460, 550, 551, 555, 558, 559, 567, 631, 669, 701, 719, 844, 996, 1038, 1045, 2000,
    2002
)

val DEFAULT_DISCOVER_TRENDING_EMOJI_THEME_IDS: List<Int> = listOf(
    1, 2, 6, 9, 12, 31, 33, 41, 44, 45, 49, 67, 71, 96, 109, 172, 203, 208, 261,
    305, 327, 329, 355, 389, 416, 437, 445, 654, 728, 751, 894, 869, 913, 919
)
