package com.asianmobile.emojibattery.shimeji.ui.shared.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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

val CutePetBodyFont: FontFamily
    @Composable get() = FontFamily(Font(R.font.roboto_regular))

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

@Composable
fun CutePetCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(dimensionResource(SdpR.dimen._14sdp)),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._20sdp)))
            .background(colorResource(R.color.colors_FFFFFB))
            .border(
                BorderStroke(
                    dimensionResource(SdpR.dimen._1sdp),
                    colorResource(R.color.colors_E9DFEF)
                ),
                RoundedCornerShape(dimensionResource(SdpR.dimen._20sdp))
            )
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun CutePetSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = colorResource(R.color.colors_2F2440),
            fontFamily = CutePetTitleFont,
            fontWeight = FontWeight.Bold,
            fontSize = dimensionResource(SspR.dimen._14ssp).value.sp
        )
        if (action != null && onAction != null) {
            Text(
                text = action,
                color = colorResource(R.color.colors_7B61FF),
                fontFamily = FontFamily(Font(R.font.roboto_semibold)),
                fontSize = dimensionResource(SspR.dimen._10ssp).value.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._8sdp)))
                    .clickable(onClick = onAction)
                    .padding(
                        horizontal = dimensionResource(SdpR.dimen._6sdp),
                        vertical = dimensionResource(SdpR.dimen._4sdp)
                    )
            )
        }
    }
}

@Composable
fun CutePetPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    isDanger: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(dimensionResource(SdpR.dimen._46sdp)),
        shape = RoundedCornerShape(dimensionResource(SdpR.dimen._15sdp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(
                if (isDanger) R.color.colors_E45D6A else R.color.colors_7B61FF
            ),
            contentColor = colorResource(R.color.colors_FFFFFF),
            disabledContainerColor = colorResource(R.color.colors_E9DFEF),
            disabledContentColor = colorResource(R.color.colors_776D84)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(dimensionResource(SdpR.dimen._16sdp)),
                strokeWidth = dimensionResource(SdpR.dimen._2sdp),
                color = colorResource(R.color.colors_FFFFFF)
            )
        } else {
            Text(
                text = text,
                fontFamily = FontFamily(Font(R.font.roboto_semibold)),
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp
            )
        }
    }
}

@Composable
fun CutePetStatusPill(
    text: String,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp)))
            .background(
                colorResource(
                    if (active) R.color.colors_E7F7F1 else R.color.colors_FFF0D6
                )
            )
            .padding(
                horizontal = dimensionResource(SdpR.dimen._10sdp),
                vertical = dimensionResource(SdpR.dimen._6sdp)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._6sdp))
                .clip(CircleShape)
                .background(
                    colorResource(
                        if (active) R.color.colors_39B87A else R.color.colors_FFB84D
                    )
                )
        )
        Spacer(Modifier.size(dimensionResource(SdpR.dimen._6sdp)))
        Text(
            text = text,
            color = colorResource(R.color.colors_2F2440),
            fontFamily = FontFamily(Font(R.font.roboto_medium)),
            fontSize = dimensionResource(SspR.dimen._9ssp).value.sp
        )
    }
}
