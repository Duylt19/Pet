package com.asianmobile.emojibattery.shimeji.ui.home.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.asianmobile.emojibattery.shimeji.R

@PreviewTest
@Preview(widthDp = 360, heightDp = 260)
@Composable
fun HomePreviewItemsScreenshotTest() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_FFEBF1))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrendingPetCard(
                pet = DiscoverPetUiState(
                    packKey = "preview-pet",
                    name = "Pink Bunny",
                    category = "Cute",
                    thumbnailPath = null
                ),
                onClick = {}
            )
            BatteryThemeCard(
                theme = DiscoverThemeUiState(
                    id = 1,
                    name = "Pink Battery",
                    thumbnailPath = null,
                    isFavorite = true
                ),
                onOpen = {},
                onFavorite = {},
                modifier = Modifier.width(100.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ComponentAssetCard(
                asset = null,
                fallbackRes = R.drawable.img_home_brand_bunny,
                onClick = {}
            )
            ComponentAssetCard(
                asset = null,
                fallbackRes = R.drawable.ic_home_battery,
                onClick = {}
            )
        }
    }
}
