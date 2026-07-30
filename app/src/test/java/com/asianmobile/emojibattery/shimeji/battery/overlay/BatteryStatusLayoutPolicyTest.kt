package com.asianmobile.emojibattery.shimeji.battery.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryStatusLayoutPolicyTest {
    private val policy = BatteryStatusLayoutPolicy()

    @Test
    fun resolve_keeps_everything_when_content_fits() {
        val result = policy.resolve(
            availableWidth = 100f,
            items = listOf(
                item(BatteryStatusComponent.TIME, 20f, 100),
                item(BatteryStatusComponent.BATTERY, 20f, 100, required = true),
                item(BatteryStatusComponent.EMOTION, 20f, 10)
            )
        )

        assertEquals(3, result.visibleComponents.size)
        assertTrue(result.hiddenComponents.isEmpty())
    }

    @Test
    fun resolve_hides_low_priority_content_before_core_status() {
        val result = policy.resolve(
            availableWidth = 62f,
            items = listOf(
                item(BatteryStatusComponent.TIME, 20f, 100),
                item(BatteryStatusComponent.BATTERY, 20f, 100, required = true),
                item(BatteryStatusComponent.WIFI, 20f, 90),
                item(BatteryStatusComponent.DATE, 20f, 10),
                item(BatteryStatusComponent.EMOTION, 20f, 20)
            )
        )

        assertTrue(result.shows(BatteryStatusComponent.TIME))
        assertTrue(result.shows(BatteryStatusComponent.BATTERY))
        assertTrue(result.shows(BatteryStatusComponent.WIFI))
        assertFalse(result.shows(BatteryStatusComponent.DATE))
        assertFalse(result.shows(BatteryStatusComponent.EMOTION))
    }

    @Test
    fun resolve_never_removes_required_component() {
        val result = policy.resolve(
            availableWidth = 1f,
            items = listOf(
                item(BatteryStatusComponent.BATTERY, 30f, 0, required = true),
                item(BatteryStatusComponent.TIME, 20f, 100)
            )
        )

        assertTrue(result.shows(BatteryStatusComponent.BATTERY))
        assertFalse(result.shows(BatteryStatusComponent.TIME))
    }

    @Test
    fun resolve_rejects_invalid_widths_fail_closed() {
        val result = policy.resolve(
            availableWidth = 50f,
            items = listOf(
                item(BatteryStatusComponent.BATTERY, 20f, 100, required = true),
                item(BatteryStatusComponent.DATE, Float.NaN, 10)
            )
        )

        assertEquals(setOf(BatteryStatusComponent.BATTERY), result.visibleComponents)
        assertEquals(setOf(BatteryStatusComponent.DATE), result.hiddenComponents)
    }

    @Test
    fun physicalSides_mirror_leading_and_trailing_groups_in_rtl() {
        assertEquals(
            BatteryStatusPhysicalSides(
                leadingFromLeft = true,
                trailingFromLeft = false
            ),
            BatteryStatusPhysicalSides.resolve(isRtl = false)
        )
        assertEquals(
            BatteryStatusPhysicalSides(
                leadingFromLeft = false,
                trailingFromLeft = true
            ),
            BatteryStatusPhysicalSides.resolve(isRtl = true)
        )
    }

    private fun item(
        component: BatteryStatusComponent,
        width: Float,
        priority: Int,
        required: Boolean = false
    ) = BatteryStatusLayoutItem(component, width, priority, required)
}
