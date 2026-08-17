package com.asianmobile.emojibattery.shimeji.battery.overlay

internal object BatteryAppExclusionPolicy {
    /**
     * System bars and keyboards create accessibility window events without replacing the app the
     * user is actually using. Retaining the last application package prevents a transient system
     * window from briefly re-enabling the overlay inside an excluded immersive app.
     */
    fun resolveForegroundPackage(
        currentForegroundPackage: String?,
        eventPackage: String?,
        transientWindowPackages: Set<String>
    ): String? {
        val normalizedEventPackage = eventPackage?.trim()?.takeIf(String::isNotEmpty)
            ?: return currentForegroundPackage
        return if (normalizedEventPackage in transientWindowPackages) {
            currentForegroundPackage
        } else {
            normalizedEventPackage
        }
    }

    fun shouldHide(
        foregroundPackage: String?,
        hiddenAppPackages: Set<String>
    ): Boolean = foregroundPackage != null && foregroundPackage in hiddenAppPackages
}
