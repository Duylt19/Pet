package com.asianmobile.emojibattery.shimeji.battery.overlay

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

object BatteryAccessibility {
    fun settingsIntent(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

    fun isEnabled(context: Context): Boolean {
        val component = ComponentName(context, StatusBarAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        return enabled.split(':').any {
            ComponentName.unflattenFromString(it) == component
        }
    }
}
