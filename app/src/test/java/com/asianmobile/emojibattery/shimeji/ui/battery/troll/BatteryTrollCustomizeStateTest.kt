package com.asianmobile.emojibattery.shimeji.ui.battery.troll

import com.asianmobile.emojibattery.shimeji.data.model.BATTERY_TROLL_LEVEL_COUNT
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollBatteryOrientation
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollCatalogError
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollEntitlement
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollEntry
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollMode
import com.asianmobile.emojibattery.shimeji.data.model.MAX_BATTERY_TROLL_FAKE_PERCENT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryTrollCustomizeStateTest {
    private val troll = BatteryTrollEntry(
        id = 7,
        name = "Black Cat",
        slug = "black-cat",
        order = 1,
        entitlement = BatteryTrollEntitlement.FREE,
        batteryOrientation = BatteryTrollBatteryOrientation.LANDSCAPE,
        thumbnailPath = "thumb/TROLL_7.webp",
        emojiPaths = (1..BATTERY_TROLL_LEVEL_COUNT).map { "emoji/TROLL_7_$it.webp" },
        batteryPaths = (1..BATTERY_TROLL_LEVEL_COUNT).map { "battery/TROLL_7_$it.webp" }
    )

    @Test
    fun apply_is_blocked_while_the_catalog_is_still_loading() {
        val state = BatteryTrollCustomizeUiState(troll = null, isLoading = true)

        assertFalse(state.isApplyEnabled)
        assertFalse(state.isUnavailable)
    }

    @Test
    fun apply_is_blocked_when_the_troll_never_resolved() {
        val state = BatteryTrollCustomizeUiState(
            troll = null,
            isLoading = false,
            catalogError = BatteryTrollCatalogError.CATALOG_UNAVAILABLE
        )

        assertFalse(state.isApplyEnabled)
        assertTrue(state.isUnavailable)
    }

    @Test
    fun apply_is_allowed_once_the_artwork_resolved() {
        val state = BatteryTrollCustomizeUiState(troll = troll, isLoading = false)

        assertTrue(state.isApplyEnabled)
        assertFalse(state.isUnavailable)
    }

    @Test
    fun emoji_tiles_follow_both_random_mode_and_the_emoji_switch() {
        val base = BatteryTrollCustomizeUiState(troll = troll, isLoading = false)

        assertTrue(base.isEmojiPickerEnabled)
        assertFalse(base.copy(draft = base.draft.copy(showEmoji = false)).isEmojiPickerEnabled)
        assertFalse(base.copy(draft = base.draft.copy(randomArtwork = true)).isEmojiPickerEnabled)
    }

    @Test
    fun codec_round_trips_a_draft() {
        val draft = BatteryTrollDraft(
            trollId = 7,
            mode = BatteryTrollMode.REAL,
            fakePercent = 404,
            showPercentage = false,
            percentSizeDp = 21.5f,
            randomArtwork = true,
            showEmoji = false,
            emojiLevelIndex = 3,
            batteryLevelIndex = 4
        )

        assertEquals(draft, BatteryTrollDraftCodec.decode(BatteryTrollDraftCodec.encode(draft)))
    }

    @Test
    fun codec_rejects_garbage_and_foreign_schemas() {
        assertNull(BatteryTrollDraftCodec.decode(null))
        assertNull(BatteryTrollDraftCodec.decode(""))
        assertNull(BatteryTrollDraftCodec.decode("not-json"))
        assertNull(BatteryTrollDraftCodec.decode("""{"schema":99,"trollId":7}"""))
    }

    @Test
    fun codec_clamps_out_of_range_values_instead_of_restoring_them() {
        val restored = BatteryTrollDraftCodec.decode(
            """
            {"schema":1,"trollId":7,"mode":"FAKE","fakePercent":9999,
             "emojiLevelIndex":99,"batteryLevelIndex":-4}
            """.trimIndent()
        )

        requireNotNull(restored)
        assertEquals(MAX_BATTERY_TROLL_FAKE_PERCENT, restored.fakePercent)
        assertEquals(BATTERY_TROLL_LEVEL_COUNT - 1, restored.emojiLevelIndex)
        assertEquals(0, restored.batteryLevelIndex)
    }

    @Test
    fun codec_falls_back_field_by_field_for_a_partial_payload() {
        val fallback = BatteryTrollDraft(trollId = 7, showEmoji = false)
        val restored = BatteryTrollDraftCodec.decode(
            """{"schema":1,"trollId":7,"fakePercent":50}""",
            fallback
        )

        requireNotNull(restored)
        assertEquals(50, restored.fakePercent)
        assertEquals(fallback.mode, restored.mode)
        assertEquals(fallback.percentSizeDp, restored.percentSizeDp, 0f)
        assertFalse(restored.showEmoji)
    }
}
