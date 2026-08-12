package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryEditorPreviewVisibilityPolicyTest {
    @Test
    fun `embedded preview is hidden only while the accessibility status bar is active`() {
        assertFalse(
            BatteryEditorPreviewVisibilityPolicy.shouldShow(
                accessibilityEnabled = true,
                statusBarEnabled = true
            )
        )
        assertTrue(
            BatteryEditorPreviewVisibilityPolicy.shouldShow(
                accessibilityEnabled = false,
                statusBarEnabled = true
            )
        )
        assertTrue(
            BatteryEditorPreviewVisibilityPolicy.shouldShow(
                accessibilityEnabled = true,
                statusBarEnabled = false
            )
        )
        assertTrue(
            BatteryEditorPreviewVisibilityPolicy.shouldShow(
                accessibilityEnabled = false,
                statusBarEnabled = false
            )
        )
    }
}
