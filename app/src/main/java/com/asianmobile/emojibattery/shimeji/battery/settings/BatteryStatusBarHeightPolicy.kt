package com.asianmobile.emojibattery.shimeji.battery.settings

import android.content.Context
import android.os.Build
import android.view.WindowInsets
import android.view.WindowManager
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_BAR_HEIGHT_DP
import com.asianmobile.emojibattery.shimeji.data.model.MAX_BATTERY_BAR_HEIGHT_DP
import com.asianmobile.emojibattery.shimeji.data.model.MIN_BATTERY_BAR_HEIGHT_DP

data class BatteryStatusBarHeightRange(
    val minimumDp: Float,
    val defaultDp: Float,
    val maximumDp: Float
)

fun resolveBatteryStatusBarHeightRange(
    systemStatusBarHeightDp: Float
): BatteryStatusBarHeightRange {
    val center = systemStatusBarHeightDp
        .takeIf { it.isFinite() && it > 0f }
        ?.coerceIn(MIN_BATTERY_BAR_HEIGHT_DP, MAX_BATTERY_BAR_HEIGHT_DP)
        ?: DEFAULT_BATTERY_BAR_HEIGHT_DP
    val desiredRadius = center / 2f
    val radius = minOf(
        desiredRadius,
        center - MIN_BATTERY_BAR_HEIGHT_DP,
        MAX_BATTERY_BAR_HEIGHT_DP - center
    ).coerceAtLeast(0f)
    return BatteryStatusBarHeightRange(
        minimumDp = center - radius,
        defaultDp = center,
        maximumDp = center + radius
    )
}

fun Context.systemStatusBarHeightDp(): Float {
    val density = resources.displayMetrics.density
        .takeIf { it.isFinite() && it > 0f }
        ?: return DEFAULT_BATTERY_BAR_HEIGHT_DP
    val insetHeightPx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        runCatching {
            getSystemService(WindowManager::class.java)
                .currentWindowMetrics
                .windowInsets
                .getInsetsIgnoringVisibility(
                    WindowInsets.Type.statusBars() or WindowInsets.Type.displayCutout()
                )
                .top
        }.getOrDefault(0)
    } else {
        0
    }
    val resourceHeightPx = resources
        .getIdentifier("status_bar_height", "dimen", "android")
        .takeIf { it != 0 }
        ?.let(resources::getDimensionPixelSize)
        ?: 0
    val heightPx = insetHeightPx.takeIf { it > 0 } ?: resourceHeightPx
    return (heightPx / density)
        .takeIf { it.isFinite() && it > 0f }
        ?: DEFAULT_BATTERY_BAR_HEIGHT_DP
}
