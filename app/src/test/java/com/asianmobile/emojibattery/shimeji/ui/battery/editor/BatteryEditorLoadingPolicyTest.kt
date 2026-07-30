package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryEditorLoadingPolicyTest {
    @Test
    fun selectionLoading_blocksApplyWithoutChangingItsActiveAppearance() {
        assertEquals(
            BatteryApplyUiState(
                enabled = false,
                keepActiveAppearance = true
            ),
            BatteryEditorLoadingPolicy.applyState(
                themeAvailable = true,
                selectionInProgress = true
            )
        )
    }

    @Test
    fun availableSelection_enablesApplyNormally() {
        assertEquals(
            BatteryApplyUiState(
                enabled = true,
                keepActiveAppearance = false
            ),
            BatteryEditorLoadingPolicy.applyState(
                themeAvailable = true,
                selectionInProgress = false
            )
        )
    }

    @Test
    fun unavailableSelection_remainsVisuallyDisabled() {
        assertEquals(
            BatteryApplyUiState(
                enabled = false,
                keepActiveAppearance = false
            ),
            BatteryEditorLoadingPolicy.applyState(
                themeAvailable = false,
                selectionInProgress = true
            )
        )
    }
}
