package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntitlement
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun componentSelection_requiresItsVerifiedRuntimeAsset() {
        val theme = BatteryThemeEntry(
            id = 77,
            name = "Remote",
            categoryId = 1,
            categoryName = "Trending",
            entitlement = BatteryThemeEntitlement.FREE,
            thumbnailPath = "https://server/thumb/77.png",
            batteryPath = "https://server/battery/77.png",
            emojiPath = "https://server/emoji/77.png",
            assetsReady = true
        )

        assertEquals(
            "https://server/emoji/77.png",
            policy.assetPath(theme, BatteryThemeComponent.EMOJI)
        )
        assertEquals(
            "https://server/battery/77.png",
            policy.assetPath(theme, BatteryThemeComponent.BATTERY)
        )
        assertFalse(policy.isMaterialized(theme, null))
        assertTrue(policy.isMaterialized(theme, "/verified/77.png"))
    }
}
