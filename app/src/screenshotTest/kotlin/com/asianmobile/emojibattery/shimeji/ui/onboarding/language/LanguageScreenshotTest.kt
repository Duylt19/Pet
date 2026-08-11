package com.asianmobile.emojibattery.shimeji.ui.onboarding.language

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.asianmobile.emojibattery.shimeji.R
import com.intuit.sdp.R as R_sdp

@PreviewTest
@Preview(widthDp = 360, heightDp = 800)
@Composable
fun LanguageOnboardingSelectedScreenshotTest() {
    val languages = LocalContext.current.mockData()
    LanguageContent(
        languages = languages,
        selectedKey = languages.first().key,
        showConfirm = true,
        isSettings = false,
        isLoading = false,
        isSupportBlur = false,
        onLanguageSelected = {},
        onConfirm = {},
        onBack = {},
        adContent = { LanguageAdPlaceholder() },
        loadingContent = {},
    )
}

@PreviewTest
@Preview(widthDp = 360, heightDp = 800)
@Composable
fun LanguageSettingsUnselectedScreenshotTest() {
    LanguageContent(
        languages = LocalContext.current.mockData(),
        selectedKey = "",
        showConfirm = false,
        isSettings = true,
        isLoading = false,
        isSupportBlur = false,
        onLanguageSelected = {},
        onConfirm = {},
        onBack = {},
        adContent = { LanguageAdPlaceholder() },
        loadingContent = {},
    )
}

@Composable
private fun LanguageAdPlaceholder() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(R_sdp.dimen._171sdp))
            .background(colorResource(R.color.colors_E5E5E5)),
    )
}
