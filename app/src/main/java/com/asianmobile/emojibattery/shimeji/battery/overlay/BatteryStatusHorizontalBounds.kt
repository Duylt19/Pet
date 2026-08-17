package com.asianmobile.emojibattery.shimeji.battery.overlay

internal data class BatteryStatusHorizontalBounds(
    val backgroundRightPx: Float,
    val contentRightPx: Float
)

internal fun resolveBatteryStatusHorizontalBounds(
    widthPx: Float
): BatteryStatusHorizontalBounds {
    val safeWidth = widthPx.coerceAtLeast(0f)
    return BatteryStatusHorizontalBounds(
        backgroundRightPx = safeWidth,
        contentRightPx = safeWidth
    )
}
