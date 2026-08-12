package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import com.asianmobile.emojibattery.shimeji.data.model.BatteryDecorationEntry
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDecorationType
import org.junit.Assert.assertEquals
import org.junit.Test

class StatusBarBackgroundPreviewPolicyTest {
    private val backgrounds = (1..7).map { id ->
        BatteryDecorationEntry(
            id = id,
            name = "Background $id",
            assetPath = "background_$id.png",
            type = BatteryDecorationType.BACKGROUND
        )
    }

    @Test
    fun `keeps the first five backgrounds stable when selected item is already visible`() {
        assertEquals(
            listOf(1, 2, 3, 4, 5),
            statusBarBackgroundPreviewItems(backgrounds, selectedId = 2).map { it.id }
        )
    }

    @Test
    fun `includes an offscreen selected background without reordering visible choices`() {
        assertEquals(
            listOf(1, 2, 3, 4, 7),
            statusBarBackgroundPreviewItems(backgrounds, selectedId = 7).map { it.id }
        )
    }
}
