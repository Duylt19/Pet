package com.asianmobile.emojibattery.shimeji.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
@Preview(widthDp = 360, heightDp = 800)
@Composable
fun GrantPermissionDialogScreenshotTest() {
    AccessibilityDisclosureScreenshotContent(isConsentGranted = false)
}

@PreviewTest
@Preview(widthDp = 360, heightDp = 800)
@Composable
fun GrantPermissionDialogAcceptedScreenshotTest() {
    AccessibilityDisclosureScreenshotContent(isConsentGranted = true)
}

@Composable
private fun AccessibilityDisclosureScreenshotContent(isConsentGranted: Boolean) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(com.asianmobile.emojibattery.shimeji.R.color.colors_80000000))
            .padding(top = dimensionResource(SdpR.dimen._37sdp)),
        contentAlignment = Alignment.BottomCenter
    ) {
        GrantPermissionDialogContent(
            isConsentGranted = isConsentGranted,
            onConsentChanged = {},
            onGrantPermission = {},
            onMaybeLater = {},
            modifier = Modifier.heightIn(max = maxHeight),
            nativeAdContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(SdpR.dimen._171sdp))
                        .background(
                            colorResource(
                                com.asianmobile.emojibattery.shimeji.R.color.colors_E6E6E6
                            )
                        )
                )
            }
        )
    }
}
