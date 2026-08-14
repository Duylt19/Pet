package com.asianmobile.emojibattery.shimeji.ui.battery.troll

import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryPowerState
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryPreviewSystemState
import com.asianmobile.emojibattery.shimeji.data.model.BATTERY_TROLL_LEVEL_COUNT
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
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
        assertFalse(state.copy(isApplyInProgress = true).isApplyEnabled)
    }

    @Test
    fun emoji_tiles_follow_both_random_mode_and_the_emoji_switch() {
        val base = BatteryTrollCustomizeUiState(troll = troll, isLoading = false)

        assertTrue(base.isEmojiPickerEnabled)
        assertFalse(base.copy(draft = base.draft.copy(showEmoji = false)).isEmojiPickerEnabled)
        assertFalse(base.copy(draft = base.draft.copy(randomArtwork = true)).isEmojiPickerEnabled)
    }

    @Test
    fun preview_config_lays_the_draft_over_the_stored_bar() {
        val stored = BatteryStatusConfig(
            backgroundColorArgb = 0x11223344,
            percentSizeDp = 10f,
            showPercentage = true,
            trollThemeId = 99,
            trollShowEmoji = true
        )
        val state = BatteryTrollCustomizeUiState(
            troll = troll,
            storedConfig = stored,
            draft = BatteryTrollDraft(
                trollId = troll.id,
                mode = BatteryTrollMode.FAKE,
                fakePercent = 999,
                showPercentage = false,
                percentSizeDp = 28f,
                randomArtwork = true,
                showEmoji = false,
                emojiLevelIndex = 2,
                batteryLevelIndex = 3
            )
        )

        val preview = state.previewConfig
        // Everything this screen does not edit survives untouched…
        assertEquals(stored.backgroundColorArgb, preview.backgroundColorArgb)
        // …and everything it does edit comes from the draft, not from the stored bar.
        assertFalse(preview.showPercentage)
        assertEquals(28f, preview.percentSizeDp, 0f)
        assertEquals(troll.id, preview.trollThemeId)
        assertEquals(999, preview.trollFakePercent)
        assertEquals(2, preview.trollEmojiLevelIndex)
        assertEquals(3, preview.trollBatteryLevelIndex)
        assertTrue(preview.trollRandomArtwork)
        assertFalse(preview.trollShowEmoji)
    }

    @Test
    fun the_written_number_lies_while_the_device_level_stays_real() {
        val state = BatteryTrollCustomizeUiState(
            troll = troll,
            systemState = BatteryPreviewSystemState(powerState = BatteryPowerState(level = 12)),
            draft = BatteryTrollDraft(trollId = troll.id, fakePercent = 999)
        )

        assertEquals(999, state.copy(draft = state.draft.copy(mode = BatteryTrollMode.FAKE))
            .previewPercent)
        assertEquals(12, state.copy(draft = state.draft.copy(mode = BatteryTrollMode.REAL))
            .previewPercent)
        assertEquals(12, state.realBatteryLevel)
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
            {"schema":1,"trollId":7,"mode":"FAKE","fakePercent":99999,
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

    @Test
    fun percent_input_keeps_the_first_invalid_digit_so_the_dialog_can_show_an_error() {
        val input = BatteryTrollPercentInputPolicy.normalize("99999")

        assertEquals("99999", input)
        assertTrue(BatteryTrollPercentInputPolicy.hasError(input))
        assertNull(BatteryTrollPercentInputPolicy.validValue(input))
    }

    @Test
    fun percent_input_accepts_the_maximum_and_ignores_non_digits() {
        val input = BatteryTrollPercentInputPolicy.normalize("99a99")

        assertEquals("9999", input)
        assertFalse(BatteryTrollPercentInputPolicy.hasError(input))
        assertEquals(MAX_BATTERY_TROLL_FAKE_PERCENT, BatteryTrollPercentInputPolicy.validValue(input))
    }
}
