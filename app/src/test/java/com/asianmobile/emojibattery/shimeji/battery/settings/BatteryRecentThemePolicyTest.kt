package com.asianmobile.emojibattery.shimeji.battery.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryRecentThemePolicyTest {
    @Test
    fun `record puts opened theme first and removes its older occurrence`() {
        val result = BatteryRecentThemePolicy.record(
            currentThemeIds = listOf(4, 2, 7),
            openedThemeId = 2
        )

        assertEquals(listOf(2, 4, 7), result)
    }

    @Test
    fun `record ignores built in and invalid theme ids`() {
        val result = BatteryRecentThemePolicy.record(
            currentThemeIds = listOf(-1, 0, 3, 3, 5),
            openedThemeId = 0
        )

        assertEquals(listOf(3, 5), result)
    }

    @Test
    fun `record retains only the newest thirty themes`() {
        val result = BatteryRecentThemePolicy.record(
            currentThemeIds = (1..40).toList(),
            openedThemeId = 40
        )

        assertEquals(30, result.size)
        assertEquals(40, result.first())
        assertEquals(29, result.last())
    }

    @Test
    fun `stored recent list round trips in MRU order`() {
        val encoded = BatteryRecentThemePolicy.encode(listOf(9, 2, 6))

        assertEquals(listOf(9, 2, 6), BatteryRecentThemePolicy.decode(encoded))
    }
}
