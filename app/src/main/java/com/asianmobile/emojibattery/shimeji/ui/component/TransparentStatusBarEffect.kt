package com.asianmobile.emojibattery.shimeji.ui.component

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Suppress("DEPRECATION")
@Composable
fun TransparentStatusBarEffect(useDarkIcons: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = remember(context) { context.findActivity() }

    DisposableEffect(activity, lifecycleOwner, useDarkIcons) {
        val window = activity?.window
        val controller = window?.let {
            WindowCompat.getInsetsController(it, it.decorView)
        }
        val previousStatusBarColor = window?.statusBarColor
        val previousLightStatusBars = controller?.isAppearanceLightStatusBars

        fun applyStatusBarAppearance() {
            window ?: return
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.TRANSPARENT
            controller?.isAppearanceLightStatusBars = useDarkIcons
        }

        applyStatusBarAppearance()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                applyStatusBarAppearance()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            previousStatusBarColor?.let { window?.statusBarColor = it }
            previousLightStatusBars?.let {
                controller?.isAppearanceLightStatusBars = it
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
