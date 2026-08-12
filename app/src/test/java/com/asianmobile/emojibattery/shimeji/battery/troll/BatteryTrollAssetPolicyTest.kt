package com.asianmobile.emojibattery.shimeji.battery.troll

import com.asianmobile.emojibattery.shimeji.data.model.BATTERY_TROLL_LEVEL_COUNT
import com.asianmobile.emojibattery.shimeji.data.model.BATTERY_TROLL_RANDOM_ROTATION_MS
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollBatteryOrientation
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollEntitlement
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollEntry
import com.asianmobile.emojibattery.shimeji.data.model.NO_BATTERY_TROLL_THEME_ID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryTrollAssetPolicyTest {

    private fun entry(
        id: Int = TROLL_ID,
        emojiPaths: List<String> = (0 until BATTERY_TROLL_LEVEL_COUNT).map { "troll/emoji_$it.webp" },
        batteryPaths: List<String> = (0 until BATTERY_TROLL_LEVEL_COUNT).map { "troll/bat_$it.webp" }
    ): BatteryTrollEntry = BatteryTrollEntry(
        id = id,
        name = "Grumpy",
        slug = "grumpy",
        order = 1,
        entitlement = BatteryTrollEntitlement.FREE,
        batteryOrientation = BatteryTrollBatteryOrientation.LANDSCAPE,
        thumbnailPath = "troll/thumb.webp",
        emojiPaths = emojiPaths,
        batteryPaths = batteryPaths
    )

    private fun config(
        trollThemeId: Int = TROLL_ID,
        trollEmojiLevelIndex: Int = 0,
        trollBatteryLevelIndex: Int = 0,
        trollRandomArtwork: Boolean = false
    ): BatteryStatusConfig = BatteryStatusConfig(
        trollThemeId = trollThemeId,
        trollEmojiLevelIndex = trollEmojiLevelIndex,
        trollBatteryLevelIndex = trollBatteryLevelIndex,
        trollRandomArtwork = trollRandomArtwork
    )

    @Test
    fun `no troll selected leaves the normal battery and emoji themes in place`() {
        val artwork = BatteryTrollAssetPolicy.artwork(
            config(trollThemeId = NO_BATTERY_TROLL_THEME_ID),
            entry(),
            elapsedMs = 0
        )
        assertNull(artwork)
    }

    @Test
    fun `a troll that is not in the catalog falls back instead of blanking the bar`() {
        assertNull(BatteryTrollAssetPolicy.artwork(config(), entry = null, elapsedMs = 0))
    }

    @Test
    fun `a catalog entry for a different troll is not used as a stand-in`() {
        assertNull(
            BatteryTrollAssetPolicy.artwork(config(), entry(id = TROLL_ID + 1), elapsedMs = 0)
        )
    }

    @Test
    fun `a troll with no artwork listed falls back rather than indexing an empty list`() {
        assertNull(
            BatteryTrollAssetPolicy.artwork(config(), entry(emojiPaths = emptyList()), elapsedMs = 0)
        )
        assertNull(
            BatteryTrollAssetPolicy.artwork(
                config(),
                entry(batteryPaths = emptyList()),
                elapsedMs = 0
            )
        )
    }

    @Test
    fun `a custom selection loads exactly the picked levels`() {
        val artwork = BatteryTrollAssetPolicy.artwork(
            config(trollEmojiLevelIndex = 3, trollBatteryLevelIndex = 1),
            entry(),
            elapsedMs = BATTERY_TROLL_RANDOM_ROTATION_MS * 7
        )
        assertEquals("troll/emoji_3.webp", artwork?.emojiPath)
        assertEquals("troll/bat_1.webp", artwork?.batteryPath)
    }

    @Test
    fun `random artwork follows the clock and wraps with the level count`() {
        val random = config(trollEmojiLevelIndex = 4, trollRandomArtwork = true)
        val paths = (0..5).map { step ->
            BatteryTrollAssetPolicy.artwork(
                random,
                entry(),
                elapsedMs = BATTERY_TROLL_RANDOM_ROTATION_MS * step
            )?.emojiPath
        }
        assertEquals(
            listOf(
                "troll/emoji_0.webp",
                "troll/emoji_1.webp",
                "troll/emoji_2.webp",
                "troll/emoji_3.webp",
                "troll/emoji_4.webp",
                "troll/emoji_0.webp"
            ),
            paths
        )
    }

    @Test
    fun `nothing is scheduled when the artwork cannot change on its own`() {
        assertNull(BatteryTrollAssetPolicy.rotationDelayMs(config(), entry(), elapsedMs = 0))
        assertNull(
            BatteryTrollAssetPolicy.rotationDelayMs(
                config(trollThemeId = NO_BATTERY_TROLL_THEME_ID, trollRandomArtwork = true),
                entry(),
                elapsedMs = 0
            )
        )
        assertNull(
            BatteryTrollAssetPolicy.rotationDelayMs(
                config(trollRandomArtwork = true),
                entry = null,
                elapsedMs = 0
            )
        )
    }

    @Test
    fun `the rotation wake-up lands on the next period boundary`() {
        val random = config(trollRandomArtwork = true)
        assertEquals(
            BATTERY_TROLL_RANDOM_ROTATION_MS,
            BatteryTrollAssetPolicy.rotationDelayMs(random, entry(), elapsedMs = 0)
        )
        assertEquals(
            BATTERY_TROLL_RANDOM_ROTATION_MS - 12_000L,
            BatteryTrollAssetPolicy.rotationDelayMs(
                random,
                entry(),
                elapsedMs = BATTERY_TROLL_RANDOM_ROTATION_MS * 4 + 12_000L
            )
        )
    }

    @Test
    fun `the scheduled wake-up is always inside one period and never immediate`() {
        val random = config(trollRandomArtwork = true)
        listOf(0L, 1L, 59_999L, 60_001L, 9_876_543L).forEach { elapsed ->
            val delay = BatteryTrollAssetPolicy.rotationDelayMs(random, entry(), elapsed)
            requireNotNull(delay)
            assert(delay in 1..BATTERY_TROLL_RANDOM_ROTATION_MS) {
                "delay $delay for elapsed $elapsed escapes one rotation period"
            }
        }
    }

    @Test
    fun `a clock reading before zero still resolves the first level`() {
        val random = config(trollRandomArtwork = true)
        assertEquals(
            "troll/emoji_0.webp",
            BatteryTrollAssetPolicy.artwork(random, entry(), elapsedMs = -5_000)?.emojiPath
        )
        assertEquals(
            BATTERY_TROLL_RANDOM_ROTATION_MS,
            BatteryTrollAssetPolicy.rotationDelayMs(random, entry(), elapsedMs = -5_000)
        )
    }

    private companion object {
        const val TROLL_ID = 4
    }
}
