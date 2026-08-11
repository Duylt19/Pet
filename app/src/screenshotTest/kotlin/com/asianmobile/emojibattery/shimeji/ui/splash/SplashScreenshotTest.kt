package com.asianmobile.emojibattery.shimeji.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.asianmobile.emojibattery.shimeji.R
import com.intuit.sdp.R as R_sdp

@PreviewTest
@Preview(widthDp = 360, heightDp = 800)
@Composable
fun SplashScreenshotTest() {
    SplashContent(
        banner = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R_sdp.dimen._50sdp))
                    .background(colorResource(R.color.colors_FFFFFF)),
            )
        },
    )
}
