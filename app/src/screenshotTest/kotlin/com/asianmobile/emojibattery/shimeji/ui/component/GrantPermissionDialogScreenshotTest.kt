package com.asianmobile.emojibattery.shimeji.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.intuit.sdp.R as SdpR

@PreviewTest
@Preview(widthDp = 360, heightDp = 440)
@Composable
fun GrantPermissionDialogScreenshotTest() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(com.asianmobile.emojibattery.shimeji.R.color.colors_80000000))
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
        contentAlignment = Alignment.Center
    ) {
        GrantPermissionDialogCard(
            onGrantPermission = {},
            onMaybeLater = {}
        )
    }
}
