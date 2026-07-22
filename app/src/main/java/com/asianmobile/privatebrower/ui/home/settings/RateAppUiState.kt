package com.asianmobile.privatebrower.ui.home.settings

import androidx.annotation.StringRes
import com.asianmobile.privatebrower.R

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
            FeedbackOption("🛡️", R.string.rate_feedback_private_mode),
            FeedbackOption("🔒", R.string.rate_feedback_vault_access),
            FeedbackOption("🌐", R.string.rate_feedback_websites_wont_load),
            FeedbackOption("🐢", R.string.rate_feedback_slow_browser),
            FeedbackOption("⬇️", R.string.rate_feedback_downloads_failed),
            FeedbackOption("📁", R.string.rate_feedback_files_missing),
            FeedbackOption("💥", R.string.rate_feedback_crashed),
            FeedbackOption("✏️", R.string.rate_feedback_others)
        )
    }
}
