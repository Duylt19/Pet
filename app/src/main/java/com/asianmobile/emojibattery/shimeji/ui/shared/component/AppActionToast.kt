package com.asianmobile.emojibattery.shimeji.ui.shared.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
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
 * Pet Store and Discover so a confirmation looks the same wherever it comes from.
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = dimensionResource(bottomPaddingRes)),
        contentAlignment = Alignment.BottomCenter
    ) {
        val shape = CircleShape
        val imageFallback = painterResource(R.drawable.img_pink_love_sticker_preview)
        Row(
            modifier = Modifier
                .fillMaxWidth(APP_ACTION_TOAST_WIDTH_FRACTION)
                .shadow(
                    elevation = dimensionResource(SdpR.dimen._9sdp),
                    shape = shape,
                    ambientColor = colorResource(R.color.colors_66666666),
                    spotColor = colorResource(R.color.colors_66666666)
                )
                .clip(shape)
                .background(colorResource(R.color.colors_FFFFFF))
                .padding(
                    horizontal = dimensionResource(SdpR.dimen._9sdp),
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
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._18sdp))
                )
                Spacer(Modifier.width(dimensionResource(SdpR.dimen._6sdp)))
            }
            Text(
                text = text,
                color = colorResource(R.color.colors_212327),
                fontFamily = ToastRobotoRegular,
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            action?.let {
                Spacer(Modifier.width(dimensionResource(SdpR.dimen._15sdp)))
                Text(
                    text = it,
                    color = colorResource(R.color.colors_FB3675),
                    fontFamily = ToastRobotoMedium,
                    fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                    lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
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
