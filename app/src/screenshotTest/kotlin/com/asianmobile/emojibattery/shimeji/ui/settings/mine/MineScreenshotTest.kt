package com.asianmobile.emojibattery.shimeji.ui.settings.mine

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.asianmobile.emojibattery.shimeji.R

@PreviewTest
@Preview(name = "Compact", widthDp = 360, heightDp = 950)
@Preview(name = "Wide", widthDp = 411, heightDp = 950)
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
        onSettingPets = {},
        onRate = {},
        onShare = {},
        onContact = {},
        onPrivacy = {}
    )
}

@PreviewTest
@Preview(name = "Apps hidden", widthDp = 360, heightDp = 800)
@Composable
fun AppsHiddenSheetScreenshotTest() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AppsHiddenSheetContent(
            apps = listOf(
                InstalledAppUiState("com.facebook.katana", "Facebook", null, true),
                InstalledAppUiState("com.instagram.android", "Instagram", null, false),
                InstalledAppUiState("com.google.android.youtube", "YouTube", null, true),
                InstalledAppUiState("com.spotify.music", "Spotify", null, false),
                InstalledAppUiState("com.google.android.gm", "Gmail", null, false),
                InstalledAppUiState("com.android.chrome", "Chrome", null, true)
            ),
            isLoading = false,
            loadFailed = false,
            onToggleApp = {},
            onRetry = {}
        )
    }
}
