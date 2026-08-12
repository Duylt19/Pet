package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryBackgroundSelectionPolicyTest {

    @Test
    fun `selecting color clears active theme`() {
        val selected = BatteryBackgroundSelectionPolicy.selectColor(
            config = BatteryStatusConfig(backgroundDecorationId = 7),
            colorArgb = 0xFFFFCFCF.toInt()
        )

        assertEquals(0, selected.backgroundDecorationId)
        assertEquals(0xFFFFCFCF.toInt(), selected.backgroundColorArgb)
        assertEquals(
            0xFFFFCFCF.toInt(),
            BatteryBackgroundSelectionPolicy.activeColor(selected)
        )
    }

    @Test
    fun `selecting theme deactivates color selection`() {
        val selected = BatteryBackgroundSelectionPolicy.selectTheme(
            config = BatteryStatusConfig(
                backgroundDecorationId = 0,
                backgroundColorArgb = 0xFFFFCFCF.toInt()
            ),
            decorationId = 9
        )

        assertEquals(9, selected.backgroundDecorationId)
        assertNull(BatteryBackgroundSelectionPolicy.activeColor(selected))
    }
}
