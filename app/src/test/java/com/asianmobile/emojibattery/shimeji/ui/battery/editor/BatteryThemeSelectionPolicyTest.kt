package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryThemeSelectionPolicyTest {
    private val policy = BatteryThemeSelectionPolicy()

    @Test
    fun catalogStyle_initializesPetAndBatteryAsOnePair() {
        val initialized = policy.initializeStyle(
            BatteryStatusConfig(
                selectedThemeId = 1,
                selectedBatteryThemeId = 2,
                selectedEmojiThemeId = 3
            ),
            themeId = 42
        )

        assertEquals(42, initialized.selectedThemeId)
        assertEquals(42, initialized.selectedBatteryThemeId)
        assertEquals(42, initialized.selectedEmojiThemeId)
    }

    @Test
    fun editorSelection_changesOnlyRequestedComponent() {
        val paired = BatteryStatusConfig(
            selectedThemeId = 42,
            selectedBatteryThemeId = 42,
            selectedEmojiThemeId = 42
        )

        val mixed = policy.selectComponent(
            config = paired,
            themeId = 77,
            component = BatteryThemeComponent.EMOJI
        )

        assertEquals(42, mixed.selectedThemeId)
        assertEquals(42, mixed.selectedBatteryThemeId)
        assertEquals(77, mixed.selectedEmojiThemeId)
    }
}
