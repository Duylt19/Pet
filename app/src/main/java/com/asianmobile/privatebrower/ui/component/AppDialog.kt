package com.asianmobile.privatebrower.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.asianmobile.privatebrower.R

/**
 * A generalized, reusable App Dialog container — White theme.
 * Matches Figma design: white bg, rounded 20px, no gradient border.
 */
@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    title: String,
    iconPainter: Painter? = null,
    showCloseButton: Boolean = true,
    centerTitle: Boolean = false,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        DismissibleDialogBackdrop(
            onDismissRequest = onDismissRequest,
            surfaceModifier = Modifier.padding(
                horizontal = dimensionResource(com.intuit.sdp.R.dimen._18sdp)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = with(androidx.compose.ui.platform.LocalConfiguration.current) {
                        (screenHeightDp * 0.9f).dp
                    })
                    .verticalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(dimensionResource(com.intuit.sdp.R.dimen._15sdp)))
                    .background(colorResource(R.color.white))
                    .padding(dimensionResource(com.intuit.sdp.R.dimen._15sdp))
            ) {
                // Header Row: Icon + Title + Close Button
            if (centerTitle && !showCloseButton && iconPainter == null) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = colorResource(R.color.colors_0D0D0D),
                        fontSize = dimensionResource(com.intuit.ssp.R.dimen._14ssp).value.sp,
                        fontFamily = FontFamily(Font(R.font.inter_medium)),
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left side: Icon + Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (iconPainter != null) {
                            Icon(
                                painter = iconPainter,
                                contentDescription = null,
                                tint = colorResource(R.color.colors_0D0D0D),
                                modifier = Modifier.size(dimensionResource(com.intuit.sdp.R.dimen._18sdp))
                            )
                            Spacer(modifier = Modifier.width(dimensionResource(com.intuit.sdp.R.dimen._6sdp)))
                        }
                        Text(
                            text = title,
                            color = colorResource(R.color.colors_0D0D0D),
                            fontSize = dimensionResource(com.intuit.ssp.R.dimen._14ssp).value.sp,
                            fontFamily = FontFamily(Font(R.font.inter_medium)),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Right side: Close Button
                    if (showCloseButton) {
                        Box(
                            modifier = Modifier
                                .size(dimensionResource(com.intuit.sdp.R.dimen._24sdp))
                                .clickable(onClick = onDismissRequest),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = "Close",
                                tint = colorResource(R.color.colors_0D0D0D),
                                modifier = Modifier.size(dimensionResource(com.intuit.sdp.R.dimen._14sdp))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimensionResource(com.intuit.sdp.R.dimen._12sdp)))

            // Content Body
            content()
        }
    }
    }
}

