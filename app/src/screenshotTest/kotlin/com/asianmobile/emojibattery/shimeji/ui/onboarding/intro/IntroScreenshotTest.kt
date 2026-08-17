package com.asianmobile.emojibattery.shimeji.ui.onboarding.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
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
fun IntroPageOneScreenshotTest() {
    IntroPageContent(
        pageIndex = 0,
        currentPage = 0,
        showNativeAd = false,
        onActionClick = {},
        adContent = {},
    )
}

@PreviewTest
@Preview(widthDp = 360, heightDp = 800)
@Composable
fun IntroPageTwoScreenshotTest() {
    IntroPageContent(
        pageIndex = 1,
        currentPage = 1,
        showNativeAd = false,
        onActionClick = {},
        adContent = {},
    )
}

@PreviewTest
@Preview(widthDp = 360, heightDp = 800)
@Composable
fun IntroPageThreeScreenshotTest() {
    IntroPageContent(
        pageIndex = 2,
        currentPage = 2,
        showNativeAd = true,
        onActionClick = {},
        adContent = { IntroAdPlaceholder() },
    )
}

@PreviewTest
@Preview(widthDp = 360, heightDp = 720)
@Composable
fun IntroPageTwoCompactScreenshotTest() {
    IntroPageContent(
        pageIndex = 1,
        currentPage = 1,
        showNativeAd = false,
        onActionClick = {},
        adContent = {},
    )
}

@PreviewTest
@Preview(widthDp = 360, heightDp = 720)
@Composable
fun IntroPageThreeCompactScreenshotTest() {
    IntroPageContent(
        pageIndex = 2,
        currentPage = 2,
        showNativeAd = true,
        onActionClick = {},
        adContent = { IntroAdPlaceholder() },
    )
}

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, locale = "vi")
@Composable
fun IntroPageTwoVietnameseScreenshotTest() {
    IntroPageContent(
        pageIndex = 1,
        currentPage = 1,
        showNativeAd = false,
        onActionClick = {},
        adContent = {},
    )
}

@PreviewTest
@Preview(widthDp = 360, heightDp = 800, locale = "pt")
@Composable
fun IntroPageThreePortugueseScreenshotTest() {
    IntroPageContent(
        pageIndex = 2,
        currentPage = 2,
        showNativeAd = true,
        onActionClick = {},
        adContent = { IntroAdPlaceholder() },
    )
}

@Composable
private fun IntroAdPlaceholder() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(R_sdp.dimen._171sdp))
            .background(colorResource(R.color.colors_E5E5E5)),
    )
}
