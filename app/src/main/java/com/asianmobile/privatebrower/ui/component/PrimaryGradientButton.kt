package com.asianmobile.privatebrower.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.asianmobile.privatebrower.R
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

@Composable
fun PrimaryGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fontSemiBold = FontFamily(Font(R.font.inter_semibold))
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._42sdp))
            .clip(shape)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        colorResource(R.color.colorPrimary),
                        colorResource(R.color.colors_9F7AFF)
                    )
                ),
                shape = shape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = colorResource(R.color.colors_FFFFFF),
            fontSize = dimensionResource(SspR.dimen._14ssp).value.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = fontSemiBold
        )
    }
}
