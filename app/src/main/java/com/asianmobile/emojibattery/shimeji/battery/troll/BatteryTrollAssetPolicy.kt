package com.asianmobile.emojibattery.shimeji.battery.troll

import com.asianmobile.emojibattery.shimeji.data.model.BATTERY_TROLL_RANDOM_ROTATION_MS
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollEntry

/** The catalog-relative artwork one troll render pass draws instead of the normal themes. */
data class BatteryTrollArtwork(
    val emojiPath: String,
    val batteryPath: String
)

/**
 * Answers "which artwork file does the status bar load right now" for Battery Troll, with no
 * Android dependency so the fallback and the rotation arithmetic stay unit-testable.
 *
 * [BatteryTrollPolicy] decides *which level index* is showing; this object turns that index into
 * concrete paths and, crucially, decides when the troll must **not** take over: a selected troll
 * whose catalog entry is missing (never downloaded, catalog empty offline, id withdrawn
 * server-side) has to fall back to the normal battery/emoji theme rather than leave the bar with
 * no artwork at all.
 */
object BatteryTrollAssetPolicy {

    /**
     * Non-null only when a troll owns the artwork *and* its entry is actually available;
     * `null` means "keep drawing the normal battery/emoji theme".
     *
     * [elapsedMs] must come from the same clock the caller schedules on — the overlay uses
     * `SystemClock.uptimeMillis()`, which is what `View.postDelayed` counts in.
     */
    fun artwork(
        config: BatteryStatusConfig,
        entry: BatteryTrollEntry?,
        elapsedMs: Long
    ): BatteryTrollArtwork? {
        if (!BatteryTrollPolicy.isArtworkActive(config)) return null
        if (entry == null || entry.id != config.trollThemeId) return null
        // A malformed catalog entry would make the clamping helpers index an empty list; the
        // normal theme is a far better outcome than a crash inside the render pass.
        if (entry.emojiPaths.isEmpty() || entry.batteryPaths.isEmpty()) return null
        return BatteryTrollArtwork(
            emojiPath = entry.emojiPathAt(
                BatteryTrollPolicy.emojiLevelIndex(config, elapsedMs)
            ),
            batteryPath = entry.batteryPathAt(
                BatteryTrollPolicy.batteryLevelIndex(config, elapsedMs)
            )
        )
    }

    /**
     * Milliseconds until the artwork changes on its own, or `null` when nothing rotates and the
     * caller must not arm a timer at all — random off, no troll selected, or the selected troll
     * missing from the catalog. Always derived from the period, never a hardcoded delay, so a
     * render that lands mid-period still wakes up exactly on the next boundary.
     */
    fun rotationDelayMs(
        config: BatteryStatusConfig,
        entry: BatteryTrollEntry?,
        elapsedMs: Long
    ): Long? {
        if (!config.trollRandomArtwork) return null
        if (artwork(config, entry, elapsedMs) == null) return null
        val elapsed = elapsedMs.coerceAtLeast(0L)
        return BATTERY_TROLL_RANDOM_ROTATION_MS - elapsed % BATTERY_TROLL_RANDOM_ROTATION_MS
    }
}
