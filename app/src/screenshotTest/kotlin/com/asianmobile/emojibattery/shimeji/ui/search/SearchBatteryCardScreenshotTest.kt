package com.asianmobile.emojibattery.shimeji.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.asianmobile.emojibattery.shimeji.R
import com.intuit.sdp.R as SdpR

@PreviewTest
@Preview(widthDp = 250, heightDp = 120)
@Composable
fun SearchBatteryCardScreenshotTest() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_FFFFFF)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
    ) {
        SearchThemeCard(
            theme = SearchThemeUiState(
                id = 1,
                name = "Premium",
                category = "Cute",
                thumbnailPath = null,
                isFavorite = false,
                isLocked = true
            ),
            onOpen = {},
            onFavorite = {},
            modifier = Modifier.weight(1f)
        )
        SearchThemeCard(
            theme = SearchThemeUiState(
                id = 2,
                name = "Free",
                category = "Cute",
                thumbnailPath = null,
                isFavorite = true,
                isLocked = false
            ),
            onOpen = {},
            onFavorite = {},
            modifier = Modifier.weight(1f)
        )
    }
}
