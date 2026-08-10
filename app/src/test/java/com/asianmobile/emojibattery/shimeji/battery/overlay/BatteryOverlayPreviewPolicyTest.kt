package com.asianmobile.emojibattery.shimeji.battery.overlay

import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryOverlayPreviewPolicyTest {
    @Test
    fun disabledStoredConfig_ignoresEnabledEditorPreview() {
        val source = resolveBatteryOverlayPreviewSource(
            storedConfig = BatteryStatusConfig(enabled = false, selectedBatteryThemeId = 1),
            preview = BatteryEditorPreview(
                ownerId = "editor",
                config = BatteryStatusConfig(enabled = true, selectedBatteryThemeId = 42),
                focusedComponent = BatteryStatusComponent.WIFI
            )
        )

        assertFalse(source.config.enabled)
        assertEquals(1, source.config.selectedBatteryThemeId)
        assertNull(source.focusedComponent)
    }

    @Test
    fun enabledStoredConfig_usesEditorVisualsAndFocus() {
        val source = resolveBatteryOverlayPreviewSource(
            storedConfig = BatteryStatusConfig(enabled = true, selectedBatteryThemeId = 1),
            preview = BatteryEditorPreview(
                ownerId = "editor",
                config = BatteryStatusConfig(enabled = false, selectedBatteryThemeId = 42),
                focusedComponent = BatteryStatusComponent.WIFI
            )
        )

        assertTrue(source.config.enabled)
        assertEquals(42, source.config.selectedBatteryThemeId)
        assertEquals(BatteryStatusComponent.WIFI, source.focusedComponent)
    }

    @Test
    fun enabledStoredConfig_withoutPreview_usesStoredVisuals() {
        val stored = BatteryStatusConfig(enabled = true, selectedEmojiThemeId = 7)

        val source = resolveBatteryOverlayPreviewSource(stored, preview = null)

        assertEquals(stored, source.config)
        assertNull(source.focusedComponent)
    }
}
