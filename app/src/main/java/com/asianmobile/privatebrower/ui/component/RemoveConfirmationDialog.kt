package com.asianmobile.privatebrower.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.asianmobile.privatebrower.R
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

@Composable
fun RemoveConfirmationDialog(
    @StringRes title: Int,
    @StringRes description: Int,
    @StringRes confirmText: Int = R.string.remove,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    RemoveConfirmationDialog(
        title = stringResource(title),
        description = stringResource(description),
        confirmText = stringResource(confirmText),
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@Composable
fun RemoveConfirmationDialog(
    title: String,
    description: String,
    confirmText: String = stringResource(R.string.remove),
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DismissibleDialogBackdrop(
            onDismissRequest = onDismiss,
            surfaceModifier = Modifier.padding(
                horizontal = dimensionResource(SdpR.dimen._12sdp)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._18sdp)))
                    .background(colorResource(R.color.colors_333538))
                    .padding(
                        horizontal = dimensionResource(SdpR.dimen._12sdp),
                        vertical = dimensionResource(SdpR.dimen._17sdp)
                    ),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._3sdp))
                ) {
                    Text(
                        text = title,
                        fontFamily = FontFamily(Font(R.font.inter_medium)),
                        fontWeight = FontWeight.Medium,
                        fontSize = with(LocalDensity.current) {
                            dimensionResource(SspR.dimen._12ssp).toSp()
                        },
                        lineHeight = with(LocalDensity.current) {
                            dimensionResource(SspR.dimen._18ssp).toSp()
                        },
                        color = colorResource(R.color.colors_FFFFFF),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = description,
                        fontFamily = FontFamily(Font(R.font.inter_regular)),
                        fontSize = with(LocalDensity.current) {
                            dimensionResource(SspR.dimen._11ssp).toSp()
                        },
                        lineHeight = with(LocalDensity.current) {
                            dimensionResource(SspR.dimen._15ssp).toSp()
                        },
                        color = colorResource(R.color.colors_FFFFFF),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
                ) {
                    RemoveDialogButton(
                        text = stringResource(R.string.cancel),
                        backgroundColor = R.color.colors_424447,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    RemoveDialogButton(
                        text = confirmText,
                        backgroundColor = R.color.colors_DC2222,
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RemoveDialogButton(
    text: String,
    backgroundColor: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(dimensionResource(SdpR.dimen._37sdp))
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp)))
            .background(colorResource(backgroundColor))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = FontFamily(Font(R.font.inter_medium)),
            fontWeight = FontWeight.Medium,
            fontSize = with(LocalDensity.current) {
                dimensionResource(SspR.dimen._12ssp).toSp()
            },
            lineHeight = with(LocalDensity.current) {
                dimensionResource(SspR.dimen._18ssp).toSp()
            },
            color = colorResource(R.color.colors_FFFFFF)
        )
    }
}
