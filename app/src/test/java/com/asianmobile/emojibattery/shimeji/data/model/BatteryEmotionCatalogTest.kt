package com.asianmobile.emojibattery.shimeji.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryEmotionCatalogTest {
    @Test
    fun `emotion taxonomy keeps classic data before eight server groups`() {
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
        assertTrue(FIGMA_BATTERY_EMOTION_GROUPS.all { it.emotionIds.size == 10 })
        assertEquals(
            (1..100).toList(),
            BATTERY_EMOTION_GROUPS.flatMap(BatteryEmotionGroup::emotionIds)
        )
    }

}
