package com.asianmobile.emojibattery.shimeji.ui.home.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.asianmobile.emojibattery.shimeji.R
import com.intuit.sdp.R as SdpR

@PreviewTest
@Preview(widthDp = 360, heightDp = 72)
@Composable
fun DiscoverAssetHeadersScreenshotTest() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_FFFFFF)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
    ) {
        SectionHeader(
            title = stringResource(R.string.discover_emoji_title),
            titleIcon = R.drawable.img_statusbar_template_emoji,
            onMore = {}
        )
        SectionHeader(
            title = stringResource(R.string.discover_battery_title),
            titleIcon = R.drawable.ic_statusbar_template_battery,
            onMore = {}
        )
    }
}
