package com.asianmobile.emojibattery.shimeji.battery.troll

import com.asianmobile.emojibattery.shimeji.data.model.BATTERY_TROLL_LEVEL_COUNT
import com.asianmobile.emojibattery.shimeji.data.model.BATTERY_TROLL_RANDOM_ROTATION_MS
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollMode
import com.asianmobile.emojibattery.shimeji.data.model.MAX_BATTERY_TROLL_FAKE_PERCENT
import com.asianmobile.emojibattery.shimeji.data.model.MIN_BATTERY_TROLL_FAKE_PERCENT
import com.asianmobile.emojibattery.shimeji.data.model.NO_BATTERY_TROLL_THEME_ID

/**
 * Everything Battery Troll decides, with no Android dependency so it stays unit-testable.
 *
 * The two decisions are deliberately independent: [displayPercent] answers "what number do we
 * write" and [emojiLevelIndex]/[batteryLevelIndex] answer "which artwork do we draw". A user can
 * fake the number without picking a troll theme, or pick a theme and keep the real number.
 */
object BatteryTrollPolicy {

    /**
     * Hands the emoji, battery and percentage back to the status-bar editor.
     *
     * Both screens write one [BatteryStatusConfig] on purpose — there is a single status bar, so
     * there must be a single source of truth. That only works if the last Apply wins: without
     * this, a troll applied earlier keeps overriding whatever the editor selects, and the
     * editor's Apply silently does nothing the user can see.
     *
     * The level picks and the random/emoji switches are deliberately kept, so returning to
     * Battery Troll still shows what the user last chose there.
     */
    fun releaseOverride(config: BatteryStatusConfig): BatteryStatusConfig = config.copy(
        trollThemeId = NO_BATTERY_TROLL_THEME_ID,
        trollMode = BatteryTrollMode.REAL
    )

    /** True when a troll theme owns the emoji/battery artwork instead of the normal theme. */
    fun isArtworkActive(config: BatteryStatusConfig): Boolean =
        config.trollThemeId != NO_BATTERY_TROLL_THEME_ID

    /**
     * The number written into the status bar. Fake values are allowed past 100 — that is the
     * whole prank — so callers must not reuse this for the battery fill fraction.
     */
    fun displayPercent(config: BatteryStatusConfig, realLevel: Int): Int =
        when (config.trollMode) {
            BatteryTrollMode.FAKE -> config.trollFakePercent.coerceIn(
                MIN_BATTERY_TROLL_FAKE_PERCENT,
                MAX_BATTERY_TROLL_FAKE_PERCENT
            )

            BatteryTrollMode.REAL -> realLevel.coerceIn(0, 100)
        }

    /**
     * Index into the theme's five emoji states, 0 = full … 4 = empty.
     *
     * Custom freezes the user's pick. Random rotates on a wall-clock timer rather than tracking
     * the real level, so a prank does not leak the true charge through the artwork.
     */
    fun emojiLevelIndex(config: BatteryStatusConfig, elapsedRealtimeMs: Long): Int =
        levelIndex(config.trollRandomArtwork, config.trollEmojiLevelIndex, elapsedRealtimeMs)

    /**
     * Index into the theme's five battery levels. Rotates in lockstep with [emojiLevelIndex] on
     * purpose: a crying character next to a full battery reads as a bug, not as a joke.
     */
    fun batteryLevelIndex(config: BatteryStatusConfig, elapsedRealtimeMs: Long): Int =
        levelIndex(config.trollRandomArtwork, config.trollBatteryLevelIndex, elapsedRealtimeMs)

    private fun levelIndex(random: Boolean, selected: Int, elapsedRealtimeMs: Long): Int =
        if (random) {
            rotationIndex(elapsedRealtimeMs)
        } else {
            selected.coerceIn(0, BATTERY_TROLL_LEVEL_COUNT - 1)
        }

    private fun rotationIndex(elapsedRealtimeMs: Long): Int {
        // A clock that has not started yet, or one that wrapped, must not produce a negative
        // index and crash the renderer mid-draw.
        val elapsed = elapsedRealtimeMs.coerceAtLeast(0L)
        return ((elapsed / BATTERY_TROLL_RANDOM_ROTATION_MS) % BATTERY_TROLL_LEVEL_COUNT).toInt()
    }
}
