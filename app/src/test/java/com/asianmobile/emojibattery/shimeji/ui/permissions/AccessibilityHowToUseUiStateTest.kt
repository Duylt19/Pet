package com.asianmobile.emojibattery.shimeji.ui.permissions

import org.junit.Assert.assertEquals
import org.junit.Test

class AccessibilityHowToUseUiStateTest {
    @Test
    fun `steps follow Android accessibility settings flow`() {
        assertEquals(
            listOf(
                AccessibilityHowToStep.OPEN_INSTALLED_APPS,
                AccessibilityHowToStep.SELECT_EMOJI_BATTERY,
                AccessibilityHowToStep.ENABLE_SERVICE,
                AccessibilityHowToStep.CONFIRM_ACCESS
            ),
            AccessibilityHowToUseUiState().steps
        )
    }
}
