package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryEditorApplyStateTest {
    @Test
    fun `apply is disabled while a selection or apply completion is in progress`() {
        val ready = BatteryEditorUiState(
            isThemeAvailable = true,
            isCatalogLoading = false
        )

        assertTrue(ready.isApplyEnabled)
        assertFalse(ready.copy(isApplyInProgress = true).isApplyEnabled)
        assertFalse(
            ready.copy(
                assetSelectionInProgress = BatteryEditorThemeSelection(
                    themeId = 2,
                    component = BatteryThemeComponent.BATTERY
                )
            ).isApplyEnabled
        )
        assertFalse(ready.copy(backgroundSelectionInProgress = 2).isApplyEnabled)
        assertFalse(ready.copy(emotionSelectionInProgress = 2).isApplyEnabled)
    }
}
