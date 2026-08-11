package com.asianmobile.emojibattery.shimeji.ui.shared.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.asianmobile.emojibattery.shimeji.R
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val fontSemiBold = FontFamily(Font(R.font.roboto_semibold))

    Column(modifier = modifier) {
        Text(
            text = title,
            color = colorResource(R.color.colors_2F2440),
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = fontSemiBold,
            modifier = Modifier.padding(
                bottom = dimensionResource(SdpR.dimen._6sdp)
            )
        )

        val cardShape = RoundedCornerShape(dimensionResource(SdpR.dimen._18sdp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(colorResource(R.color.colors_FFFFFB))
                .padding(dimensionResource(SdpR.dimen._14sdp)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp)),
            content = content
        )
    }
}
