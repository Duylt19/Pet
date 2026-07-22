package com.asianmobile.privatebrower.ui.home.browsertab

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.asianmobile.privatebrower.R
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

/**
 * Popular Sites section for Home tab - Figma node 11280:2260.
 * Title "Popular Sites" + 4-column grid of app icons with labels.
 * Grid: 2 rows x 4 columns, justify-between, gap 20px between rows, 4px icon-text gap.
 * Icons: 56px (to _43sdp), text: 12px Inter Medium, white, centered.
 */
@Composable
fun PopularSitesSection(
    sites: List<PopularSite>,
    onSiteClick: (PopularSite) -> Unit,
    modifier: Modifier = Modifier
) {
    val fontSemiBold = FontFamily(Font(R.font.inter_semibold))
    val itemWidth = dimensionResource(SdpR.dimen._43sdp)
    val rows = sites.chunked(4)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.home_popular_sites_title),
            fontFamily = fontSemiBold,
            fontWeight = FontWeight.SemiBold,
            fontSize = dimensionResource(SspR.dimen._14ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._20ssp).value.sp,
            color = colorResource(R.color.colors_FFFFFF),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._9sdp)))

        rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                row.forEach { site ->
                    PopularSiteItem(
                        site = site,
                        onClick = { onSiteClick(site) },
                        modifier = Modifier.width(itemWidth)
                    )
                }
                repeat(4 - row.size) {
                    Spacer(modifier = Modifier.width(itemWidth))
                }
            }
            if (rowIndex < rows.lastIndex) {
                Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._15sdp)))
            }
        }
    }
}

/**
 * Single popular site item - Figma node 11838:7889.
 * Column: 56px icon (round) + 4px gap + 12px text centered
 */
@Composable
private fun PopularSiteItem(
    site: PopularSite,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fontMedium = FontFamily(Font(R.font.inter_medium))
    val label = stringResource(site.labelRes)

    Column(
        modifier = modifier
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App logo
        Image(
            painter = painterResource(site.iconRes),
            contentDescription = label,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._43sdp))
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp)))
        )

        Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._3sdp)))

        // Label
        Text(
            text = label,
            fontFamily = fontMedium,
            fontWeight = FontWeight.Medium,
            fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._12ssp).value.sp,
            color = colorResource(R.color.colors_FFFFFF),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Visible,
            modifier = Modifier.requiredWidth(dimensionResource(SdpR.dimen._46sdp))
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF141414)
@Composable
private fun PopularSitesSectionPreview() {
    PopularSitesSection(
        sites = PopularSites.DEFAULTS,
        onSiteClick = {}
    )
}
