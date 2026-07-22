package com.asianmobile.privatebrower.ui.permission

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.ads.config.SCREEN_PERMISSION
import com.asianmobile.privatebrower.ads.ui.compose.NativeAdInternal
import com.asianmobile.privatebrower.ui.component.TransparentStatusBarEffect
import com.asianmobile.privatebrower.utils.ScreenName
import com.asianmobile.privatebrower.utils.TrackScreenView
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

@Composable
fun PermissionScreen(
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    viewModel: PermissionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PermissionScreenContent(
        uiState = uiState,
        onContinue = onContinue,
        onSkip = onSkip
    )
}

@Composable
private fun PermissionScreenContent(
    uiState: PermissionUiState,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    TransparentStatusBarEffect(useDarkIcons = false)
    TrackScreenView(ScreenName.PERMISSION)
    BackHandler { }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_161718))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(SdpR.dimen._18sdp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))
            Image(
                painter = painterResource(R.drawable.img_permission_dark),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(
                    width = dimensionResource(SdpR.dimen._143sdp),
                    height = dimensionResource(SdpR.dimen._98sdp)
                )
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
            Text(
                text = stringResource(R.string.permission_title),
                color = colorResource(R.color.white),
                fontFamily = FontFamily(Font(R.font.inter_semibold)),
                fontSize = dimensionResource(SspR.dimen._14ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._20ssp).value.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._3sdp)))
            Text(
                text = stringResource(R.string.permission_subtitle),
                color = colorResource(R.color.colors_9B9C9E),
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.weight(1f))
            PermissionContinueActions(
                enabled = uiState.actionsEnabled,
                onContinue = onContinue,
                onSkip = onSkip
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
        }

        NativeAdInternal(
            screenCode = SCREEN_PERMISSION,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PermissionContinueActions(
    enabled: Boolean,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .height(dimensionResource(SdpR.dimen._24sdp))
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._3sdp)))
                .clickable(enabled = enabled, onClick = onContinue)
                .padding(horizontal = dimensionResource(SdpR.dimen._6sdp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.permission_continue),
                color = colorResource(R.color.colors_3369FD),
                fontFamily = FontFamily(Font(R.font.inter_medium)),
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp
            )
            Spacer(Modifier.width(dimensionResource(SdpR.dimen._3sdp)))
            Box(
                modifier = Modifier.size(dimensionResource(SdpR.dimen._18sdp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_continue),
                    contentDescription = null,
                    tint = colorResource(R.color.colors_3369FD),
                    modifier = Modifier.size(
                        width = dimensionResource(SdpR.dimen._12sdp),
                        height = dimensionResource(SdpR.dimen._11sdp)
                    )
                )
            }
        }
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._2sdp)))
        Text(
            text = stringResource(R.string.grant_permission_later),
            color = colorResource(R.color.colors_9B9C9E),
            fontFamily = FontFamily(Font(R.font.inter_regular)),
            fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._12ssp).value.sp,
            modifier = Modifier.clickable(enabled = enabled, onClick = onSkip)
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF161718,
    widthDp = 360,
    heightDp = 800
)
@Composable
private fun PermissionScreenPreview() {
    PermissionScreenContent(
        uiState = PermissionUiState(),
        onContinue = {},
        onSkip = {}
    )
}
