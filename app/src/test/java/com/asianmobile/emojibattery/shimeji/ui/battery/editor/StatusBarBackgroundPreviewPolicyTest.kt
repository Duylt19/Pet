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

    @Test
    fun `scrolls when selected preview is missing or clipped by viewport`() {
        assertEquals(
            true,
            shouldScrollToStatusBarBackgroundSelection(
                itemOffset = null,
                itemSize = null,
                viewportStartOffset = 0,
                viewportEndOffset = 336
            )
        )
        assertEquals(
            true,
            shouldScrollToStatusBarBackgroundSelection(
                itemOffset = 300,
                itemSize = 77,
                viewportStartOffset = 0,
                viewportEndOffset = 336
            )
        )
    }

    @Test
    fun `keeps list position when selected preview is fully visible`() {
        assertEquals(
            false,
            shouldScrollToStatusBarBackgroundSelection(
                itemOffset = 172,
                itemSize = 77,
                viewportStartOffset = 0,
                viewportEndOffset = 336
            )
        )
    }
}
