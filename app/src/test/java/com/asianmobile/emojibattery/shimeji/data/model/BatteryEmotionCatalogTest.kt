package com.asianmobile.emojibattery.shimeji.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryEmotionCatalogTest {
    @Test
    fun `emotion taxonomy keeps classic data before eight bundled groups`() {
        assertEquals(
            listOf(
                "classic",
                "emoji",
                "cony",
                "kiiroitori",
                "molang",
                "mochi",
                "tobi",
                "keroppi",
                "pochacco"
            ),
            BATTERY_EMOTION_GROUPS.map(BatteryEmotionGroup::key)
        )
        assertEquals((1..20).toList(), LEGACY_BATTERY_EMOTION_GROUP.emotionIds)
        assertTrue(BUNDLED_BATTERY_EMOTION_GROUPS.all { it.emotionIds.size == 10 })
        assertEquals((21..100).toList(), BUNDLED_BATTERY_EMOTIONS.map(BatteryDecorationEntry::id))
        assertEquals(80, BUNDLED_BATTERY_EMOTIONS.map(BatteryDecorationEntry::id).distinct().size)
        assertEquals(
            (1..100).toList(),
            BATTERY_EMOTION_GROUPS.flatMap(BatteryEmotionGroup::emotionIds)
        )
    }

    @Test
    fun `bundled emotion paths resolve to stable android asset locations`() {
        assertEquals(
            "file:///android_asset/battery_emotions/emoji/emotion_emoji_01.png",
            BUNDLED_BATTERY_EMOTIONS.first().assetPath
        )
        assertEquals(
            "file:///android_asset/battery_emotions/pochacco/emotion_pochacco_10.png",
            BUNDLED_BATTERY_EMOTIONS.last().assetPath
        )
        assertTrue(BUNDLED_BATTERY_EMOTIONS.all { it.type == BatteryDecorationType.EMOTION })
    }
}
