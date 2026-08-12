package com.asianmobile.emojibattery.shimeji.battery.overlay

internal object BatteryAppExclusionPolicy {
    fun shouldHide(
        foregroundPackage: String?,
        hiddenAppPackages: Set<String>
    ): Boolean = foregroundPackage != null && foregroundPackage in hiddenAppPackages
}
