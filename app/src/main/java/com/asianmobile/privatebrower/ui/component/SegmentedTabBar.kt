package com.asianmobile.privatebrower.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.asianmobile.privatebrower.R
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

/**
 * Shared pill-style segmented tab bar used below [AppHeaderBar] (Downloads, Bookmarks/History).
 * Keeps position/spacing/colors identical across every screen that uses it.
 */
@Composable
fun SegmentedTabBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(SdpR.dimen._12sdp),
                vertical = dimensionResource(SdpR.dimen._6sdp)
            )
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp)))
            .background(colorResource(R.color.colors_212327))
            .padding(dimensionResource(SdpR.dimen._3sdp)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._3sdp)),
        content = content
    )
}

@Composable
fun RowScope.SegmentedTabItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    fontMedium: FontFamily,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .weight(1f)
            .height(dimensionResource(SdpR.dimen._31sdp))
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._7sdp)))
            .background(
                if (selected) colorResource(R.color.colors_424447) else Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) {
                colorResource(R.color.colors_FFFFFF)
            } else {
                colorResource(R.color.colors_9B9C9E)
            },
            fontFamily = fontMedium,
            fontWeight = FontWeight.Medium,
            fontSize = with(LocalDensity.current) {
                dimensionResource(SspR.dimen._11ssp).toSp()
            }
        )
    }
}
