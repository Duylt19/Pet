package com.asianmobile.emojibattery.shimeji.ui.home.shell

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.asianmobile.emojibattery.shimeji.ads.config.BANNER_HOME_BOTTOM
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.BannerAd

/**
 * Chrome owned by the Home graph rather than by any individual tab.
 *
 * A child Home route can keep the Home banner while hiding the bottom navigation by passing a
 * null [selectedTab]. Top-level tabs always provide their selected tab.
 */
@Composable
fun HomeShell(
    selectedTab: HomeTab?,
    showBottomBanner: Boolean,
    onTabSelected: (HomeTab) -> Unit
) {
    if (selectedTab != null) {
        HomeBottomNavigation(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected
        )
    }
    if (showBottomBanner) {
        BannerAd(
            modifier = Modifier.fillMaxWidth(),
            adPosition = BANNER_HOME_BOTTOM
        )
        Spacer(Modifier.navigationBarsPadding())
    }
}
