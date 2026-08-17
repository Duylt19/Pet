package com.asianmobile.emojibattery.shimeji.battery.overlay

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryAppExclusionPolicyTest {
    @Test
    fun `system UI event retains hidden foreground app`() {
        val resolved = BatteryAppExclusionPolicy.resolveForegroundPackage(
            currentForegroundPackage = "com.example.immersive",
            eventPackage = "com.android.systemui",
            transientWindowPackages = setOf("com.android.systemui")
        )

        assertTrue(
            BatteryAppExclusionPolicy.shouldHide(
                foregroundPackage = resolved,
                hiddenAppPackages = setOf("com.example.immersive")
            )
        )
    }

    @Test
    fun `keyboard event retains hidden foreground app`() {
        val resolved = BatteryAppExclusionPolicy.resolveForegroundPackage(
            currentForegroundPackage = "com.example.immersive",
            eventPackage = "com.example.keyboard",
            transientWindowPackages = setOf("com.android.systemui", "com.example.keyboard")
        )

        assertTrue(
            BatteryAppExclusionPolicy.shouldHide(
                foregroundPackage = resolved,
                hiddenAppPackages = setOf("com.example.immersive")
            )
        )
    }

    @Test
    fun `vendor transient window retains hidden foreground app`() {
        val resolved = BatteryAppExclusionPolicy.resolveForegroundPackage(
            currentForegroundPackage = "com.example.immersive",
            eventPackage = "com.vendor.edgepanel",
            transientWindowPackages = setOf(
                "com.android.systemui",
                "com.vendor.edgepanel"
            )
        )

        assertTrue(
            BatteryAppExclusionPolicy.shouldHide(
                foregroundPackage = resolved,
                hiddenAppPackages = setOf("com.example.immersive")
            )
        )
    }

    @Test
    fun `launcher framework window replaces hidden foreground app`() {
        val resolved = BatteryAppExclusionPolicy.resolveForegroundPackage(
            currentForegroundPackage = "com.example.immersive",
            eventPackage = "com.vendor.launcher",
            transientWindowPackages = setOf(
                "com.android.systemui",
                "com.vendor.edgepanel"
            )
        )

        assertFalse(
            BatteryAppExclusionPolicy.shouldHide(
                foregroundPackage = resolved,
                hiddenAppPackages = setOf("com.example.immersive")
            )
        )
    }

    @Test
    fun `real app transition replaces foreground package`() {
        val resolved = BatteryAppExclusionPolicy.resolveForegroundPackage(
            currentForegroundPackage = "com.example.immersive",
            eventPackage = "com.example.reader",
            transientWindowPackages = setOf("com.android.systemui")
        )

        assertFalse(
            BatteryAppExclusionPolicy.shouldHide(
                foregroundPackage = resolved,
                hiddenAppPackages = setOf("com.example.immersive")
            )
        )
    }

    @Test
    fun `blank event retains foreground package`() {
        val resolved = BatteryAppExclusionPolicy.resolveForegroundPackage(
            currentForegroundPackage = "com.example.immersive",
            eventPackage = " ",
            transientWindowPackages = setOf("com.android.systemui")
        )

        assertTrue(resolved == "com.example.immersive")
    }

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
