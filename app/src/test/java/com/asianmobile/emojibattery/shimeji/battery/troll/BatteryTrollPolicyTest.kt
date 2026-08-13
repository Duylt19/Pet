package com.asianmobile.emojibattery.shimeji.battery.troll

import com.asianmobile.emojibattery.shimeji.data.model.BATTERY_TROLL_RANDOM_ROTATION_MS
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollMode
import com.asianmobile.emojibattery.shimeji.data.model.NO_BATTERY_TROLL_THEME_ID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryTrollPolicyTest {

    private fun config(
        trollMode: BatteryTrollMode = BatteryTrollMode.REAL,
        trollFakePercent: Int = 999,
        trollThemeId: Int = NO_BATTERY_TROLL_THEME_ID,
        trollEmojiLevelIndex: Int = 0,
        trollBatteryLevelIndex: Int = 0,
        trollRandomArtwork: Boolean = false
    ): BatteryStatusConfig = BatteryStatusConfig(
        trollMode = trollMode,
        trollFakePercent = trollFakePercent,
        trollThemeId = trollThemeId,
        trollEmojiLevelIndex = trollEmojiLevelIndex,
        trollBatteryLevelIndex = trollBatteryLevelIndex,
        trollRandomArtwork = trollRandomArtwork
    )

    @Test
    fun `applying the status bar editor takes the artwork back from a troll`() {
        val trolling = config(trollMode = BatteryTrollMode.FAKE, trollThemeId = 3)
        val released = BatteryTrollPolicy.releaseOverride(trolling)
        assertFalse(BatteryTrollPolicy.isArtworkActive(released))
        assertEquals(64, BatteryTrollPolicy.displayPercent(released, realLevel = 64))
    }

    @Test
    fun `releasing the override still remembers what the user picked in Battery Troll`() {
        val trolling = config(
            trollMode = BatteryTrollMode.FAKE,
            trollThemeId = 3,
            trollEmojiLevelIndex = 2,
            trollBatteryLevelIndex = 4,
            trollRandomArtwork = true
        )
        val released = BatteryTrollPolicy.releaseOverride(trolling)
        assertEquals(2, released.trollEmojiLevelIndex)
        assertEquals(4, released.trollBatteryLevelIndex)
        assertTrue(released.trollRandomArtwork)
        assertEquals(999, released.trollFakePercent)
    }

    @Test
    fun `no troll theme means the normal artwork keeps the status bar`() {
        assertFalse(BatteryTrollPolicy.isArtworkActive(config()))
        assertTrue(BatteryTrollPolicy.isArtworkActive(config(trollThemeId = 3)))
    }

    @Test
    fun `real mode reports the level the device actually has`() {
        assertEquals(42, BatteryTrollPolicy.displayPercent(config(), realLevel = 42))
    }

    @Test
    fun `a device reporting nonsense is still clamped to a real percentage`() {
        assertEquals(100, BatteryTrollPolicy.displayPercent(config(), realLevel = 130))
        assertEquals(0, BatteryTrollPolicy.displayPercent(config(), realLevel = -1))
    }

    @Test
    fun `fake mode ignores the real level entirely`() {
        val fake = config(trollMode = BatteryTrollMode.FAKE, trollFakePercent = 999)
        assertEquals(999, BatteryTrollPolicy.displayPercent(fake, realLevel = 7))
    }

    @Test
    fun `a fake percentage past the prank ceiling is brought back into range`() {
        val tooHigh = config(trollMode = BatteryTrollMode.FAKE, trollFakePercent = 5_000)
        assertEquals(999, BatteryTrollPolicy.displayPercent(tooHigh, realLevel = 50))
        val negative = config(trollMode = BatteryTrollMode.FAKE, trollFakePercent = -8)
        assertEquals(0, BatteryTrollPolicy.displayPercent(negative, realLevel = 50))
    }

    @Test
    fun `custom selection stays put no matter how long the bar has been up`() {
        val custom = config(trollThemeId = 1, trollEmojiLevelIndex = 3, trollBatteryLevelIndex = 2)
        assertEquals(3, BatteryTrollPolicy.emojiLevelIndex(custom, elapsedRealtimeMs = 0))
        assertEquals(
            3,
            BatteryTrollPolicy.emojiLevelIndex(custom, elapsedRealtimeMs = 9_000_000)
        )
        assertEquals(2, BatteryTrollPolicy.batteryLevelIndex(custom, elapsedRealtimeMs = 9_000_000))
    }

    @Test
    fun `a stored index outside the five shipped levels is brought back in range`() {
        val broken = config(trollThemeId = 1, trollEmojiLevelIndex = 9)
        assertEquals(4, BatteryTrollPolicy.emojiLevelIndex(broken, elapsedRealtimeMs = 0))
    }

    @Test
    fun `random rotates one step per period and wraps after the fifth`() {
        val random = config(trollThemeId = 1, trollRandomArtwork = true)
        val period = BATTERY_TROLL_RANDOM_ROTATION_MS
        val seen = (0..5).map { step ->
            BatteryTrollPolicy.emojiLevelIndex(random, elapsedRealtimeMs = period * step)
        }
        assertEquals(listOf(0, 1, 2, 3, 4, 0), seen)
    }

    @Test
    fun `random keeps the character and the battery telling the same story`() {
        val random = config(
            trollThemeId = 1,
            trollEmojiLevelIndex = 4,
            trollBatteryLevelIndex = 0,
            trollRandomArtwork = true
        )
        val at = BATTERY_TROLL_RANDOM_ROTATION_MS * 3
        assertEquals(
            BatteryTrollPolicy.emojiLevelIndex(random, at),
            BatteryTrollPolicy.batteryLevelIndex(random, at)
        )
    }

    @Test
    fun `a clock that has not started yet cannot produce a negative index`() {
        val random = config(trollThemeId = 1, trollRandomArtwork = true)
        assertEquals(0, BatteryTrollPolicy.emojiLevelIndex(random, elapsedRealtimeMs = -5_000))
    }
}
