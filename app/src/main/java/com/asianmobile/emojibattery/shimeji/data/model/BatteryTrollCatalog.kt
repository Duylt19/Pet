package com.asianmobile.emojibattery.shimeji.data.model

enum class BatteryTrollEntitlement {
    FREE,
    PREMIUM
}

/** Orientation the troll art was drawn for; the status bar cover never rotates it. */
enum class BatteryTrollBatteryOrientation {
    LANDSCAPE,
    PORTRAIT
}

enum class BatteryTrollDistributionStatus {
    APPROVED,
    REVIEW_REQUIRED
}

enum class BatteryTrollCatalogError {
    CATALOG_UNAVAILABLE,
    CATALOG_INVALID,
    DISTRIBUTION_NOT_APPROVED
}

/**
 * One Battery Troll character. All paths are catalog-relative (for example
 * `thumb/TROLL_1.webp`); resolve them with `BatteryTrollServerConfig.resolve` for a remote
 * URL, or hand them to `BatteryTrollCatalogRepository.materializeAsset` for a verified local
 * file. [emojiPaths] and [batteryPaths] always hold [BATTERY_TROLL_LEVEL_COUNT] entries with
 * index `0` = full battery and index `4` = empty battery.
 */
data class BatteryTrollEntry(
    val id: Int,
    val name: String,
    val slug: String,
    val order: Int,
    val entitlement: BatteryTrollEntitlement,
    val batteryOrientation: BatteryTrollBatteryOrientation,
    val thumbnailPath: String,
    val emojiPaths: List<String>,
    val batteryPaths: List<String>
) {
    /** Emoji artwork for a level index produced by `BatteryTrollPolicy`; out of range clamps. */
    fun emojiPathAt(levelIndex: Int): String =
        emojiPaths[levelIndex.coerceIn(0, emojiPaths.lastIndex)]

    /** Battery artwork for a level index produced by `BatteryTrollPolicy`. */
    fun batteryPathAt(levelIndex: Int): String =
        batteryPaths[levelIndex.coerceIn(0, batteryPaths.lastIndex)]
}

data class BatteryTrollCatalogSnapshot(
    val trolls: List<BatteryTrollEntry> = emptyList(),
    val catalogVersion: String? = null,
    val capturedAt: String? = null,
    val distributionStatus: BatteryTrollDistributionStatus? = null,
    val isLoading: Boolean = true,
    val error: BatteryTrollCatalogError? = null
) {
    fun findTroll(trollId: Int): BatteryTrollEntry? = trolls.firstOrNull { it.id == trollId }
}

/**
 * Release builds only render catalogs the server marked `APPROVED`; debug keeps working on a
 * catalog that is still under review. Pure so it stays unit-testable for both build types.
 */
object BatteryTrollDistributionPolicy {
    fun isDistributionAllowed(
        status: BatteryTrollDistributionStatus,
        isDebugBuild: Boolean
    ): Boolean = isDebugBuild || status == BatteryTrollDistributionStatus.APPROVED
}
