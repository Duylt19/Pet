package com.asianmobile.emojibattery.shimeji.ui.home.shell

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(widthDp = 360, heightDp = 62)
@Composable
fun HomeBottomNavigationScreenshotTest() {
    HomeBottomNavigation(
        selectedTab = HomeTab.PET_STORE,
        onTabSelected = {}
    )
}

@PreviewTest
@Preview(widthDp = 360, heightDp = 62)
@Composable
fun HomeBottomNavigationMineScreenshotTest() {
    HomeBottomNavigation(
        selectedTab = HomeTab.MINE,
        onTabSelected = {}
    )
}

@PreviewTest
@Preview(widthDp = 360, heightDp = 62)
@Composable
fun HomeBottomNavigationBatteryScreenshotTest() {
    HomeBottomNavigation(
        selectedTab = HomeTab.BATTERY,
        onTabSelected = {}
    )
}
