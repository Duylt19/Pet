package com.asianmobile.emojibattery.shimeji.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ads.config.DIALOG_EXIT_APP
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.NativeAdInternal
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

private const val ExitDialogWidthFraction = 320f / 360f
private val ExitDialogRobotoMedium = FontFamily(Font(R.font.roboto_medium))

@Composable
fun ExitDialog(
    onDismissRequest: () -> Unit = {},
    onExit: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DismissibleDialogBackdrop(onDismissRequest = onDismissRequest) {
            ExitDialogCard(
                onExit = onExit,
                adContent = {
                    NativeAdInternal(
                        screenCode = DIALOG_EXIT_APP,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        }
    }
}

@Composable
internal fun ExitDialogCard(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    adContent: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth(ExitDialogWidthFraction)
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._18sdp)))
            .background(colorResource(R.color.colors_FFFFFF))
            .padding(
                horizontal = dimensionResource(SdpR.dimen._12sdp),
                vertical = dimensionResource(SdpR.dimen._15sdp)
            ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.ic_splash_pet),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._54sdp))
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp)))
        )

        Text(
            text = stringResource(R.string.exit_dialog_title),
            fontFamily = ExitDialogRobotoMedium,
            fontSize = dimensionResource(SspR.dimen._14ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._20ssp).value.sp,
            color = colorResource(R.color.colors_212327),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        adContent()

        Text(
            text = stringResource(R.string.exit_dialog_tap_to_exit),
            fontFamily = ExitDialogRobotoMedium,
            fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp,
            color = colorResource(R.color.colors_6F7073),
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable(onClick = onExit)
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun ExitDialogPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_80000000)),
        contentAlignment = Alignment.Center
    ) {
        ExitDialogCard(
            onExit = {},
            adContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(SdpR.dimen._171sdp))
                        .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._8sdp)))
                        .background(colorResource(R.color.colors_E6E6E6))
                )
            }
        )
    }
}
