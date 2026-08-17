package com.asianmobile.emojibattery.shimeji.ui.shared.component

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.asianmobile.emojibattery.shimeji.ads.ui.interstitial.InterstitialUtil
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryAccessibility

/**
 * Opens the Accessibility list first so the four in-app How-to-use steps match the ROM handoff.
 * The service details screen remains a fallback for devices without the general list surface.
 * Returning from a settings surface is user-driven, so App Open Ads must not interrupt it.
 */
@Composable
fun rememberAccessibilitySettingsLauncher(
    onResult: () -> Unit
): () -> Unit {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        currentOnResult()
    }

    return remember(launcher, context) {
        {
            InterstitialUtil.getInstance().openAd?.needShowOpenAds = false
            launcher.launchFirstAvailable(
                BatteryAccessibility.settingsIntent(),
                BatteryAccessibility.detailsSettingsIntent(context)
            )
        }
    }
}

/**
 * Launches the first intent the device can actually handle.
 *
 * Every settings surface the app hands off to belongs to the ROM, and none of them is guaranteed:
 * a build can ship without the screen, and a vendor activity can resolve through the package
 * manager while still refusing to launch because it is not exported. Both throw out of [launch]
 * rather than returning a result, which would take down a screen over a settings page that is only
 * ever a convenience. Resolving is not enough to tell the two apart, so this just tries in order.
 */
internal fun ActivityResultLauncher<Intent>.launchFirstAvailable(vararg intents: Intent) {
    intents.firstOrNull { intent -> runCatching { launch(intent) }.isSuccess }
}
