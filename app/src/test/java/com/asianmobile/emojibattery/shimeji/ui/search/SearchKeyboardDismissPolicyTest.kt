package com.asianmobile.emojibattery.shimeji.ui.search

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchKeyboardDismissPolicyTest {
    private val inputBounds = Rect(left = 20f, top = 10f, right = 320f, bottom = 60f)

    @Test
    fun `tap outside input dismisses keyboard`() {
        assertTrue(
            shouldDismissSearchKeyboard(
                inputBounds = inputBounds,
                tapPosition = Offset(100f, 120f),
                isTap = true
            )
        )
    }

    @Test
    fun `tap inside input keeps keyboard visible`() {
        assertFalse(
            shouldDismissSearchKeyboard(
                inputBounds = inputBounds,
                tapPosition = Offset(100f, 30f),
                isTap = true
            )
        )
    }

    @Test
    fun `drag outside input does not count as tap`() {
        assertFalse(
            shouldDismissSearchKeyboard(
                inputBounds = inputBounds,
                tapPosition = Offset(100f, 120f),
                isTap = false
            )
        )
    }
}
