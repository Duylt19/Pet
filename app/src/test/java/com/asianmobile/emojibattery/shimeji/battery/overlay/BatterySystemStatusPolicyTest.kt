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
    fun previewState_usesActualStateAndOnlySimulatesTheFocusedConditionalComponent() {
        val actualDevice = BatteryDeviceState(
            airplaneMode = false,
            hotspot = BatteryHotspotState.DISABLED,
            ringer = BatteryRingerState.NORMAL
        )
        val actualPower = BatteryPowerState(chargeState = BatteryChargeState.DISCHARGING)

        assertEquals(
            actualDevice,
            BatteryPreviewSystemStatePolicy.deviceState(actualDevice, focusedComponent = null)
        )
        assertEquals(
            actualPower,
            BatteryPreviewSystemStatePolicy.powerState(actualPower, focusedComponent = null)
        )
        assertTrue(
            BatteryPreviewSystemStatePolicy.powerState(
                actualPower,
                BatteryStatusComponent.CHARGE
            ).isCharging
        )
        assertEquals(
            BatteryHotspotState.ENABLED,
            BatteryPreviewSystemStatePolicy.deviceState(
                actualDevice,
                BatteryStatusComponent.HOTSPOT
            ).hotspot
        )
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
            "ic_status_wifi_solid",
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
            "ic_status_signal_rounded",
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
            "ic_status_hotspot_orbit",
            BatterySystemStatusPolicy.hotspotIcon(BatteryHotspotState.ENABLED)
        )
    }

    @Test
    fun icon_policy_maps_every_selectable_style_and_falls_back_safely() {
        assertEquals(
            listOf(
                "ic_status_wifi_solid",
                "ic_status_wifi_waves",
                "ic_status_wifi_outline",
                "ic_status_wifi_compact"
            ),
            (1..4).map {
                BatterySystemStatusPolicy.wifiIcon(BatteryConnectivityState.CONNECTED, it)
            }
        )
        assertEquals(
            listOf(
                "ic_status_signal_rounded",
                "ic_status_signal_steps",
                "ic_status_signal_outline",
                "ic_status_signal_dots"
            ),
            (1..4).map {
                BatterySystemStatusPolicy.cellularIcon(BatteryConnectivityState.CONNECTED, it)
            }
        )
        assertEquals(
            listOf(
                "ic_status_airplane_classic",
                "ic_status_airplane_round",
                "ic_status_airplane_takeoff",
                "ic_status_airplane_paper"
            ),
            (1..4).map(BatterySystemStatusPolicy::airplaneIcon)
        )
        assertEquals(
            listOf(
                "ic_status_hotspot_orbit",
                "ic_status_hotspot_ring",
                "ic_status_hotspot_compact",
                "ic_status_hotspot_tower"
            ),
            (1..4).map {
                BatterySystemStatusPolicy.hotspotIcon(BatteryHotspotState.ENABLED, it)
            }
        )
        assertEquals(
            listOf(
                "ic_status_vibrate",
                "ic_status_vibrate_bell",
                "ic_status_vibrate_phone_solid",
                "ic_status_vibrate_wave"
            ),
            (1..4).map {
                BatterySystemStatusPolicy.ringerIcon(BatteryRingerState.VIBRATE, it)
            }
        )
        assertEquals(
            listOf(
                "ic_status_ringer_silent",
                "ic_status_silent_bell_outline",
                "ic_status_silent_bell_solid",
                "ic_status_silent_phone"
            ),
            (1..4).map {
                BatterySystemStatusPolicy.ringerIcon(BatteryRingerState.SILENT, it)
            }
        )

        assertEquals(
            "ic_status_wifi_solid",
            BatterySystemStatusPolicy.wifiIcon(BatteryConnectivityState.CONNECTED, 99)
        )
        assertEquals(
            "ic_status_airplane_classic",
            BatterySystemStatusPolicy.airplaneIcon(-1)
        )
        assertEquals(
            "ic_status_wifi_off",
            BatterySystemStatusPolicy.wifiIcon(BatteryConnectivityState.DISABLED, 4)
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
