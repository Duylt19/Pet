package com.asianmobile.emojibattery.shimeji.ui.battery.troll

import com.asianmobile.emojibattery.shimeji.data.model.BATTERY_TROLL_LEVEL_COUNT
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollCatalogError
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollEntry
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollMode
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_PERCENT_SIZE_DP
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_TROLL_FAKE_PERCENT
import com.asianmobile.emojibattery.shimeji.data.model.MAX_BATTERY_TROLL_FAKE_PERCENT
import com.asianmobile.emojibattery.shimeji.data.model.MIN_BATTERY_TROLL_FAKE_PERCENT
import com.asianmobile.emojibattery.shimeji.data.model.NO_BATTERY_TROLL_THEME_ID
import org.json.JSONObject

/**
 * State of the Battery Troll Customize screen (Figma `8315:8232` / `8359:6992` / `8359:7165`).
 *
 * [draft] is what the controls show; [applied] is what the status bar is actually running. Apply
 * copies one onto the other, and Back compares them to decide whether the discard sheet is owed.
 */
data class BatteryTrollCustomizeUiState(
    val troll: BatteryTrollEntry? = null,
    val draft: BatteryTrollDraft = BatteryTrollDraft(),
    val applied: BatteryTrollDraft = BatteryTrollDraft(),
    val realBatteryLevel: Int = 100,
    val isBatteryEnabled: Boolean = false,
    val isEditingFakePercent: Boolean = false,
    val isDiscardVisible: Boolean = false,
    val isLoading: Boolean = true,
    /** Why the catalog could not produce [troll]; `null` while it still can. */
    val catalogError: BatteryTrollCatalogError? = null
) {
    val hasUnsavedChanges: Boolean get() = draft != applied

    /** The number the preview strip writes, which is the whole point of Fake mode. */
    val previewPercent: Int
        get() = when (draft.mode) {
            BatteryTrollMode.FAKE -> draft.fakePercent
            BatteryTrollMode.REAL -> realBatteryLevel
        }

    /** Real mode has nothing to edit, and Random takes both pickers away from the user. */
    val isEditEnabled: Boolean get() = draft.mode == BatteryTrollMode.FAKE
    val isArtworkPickerEnabled: Boolean get() = !draft.randomArtwork

    /** Picking an emoji level is meaningless while the character is switched off. */
    val isEmojiPickerEnabled: Boolean get() = isArtworkPickerEnabled && draft.showEmoji

    /** The controls are only worth showing once there is artwork behind them. */
    val isTrollResolved: Boolean get() = troll != null
    val isUnavailable: Boolean get() = !isLoading && !isTrollResolved

    /**
     * Apply writes `trollThemeId`, so it must not run for a troll the catalog never resolved: the
     * status bar would silently fall back to the normal theme and the button would look broken.
     */
    val isApplyEnabled: Boolean get() = isTrollResolved && !isLoading
}

data class BatteryTrollDraft(
    val trollId: Int = NO_BATTERY_TROLL_THEME_ID,
    val mode: BatteryTrollMode = BatteryTrollMode.FAKE,
    val fakePercent: Int = DEFAULT_BATTERY_TROLL_FAKE_PERCENT,
    val showPercentage: Boolean = true,
    val percentSizeDp: Float = DEFAULT_BATTERY_PERCENT_SIZE_DP,
    val randomArtwork: Boolean = false,
    /** Drives `BatteryStatusConfig.trollShowEmoji`: off leaves the battery shell without a face. */
    val showEmoji: Boolean = true,
    val emojiLevelIndex: Int = 0,
    val batteryLevelIndex: Int = 0
)

/**
 * Versioned process-death state for the Battery Troll draft, mirroring `BatteryDraftCodec` in the
 * status-bar editor: DataStore stays the source of applied settings and only the *unapplied* draft
 * travels in `SavedStateHandle`. Malformed state falls back to "no draft" instead of failing the
 * screen.
 *
 * It lives beside [BatteryTrollDraft] rather than in its own file because it encodes exactly this
 * one type and nothing else may produce it.
 */
object BatteryTrollDraftCodec {
    fun encode(draft: BatteryTrollDraft): String = JSONObject()
        .put(KEY_SCHEMA, SCHEMA_VERSION)
        .put("trollId", draft.trollId)
        .put("mode", draft.mode.name)
        .put("fakePercent", draft.fakePercent)
        .put("showPercentage", draft.showPercentage)
        .put("percentSizeDp", draft.percentSizeDp.toDouble())
        .put("randomArtwork", draft.randomArtwork)
        .put("showEmoji", draft.showEmoji)
        .put("emojiLevelIndex", draft.emojiLevelIndex)
        .put("batteryLevelIndex", draft.batteryLevelIndex)
        .toString()

    fun decode(
        value: String?,
        fallback: BatteryTrollDraft = BatteryTrollDraft()
    ): BatteryTrollDraft? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            val json = JSONObject(value)
            if (json.optInt(KEY_SCHEMA, 0) != SCHEMA_VERSION) return@runCatching null
            fallback.copy(
                trollId = json.int("trollId", fallback.trollId),
                mode = BatteryTrollMode.entries
                    .firstOrNull { it.name == json.optString("mode") }
                    ?: fallback.mode,
                fakePercent = json.int("fakePercent", fallback.fakePercent).coerceIn(
                    MIN_BATTERY_TROLL_FAKE_PERCENT,
                    MAX_BATTERY_TROLL_FAKE_PERCENT
                ),
                showPercentage = json.boolean("showPercentage", fallback.showPercentage),
                percentSizeDp = json.float("percentSizeDp", fallback.percentSizeDp),
                randomArtwork = json.boolean("randomArtwork", fallback.randomArtwork),
                showEmoji = json.boolean("showEmoji", fallback.showEmoji),
                emojiLevelIndex = json.int("emojiLevelIndex", fallback.emojiLevelIndex)
                    .coerceIn(0, BATTERY_TROLL_LEVEL_COUNT - 1),
                batteryLevelIndex = json.int("batteryLevelIndex", fallback.batteryLevelIndex)
                    .coerceIn(0, BATTERY_TROLL_LEVEL_COUNT - 1)
            )
        }.getOrNull()
    }

    private fun JSONObject.boolean(key: String, fallback: Boolean): Boolean =
        if (has(key)) optBoolean(key, fallback) else fallback

    private fun JSONObject.int(key: String, fallback: Int): Int =
        if (has(key)) optInt(key, fallback) else fallback

    private fun JSONObject.float(key: String, fallback: Float): Float =
        if (has(key)) optDouble(key, fallback.toDouble()).toFloat() else fallback

    private const val KEY_SCHEMA = "schema"
    private const val SCHEMA_VERSION = 1
}

sealed interface BatteryTrollCustomizeEffect {
    data object Close : BatteryTrollCustomizeEffect
    data object RequestBatteryAccessibility : BatteryTrollCustomizeEffect
}
