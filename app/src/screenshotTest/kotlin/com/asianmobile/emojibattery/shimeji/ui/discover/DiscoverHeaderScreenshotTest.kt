package com.asianmobile.emojibattery.shimeji.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.asianmobile.emojibattery.shimeji.R

@PreviewTest
@Preview(widthDp = 360, heightDp = 80)
@Composable
fun DiscoverHeaderScreenshotTest() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_FFFFFF)),
        contentAlignment = Alignment.Center
    ) {
        DiscoverHeader(onSearch = {}, onPremium = {})
    }
}
