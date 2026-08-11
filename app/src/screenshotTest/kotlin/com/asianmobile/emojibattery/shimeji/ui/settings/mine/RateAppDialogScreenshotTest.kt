package com.asianmobile.emojibattery.shimeji.ui.settings.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.asianmobile.emojibattery.shimeji.R

@PreviewTest
@Preview(widthDp = 360, heightDp = 800)
@Composable
fun RateAppInitialScreenshotTest() {
    RateDialogPreview(state = RateAppUiState(step = RateAppStep.Initial))
}

@PreviewTest
@Preview(widthDp = 360, heightDp = 800)
@Composable
fun RateAppHighRatingScreenshotTest() {
    RateDialogPreview(
        state = RateAppUiState(step = RateAppStep.HighRating, selectedStars = 4)
    )
}

@PreviewTest
@Preview(widthDp = 360, heightDp = 800)
@Composable
fun RateAppLowRatingScreenshotTest() {
    RateDialogPreview(
        state = RateAppUiState(step = RateAppStep.LowRating, selectedStars = 2),
        artworkProgress = 0f
    )
}

@PreviewTest
@Preview(widthDp = 360, heightDp = 800)
@Composable
fun RateAppFeedbackEmptyScreenshotTest() {
    RateDialogPreview(state = RateAppUiState(step = RateAppStep.FeedbackForm))
}

@PreviewTest
@Preview(widthDp = 360, heightDp = 800)
@Composable
fun RateAppFeedbackOtherScreenshotTest() {
    val options = FeedbackOption.defaultOptions().mapIndexed { index, option ->
        option.copy(isSelected = index == FeedbackOption.defaultOptions().lastIndex)
    }
    RateDialogPreview(
        state = RateAppUiState(
            step = RateAppStep.FeedbackForm,
            feedbackOptions = options
        )
    )
}

@PreviewTest
@Preview(widthDp = 360, heightDp = 800)
@Composable
fun RateAppFeedbackTextScreenshotTest() {
    val options = FeedbackOption.defaultOptions().mapIndexed { index, option ->
        option.copy(isSelected = index == FeedbackOption.defaultOptions().lastIndex)
    }
    RateDialogPreview(
        state = RateAppUiState(
            step = RateAppStep.FeedbackForm,
            feedbackOptions = options,
            otherFeedbackText = "Hello"
        )
    )
}

@PreviewTest
@Preview(widthDp = 360, heightDp = 800)
@Composable
fun RateAppThankYouScreenshotTest() {
    RateDialogPreview(state = RateAppUiState(step = RateAppStep.ThankYou))
}

@Composable
private fun RateDialogPreview(
    state: RateAppUiState,
    artworkProgress: Float = 0.5f
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_80000000)),
        contentAlignment = Alignment.Center
    ) {
        RateAppDialogCard(
            state = state,
            onSelectStars = {},
            onDismiss = {},
            onRateOnPlayStore = {},
            onGoToFeedbackForm = {},
            onToggleFeedbackOption = {},
            onUpdateOtherText = {},
            onSendFeedback = {},
            artworkProgress = artworkProgress,
            showStarIntro = false
        )
    }
}
