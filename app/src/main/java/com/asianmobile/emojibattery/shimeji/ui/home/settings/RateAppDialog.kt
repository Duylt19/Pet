package com.asianmobile.emojibattery.shimeji.ui.home.settings

import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ui.component.DismissibleDialogBackdrop
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import com.asianmobile.emojibattery.shimeji.utils.ToastHelper

@Composable
fun RateAppDialog(
    state: RateAppUiState,
    onSelectStars: (Int) -> Unit,
    onDismiss: () -> Unit,
    onRateOnPlayStore: () -> Unit,
    onGoToFeedbackForm: () -> Unit,
    onToggleFeedbackOption: (Int) -> Unit,
    onUpdateOtherText: (String) -> Unit,
    onSendFeedback: () -> Unit,
    onShowThankYou: () -> Unit
) {
    if (!state.isDialogVisible) return

    val fontRegular = FontFamily(Font(R.font.inter_regular))
    val fontMedium = FontFamily(Font(R.font.inter_medium))
    val fontSemiBold = FontFamily(Font(R.font.inter_semibold))

    val step = state.step
    val isRatingStep = step is RateAppStep.Initial
            || step is RateAppStep.HighRating
            || step is RateAppStep.LowRating
    val isFeedbackStep = step is RateAppStep.FeedbackForm
    val isThankYouStep = step is RateAppStep.ThankYou

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val dialogWidth = LocalConfiguration.current.screenWidthDp * 0.9f

        DismissibleDialogBackdrop(onDismissRequest = onDismiss) {
            Box(
                modifier = Modifier
                    .width(dialogWidth.dp)
                    .clip(RoundedCornerShape(dimensionResource(com.intuit.sdp.R.dimen._18sdp)))
                    .background(colorResource(R.color.colors_333538))
            ) {
            // Rating content (Initial / HighRating / LowRating)
            if (isRatingStep) {
                RatingContent(
                    state = state,
                    fontMedium = fontMedium,
                    fontRegular = fontRegular,
                    fontSemiBold = fontSemiBold,
                    onSelectStars = onSelectStars,
                    onRateClick = when (step) {
                        is RateAppStep.HighRating -> onRateOnPlayStore
                        is RateAppStep.LowRating -> onGoToFeedbackForm
                        else -> {{}}
                    },
                    onDismiss = onDismiss
                )
            }

            // Feedback form content
            if (isFeedbackStep) {
                FeedbackFormContent(
                    state = state,
                    fontMedium = fontMedium,
                    fontRegular = fontRegular,
                    fontSemiBold = fontSemiBold,
                    onToggleOption = onToggleFeedbackOption,
                    onUpdateOtherText = onUpdateOtherText,
                    onSendClick = onSendFeedback,
                    onDismiss = onDismiss
                )
            }

            // Thank you content
                if (isThankYouStep) {
                    ThankYouContent(
                        fontMedium = fontMedium,
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

/**
 * Single layout for Initial / HighRating / LowRating.
 * Only the Lottie icon and title/subtitle texts change based on step.
 * Stars, hint, button, "maybe later" stay in place - no recomposition flicker.
 */
@Composable
private fun RatingContent(
    state: RateAppUiState,
    fontMedium: FontFamily,
    fontRegular: FontFamily,
    fontSemiBold: FontFamily,
    onSelectStars: (Int) -> Unit,
    onRateClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val step = state.step

    // Derive lottie + texts from step
    @RawRes val lottieRes = when (step) {
        is RateAppStep.HighRating -> R.raw.anim_star_awesome
        is RateAppStep.LowRating -> R.raw.anim_star_bad
        else -> R.raw.anim_star_good
    }
    val titleRes = when (step) {
        is RateAppStep.HighRating -> R.string.rate_thank_love
        is RateAppStep.LowRating -> R.string.rate_not_enjoying
        else -> R.string.rate_do_you_like
    }
    val subtitleRes = when (step) {
        is RateAppStep.HighRating -> R.string.rate_made_our_day
        is RateAppStep.LowRating -> R.string.rate_share_wrong
        else -> R.string.rate_feedback_helps
    }
    val buttonEnabled = state.selectedStars > 0

    // Pre-load all 3 compositions so switching is instant
    val goodComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.anim_star_good))
    val awesomeComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.anim_star_awesome))
    val badComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.anim_star_bad))

    val activeComposition = when (step) {
        is RateAppStep.HighRating -> awesomeComposition
        is RateAppStep.LowRating -> badComposition
        else -> goodComposition
    }
    val activeProgress by animateLottieCompositionAsState(
        activeComposition,
        iterations = LottieConstants.IterateForever
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(com.intuit.sdp.R.dimen._9sdp),
                vertical = dimensionResource(com.intuit.sdp.R.dimen._15sdp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Lottie icon
        LottieAnimation(
            composition = activeComposition,
            progress = { activeProgress },
            modifier = Modifier.size(dimensionResource(com.intuit.sdp.R.dimen._55sdp))
        )

        Spacer(modifier = Modifier.height(dimensionResource(com.intuit.sdp.R.dimen._12sdp)))

        // Title
        Text(
            text = stringResource(titleRes),
            fontFamily = fontMedium,
            fontSize = dimensionResource(com.intuit.ssp.R.dimen._14ssp).value.sp,
            color = colorResource(R.color.colors_FFFFFF),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(dimensionResource(com.intuit.sdp.R.dimen._6sdp)))

        // Subtitle
        Text(
            text = stringResource(subtitleRes),
            fontFamily = fontRegular,
            fontSize = dimensionResource(com.intuit.ssp.R.dimen._12ssp).value.sp,
            color = colorResource(R.color.colors_9B9C9E),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(dimensionResource(com.intuit.sdp.R.dimen._12sdp)))

        // Stars with intro animation overlay
        StarRatingWithIntro(
            selectedStars = state.selectedStars,
            onSelectStars = onSelectStars
        )

        Spacer(modifier = Modifier.height(dimensionResource(com.intuit.sdp.R.dimen._9sdp)))

        // Hint
        RatingHintText(fontRegular)

        Spacer(modifier = Modifier.height(dimensionResource(com.intuit.sdp.R.dimen._12sdp)))

        // Button
        RateButton(
            text = stringResource(R.string.rate_us),
            fontSemiBold = fontSemiBold,
            enabled = buttonEnabled,
            disabledMessage = stringResource(R.string.rate_select_star),
            modifier = Modifier.width(dimensionResource(com.intuit.sdp.R.dimen._208sdp)),
            onClick = onRateClick
        )

        Spacer(modifier = Modifier.height(dimensionResource(com.intuit.sdp.R.dimen._7sdp)))

        // Maybe later
        MaybeLaterText(fontRegular = fontRegular, onDismiss = onDismiss)
    }
}

@Composable
private fun FeedbackFormContent(
    state: RateAppUiState,
    fontMedium: FontFamily,
    fontRegular: FontFamily,
    fontSemiBold: FontFamily,
    onToggleOption: (Int) -> Unit,
    onUpdateOtherText: (String) -> Unit,
    onSendClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val isOthersSelected = state.feedbackOptions.lastOrNull()?.isSelected == true
    val sendEnabled = state.canSendFeedback()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = dimensionResource(com.intuit.sdp.R.dimen._12sdp),
                vertical = dimensionResource(com.intuit.sdp.R.dimen._15sdp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.rate_tell_us),
            fontFamily = fontMedium,
            fontSize = dimensionResource(com.intuit.ssp.R.dimen._14ssp).value.sp,
            color = colorResource(R.color.colors_FFFFFF),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(dimensionResource(com.intuit.sdp.R.dimen._12sdp)))

        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(com.intuit.sdp.R.dimen._9sdp))
        ) {
            state.feedbackOptions.forEachIndexed { index, option ->
                FeedbackOptionRow(
                    option = option,
                    fontRegular = fontRegular,
                    onClick = { onToggleOption(index) }
                )
            }
        }

        if (isOthersSelected) {
            Spacer(modifier = Modifier.height(dimensionResource(com.intuit.sdp.R.dimen._9sdp)))

            BasicTextField(
                value = state.otherFeedbackText,
                onValueChange = onUpdateOtherText,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(com.intuit.sdp.R.dimen._92sdp))
                    .clip(RoundedCornerShape(dimensionResource(com.intuit.sdp.R.dimen._9sdp)))
                    .background(colorResource(R.color.colors_424447))
                    .padding(
                        horizontal = dimensionResource(com.intuit.sdp.R.dimen._12sdp),
                        vertical = dimensionResource(com.intuit.sdp.R.dimen._8sdp)
                    ),
                textStyle = TextStyle(
                    fontFamily = fontRegular,
                    fontSize = dimensionResource(com.intuit.ssp.R.dimen._11ssp).value.sp,
                    color = colorResource(R.color.colors_FFFFFF)
                ),
                cursorBrush = SolidColor(colorResource(R.color.colors_3369FD)),
                decorationBox = { innerTextField ->
                    Box {
                        if (state.otherFeedbackText.isEmpty()) {
                            Text(
                                text = stringResource(R.string.rate_feedback_hint),
                                fontFamily = fontRegular,
                                fontSize = dimensionResource(com.intuit.ssp.R.dimen._11ssp).value.sp,
                                color = colorResource(R.color.colors_6F7073)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(dimensionResource(com.intuit.sdp.R.dimen._12sdp)))

        RateButton(
            text = stringResource(R.string.rate_send),
            fontSemiBold = fontSemiBold,
            enabled = sendEnabled,
            disabledMessage = stringResource(
                if (isOthersSelected && state.otherFeedbackText.isBlank()) {
                    R.string.rate_enter_other_feedback
                } else {
                    R.string.rate_select_feedback
                }
            ),
            modifier = Modifier.width(dimensionResource(com.intuit.sdp.R.dimen._154sdp)),
            onClick = onSendClick
        )

        Spacer(modifier = Modifier.height(dimensionResource(com.intuit.sdp.R.dimen._7sdp)))

        MaybeLaterText(fontRegular = fontRegular, onDismiss = onDismiss)
    }
}

@Composable
private fun ThankYouContent(
    fontMedium: FontFamily,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(2000)
        onDismiss()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(com.intuit.sdp.R.dimen._9sdp),
                vertical = dimensionResource(com.intuit.sdp.R.dimen._15sdp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RateLottieIcon(rawRes = R.raw.anim_star_awesome)

        Spacer(modifier = Modifier.height(dimensionResource(com.intuit.sdp.R.dimen._12sdp)))

        Text(
            text = stringResource(R.string.rate_thanks_feedback),
            fontFamily = fontMedium,
            fontSize = dimensionResource(com.intuit.ssp.R.dimen._14ssp).value.sp,
            color = colorResource(R.color.colors_FFFFFF),
            textAlign = TextAlign.Center
        )
    }
}

// region Shared Components

@Composable
private fun RateLottieIcon(@RawRes rawRes: Int) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(rawRes))
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier.size(dimensionResource(com.intuit.sdp.R.dimen._55sdp))
    )
}

@Composable
private fun StarRatingWithIntro(
    selectedStars: Int,
    onSelectStars: (Int) -> Unit
) {
    var introFinished by remember { mutableStateOf(false) }

    val introComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.anim_star_rate))
    val introProgress by animateLottieCompositionAsState(
        introComposition,
        iterations = 1,
        isPlaying = !introFinished
    )

    // When intro animation completes, hide it
    LaunchedEffect(introProgress) {
        if (introProgress >= 1f) {
            introFinished = true
        }
    }

    // Match real star row dimensions: 5 stars * _22sdp + 4 gaps * _5sdp
    val starSize = dimensionResource(com.intuit.sdp.R.dimen._22sdp)
    val gapSize = dimensionResource(com.intuit.sdp.R.dimen._5sdp)
    val totalWidth = starSize * 5 + gapSize * 4

    Box(
        modifier = Modifier
            .width(totalWidth)
            .height(starSize),
        contentAlignment = Alignment.Center
    ) {
        // Real stars always present underneath, clickable anytime
        StarRatingRow(
            modifier = Modifier.alpha(if (introFinished) 1f else 0f),
            selectedStars = selectedStars,
            onSelectStars = { star ->
                introFinished = true
                onSelectStars(star)
            }
        )

        // Lottie overlay
        if (!introFinished) {
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.Center
            ) {
                LottieAnimation(
                    composition = introComposition,
                    progress = { introProgress },
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.width(totalWidth)
                )
            }
        }
    }
}

@Composable
private fun StarRatingRow(
    selectedStars: Int,
    modifier: Modifier = Modifier,
    onSelectStars: (Int) -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(com.intuit.sdp.R.dimen._5sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..5) {
            Icon(
                painter = painterResource(
                    if (i <= selectedStars) R.drawable.ic_rate_star_filled
                    else R.drawable.ic_rate_star_empty
                ),
                contentDescription = "Star $i",
                modifier = Modifier
                    .size(dimensionResource(com.intuit.sdp.R.dimen._22sdp))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onSelectStars(i) },
                tint = Color.Unspecified
            )
        }
    }
}

@Composable
private fun RatingHintText(fontRegular: FontFamily) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(com.intuit.sdp.R.dimen._2sdp))
    ) {
        Text(
            text = stringResource(R.string.rate_best_rating),
            fontFamily = fontRegular,
            fontSize = dimensionResource(com.intuit.ssp.R.dimen._9ssp).value.sp,
            color = colorResource(R.color.colors_3369FD)
        )
        Icon(
            painter = painterResource(R.drawable.ic_hint_rate1),
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(com.intuit.sdp.R.dimen._9sdp)),
            tint = Color.Unspecified
        )
        Icon(
            painter = painterResource(R.drawable.ic_hint_rate2),
            contentDescription = null,
            modifier = Modifier.size(
                width = dimensionResource(com.intuit.sdp.R.dimen._12sdp),
                height = dimensionResource(com.intuit.sdp.R.dimen._11sdp)
            ),
            tint = Color.Unspecified
        )
    }
}

@Composable
private fun RateButton(
    text: String,
    fontSemiBold: FontFamily,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    disabledMessage: String = ""
) {
    val context = LocalContext.current
    val btnWidth = if (modifier == Modifier) Modifier.fillMaxWidth() else Modifier
    
    Box(
        modifier = modifier
            .then(btnWidth)
            .height(dimensionResource(com.intuit.sdp.R.dimen._37sdp))
            .clip(RoundedCornerShape(percent = 50))
            .alpha(if (enabled) 1f else 0.5f)
            .then(
                Modifier.background(
                    Brush.horizontalGradient(
                        colors = if (enabled) {
                            listOf(
                                colorResource(R.color.colors_1D86F6),
                                colorResource(R.color.colors_0D45ED)
                            )
                        } else {
                            listOf(
                                colorResource(R.color.colors_A8D2FF),
                                colorResource(R.color.colors_6C90FB)
                            )
                        }
                    )
                )
            )
            .clickable {
                if (enabled) {
                    onClick()
                } else if (disabledMessage.isNotEmpty()) {
                    ToastHelper.show(context, disabledMessage)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = fontSemiBold,
            fontSize = dimensionResource(com.intuit.ssp.R.dimen._14ssp).value.sp,
            color = colorResource(
                if (enabled) R.color.colors_FFFFFF else R.color.colors_C0D1FE
            )
        )
    }
}

@Composable
private fun MaybeLaterText(
    fontRegular: FontFamily,
    onDismiss: () -> Unit
) {
    Text(
        text = stringResource(R.string.maybe_later),
        fontFamily = fontRegular,
        fontSize = dimensionResource(com.intuit.ssp.R.dimen._11ssp).value.sp,
        color = colorResource(R.color.colors_9B9C9E),
        textDecoration = TextDecoration.Underline,
        modifier = Modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = onDismiss
        )
    )
}

@Composable
private fun FeedbackOptionRow(
    option: FeedbackOption,
    fontRegular: FontFamily,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(
                if (option.isSelected) R.drawable.ic_rate_checkbox_checked
                else R.drawable.ic_rate_checkbox_unchecked
            ),
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(com.intuit.sdp.R.dimen._13sdp)),
            tint = Color.Unspecified
        )

        Spacer(modifier = Modifier.width(dimensionResource(com.intuit.sdp.R.dimen._9sdp)))

        Text(
            text = "${option.emoji} ${stringResource(option.textResId)}",
            fontFamily = fontRegular,
            fontSize = dimensionResource(com.intuit.ssp.R.dimen._11ssp).value.sp,
            color = colorResource(R.color.colors_FFFFFF)
        )
    }
}

// endregion
