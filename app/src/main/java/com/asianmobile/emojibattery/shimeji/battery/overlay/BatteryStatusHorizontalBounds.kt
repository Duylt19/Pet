package com.asianmobile.emojibattery.shimeji.battery.overlay

internal data class BatteryStatusHorizontalBounds(
    val backgroundRightPx: Float,
    val contentRightPx: Float
)

internal fun resolveBatteryStatusHorizontalBounds(
    widthPx: Float,
    minimumContentRightPx: Float,
    privacyReservePx: Float
): BatteryStatusHorizontalBounds {
    val safeWidth = widthPx.coerceAtLeast(0f)
    val minimumContentRight = minimumContentRightPx.coerceIn(0f, safeWidth)
    val contentRight = (safeWidth - privacyReservePx.coerceAtLeast(0f))
        .coerceIn(minimumContentRight, safeWidth)
    return BatteryStatusHorizontalBounds(
        backgroundRightPx = safeWidth,
        contentRightPx = contentRight
    )
}
