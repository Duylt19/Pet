package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import com.asianmobile.emojibattery.shimeji.data.model.BatteryAnimationEntry
import com.asianmobile.emojibattery.shimeji.data.model.BatteryAnimationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryAnimationPreviewPolicyTest {
    @Test
    fun `remote Lottie requires a local preview file`() {
        assertTrue(BatteryAnimationPreviewPolicy.requiresLocalFile(animation(
            path = "https://example.com/cute.json",
            type = BatteryAnimationType.LOTTIE
        )))
        assertFalse(BatteryAnimationPreviewPolicy.requiresLocalFile(animation(
            path = "https://example.com/cute.gif",
            type = BatteryAnimationType.GIF
        )))
        assertFalse(BatteryAnimationPreviewPolicy.requiresLocalFile(animation(
            path = "/data/user/0/app/files/cute.json",
            type = BatteryAnimationType.LOTTIE
        )))
    }

    @Test
    fun `local file replaces only its matching remote asset`() {
        val remote = animation(
            path = "https://example.com/cute.json",
            type = BatteryAnimationType.LOTTIE
        )
        val other = animation(
            id = 2,
            name = "other.json",
            path = "https://example.com/other.json",
            type = BatteryAnimationType.LOTTIE
        )

        val resolved = BatteryAnimationPreviewPolicy.applyLocalFiles(
            listOf(remote, other),
            mapOf(remote.assetPath to "/data/user/0/app/files/cute.json")
        )

        assertEquals("/data/user/0/app/files/cute.json", resolved[0].assetPath)
        assertEquals(other, resolved[1])
    }

    private fun animation(
        id: Int = 1,
        name: String = "cute.json",
        path: String,
        type: BatteryAnimationType
    ) = BatteryAnimationEntry(id = id, name = name, assetPath = path, type = type)
}
