package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import com.asianmobile.emojibattery.shimeji.data.model.BatteryDecorationEntry
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDecorationType
import org.junit.Assert.assertEquals
import org.junit.Test

class StatusBarBackgroundPreviewPolicyTest {
    private val backgrounds = (1..5).map { id ->
        BatteryDecorationEntry(
            id = id,
            name = "Background $id",
            assetPath = "background_$id.png",
            type = BatteryDecorationType.BACKGROUND
        )
    }

    @Test
    fun `keeps the first three backgrounds stable when selected item is already visible`() {
        assertEquals(
            listOf(1, 2, 3),
            statusBarBackgroundPreviewItems(backgrounds, selectedId = 2).map { it.id }
        )
    }

    @Test
    fun `includes an offscreen selected background without reordering visible choices`() {
        assertEquals(
            listOf(1, 2, 5),
            statusBarBackgroundPreviewItems(backgrounds, selectedId = 5).map { it.id }
        )
    }
}
