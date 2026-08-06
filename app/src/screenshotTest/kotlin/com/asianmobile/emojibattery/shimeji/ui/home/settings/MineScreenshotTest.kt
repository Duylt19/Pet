package com.asianmobile.emojibattery.shimeji.ui.home.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.asianmobile.emojibattery.shimeji.R

@PreviewTest
@Preview(widthDp = 360, heightDp = 950)
@Composable
fun MineScreenshotTest() {
    MineContent(
        state = SettingsUiState(versionName = "100"),
        languageName = "English",
        languageFlagRes = R.drawable.ic_flag_en,
        onSearch = {},
        onPremium = {},
        onBatteryToggle = {},
        onMyPet = {},
        onFavouriteRecent = {},
        onLanguage = {},
        onAppsHidden = {},
        onGrantPermission = {},
        onRate = {},
        onShare = {},
        onContact = {},
        onPrivacy = {}
    )
}
