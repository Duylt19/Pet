package com.asianmobile.emojibattery.shimeji.data.model

data class BatteryCatalogCategory(
    val id: Int,
    val name: String,
    val slug: String,
    val priority: Int
)

enum class BatteryThemeEntitlement {
    FREE,
    PREMIUM
}

data class BatteryThemeEntry(
    val id: Int,
    val name: String,
    val categoryId: Int,
    val categoryName: String,
    val entitlement: BatteryThemeEntitlement,
    val thumbnailPath: String?,
    val batteryPath: String?,
    val emojiPath: String?,
    val assetsReady: Boolean
) {
    val isBuiltIn: Boolean
        get() = id == BUILT_IN_BATTERY_THEME_ID
}

data class BatteryCatalogSnapshot(
    val categories: List<BatteryCatalogCategory> = listOf(BUILT_IN_BATTERY_CATEGORY),
    val themes: List<BatteryThemeEntry> = listOf(BUILT_IN_BATTERY_THEME),
    val catalogVersion: String? = null,
    val capturedAt: String? = null,
    val distributionStatus: BatteryCatalogDistributionStatus =
        BatteryCatalogDistributionStatus.BUILT_IN_ONLY,
    val localRootPath: String = "",
    val isLoading: Boolean = true,
    val error: BatteryCatalogError? = null
)

enum class BatteryCatalogDistributionStatus {
    BUILT_IN_ONLY,
    REVIEW_REQUIRED,
    APPROVED
}

enum class BatteryCatalogError {
    LOCAL_CATALOG_MISSING,
    LOCAL_CATALOG_INVALID,
    LOCAL_STORAGE_UNAVAILABLE,
    DISTRIBUTION_NOT_APPROVED
}

const val BUILT_IN_BATTERY_THEME_ID = 0
const val BUILT_IN_BATTERY_CATEGORY_ID = 0

val BUILT_IN_BATTERY_CATEGORY = BatteryCatalogCategory(
    id = BUILT_IN_BATTERY_CATEGORY_ID,
    name = "Starter",
    slug = "starter",
    priority = Int.MIN_VALUE
)

val BUILT_IN_BATTERY_THEME = BatteryThemeEntry(
    id = BUILT_IN_BATTERY_THEME_ID,
    name = "Cute Mint",
    categoryId = BUILT_IN_BATTERY_CATEGORY_ID,
    categoryName = BUILT_IN_BATTERY_CATEGORY.name,
    entitlement = BatteryThemeEntitlement.FREE,
    thumbnailPath = null,
    batteryPath = null,
    emojiPath = null,
    assetsReady = true
)
