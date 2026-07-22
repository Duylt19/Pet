package com.asianmobile.privatebrower.ui.home.browsertab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.asianmobile.privatebrower.R
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

/**
 * Private Browsing info card — Figma node 11010:75
 * Dark card with incognito icon, title and description.
 * Specs: bg #333333, rounded 15px (~12sdp), padding h10 v17, gap 8px
 */
@Composable
fun PrivateBrowsingCard(
    modifier: Modifier = Modifier
) {
    val fontMedium = FontFamily(Font(R.font.inter_medium))
    val fontRegular = FontFamily(Font(R.font.inter_regular))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp)))
            .background(colorResource(R.color.colors_333333))
            .padding(
                horizontal = dimensionResource(SdpR.dimen._8sdp),
                vertical = dimensionResource(SdpR.dimen._13sdp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Incognito icon
        Icon(
            painter = painterResource(R.drawable.ic_private_browsing),
            contentDescription = null,
            tint = colorResource(R.color.colors_FFFFFF),
            modifier = Modifier.size(dimensionResource(SdpR.dimen._32sdp))
        )

        Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._6sdp)))

        // Title
        Text(
            text = stringResource(R.string.home_private_browsing_title),
            fontFamily = fontMedium,
            fontWeight = FontWeight.Medium,
            fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp,
            color = colorResource(R.color.colors_FFFFFF),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._2sdp)))

        // Description
        Text(
            text = stringResource(R.string.home_private_browsing_desc),
            fontFamily = fontRegular,
            fontWeight = FontWeight.Normal,
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
            color = colorResource(R.color.colors_FFFFFF),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF141414)
@Composable
private fun PrivateBrowsingCardPreview() {
    PrivateBrowsingCard()
}
