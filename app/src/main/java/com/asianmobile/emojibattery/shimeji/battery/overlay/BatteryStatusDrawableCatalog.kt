package com.asianmobile.emojibattery.shimeji.battery.overlay

import androidx.annotation.DrawableRes
import com.asianmobile.emojibattery.shimeji.R

/**
 * Compile-time registry for status-bar icons that are selected by their persisted string name.
 *
 * Keeping the names mapped to [R.drawable] references prevents resource cleanup from silently
 * deleting a runtime-selected icon. Unknown server or legacy names still resolve to zero and use
 * the renderer's existing fallback.
 */
internal object BatteryStatusDrawableCatalog {
    @DrawableRes
    fun resolve(name: String): Int = DRAWABLES[name] ?: 0

    private val DRAWABLES = mapOf(
        "charge_01" to R.drawable.charge_01,
        "charge_02" to R.drawable.charge_02,
        "charge_03" to R.drawable.charge_03,
        "charge_04" to R.drawable.charge_04,
        "charge_05" to R.drawable.charge_05,
        "charge_06" to R.drawable.charge_06,
        "charge_07" to R.drawable.charge_07,
        "charge_08" to R.drawable.charge_08,
        "charge_09" to R.drawable.charge_09,
        "charge_10" to R.drawable.charge_10,
        "charge_11" to R.drawable.charge_11,
        "charge_12" to R.drawable.charge_12,
        "ic_status_airplane_classic" to R.drawable.ic_status_airplane_classic,
        "ic_status_airplane_paper" to R.drawable.ic_status_airplane_paper,
        "ic_status_airplane_round" to R.drawable.ic_status_airplane_round,
        "ic_status_airplane_takeoff" to R.drawable.ic_status_airplane_takeoff,
        "ic_status_hotspot_compact" to R.drawable.ic_status_hotspot_compact,
        "ic_status_hotspot_error" to R.drawable.ic_status_hotspot_error,
        "ic_status_hotspot_orbit" to R.drawable.ic_status_hotspot_orbit,
        "ic_status_hotspot_pending" to R.drawable.ic_status_hotspot_pending,
        "ic_status_hotspot_ring" to R.drawable.ic_status_hotspot_ring,
        "ic_status_hotspot_tower" to R.drawable.ic_status_hotspot_tower,
        "ic_status_ringer_silent" to R.drawable.ic_status_ringer_silent,
        "ic_status_signal_dots" to R.drawable.ic_status_signal_dots,
        "ic_status_signal_limited" to R.drawable.ic_status_signal_limited,
        "ic_status_signal_off" to R.drawable.ic_status_signal_off,
        "ic_status_signal_outline" to R.drawable.ic_status_signal_outline,
        "ic_status_signal_rounded" to R.drawable.ic_status_signal_rounded,
        "ic_status_signal_steps" to R.drawable.ic_status_signal_steps,
        "ic_status_silent_bell_outline" to R.drawable.ic_status_silent_bell_outline,
        "ic_status_silent_bell_solid" to R.drawable.ic_status_silent_bell_solid,
        "ic_status_silent_phone" to R.drawable.ic_status_silent_phone,
        "ic_status_vibrate" to R.drawable.ic_status_vibrate,
        "ic_status_vibrate_bell" to R.drawable.ic_status_vibrate_bell,
        "ic_status_vibrate_phone_solid" to R.drawable.ic_status_vibrate_phone_solid,
        "ic_status_vibrate_wave" to R.drawable.ic_status_vibrate_wave,
        "ic_status_wifi_compact" to R.drawable.ic_status_wifi_compact,
        "ic_status_wifi_limited" to R.drawable.ic_status_wifi_limited,
        "ic_status_wifi_off" to R.drawable.ic_status_wifi_off,
        "ic_status_wifi_outline" to R.drawable.ic_status_wifi_outline,
        "ic_status_wifi_solid" to R.drawable.ic_status_wifi_solid,
        "ic_status_wifi_waves" to R.drawable.ic_status_wifi_waves
    )
}
