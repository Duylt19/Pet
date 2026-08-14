package com.asianmobile.emojibattery.shimeji.ui.shared.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.asianmobile.emojibattery.shimeji.R
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR
import kotlinx.coroutines.delay

private val ToastRobotoRegular = FontFamily(Font(R.font.roboto_regular))
private val ToastRobotoMedium = FontFamily(Font(R.font.roboto_medium))

/**
 * The white pill the app uses to confirm something happened, with an optional action. Shared by
 * Pet Store and Pet Room so a confirmation looks the same wherever it comes from.
 */
@Composable
fun AppActionToast(
    text: String,
    action: String?,
    onDismiss: () -> Unit,
    onAction: () -> Unit,
    leadingImageModel: Any? = R.drawable.img_pink_love_sticker,
    bottomPaddingRes: Int = SdpR.dimen._115sdp
) {
    LaunchedEffect(text) {
        delay(TOAST_DURATION_MILLIS)
        onDismiss()
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = dimensionResource(bottomPaddingRes)),
        contentAlignment = Alignment.BottomCenter
    ) {
        val shape = CircleShape
        val imageFallback = painterResource(R.drawable.img_pink_love_sticker_preview)
        val horizontalPadding = dimensionResource(SdpR.dimen._9sdp)
        val leadingImageSize = dimensionResource(SdpR.dimen._18sdp)
        val leadingGap = dimensionResource(SdpR.dimen._6sdp)
        val actionGap = dimensionResource(SdpR.dimen._15sdp)
        val messageStyle = TextStyle(
            fontFamily = ToastRobotoRegular,
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp
        )
        val actionStyle = TextStyle(
            fontFamily = ToastRobotoMedium,
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp
        )
        val textMeasurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val actionWidth = action?.let {
            with(density) {
                textMeasurer.measure(
                    text = it,
                    style = actionStyle,
                    maxLines = 1
                ).size.width.toDp()
            }
        } ?: 0.dp
        val toastMaxWidth = maxWidth * APP_ACTION_TOAST_WIDTH_FRACTION
        val leadingWidth = if (leadingImageModel == null) 0.dp else leadingImageSize + leadingGap
        val trailingWidth = if (action == null) 0.dp else actionGap + actionWidth
        val messageMaxWidth = (
            toastMaxWidth - horizontalPadding * 2 - leadingWidth - trailingWidth
            ).coerceAtLeast(0.dp)

        Row(
            modifier = Modifier
                .widthIn(max = toastMaxWidth)
                .shadow(
                    elevation = dimensionResource(SdpR.dimen._9sdp),
                    shape = shape,
                    ambientColor = colorResource(R.color.colors_66666666),
                    spotColor = colorResource(R.color.colors_66666666)
                )
                .clip(shape)
                .background(colorResource(R.color.colors_FFFFFF))
                .padding(
                    horizontal = horizontalPadding,
                    vertical = dimensionResource(SdpR.dimen._6sdp)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingImageModel?.let { model ->
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    placeholder = imageFallback,
                    error = imageFallback,
                    fallback = imageFallback,
                    modifier = Modifier.size(leadingImageSize)
                )
                Spacer(Modifier.width(leadingGap))
            }
            Text(
                text = text,
                color = colorResource(R.color.colors_212327),
                style = messageStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = messageMaxWidth)
            )
            action?.let {
                Spacer(Modifier.width(actionGap))
                Text(
                    text = it,
                    color = colorResource(R.color.colors_FB3675),
                    style = actionStyle,
                    maxLines = 1,
                    modifier = Modifier.clickable(onClick = onAction)
                )
            }
        }
    }
}

private const val TOAST_DURATION_MILLIS = 3_000L
private const val APP_ACTION_TOAST_WIDTH_FRACTION = 305f / 360f

@Preview(showBackground = true, widthDp = 360, heightDp = 200)
@Composable
private fun AppActionToastPreview() {
    AppActionToast(
        text = "Cattey has joined your home!",
        action = "View",
        onDismiss = {},
        onAction = {},
        bottomPaddingRes = SdpR.dimen._12sdp
    )
}
