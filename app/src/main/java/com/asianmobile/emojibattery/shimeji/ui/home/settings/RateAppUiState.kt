package com.asianmobile.emojibattery.shimeji.ui.home.settings

import androidx.annotation.StringRes
import com.asianmobile.emojibattery.shimeji.R

sealed class RateAppStep {
    data object Initial : RateAppStep()
    data object HighRating : RateAppStep()
    data object LowRating : RateAppStep()
    data object FeedbackForm : RateAppStep()
    data object ThankYou : RateAppStep()
}

data class RateAppUiState(
    val isDialogVisible: Boolean = false,
    val step: RateAppStep = RateAppStep.Initial,
    val selectedStars: Int = 0,
    val feedbackOptions: List<FeedbackOption> = FeedbackOption.defaultOptions(),
    val otherFeedbackText: String = "",
    val isSendingFeedback: Boolean = false
)

internal fun RateAppUiState.canSendFeedback(): Boolean {
    val hasSelectedOption = feedbackOptions.any(FeedbackOption::isSelected)
    val isOtherSelected = feedbackOptions.lastOrNull()?.isSelected == true
    val hasValidOtherFeedback = !isOtherSelected || otherFeedbackText.isNotBlank()

    return hasSelectedOption && hasValidOtherFeedback && !isSendingFeedback
}

data class FeedbackOption(
    val emoji: String,
    @param:StringRes val textResId: Int,
    val isSelected: Boolean = false
) {
    companion object {
        fun defaultOptions() = listOf(
            FeedbackOption("🔋", R.string.rate_feedback_battery_not_showing),
            FeedbackOption("🐾", R.string.rate_feedback_pet_not_showing),
            FeedbackOption("🎨", R.string.rate_feedback_pet_appearance),
            FeedbackOption("⚡", R.string.rate_feedback_disappears),
            FeedbackOption("👆", R.string.rate_feedback_swipe),
            FeedbackOption("🔐", R.string.rate_feedback_permissions),
            FeedbackOption("🔋", R.string.rate_feedback_battery_drain),
            FeedbackOption("❗️", R.string.rate_feedback_crashed),
            FeedbackOption("👀", R.string.rate_feedback_others)
        )
    }
}
