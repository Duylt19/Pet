package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryStatusComponent
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryStatusLayoutResult
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryChargeState
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryDeviceState
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryHotspotState
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryPowerState
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryRingerState
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryPreviewLayoutTest {
    @Test
    fun overviewPreview_reflectsRealConditionalSystemStates() {
        val layout = batteryPreviewLayout(
            config = BatteryStatusConfig(
                showAirplane = true,
                showRinger = true,
                showHotspot = true,
                showCharge = true
            ),
            availableWidthDp = 400f,
            hasEmoji = false,
            hasEmotion = false,
            hasAnimation = false,
            deviceState = BatteryDeviceState(
                airplaneMode = true,
                ringer = BatteryRingerState.VIBRATE,
                hotspot = BatteryHotspotState.ENABLED
            ),
            powerState = BatteryPowerState(chargeState = BatteryChargeState.CHARGING)
        )

        assertTrue(layout.shows(BatteryStatusComponent.AIRPLANE))
        assertTrue(layout.shows(BatteryStatusComponent.RINGER))
        assertTrue(layout.shows(BatteryStatusComponent.HOTSPOT))
        assertTrue(layout.shows(BatteryStatusComponent.CHARGE))
        assertFalse(layout.shows(BatteryStatusComponent.CELLULAR))
    }

    @Test
    fun overviewPreview_hidesChargeWhenSwitchIsOffEvenWhileCharging() {
        val layout = batteryPreviewLayout(
            config = BatteryStatusConfig(showCharge = false),
            availableWidthDp = 320f,
            hasEmoji = false,
            hasEmotion = false,
            hasAnimation = false,
            powerState = BatteryPowerState(chargeState = BatteryChargeState.CHARGING)
        )

        assertFalse(layout.shows(BatteryStatusComponent.CHARGE))
    }

    @Test
    fun focusedStatusComponent_remainsVisibleInConstrainedPreview() {
        val focusedComponents = listOf(
            BatteryStatusComponent.DATE,
            BatteryStatusComponent.AIRPLANE,
            BatteryStatusComponent.RINGER,
            BatteryStatusComponent.ANIMATION,
            BatteryStatusComponent.EMOTION,
            BatteryStatusComponent.CHARGE,
            BatteryStatusComponent.WIFI,
            BatteryStatusComponent.CELLULAR,
            BatteryStatusComponent.HOTSPOT
        )

        focusedComponents.forEach { focusedComponent ->
            val layout = batteryPreviewLayout(
                config = BatteryStatusConfig(
                    showDateTime = true,
                    showAnimation = true,
                    showEmotion = true,
                    showData = true,
                    showHotspot = true
                ),
                availableWidthDp = 120f,
                hasEmoji = true,
                hasEmotion = true,
                hasAnimation = true,
                focusedComponent = focusedComponent,
                deviceState = BatteryDeviceState(
                    ringer = if (focusedComponent == BatteryStatusComponent.RINGER) {
                        BatteryRingerState.VIBRATE
                    } else {
                        BatteryRingerState.NORMAL
                    }
                )
            )

            assertTrue(
                "$focusedComponent should remain visible while it is edited",
                layout.shows(focusedComponent)
            )
            if (focusedComponent == BatteryStatusComponent.DATE) {
                assertTrue(
                    "Time should remain visible while date and time are edited",
                    layout.shows(BatteryStatusComponent.TIME)
                )
            }
        }
    }

    @Test
    fun focusedRinger_reflectsNormalModeInsteadOfShowingSyntheticSilentIcon() {
        val layout = batteryPreviewLayout(
            config = BatteryStatusConfig(showRinger = true),
            availableWidthDp = 320f,
            hasEmoji = false,
            hasEmotion = false,
            hasAnimation = false,
            focusedComponent = BatteryStatusComponent.RINGER,
            deviceState = BatteryDeviceState(ringer = BatteryRingerState.NORMAL)
        )

        assertFalse(layout.shows(BatteryStatusComponent.RINGER))
    }

    @Test
    fun airplanePreview_hidesMutuallyExclusiveCellularGroup() {
        val layout = batteryPreviewLayout(
            config = BatteryStatusConfig(),
            availableWidthDp = 320f,
            hasEmoji = true,
            hasEmotion = true,
            hasAnimation = true,
            focusedComponent = BatteryStatusComponent.AIRPLANE
        )

        assertTrue(layout.shows(BatteryStatusComponent.AIRPLANE))
        assertFalse(layout.shows(BatteryStatusComponent.CELLULAR))
    }

    @Test
    fun datePreview_respectsVisibilityToggle() {
        val layout = batteryPreviewLayout(
            config = BatteryStatusConfig(showDateTime = false),
            availableWidthDp = 320f,
            hasEmoji = false,
            hasEmotion = false,
            hasAnimation = false,
            focusedComponent = BatteryStatusComponent.DATE
        )

        assertFalse(layout.shows(BatteryStatusComponent.DATE))
    }

    @Test
    fun emotionPreview_respectsVisibilityToggle_evenWhileFocused() {
        val layout = batteryPreviewLayout(
            config = BatteryStatusConfig(showEmotion = false),
            availableWidthDp = 320f,
            hasEmoji = false,
            hasEmotion = true,
            hasAnimation = false,
            focusedComponent = BatteryStatusComponent.EMOTION
        )

        assertFalse(layout.shows(BatteryStatusComponent.EMOTION))
    }

    @Test
    fun focusedDeviceComponents_respectTheirEditorSwitches() {
        val cases = listOf(
            BatteryStatusComponent.AIRPLANE to BatteryStatusConfig(showAirplane = false),
            BatteryStatusComponent.RINGER to BatteryStatusConfig(showRinger = false),
            BatteryStatusComponent.HOTSPOT to BatteryStatusConfig(showHotspot = false),
            BatteryStatusComponent.CHARGE to BatteryStatusConfig(showCharge = false)
        )

        cases.forEach { (component, config) ->
            val layout = batteryPreviewLayout(
                config = config,
                availableWidthDp = 320f,
                hasEmoji = false,
                hasEmotion = false,
                hasAnimation = false,
                focusedComponent = component
            )
            assertFalse("$component must stay hidden after its switch is off", layout.shows(component))
        }
    }

    @Test
    fun connectivityComponents_respectTheirEditorSwitches() {
        val wifi = batteryPreviewLayout(
            config = BatteryStatusConfig(showWifi = false),
            availableWidthDp = 320f,
            hasEmoji = false,
            hasEmotion = false,
            hasAnimation = false,
            focusedComponent = BatteryStatusComponent.WIFI
        )
        val cellular = batteryPreviewLayout(
            config = BatteryStatusConfig(showSignal = false, showData = false),
            availableWidthDp = 320f,
            hasEmoji = false,
            hasEmotion = false,
            hasAnimation = false,
            focusedComponent = BatteryStatusComponent.CELLULAR
        )

        assertFalse(wifi.shows(BatteryStatusComponent.WIFI))
        assertFalse(cellular.shows(BatteryStatusComponent.CELLULAR))
    }

    @Test
    fun themedEmoji_isPartOfBatteryPair_notLeadingStatusGroup() {
        val layout = batteryPreviewLayout(
            config = BatteryStatusConfig(
                batterySizeDp = 20f,
                emojiSizeDp = 36f
            ),
            availableWidthDp = 320f,
            hasEmoji = true,
            hasEmotion = false,
            hasAnimation = false
        )

        assertTrue(layout.shows(BatteryStatusComponent.BATTERY))
        assertFalse(layout.shows(BatteryStatusComponent.THEME_EMOJI))
    }

    @Test
    fun trailingComponents_followRuntimePhysicalLeftToRightOrder() {
        val allTrailingComponents = linkedSetOf(
            BatteryStatusComponent.HOTSPOT,
            BatteryStatusComponent.CELLULAR,
            BatteryStatusComponent.WIFI,
            BatteryStatusComponent.PERCENTAGE,
            BatteryStatusComponent.BATTERY,
            BatteryStatusComponent.CHARGE
        )

        assertEquals(
            allTrailingComponents.toList(),
            batteryPreviewTrailingOrder(
                BatteryStatusLayoutResult(
                    visibleComponents = allTrailingComponents,
                    hiddenComponents = emptySet()
                )
            )
        )
    }
}
