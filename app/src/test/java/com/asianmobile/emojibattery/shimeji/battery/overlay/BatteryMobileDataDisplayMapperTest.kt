package com.asianmobile.emojibattery.shimeji.battery.overlay

import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryMobileDataDisplayMapperTest {
    @Test
    fun `maps carrier display technologies to status badges`() {
        assertEquals(BatteryMobileDataBadge.G, map(TelephonyManager.NETWORK_TYPE_GPRS))
        assertEquals(BatteryMobileDataBadge.EDGE, map(TelephonyManager.NETWORK_TYPE_EDGE))
        assertEquals(BatteryMobileDataBadge.G3, map(TelephonyManager.NETWORK_TYPE_UMTS))
        assertEquals(BatteryMobileDataBadge.H, map(TelephonyManager.NETWORK_TYPE_HSPA))
        assertEquals(BatteryMobileDataBadge.H_PLUS, map(TelephonyManager.NETWORK_TYPE_HSPAP))
        assertEquals(BatteryMobileDataBadge.G4, map(TelephonyManager.NETWORK_TYPE_LTE))
        assertEquals(BatteryMobileDataBadge.G5, map(TelephonyManager.NETWORK_TYPE_NR))
        assertNull(map(TelephonyManager.NETWORK_TYPE_IWLAN))
    }

    @Test
    fun `display override wins for 5g nsa and advanced networks`() {
        assertEquals(
            BatteryMobileDataBadge.G5,
            map(
                TelephonyManager.NETWORK_TYPE_LTE,
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA
            )
        )
        assertEquals(
            BatteryMobileDataBadge.G5_PLUS,
            map(
                TelephonyManager.NETWORK_TYPE_NR,
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED
            )
        )
        assertEquals(
            BatteryMobileDataBadge.G4_PLUS,
            map(
                TelephonyManager.NETWORK_TYPE_LTE,
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA
            )
        )
    }

    private fun map(
        networkType: Int,
        overrideNetworkType: Int = TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE
    ): BatteryMobileDataBadge? =
        BatteryMobileDataDisplayMapper.map(networkType, overrideNetworkType)
}
