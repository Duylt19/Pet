package com.asianmobile.emojibattery.shimeji.ui.home.shell

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.asianmobile.emojibattery.shimeji.ads.config.BANNER_HOME_BOTTOM
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.BannerAd

/**
 * Chrome owned by the four-tab Home container rather than by any individual tab.
 * Root app destinations never compose this shell and own their own optional ad slot.
 */
@Composable
fun HomeShell(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit
) {
    HomeBottomNavigation(
        selectedTab = selectedTab,
        onTabSelected = onTabSelected
    )
    BannerAd(
        modifier = Modifier.fillMaxWidth(),
        adPosition = BANNER_HOME_BOTTOM
    )
}
