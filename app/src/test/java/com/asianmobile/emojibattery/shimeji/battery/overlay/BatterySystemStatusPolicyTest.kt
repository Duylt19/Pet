package com.asianmobile.emojibattery.shimeji.battery.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatterySystemStatusPolicyTest {
    @Test
    fun connectivity_distinguishes_disabled_disconnected_limited_and_validated() {
        assertEquals(
            BatteryConnectivityState.DISABLED,
            BatterySystemStatusPolicy.connectivity(
                enabled = false,
                observations = listOf(observation(validated = true))
            )
        )
        assertEquals(
            BatteryConnectivityState.DISCONNECTED,
            BatterySystemStatusPolicy.connectivity(enabled = true, observations = emptyList())
        )
        assertEquals(
            BatteryConnectivityState.LIMITED,
            BatterySystemStatusPolicy.connectivity(
                enabled = true,
                observations = listOf(observation(internet = true))
            )
        )
        assertEquals(
            BatteryConnectivityState.LIMITED,
            BatterySystemStatusPolicy.connectivity(
                enabled = true,
                observations = listOf(observation())
            )
        )
        assertEquals(
            BatteryConnectivityState.CONNECTED,
            BatterySystemStatusPolicy.connectivity(
                enabled = true,
                observations = listOf(observation(validated = true))
            )
        )
    }

    @Test
    fun connectivity_treats_captive_portal_as_limited() {
        assertEquals(
            BatteryConnectivityState.LIMITED,
            BatterySystemStatusPolicy.connectivity(
                enabled = true,
                observations = listOf(observation(captivePortal = true))
            )
        )
    }

    @Test
    fun ringer_maps_all_platform_modes() {
        assertEquals(BatteryRingerState.SILENT, BatterySystemStatusPolicy.ringer(0))
        assertEquals(BatteryRingerState.VIBRATE, BatterySystemStatusPolicy.ringer(1))
        assertEquals(BatteryRingerState.NORMAL, BatterySystemStatusPolicy.ringer(2))
        assertEquals(BatteryRingerState.NORMAL, BatterySystemStatusPolicy.ringer(Int.MAX_VALUE))
    }

    @Test
    fun ringerPreview_preservesActiveMode_andOnlySamplesSilentForNormalMode() {
        assertEquals(
            BatteryRingerState.SILENT,
            BatterySystemStatusPolicy.ringerForPreview(BatteryRingerState.NORMAL)
        )
        assertEquals(
            BatteryRingerState.VIBRATE,
            BatterySystemStatusPolicy.ringerForPreview(BatteryRingerState.VIBRATE)
        )
        assertEquals(
            BatteryRingerState.SILENT,
            BatterySystemStatusPolicy.ringerForPreview(BatteryRingerState.SILENT)
        )
    }

    @Test
    fun hotspot_maps_all_broadcast_states() {
        assertEquals(BatteryHotspotState.DISABLING, BatterySystemStatusPolicy.hotspot(10))
        assertEquals(BatteryHotspotState.DISABLED, BatterySystemStatusPolicy.hotspot(11))
        assertEquals(BatteryHotspotState.ENABLING, BatterySystemStatusPolicy.hotspot(12))
        assertEquals(BatteryHotspotState.ENABLED, BatterySystemStatusPolicy.hotspot(13))
        assertEquals(BatteryHotspotState.FAILED, BatterySystemStatusPolicy.hotspot(14))
        assertEquals(BatteryHotspotState.UNKNOWN, BatterySystemStatusPolicy.hotspot(-1))
    }

    @Test
    fun charge_and_plug_map_all_battery_broadcast_states() {
        assertEquals(BatteryChargeState.UNKNOWN, BatterySystemStatusPolicy.charge(1))
        assertEquals(BatteryChargeState.CHARGING, BatterySystemStatusPolicy.charge(2))
        assertEquals(BatteryChargeState.DISCHARGING, BatterySystemStatusPolicy.charge(3))
        assertEquals(BatteryChargeState.NOT_CHARGING, BatterySystemStatusPolicy.charge(4))
        assertEquals(BatteryChargeState.FULL, BatterySystemStatusPolicy.charge(5))

        assertEquals(BatteryPlugType.NONE, BatterySystemStatusPolicy.plug(0))
        assertEquals(BatteryPlugType.AC, BatterySystemStatusPolicy.plug(1))
        assertEquals(BatteryPlugType.USB, BatterySystemStatusPolicy.plug(2))
        assertEquals(BatteryPlugType.WIRELESS, BatterySystemStatusPolicy.plug(4))
        assertEquals(BatteryPlugType.DOCK, BatterySystemStatusPolicy.plug(8))
        assertEquals(BatteryPlugType.UNKNOWN, BatterySystemStatusPolicy.plug(-1))

        assertTrue(BatteryPowerState(chargeState = BatteryChargeState.CHARGING).isCharging)
        assertTrue(BatteryPowerState(chargeState = BatteryChargeState.FULL).isCharging)
        assertFalse(BatteryPowerState(chargeState = BatteryChargeState.DISCHARGING).isCharging)
    }

    @Test
    fun icon_policy_covers_every_visible_state() {
        assertEquals(
            "ic_status_wifi_off",
            BatterySystemStatusPolicy.wifiIcon(BatteryConnectivityState.DISABLED)
        )
        assertEquals(
            "ic_status_wifi_limited",
            BatterySystemStatusPolicy.wifiIcon(BatteryConnectivityState.LIMITED)
        )
        assertEquals(
            "ic_wifi",
            BatterySystemStatusPolicy.wifiIcon(BatteryConnectivityState.CONNECTED)
        )
        assertEquals(
            "ic_status_signal_off",
            BatterySystemStatusPolicy.cellularIcon(BatteryConnectivityState.DISCONNECTED)
        )
        assertEquals(
            "ic_status_signal_limited",
            BatterySystemStatusPolicy.cellularIcon(BatteryConnectivityState.LIMITED)
        )
        assertEquals(
            "ic_signal",
            BatterySystemStatusPolicy.cellularIcon(BatteryConnectivityState.CONNECTED)
        )
        assertNull(BatterySystemStatusPolicy.ringerIcon(BatteryRingerState.NORMAL))
        assertEquals(
            "ic_status_vibrate",
            BatterySystemStatusPolicy.ringerIcon(BatteryRingerState.VIBRATE)
        )
        assertEquals(
            "ic_status_ringer_silent",
            BatterySystemStatusPolicy.ringerIcon(BatteryRingerState.SILENT)
        )
        assertNull(BatterySystemStatusPolicy.hotspotIcon(BatteryHotspotState.DISABLED))
        assertEquals(
            "ic_status_hotspot_pending",
            BatterySystemStatusPolicy.hotspotIcon(BatteryHotspotState.ENABLING)
        )
        assertEquals(
            "ic_status_hotspot_error",
            BatterySystemStatusPolicy.hotspotIcon(BatteryHotspotState.FAILED)
        )
        assertEquals(
            "ic_hostpot",
            BatterySystemStatusPolicy.hotspotIcon(BatteryHotspotState.ENABLED)
        )
    }

    private fun observation(
        internet: Boolean = false,
        validated: Boolean = false,
        captivePortal: Boolean = false
    ) = BatteryNetworkObservation(
        isWifi = true,
        isCellular = false,
        hasInternetCapability = internet,
        isValidated = validated,
        isCaptivePortal = captivePortal
    )
}
