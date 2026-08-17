package com.asianmobile.emojibattery.shimeji.ads.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.asianmobile.emojibattery.shimeji.ads.R
import kotlinx.coroutines.delay

/**
 * Compose replacement for LoadingDialogFragment.
 * Shows a full-screen loading overlay before interstitial/open ads.
 */
@Composable
fun LoadingAdDialog(
    isShowing: Boolean,
    timeoutMs: Long = 1500L,
    onTimeout: () -> Unit = {}
) {
    if (!isShowing) return

    LaunchedEffect(Unit) {
        delay(timeoutMs)
        onTimeout()
    }

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(R.color.colors_161718)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = colorResource(R.color.colors_5FADFF),
                strokeWidth = 4.dp
            )
        }
    }
}
