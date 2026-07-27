package com.asianmobile.emojibattery.shimeji.ui.home.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RateAppUiStateTest {

    @Test
    fun `send is disabled when no feedback option is selected`() {
        assertFalse(RateAppUiState().canSendFeedback())
    }

    @Test
    fun `send is enabled when a feedback option is selected`() {
        val state = RateAppUiState(
            feedbackOptions = FeedbackOption.defaultOptions().selectOption(index = 0)
        )

        assertTrue(state.canSendFeedback())
    }

    @Test
    fun `send is disabled for others with empty additional text`() {
        val options = FeedbackOption.defaultOptions()
        val state = RateAppUiState(
            feedbackOptions = options.selectOption(index = options.lastIndex),
            otherFeedbackText = ""
        )

        assertFalse(state.canSendFeedback())
    }

    @Test
    fun `send is disabled for others with blank additional text`() {
        val options = FeedbackOption.defaultOptions()
        val state = RateAppUiState(
            feedbackOptions = options.selectOption(index = options.lastIndex),
            otherFeedbackText = "   "
        )

        assertFalse(state.canSendFeedback())
    }

    @Test
    fun `send is enabled for others with additional text`() {
        val options = FeedbackOption.defaultOptions()
        val state = RateAppUiState(
            feedbackOptions = options.selectOption(index = options.lastIndex),
            otherFeedbackText = "The page does not respond"
        )

        assertTrue(state.canSendFeedback())
    }

    @Test
    fun `send is disabled while feedback is being submitted`() {
        val state = RateAppUiState(
            feedbackOptions = FeedbackOption.defaultOptions().selectOption(index = 0),
            isSendingFeedback = true
        )

        assertFalse(state.canSendFeedback())
    }

    private fun List<FeedbackOption>.selectOption(index: Int): List<FeedbackOption> =
        mapIndexed { optionIndex, option ->
            option.copy(isSelected = optionIndex == index)
        }
}
