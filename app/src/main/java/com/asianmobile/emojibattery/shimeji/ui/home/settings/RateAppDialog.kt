package com.asianmobile.emojibattery.shimeji.ui.home.settings

import androidx.annotation.RawRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ui.component.DismissibleDialogBackdrop
import com.asianmobile.emojibattery.shimeji.utils.ToastHelper
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR
import kotlinx.coroutines.delay

private val RateRobotoRegular = FontFamily.SansSerif
private val RateRobotoMedium = FontFamily(Font(R.font.roboto_medium))
private val RateRobotoSemiBold = FontFamily(Font(R.font.roboto_600))
private const val RateDialogWidthFraction = 312f / 360f
private const val RateThankYouDialogWidthFraction = 320f / 360f

@Suppress("UNUSED_PARAMETER")
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DismissibleDialogBackdrop(onDismissRequest = onDismiss) {
            RateAppDialogCard(
                state = state,
                onSelectStars = onSelectStars,
                onDismiss = onDismiss,
                onRateOnPlayStore = onRateOnPlayStore,
                onGoToFeedbackForm = onGoToFeedbackForm,
                onToggleFeedbackOption = onToggleFeedbackOption,
                onUpdateOtherText = onUpdateOtherText,
                onSendFeedback = onSendFeedback
            )
        }
    }
}

@Composable
internal fun RateAppDialogCard(
    state: RateAppUiState,
    onSelectStars: (Int) -> Unit,
    onDismiss: () -> Unit,
    onRateOnPlayStore: () -> Unit,
    onGoToFeedbackForm: () -> Unit,
    onToggleFeedbackOption: (Int) -> Unit,
    onUpdateOtherText: (String) -> Unit,
    onSendFeedback: () -> Unit,
    modifier: Modifier = Modifier,
    artworkProgress: Float? = null,
    showStarIntro: Boolean = true
) {
    val cardWidthFraction = if (state.step is RateAppStep.ThankYou) {
        RateThankYouDialogWidthFraction
    } else {
        RateDialogWidthFraction
    }

    Box(
        modifier = modifier
            .fillMaxWidth(cardWidthFraction)
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._18sdp)))
            .background(colorResource(R.color.colors_FFFFFF))
    ) {
        when (val step = state.step) {
            RateAppStep.Initial,
            RateAppStep.HighRating,
            RateAppStep.LowRating -> RatingContent(
                state = state,
                onSelectStars = onSelectStars,
                onRateClick = when (step) {
                    RateAppStep.HighRating -> onRateOnPlayStore
                    RateAppStep.LowRating -> onGoToFeedbackForm
                    else -> ({})
                },
                onDismiss = onDismiss,
                artworkProgress = artworkProgress,
                showStarIntro = showStarIntro
            )

            RateAppStep.FeedbackForm -> FeedbackFormContent(
                state = state,
                onToggleOption = onToggleFeedbackOption,
                onUpdateOtherText = onUpdateOtherText,
                onSendClick = onSendFeedback,
                onDismiss = onDismiss
            )

            RateAppStep.ThankYou -> ThankYouContent(onDismiss = onDismiss)
        }
    }
}

@Composable
private fun RatingContent(
    state: RateAppUiState,
    onSelectStars: (Int) -> Unit,
    onRateClick: () -> Unit,
    onDismiss: () -> Unit,
    artworkProgress: Float?,
    showStarIntro: Boolean
) {
    val titleRes = when (state.step) {
        RateAppStep.HighRating -> R.string.rate_thank_love
        RateAppStep.LowRating -> R.string.rate_not_enjoying
        else -> R.string.rate_do_you_like
    }
    val subtitleRes = when (state.step) {
        RateAppStep.HighRating -> R.string.rate_made_our_day
        RateAppStep.LowRating -> R.string.rate_share_wrong
        else -> R.string.rate_feedback_helps
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(SdpR.dimen._9sdp),
                vertical = dimensionResource(SdpR.dimen._15sdp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
    ) {
        RatingArtwork(step = state.step, progressOverride = artworkProgress)

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._3sdp))
        ) {
            Text(
                text = stringResource(titleRes),
                fontFamily = RateRobotoMedium,
                fontSize = dimensionResource(SspR.dimen._14ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._20ssp).value.sp,
                color = colorResource(R.color.colors_212327),
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(subtitleRes),
                fontFamily = RateRobotoRegular,
                fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp,
                color = colorResource(R.color.colors_6F7073),
                textAlign = TextAlign.Center
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
        ) {
            StarRatingWithIntro(
                selectedStars = state.selectedStars,
                onSelectStars = onSelectStars,
                showIntro = showStarIntro
            )
            RatingHintText()
        }

        RateButton(
            text = stringResource(R.string.rate_us),
            enabled = state.selectedStars > 0,
            disabledMessage = stringResource(R.string.rate_select_star),
            modifier = Modifier.width(dimensionResource(SdpR.dimen._208sdp)),
            onClick = onRateClick
        )

        MaybeLaterText(onDismiss = onDismiss)
    }
}

@Composable
private fun RatingArtwork(
    step: RateAppStep,
    progressOverride: Float?
) {
    @RawRes val rawRes = when (step) {
        RateAppStep.HighRating -> R.raw.anim_star_awesome
        RateAppStep.LowRating -> R.raw.anim_star_bad
        else -> R.raw.anim_star_good
    }
    val context = LocalContext.current
    val composition = remember(rawRes) {
        LottieCompositionFactory.fromRawResSync(context, rawRes).value
    }
    val animatedProgress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = progressOverride == null
    )

    LottieAnimation(
        composition = composition,
        progress = { progressOverride ?: animatedProgress },
        modifier = Modifier.size(
            width = dimensionResource(SdpR.dimen._69sdp),
            height = dimensionResource(SdpR.dimen._66sdp)
        ),
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun FeedbackFormContent(
    state: RateAppUiState,
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
                horizontal = dimensionResource(SdpR.dimen._9sdp),
                vertical = dimensionResource(SdpR.dimen._15sdp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
    ) {
        Text(
            text = stringResource(R.string.rate_tell_us),
            fontFamily = RateRobotoMedium,
            fontSize = dimensionResource(SspR.dimen._14ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._20ssp).value.sp,
            color = colorResource(R.color.colors_212327),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
        ) {
            state.feedbackOptions.forEachIndexed { index, option ->
                FeedbackOptionRow(
                    option = option,
                    onClick = { onToggleOption(index) }
                )
            }
        }

        if (isOthersSelected) {
            FeedbackTextField(
                value = state.otherFeedbackText,
                onValueChange = onUpdateOtherText
            )
        }

        RateButton(
            text = stringResource(R.string.rate_send),
            enabled = sendEnabled,
            disabledMessage = stringResource(
                if (isOthersSelected && state.otherFeedbackText.isBlank()) {
                    R.string.rate_enter_other_feedback
                } else {
                    R.string.rate_select_feedback
                }
            ),
            modifier = Modifier.width(dimensionResource(SdpR.dimen._154sdp)),
            onClick = onSendClick
        )

        MaybeLaterText(onDismiss = onDismiss)
    }
}

@Composable
private fun FeedbackTextField(
    value: String,
    onValueChange: (String) -> Unit
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._92sdp))
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp)))
            .background(colorResource(R.color.colors_F2F2F2))
            .padding(
                horizontal = dimensionResource(SdpR.dimen._12sdp),
                vertical = dimensionResource(SdpR.dimen._8sdp)
            ),
        textStyle = TextStyle(
            fontFamily = RateRobotoRegular,
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
            color = colorResource(R.color.colors_212327)
        ),
        cursorBrush = SolidColor(colorResource(R.color.colors_FB3675)),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = stringResource(R.string.rate_feedback_hint),
                        fontFamily = RateRobotoRegular,
                        fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                        lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
                        color = colorResource(R.color.colors_9B9C9E)
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun ThankYouContent(
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(2000)
        onDismiss()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(SdpR.dimen._12sdp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
    ) {
        Image(
            painter = painterResource(R.drawable.img_rate_feedback_thanks),
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(SdpR.dimen._74sdp)),
            contentScale = ContentScale.Fit
        )
        Text(
            text = stringResource(R.string.rate_thanks_feedback),
            fontFamily = RateRobotoMedium,
            fontSize = dimensionResource(SspR.dimen._14ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._20ssp).value.sp,
            color = colorResource(R.color.colors_212327),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StarRatingWithIntro(
    selectedStars: Int,
    onSelectStars: (Int) -> Unit,
    showIntro: Boolean
) {
    var introFinished by remember(showIntro) { mutableStateOf(!showIntro) }
    val introComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.anim_star_rate))
    val introProgress by animateLottieCompositionAsState(
        composition = introComposition,
        iterations = 1,
        isPlaying = showIntro && !introFinished
    )

    LaunchedEffect(introProgress, showIntro) {
        if (!showIntro || introProgress >= 1f) introFinished = true
    }

    val starSize = dimensionResource(SdpR.dimen._29sdp)
    val gapSize = dimensionResource(SdpR.dimen._6sdp)
    val totalWidth = starSize * 5 + gapSize * 4

    Box(
        modifier = Modifier
            .width(totalWidth)
            .height(dimensionResource(SdpR.dimen._42sdp)),
        contentAlignment = Alignment.Center
    ) {
        if (introFinished) {
            StarRatingRow(
                selectedStars = selectedStars,
                onSelectStars = onSelectStars
            )
        } else {
            StarRatingRow(
                selectedStars = selectedStars,
                modifier = Modifier.alpha(0f),
                onSelectStars = { star ->
                    introFinished = true
                    onSelectStars(star)
                }
            )
            LottieAnimation(
                composition = introComposition,
                progress = { introProgress },
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.width(totalWidth)
            )
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
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (star in 1..5) {
            Icon(
                painter = painterResource(
                    if (star <= selectedStars) R.drawable.ic_rate_star_filled
                    else R.drawable.ic_rate_star_empty
                ),
                contentDescription = stringResource(R.string.rate_star_content_description, star),
                modifier = Modifier
                    .size(dimensionResource(SdpR.dimen._29sdp))
                    .clickable(
                        role = Role.Button,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onSelectStars(star) },
                tint = Color.Unspecified
            )
        }
    }
}

@Composable
private fun RatingHintText() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._3sdp))
    ) {
        Text(
            text = stringResource(R.string.rate_best_rating),
            fontFamily = RateRobotoRegular,
            fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._12ssp).value.sp,
            color = colorResource(R.color.colors_FB3675)
        )
        Icon(
            painter = painterResource(R.drawable.ic_hint_rate1),
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(SdpR.dimen._12sdp)),
            tint = Color.Unspecified
        )
        Icon(
            painter = painterResource(R.drawable.ic_hint_rate2),
            contentDescription = null,
            modifier = Modifier.size(
                width = dimensionResource(SdpR.dimen._15sdp),
                height = dimensionResource(SdpR.dimen._14sdp)
            ),
            tint = Color.Unspecified
        )
    }
}

@Composable
private fun RateButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    disabledMessage: String = ""
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .height(dimensionResource(SdpR.dimen._37sdp))
            .clip(CircleShape)
            .background(
                Brush.horizontalGradient(
                    colors = if (enabled) {
                        listOf(
                            colorResource(R.color.colors_C95DFF),
                            colorResource(R.color.colors_FB54BB)
                        )
                    } else {
                        listOf(
                            colorResource(R.color.colors_E1ABFB),
                            colorResource(R.color.colors_FF90D4)
                        )
                    }
                )
            )
            .clickable(role = Role.Button) {
                if (enabled) onClick()
                else if (disabledMessage.isNotEmpty()) ToastHelper.show(context, disabledMessage)
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = RateRobotoSemiBold,
            fontSize = dimensionResource(SspR.dimen._14ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._20ssp).value.sp,
            color = colorResource(
                if (enabled) R.color.colors_FFFFFF else R.color.colors_FFEBF1
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MaybeLaterText(onDismiss: () -> Unit) {
    Text(
        text = stringResource(R.string.maybe_later),
        fontFamily = RateRobotoRegular,
        fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
        lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
        color = colorResource(R.color.colors_6F7073),
        textAlign = TextAlign.Center,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss
            )
    )
}

@Composable
private fun FeedbackOptionRow(
    option: FeedbackOption,
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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
    ) {
        Icon(
            painter = painterResource(
                if (option.isSelected) R.drawable.ic_rate_checkbox_checked
                else R.drawable.ic_rate_checkbox_unchecked
            ),
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(SdpR.dimen._15sdp)),
            tint = Color.Unspecified
        )
        Text(
            text = "${option.emoji} ${stringResource(option.textResId)}",
            fontFamily = RateRobotoRegular,
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
            color = colorResource(R.color.colors_212327),
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF808080, widthDp = 360, heightDp = 640)
@Composable
private fun RateAppHighRatingPreview() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        RateAppDialogCard(
            state = RateAppUiState(step = RateAppStep.HighRating, selectedStars = 4),
            onSelectStars = {},
            onDismiss = {},
            onRateOnPlayStore = {},
            onGoToFeedbackForm = {},
            onToggleFeedbackOption = {},
            onUpdateOtherText = {},
            onSendFeedback = {},
            artworkProgress = 0.5f,
            showStarIntro = false
        )
    }
}
