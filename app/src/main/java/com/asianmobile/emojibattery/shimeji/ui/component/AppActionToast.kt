package com.asianmobile.emojibattery.shimeji.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.asianmobile.emojibattery.shimeji.R
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR
import kotlinx.coroutines.delay

private val ToastRobotoMedium = FontFamily(Font(R.font.roboto_medium))
private val ToastRobotoSemiBold = FontFamily(Font(R.font.roboto_600))

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
    bottomPaddingRes: Int = SdpR.dimen._91sdp
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
        Row(
            modifier = Modifier
                .padding(horizontal = dimensionResource(SdpR.dimen._12sdp))
                .shadow(dimensionResource(SdpR.dimen._6sdp), CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .padding(
                    horizontal = dimensionResource(SdpR.dimen._12sdp),
                    vertical = dimensionResource(SdpR.dimen._8sdp)
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._8sdp))
        ) {
            PinkLoveSticker(Modifier.size(dimensionResource(SdpR.dimen._20sdp)))
            Text(
                text = text,
                color = colorResource(R.color.colors_212327),
                fontFamily = ToastRobotoMedium,
                fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            action?.let {
                Text(
                    text = it,
                    color = colorResource(R.color.colors_FB3675),
                    fontFamily = ToastRobotoSemiBold,
                    fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
                    modifier = Modifier.clickable(onClick = onAction)
                )
            }
        }
    }
}

private const val TOAST_DURATION_MILLIS = 3_000L

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
