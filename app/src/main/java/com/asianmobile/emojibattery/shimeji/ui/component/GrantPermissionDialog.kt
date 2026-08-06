package com.asianmobile.emojibattery.shimeji.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.asianmobile.emojibattery.shimeji.R
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

private val PermissionDialogRobotoRegular = FontFamily.SansSerif
private val PermissionDialogRobotoMedium = FontFamily(Font(R.font.roboto_medium))
private val PermissionDialogRobotoSemiBold = FontFamily(Font(R.font.roboto_600))

@Composable
fun GrantPermissionDialog(
    onGrantPermission: () -> Unit,
    onMaybeLater: () -> Unit
) {
    Dialog(
        onDismissRequest = onMaybeLater,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
            contentAlignment = Alignment.Center
        ) {
            GrantPermissionDialogCard(
                onGrantPermission = onGrantPermission,
                onMaybeLater = onMaybeLater
            )
        }
    }
}

@Composable
internal fun GrantPermissionDialogCard(
    onGrantPermission: () -> Unit,
    onMaybeLater: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._18sdp)))
            .background(colorResource(R.color.colors_FFFFFF))
            .padding(
                horizontal = dimensionResource(SdpR.dimen._12sdp),
                vertical = dimensionResource(SdpR.dimen._18sdp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
    ) {
        Image(
            painter = painterResource(R.drawable.ic_permission_disclosure),
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(SdpR.dimen._108sdp))
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._3sdp))
        ) {
            Text(
                text = stringResource(R.string.permission_dialog_title),
                color = colorResource(R.color.colors_212327),
                fontFamily = PermissionDialogRobotoSemiBold,
                fontSize = dimensionResource(SspR.dimen._15ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._22ssp).value.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.permission_dialog_description),
                color = colorResource(R.color.colors_6F7073),
                fontFamily = PermissionDialogRobotoRegular,
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(
                    horizontal = dimensionResource(SdpR.dimen._12sdp)
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(SdpR.dimen._37sdp))
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            colorResource(R.color.colors_C95DFF),
                            colorResource(R.color.colors_FB54BB)
                        )
                    )
                )
                .clickable(role = Role.Button, onClick = onGrantPermission),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.permission_dialog_grant),
                color = colorResource(R.color.colors_FFFFFF),
                fontFamily = PermissionDialogRobotoMedium,
                fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp,
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = stringResource(R.string.maybe_later),
            color = colorResource(R.color.colors_6F7073),
            fontFamily = PermissionDialogRobotoRegular,
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
            textAlign = TextAlign.Center,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onMaybeLater)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF9B9C9E, widthDp = 360, heightDp = 440)
@Composable
private fun GrantPermissionDialogPreview() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
        contentAlignment = Alignment.Center
    ) {
        GrantPermissionDialogCard(
            onGrantPermission = {},
            onMaybeLater = {}
        )
    }
}
