package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import com.asianmobile.emojibattery.shimeji.data.model.BatteryDataType
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDateFont
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDateFormat
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusDisplayMode
import com.asianmobile.emojibattery.shimeji.data.model.normalizeSelectableBatteryThemeId
import org.json.JSONArray
import org.json.JSONObject

/**
 * Versioned process-death state for the battery editor.
 *
 * DataStore remains the source of applied settings. This codec stores only an unapplied draft in
 * SavedStateHandle and deliberately falls back instead of failing the editor on malformed state.
 */
object BatteryDraftCodec {
    fun encode(config: BatteryStatusConfig): String = JSONObject()
        .put(KEY_SCHEMA, SCHEMA_VERSION)
        .put("enabled", config.enabled)
        .put("hasApplied", config.hasApplied)
        .put("selectedThemeId", config.selectedThemeId)
        .put("selectedBatteryThemeId", config.selectedBatteryThemeId)
        .put("selectedEmojiThemeId", config.selectedEmojiThemeId)
        .put("displayMode", config.displayMode.name)
        .put("showTime", config.showTime)
        .put("showPercentage", config.showPercentage)
        .put("backgroundDecorationId", config.backgroundDecorationId)
        .put("showEmotion", config.showEmotion)
        .put("emotionDecorationId", config.emotionDecorationId)
        .put("showAnimation", config.showAnimation)
        .put("animationAssetName", config.animationAssetName)
        .put("barHeightDp", config.barHeightDp.toDouble())
        .put("horizontalPaddingDp", config.horizontalPaddingDp.toDouble())
        .put("leftPaddingDp", config.leftPaddingDp.toDouble())
        .put("rightPaddingDp", config.rightPaddingDp.toDouble())
        .put("percentSizeDp", config.percentSizeDp.toDouble())
        .put("emojiSizeDp", config.emojiSizeDp.toDouble())
        .put("animationSizeDp", config.animationSizeDp.toDouble())
        .put("batterySizeDp", config.batterySizeDp.toDouble())
        .put("backgroundColorArgb", config.backgroundColorArgb)
        .put("foregroundColorArgb", config.foregroundColorArgb)
        .put("percentColorArgb", config.percentColorArgb)
        .put("showWifi", config.showWifi)
        .put("wifiSizeDp", config.wifiSizeDp.toDouble())
        .put("wifiColorArgb", config.wifiColorArgb)
        .put("wifiIconStyleIndex", config.wifiIconStyleIndex)
        .put("dataType", config.dataType.name)
        .put("showData", config.showData)
        .put("dataSizeDp", config.dataSizeDp.toDouble())
        .put("dataColorArgb", config.dataColorArgb)
        .put("showSignal", config.showSignal)
        .put("signalSizeDp", config.signalSizeDp.toDouble())
        .put("signalColorArgb", config.signalColorArgb)
        .put("signalIconStyleIndex", config.signalIconStyleIndex)
        .put("airplaneSizeDp", config.airplaneSizeDp.toDouble())
        .put("showAirplane", config.showAirplane)
        .put("airplaneColorArgb", config.airplaneColorArgb)
        .put("airplaneIconStyleIndex", config.airplaneIconStyleIndex)
        .put("hotspotSizeDp", config.hotspotSizeDp.toDouble())
        .put("showHotspot", config.showHotspot)
        .put("hotspotColorArgb", config.hotspotColorArgb)
        .put("hotspotIconStyleIndex", config.hotspotIconStyleIndex)
        .put("ringerSizeDp", config.ringerSizeDp.toDouble())
        .put("showRinger", config.showRinger)
        .put("ringerColorArgb", config.ringerColorArgb)
        .put("ringerIconStyleIndex", config.ringerIconStyleIndex)
        .put("chargeSizeDp", config.chargeSizeDp.toDouble())
        .put("showCharge", config.showCharge)
        .put("chargeIconIndex", config.chargeIconIndex)
        .put("chargeColorArgb", config.chargeColorArgb)
        .put("showDateTime", config.showDateTime)
        .put("dateTimeColorArgb", config.dateTimeColorArgb)
        .put("dateTimeSizeDp", config.dateTimeSizeDp.toDouble())
        .put("dateFormat", config.dateFormat.name)
        .put("dateTimeFont", config.dateTimeFont.name)
        .put("clockColorArgb", config.clockColorArgb)
        .put("clockSizeDp", config.clockSizeDp.toDouble())
        .put("privacyReserveDp", config.privacyReserveDp.toDouble())
        .put(
            "favoriteThemeIds",
            JSONArray().apply { config.favoriteThemeIds.sorted().forEach(::put) }
        )
        .put(
            "rewardUnlockedThemeIds",
            JSONArray().apply { config.rewardUnlockedThemeIds.sorted().forEach(::put) }
        )
        .toString()

    fun decode(
        value: String?,
        fallback: BatteryStatusConfig = BatteryStatusConfig()
    ): BatteryStatusConfig? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            val json = JSONObject(value)
            if (json.optInt(KEY_SCHEMA, 0) != SCHEMA_VERSION) return@runCatching null
            val selectedThemeId = normalizeSelectableBatteryThemeId(
                json.int("selectedThemeId", fallback.selectedThemeId)
            )
            fallback.copy(
                enabled = json.boolean("enabled", fallback.enabled),
                hasApplied = json.boolean("hasApplied", fallback.hasApplied),
                selectedThemeId = selectedThemeId,
                selectedBatteryThemeId = normalizeSelectableBatteryThemeId(
                    json.int("selectedBatteryThemeId", selectedThemeId)
                ),
                selectedEmojiThemeId = normalizeSelectableBatteryThemeId(
                    json.int("selectedEmojiThemeId", selectedThemeId)
                ),
                displayMode = json.enum(
                    "displayMode",
                    fallback.displayMode,
                    BatteryStatusDisplayMode.entries
                ),
                showTime = json.boolean("showTime", fallback.showTime),
                showPercentage = json.boolean("showPercentage", fallback.showPercentage),
                backgroundDecorationId = json.int(
                    "backgroundDecorationId",
                    fallback.backgroundDecorationId
                ),
                showEmotion = json.boolean("showEmotion", fallback.showEmotion),
                emotionDecorationId = json.int(
                    "emotionDecorationId",
                    fallback.emotionDecorationId
                ),
                showAnimation = json.boolean("showAnimation", fallback.showAnimation),
                animationAssetName = json.string(
                    "animationAssetName",
                    fallback.animationAssetName
                ),
                barHeightDp = json.float("barHeightDp", fallback.barHeightDp),
                horizontalPaddingDp = json.float(
                    "horizontalPaddingDp",
                    fallback.horizontalPaddingDp
                ),
                leftPaddingDp = json.float("leftPaddingDp", fallback.leftPaddingDp),
                rightPaddingDp = json.float("rightPaddingDp", fallback.rightPaddingDp),
                percentSizeDp = json.float("percentSizeDp", fallback.percentSizeDp),
                emojiSizeDp = json.float("emojiSizeDp", fallback.emojiSizeDp),
                animationSizeDp = json.float("animationSizeDp", fallback.animationSizeDp),
                batterySizeDp = json.float("batterySizeDp", fallback.batterySizeDp),
                backgroundColorArgb = json.int(
                    "backgroundColorArgb",
                    fallback.backgroundColorArgb
                ),
                foregroundColorArgb = json.int(
                    "foregroundColorArgb",
                    fallback.foregroundColorArgb
                ),
                percentColorArgb = json.int("percentColorArgb", fallback.percentColorArgb),
                showWifi = json.boolean("showWifi", fallback.showWifi),
                wifiSizeDp = json.float("wifiSizeDp", fallback.wifiSizeDp),
                wifiColorArgb = json.int("wifiColorArgb", fallback.wifiColorArgb),
                wifiIconStyleIndex = json.int(
                    "wifiIconStyleIndex",
                    fallback.wifiIconStyleIndex
                ),
                dataType = json.enum("dataType", fallback.dataType, BatteryDataType.entries),
                showData = json.boolean("showData", fallback.showData),
                dataSizeDp = json.float("dataSizeDp", fallback.dataSizeDp),
                dataColorArgb = json.int("dataColorArgb", fallback.dataColorArgb),
                showSignal = json.boolean("showSignal", fallback.showSignal),
                signalSizeDp = json.float("signalSizeDp", fallback.signalSizeDp),
                signalColorArgb = json.int("signalColorArgb", fallback.signalColorArgb),
                signalIconStyleIndex = json.int(
                    "signalIconStyleIndex",
                    fallback.signalIconStyleIndex
                ),
                airplaneSizeDp = json.float("airplaneSizeDp", fallback.airplaneSizeDp),
                showAirplane = json.boolean("showAirplane", fallback.showAirplane),
                airplaneColorArgb = json.int(
                    "airplaneColorArgb",
                    fallback.airplaneColorArgb
                ),
                airplaneIconStyleIndex = json.int(
                    "airplaneIconStyleIndex",
                    fallback.airplaneIconStyleIndex
                ),
                hotspotSizeDp = json.float("hotspotSizeDp", fallback.hotspotSizeDp),
                showHotspot = json.boolean("showHotspot", fallback.showHotspot),
                hotspotColorArgb = json.int("hotspotColorArgb", fallback.hotspotColorArgb),
                hotspotIconStyleIndex = json.int(
                    "hotspotIconStyleIndex",
                    fallback.hotspotIconStyleIndex
                ),
                ringerSizeDp = json.float("ringerSizeDp", fallback.ringerSizeDp),
                showRinger = json.boolean("showRinger", fallback.showRinger),
                ringerColorArgb = json.int("ringerColorArgb", fallback.ringerColorArgb),
                ringerIconStyleIndex = json.int(
                    "ringerIconStyleIndex",
                    fallback.ringerIconStyleIndex
                ),
                chargeSizeDp = json.float("chargeSizeDp", fallback.chargeSizeDp),
                showCharge = json.boolean("showCharge", fallback.showCharge),
                chargeIconIndex = json.int("chargeIconIndex", fallback.chargeIconIndex),
                chargeColorArgb = json.int("chargeColorArgb", fallback.chargeColorArgb),
                showDateTime = json.boolean("showDateTime", fallback.showDateTime),
                dateTimeColorArgb = json.int(
                    "dateTimeColorArgb",
                    fallback.dateTimeColorArgb
                ),
                dateTimeSizeDp = json.float("dateTimeSizeDp", fallback.dateTimeSizeDp),
                dateFormat = json.enum(
                    "dateFormat",
                    fallback.dateFormat,
                    BatteryDateFormat.entries
                ),
                dateTimeFont = json.enum(
                    "dateTimeFont",
                    fallback.dateTimeFont,
                    BatteryDateFont.entries
                ),
                clockColorArgb = json.int("clockColorArgb", fallback.clockColorArgb),
                clockSizeDp = json.float("clockSizeDp", fallback.clockSizeDp),
                privacyReserveDp = json.float(
                    "privacyReserveDp",
                    fallback.privacyReserveDp
                ),
                favoriteThemeIds = json.intSet(
                    "favoriteThemeIds",
                    fallback.favoriteThemeIds
                ),
                rewardUnlockedThemeIds = json.intSet(
                    "rewardUnlockedThemeIds",
                    fallback.rewardUnlockedThemeIds
                )
            )
        }.getOrNull()
    }

    private fun JSONObject.boolean(key: String, fallback: Boolean): Boolean =
        if (has(key)) optBoolean(key, fallback) else fallback

    private fun JSONObject.int(key: String, fallback: Int): Int =
        if (has(key)) optInt(key, fallback) else fallback

    private fun JSONObject.float(key: String, fallback: Float): Float =
        if (has(key)) optDouble(key, fallback.toDouble()).toFloat() else fallback

    private fun JSONObject.string(key: String, fallback: String): String =
        if (has(key)) optString(key, fallback) else fallback

    private fun <T : Enum<T>> JSONObject.enum(
        key: String,
        fallback: T,
        values: List<T>
    ): T = optString(key)
        .let { name -> values.firstOrNull { it.name == name } }
        ?: fallback

    private fun JSONObject.intSet(key: String, fallback: Set<Int>): Set<Int> {
        val values = optJSONArray(key) ?: return fallback
        return buildSet {
            repeat(values.length()) { index ->
                val value = values.optInt(index, Int.MIN_VALUE)
                if (value >= 0) add(value)
            }
        }
    }

    private const val KEY_SCHEMA = "schema"
    private const val SCHEMA_VERSION = 5
}
