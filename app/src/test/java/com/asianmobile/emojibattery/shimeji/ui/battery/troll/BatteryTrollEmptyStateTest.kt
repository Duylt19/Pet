package com.asianmobile.emojibattery.shimeji.ui.battery.troll

import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollCatalogError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The empty grid used to collapse every catalog failure into one message with a retry that could
 * never succeed for an unpublished catalog. These lock in that the three failures stay distinct.
 */
class BatteryTrollEmptyStateTest {
    @Test
    fun `an empty but healthy catalog says there is nothing yet`() {
        assertEquals(
            R.string.battery_troll_empty,
            batteryTrollEmptyMessageRes(null)
        )
        assertTrue(batteryTrollCanRetry(null))
    }

    @Test
    fun `a network failure offers a retry`() {
        assertEquals(
            R.string.battery_troll_error_offline,
            batteryTrollEmptyMessageRes(BatteryTrollCatalogError.CATALOG_UNAVAILABLE)
        )
        assertTrue(batteryTrollCanRetry(BatteryTrollCatalogError.CATALOG_UNAVAILABLE))
    }

    @Test
    fun `an unreadable catalog offers a retry with its own message`() {
        assertEquals(
            R.string.battery_troll_error_invalid,
            batteryTrollEmptyMessageRes(BatteryTrollCatalogError.CATALOG_INVALID)
        )
        assertTrue(batteryTrollCanRetry(BatteryTrollCatalogError.CATALOG_INVALID))
    }

    @Test
    fun `an unpublished catalog says coming soon and never offers a retry`() {
        assertEquals(
            R.string.battery_troll_unpublished,
            batteryTrollEmptyMessageRes(BatteryTrollCatalogError.DISTRIBUTION_NOT_APPROVED)
        )
        assertFalse(batteryTrollCanRetry(BatteryTrollCatalogError.DISTRIBUTION_NOT_APPROVED))
    }

    @Test
    fun `every failure maps to its own string`() {
        val messages = BatteryTrollCatalogError.entries.map(::batteryTrollEmptyMessageRes) +
            batteryTrollEmptyMessageRes(null)
        assertEquals(messages.size, messages.toSet().size)
    }
}
