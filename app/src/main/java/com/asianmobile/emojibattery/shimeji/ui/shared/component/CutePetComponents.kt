package com.asianmobile.emojibattery.shimeji.ui.shared.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.asianmobile.emojibattery.shimeji.R
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

val CutePetTitleFont: FontFamily
    @Composable get() = FontFamily(Font(R.font.roboto_bold))

@Composable
fun CutePetTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(dimensionResource(SdpR.dimen._52sdp))
            .padding(horizontal = dimensionResource(SdpR.dimen._16sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            CutePetIconAction(
                iconRes = R.drawable.ic_arrow_back,
                contentDescription = title,
                onClick = onBack
            )
            Spacer(Modifier.size(dimensionResource(SdpR.dimen._10sdp)))
        }
        Text(
            text = title,
            color = colorResource(R.color.colors_2F2440),
            fontFamily = CutePetTitleFont,
            fontWeight = FontWeight.Bold,
            fontSize = dimensionResource(SspR.dimen._16ssp).value.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        trailing()
    }
}

@Composable
fun CutePetIconAction(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(dimensionResource(SdpR.dimen._34sdp))
            .clip(CircleShape)
            .background(colorResource(R.color.colors_FFFFFB))
            .border(
                BorderStroke(
                    dimensionResource(SdpR.dimen._1sdp),
                    colorResource(R.color.colors_E9DFEF)
                ),
                CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = colorResource(R.color.colors_2F2440),
            modifier = Modifier.size(dimensionResource(SdpR.dimen._18sdp))
        )
    }
}
