package com.asianmobile.privatebrower.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.ads.config.DIALOG_EXIT_APP
import com.asianmobile.privatebrower.ads.ui.compose.NativeAdInternal

@Preview
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
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.89f)
                    .clip(RoundedCornerShape(dimensionResource(com.intuit.sdp.R.dimen._18sdp)))
                    .background(colorResource(R.color.colors_333538))
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(com.intuit.sdp.R.dimen._9sdp),
                        vertical = dimensionResource(com.intuit.sdp.R.dimen._18sdp)
                    ),
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(com.intuit.sdp.R.dimen._12sdp)
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_splash_pet),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(dimensionResource(com.intuit.sdp.R.dimen._60sdp))
                )

                Text(
                    text = stringResource(R.string.exit_dialog_title),
                    fontFamily = FontFamily(Font(R.font.inter_semibold)),
                    fontSize = dimensionResource(com.intuit.ssp.R.dimen._14ssp).value.sp,
                    lineHeight = dimensionResource(com.intuit.ssp.R.dimen._20ssp).value.sp,
                    color = colorResource(R.color.colors_FFFFFF),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(com.intuit.sdp.R.dimen._3sdp))
                )

                NativeAdInternal(
                    screenCode = DIALOG_EXIT_APP,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.exit_dialog_tap_to_exit),
                    fontFamily = FontFamily(Font(R.font.inter_medium)),
                    fontSize = dimensionResource(com.intuit.ssp.R.dimen._12ssp).value.sp,
                    lineHeight = dimensionResource(com.intuit.ssp.R.dimen._18ssp).value.sp,
                    color = colorResource(R.color.gray_808080),
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable(onClick = onExit)
                )
            }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(
                            top = dimensionResource(com.intuit.sdp.R.dimen._12sdp),
                            end = dimensionResource(com.intuit.sdp.R.dimen._12sdp)
                        )
                        .size(dimensionResource(com.intuit.sdp.R.dimen._18sdp))
                        .clip(CircleShape)
                        .background(colorResource(R.color.colors_80000000))
                        .clickable(onClick = onDismissRequest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.exit_dialog_close),
                        tint = colorResource(R.color.colors_FFFFFF),
                        modifier = Modifier.size(dimensionResource(com.intuit.sdp.R.dimen._8sdp))
                    )
                }
            }
        }
    }
}
