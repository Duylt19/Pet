package com.asianmobile.emojibattery.shimeji.battery.overlay

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryAppExclusionPolicyTest {
    @Test
    fun `hidden foreground app suppresses battery overlay`() {
        assertTrue(
            BatteryAppExclusionPolicy.shouldHide(
                foregroundPackage = "com.example.video",
                hiddenAppPackages = setOf("com.example.video")
            )
        )
    }

    @Test
    fun `other foreground app keeps battery overlay visible`() {
        assertFalse(
            BatteryAppExclusionPolicy.shouldHide(
                foregroundPackage = "com.example.reader",
                hiddenAppPackages = setOf("com.example.video")
            )
        )
    }

    @Test
    fun `unknown foreground app keeps battery overlay visible`() {
        assertFalse(
            BatteryAppExclusionPolicy.shouldHide(
                foregroundPackage = null,
                hiddenAppPackages = setOf("com.example.video")
            )
        )
    }
}
