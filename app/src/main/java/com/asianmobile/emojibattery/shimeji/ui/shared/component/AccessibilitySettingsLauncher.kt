package com.asianmobile.emojibattery.shimeji.ui.shared.component

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.asianmobile.emojibattery.shimeji.ads.ui.interstitial.InterstitialUtil
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryAccessibility

/**
 * Keeps every Accessibility grant entry point on the same system-settings handoff contract.
 * Returning from a settings surface is user-driven, so App Open Ads must not interrupt it.
 */
@Composable
fun rememberAccessibilitySettingsLauncher(
    onResult: () -> Unit
): () -> Unit {
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        currentOnResult()
    }

    return remember(launcher) {
        {
            InterstitialUtil.getInstance().openAd?.needShowOpenAds = false
            launcher.launch(BatteryAccessibility.settingsIntent())
        }
    }
}
