package com.asianmobile.emojibattery.shimeji.battery.overlay

enum class BatteryConnectivityState {
    DISABLED,
    DISCONNECTED,
    LIMITED,
    CONNECTED
}

enum class BatteryRingerState {
    NORMAL,
    VIBRATE,
    SILENT
}

enum class BatteryHotspotState {
    UNKNOWN,
    DISABLED,
    DISABLING,
    ENABLING,
    ENABLED,
    FAILED
}

enum class BatteryChargeState {
    UNKNOWN,
    DISCHARGING,
    NOT_CHARGING,
    CHARGING,
    FULL
}

enum class BatteryPlugType {
    NONE,
    AC,
    USB,
    WIRELESS,
    DOCK,
    UNKNOWN
}

data class BatteryPowerState(
    val level: Int = 100,
    val chargeState: BatteryChargeState = BatteryChargeState.UNKNOWN,
    val plugType: BatteryPlugType = BatteryPlugType.UNKNOWN,
    val present: Boolean = true
) {
    val isCharging: Boolean
        get() = chargeState == BatteryChargeState.CHARGING ||
            chargeState == BatteryChargeState.FULL
}

data class BatteryDeviceState(
    val wifi: BatteryConnectivityState = BatteryConnectivityState.DISCONNECTED,
    val cellular: BatteryConnectivityState = BatteryConnectivityState.DISCONNECTED,
    val airplaneMode: Boolean = false,
    val hotspot: BatteryHotspotState = BatteryHotspotState.UNKNOWN,
    val ringer: BatteryRingerState = BatteryRingerState.NORMAL
)

data class BatteryNetworkObservation(
    val isWifi: Boolean,
    val isCellular: Boolean,
    val hasInternetCapability: Boolean,
    val isValidated: Boolean,
    val isCaptivePortal: Boolean
)

internal object BatterySystemStatusPolicy {
    fun connectivity(
        enabled: Boolean,
        observations: List<BatteryNetworkObservation>
    ): BatteryConnectivityState {
        if (!enabled) return BatteryConnectivityState.DISABLED
        if (observations.any(BatteryNetworkObservation::isValidated)) {
            return BatteryConnectivityState.CONNECTED
        }
        if (observations.isNotEmpty()) {
            return BatteryConnectivityState.LIMITED
        }
        return BatteryConnectivityState.DISCONNECTED
    }

    fun ringer(mode: Int): BatteryRingerState = when (mode) {
        RINGER_MODE_VIBRATE -> BatteryRingerState.VIBRATE
        RINGER_MODE_SILENT -> BatteryRingerState.SILENT
        else -> BatteryRingerState.NORMAL
    }

    fun hotspot(state: Int): BatteryHotspotState = when (state) {
        WIFI_AP_STATE_DISABLED -> BatteryHotspotState.DISABLED
        WIFI_AP_STATE_DISABLING -> BatteryHotspotState.DISABLING
        WIFI_AP_STATE_ENABLING -> BatteryHotspotState.ENABLING
        WIFI_AP_STATE_ENABLED -> BatteryHotspotState.ENABLED
        WIFI_AP_STATE_FAILED -> BatteryHotspotState.FAILED
        else -> BatteryHotspotState.UNKNOWN
    }

    fun charge(state: Int): BatteryChargeState = when (state) {
        BATTERY_STATUS_CHARGING -> BatteryChargeState.CHARGING
        BATTERY_STATUS_DISCHARGING -> BatteryChargeState.DISCHARGING
        BATTERY_STATUS_NOT_CHARGING -> BatteryChargeState.NOT_CHARGING
        BATTERY_STATUS_FULL -> BatteryChargeState.FULL
        else -> BatteryChargeState.UNKNOWN
    }

    fun plug(type: Int): BatteryPlugType = when (type) {
        BATTERY_PLUGGED_NONE -> BatteryPlugType.NONE
        BATTERY_PLUGGED_AC -> BatteryPlugType.AC
        BATTERY_PLUGGED_USB -> BatteryPlugType.USB
        BATTERY_PLUGGED_WIRELESS -> BatteryPlugType.WIRELESS
        BATTERY_PLUGGED_DOCK -> BatteryPlugType.DOCK
        else -> BatteryPlugType.UNKNOWN
    }

    fun wifiIcon(state: BatteryConnectivityState): String = when (state) {
        BatteryConnectivityState.CONNECTED -> "ic_wifi"
        BatteryConnectivityState.LIMITED -> "ic_status_wifi_limited"
        BatteryConnectivityState.DISCONNECTED,
        BatteryConnectivityState.DISABLED -> "ic_status_wifi_off"
    }

    fun cellularIcon(state: BatteryConnectivityState): String = when (state) {
        BatteryConnectivityState.CONNECTED -> "ic_signal"
        BatteryConnectivityState.LIMITED -> "ic_status_signal_limited"
        BatteryConnectivityState.DISCONNECTED,
        BatteryConnectivityState.DISABLED -> "ic_status_signal_off"
    }

    fun ringerIcon(state: BatteryRingerState): String? = when (state) {
        BatteryRingerState.NORMAL -> null
        BatteryRingerState.VIBRATE -> "ic_status_vibrate"
        BatteryRingerState.SILENT -> "ic_status_ringer_silent"
    }

    fun hotspotIcon(state: BatteryHotspotState): String? = when (state) {
        BatteryHotspotState.ENABLED -> "ic_hostpot"
        BatteryHotspotState.ENABLING,
        BatteryHotspotState.DISABLING -> "ic_status_hotspot_pending"
        BatteryHotspotState.FAILED -> "ic_status_hotspot_error"
        BatteryHotspotState.UNKNOWN,
        BatteryHotspotState.DISABLED -> null
    }

    private const val RINGER_MODE_SILENT = 0
    private const val RINGER_MODE_VIBRATE = 1

    private const val WIFI_AP_STATE_DISABLING = 10
    private const val WIFI_AP_STATE_DISABLED = 11
    private const val WIFI_AP_STATE_ENABLING = 12
    private const val WIFI_AP_STATE_ENABLED = 13
    private const val WIFI_AP_STATE_FAILED = 14

    private const val BATTERY_STATUS_UNKNOWN = 1
    private const val BATTERY_STATUS_CHARGING = 2
    private const val BATTERY_STATUS_DISCHARGING = 3
    private const val BATTERY_STATUS_NOT_CHARGING = 4
    private const val BATTERY_STATUS_FULL = 5

    private const val BATTERY_PLUGGED_NONE = 0
    private const val BATTERY_PLUGGED_AC = 1
    private const val BATTERY_PLUGGED_USB = 2
    private const val BATTERY_PLUGGED_WIRELESS = 4
    private const val BATTERY_PLUGGED_DOCK = 8
}
