package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryStatusComponent
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryPreviewLayoutTest {
    @Test
    fun focusedStatusComponent_remainsVisibleInConstrainedPreview() {
        val focusedComponents = listOf(
            BatteryStatusComponent.DATE,
            BatteryStatusComponent.AIRPLANE,
            BatteryStatusComponent.RINGER,
            BatteryStatusComponent.ANIMATION,
            BatteryStatusComponent.CHARGE,
            BatteryStatusComponent.WIFI,
            BatteryStatusComponent.CELLULAR,
            BatteryStatusComponent.HOTSPOT
        )

        focusedComponents.forEach { focusedComponent ->
            val layout = batteryPreviewLayout(
                config = BatteryStatusConfig(
                    showDateTime = true,
                    showAnimation = true
                ),
                availableWidthDp = 120f,
                hasEmoji = true,
                hasEmotion = true,
                hasAnimation = true,
                focusedComponent = focusedComponent
            )

            assertTrue(
                "$focusedComponent should remain visible while it is edited",
                layout.shows(focusedComponent)
            )
        }
    }

    @Test
    fun airplanePreview_hidesMutuallyExclusiveCellularGroup() {
        val layout = batteryPreviewLayout(
            config = BatteryStatusConfig(),
            availableWidthDp = 320f,
            hasEmoji = true,
            hasEmotion = true,
            hasAnimation = true,
            focusedComponent = BatteryStatusComponent.AIRPLANE
        )

        assertTrue(layout.shows(BatteryStatusComponent.AIRPLANE))
        assertFalse(layout.shows(BatteryStatusComponent.CELLULAR))
    }

    @Test
    fun datePreview_respectsVisibilityToggle() {
        val layout = batteryPreviewLayout(
            config = BatteryStatusConfig(showDateTime = false),
            availableWidthDp = 320f,
            hasEmoji = false,
            hasEmotion = false,
            hasAnimation = false,
            focusedComponent = BatteryStatusComponent.DATE
        )

        assertFalse(layout.shows(BatteryStatusComponent.DATE))
    }
}
