package com.asianmobile.emojibattery.shimeji.battery.overlay

import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Test

class BatteryEditorPreviewSessionTest {
    @Test
    fun preview_preservesActivationState_andTracksFocusedComponent() {
        val session = BatteryEditorPreviewSession()
        session.start("editor", BatteryStatusConfig(enabled = false))
        session.update(
            ownerId = "editor",
            config = BatteryStatusConfig(enabled = false, selectedEmojiThemeId = 42),
            focusedComponent = BatteryStatusComponent.WIFI
        )

        val preview = session.preview.value
        assertFalse(requireNotNull(preview).config.enabled)
        assertEquals(42, preview.config.selectedEmojiThemeId)
        assertEquals(BatteryStatusComponent.WIFI, preview.focusedComponent)
    }

    @Test
    fun staleOwner_cannotUpdateOrStopActivePreview() {
        val session = BatteryEditorPreviewSession()
        session.start("active", BatteryStatusConfig(selectedBatteryThemeId = 8))

        session.update(
            ownerId = "stale",
            config = BatteryStatusConfig(selectedBatteryThemeId = 99),
            focusedComponent = null
        )
        session.stop("stale")

        assertEquals(8, session.preview.value?.config?.selectedBatteryThemeId)
        session.stop("active")
        assertNull(session.preview.value)
    }
}
