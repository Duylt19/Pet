package com.asianmobile.emojibattery.shimeji.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.asianmobile.emojibattery.shimeji.R
import com.intuit.sdp.R as SdpR

@PreviewTest
@Preview(widthDp = 360, heightDp = 800)
@Composable
fun ExitDialogScreenshotTest() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_80000000)),
        contentAlignment = Alignment.Center
    ) {
        ExitDialogCard(
            onExit = {},
            adContent = {
                ExitDialogAdLoadingPreview()
            }
        )
    }
}

@Composable
private fun ExitDialogAdLoadingPreview() {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._8sdp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._171sdp))
            .clip(shape)
            .background(colorResource(R.color.colors_FFFFFF))
            .border(
                width = dimensionResource(SdpR.dimen._1sdp),
                color = colorResource(R.color.colors_E6E6E6),
                shape = shape
            )
            .padding(dimensionResource(SdpR.dimen._12sdp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._3sdp)))
                .background(colorResource(R.color.colors_E6E6E6))
        )
    }
}
