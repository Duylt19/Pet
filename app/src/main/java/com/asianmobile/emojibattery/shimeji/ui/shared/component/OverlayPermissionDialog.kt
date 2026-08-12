package com.asianmobile.emojibattery.shimeji.ui.shared.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_PERMISSION
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.AdType
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.NativeAdInternal
import com.asianmobile.emojibattery.shimeji.ui.shared.theme.RobotoFontFamily
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

@Composable
fun OverlayPermissionDialog(
    onAllowAccess: () -> Unit,
    onNotNow: () -> Unit
) {
    PermissionDisclosureBottomSheet(
        onDismissRequest = onNotNow
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            OverlayPermissionDialogContent(
                onAllowAccess = onAllowAccess,
                onNotNow = onNotNow,
                modifier = Modifier.heightIn(max = maxHeight)
            )
        }
    }
}

@Composable
internal fun OverlayPermissionDialogContent(
    onAllowAccess: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
    nativeAdContent: @Composable () -> Unit = { OverlayPermissionNativeAd() }
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    topStart = dimensionResource(SdpR.dimen._18sdp),
                    topEnd = dimensionResource(SdpR.dimen._18sdp)
                )
            )
            .background(colorResource(R.color.colors_FFFFFF)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(SdpR.dimen._28sdp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(
                        width = dimensionResource(SdpR.dimen._25sdp),
                        height = dimensionResource(SdpR.dimen._3sdp)
                    )
                    .clip(CircleShape)
                    .background(colorResource(R.color.colors_C8C8C9))
            )
        }

        Spacer(Modifier.height(dimensionResource(SdpR.dimen._9sdp)))

        Image(
            painter = painterResource(R.drawable.img_overlay_permission_hero),
            contentDescription = null,
            modifier = Modifier.size(
                width = dimensionResource(SdpR.dimen._122sdp),
                height = dimensionResource(SdpR.dimen._77sdp)
            )
        )

        Spacer(Modifier.height(dimensionResource(SdpR.dimen._9sdp)))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._3sdp))
        ) {
            Text(
                text = stringResource(R.string.overlay_permission_disclosure_title),
                color = colorResource(R.color.colors_212327),
                fontFamily = RobotoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = dimensionResource(SspR.dimen._14ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._20ssp).value.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(312f / 336f)
            )
            Text(
                text = stringResource(R.string.overlay_permission_disclosure_body),
                color = colorResource(R.color.colors_6F7073),
                fontFamily = RobotoFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(dimensionResource(SdpR.dimen._9sdp)))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
        ) {
            RewardGradientButton(
                text = stringResource(R.string.overlay_permission_disclosure_allow),
                onClick = onAllowAccess,
                modifier = Modifier.fillMaxWidth()
            )
            RewardOutlineButton(
                text = stringResource(R.string.overlay_permission_disclosure_not_now),
                onClick = onNotNow,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(dimensionResource(SdpR.dimen._9sdp)))
        nativeAdContent()
    }
}

@Composable
private fun OverlayPermissionNativeAd() {
    NativeAdInternal(
        screenCode = SCREEN_PERMISSION,
        instanceKey = "overlay_permission_disclosure",
        adTypeOverride = AdType.HEIGHT_222,
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun OverlayPermissionDialogPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_000000).copy(alpha = 0.5f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        OverlayPermissionDialogContent(
            onAllowAccess = {},
            onNotNow = {},
            nativeAdContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(SdpR.dimen._171sdp))
                        .background(colorResource(R.color.colors_E6E6E6))
                )
            }
        )
    }
}
