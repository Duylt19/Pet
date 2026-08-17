package com.asianmobile.emojibattery.shimeji.battery.overlay

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

object BatteryAccessibility {
    fun settingsIntent(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

    /**
     * Opens this service's own page instead of the full Accessibility list, which on a device with
     * a dozen services is a real difference in whether the user finds us at all.
     *
     * The action is not public API — `android.provider.Settings` exposes only the list — so it is
     * spelled out here and every caller must fall back to [settingsIntent]. AOSP's
     * `AccessibilityDetailsSettingsFragment` reads the target from [Intent.EXTRA_COMPONENT_NAME]
     * and unflattens it, which is why the component is passed flattened.
     */
    fun detailsSettingsIntent(context: Context): Intent =
        Intent(ACTION_ACCESSIBILITY_DETAILS_SETTINGS)
            .putExtra(Intent.EXTRA_COMPONENT_NAME, component(context).flattenToString())

    fun isEnabled(context: Context): Boolean {
        val component = component(context)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        return enabled.split(':').any {
            ComponentName.unflattenFromString(it) == component
        }
    }

    private fun component(context: Context): ComponentName =
        ComponentName(context, StatusBarAccessibilityService::class.java)

    private const val ACTION_ACCESSIBILITY_DETAILS_SETTINGS =
        "android.settings.ACCESSIBILITY_DETAILS_SETTINGS"
}
