package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryEditorLivePreviewPolicyTest {
    @Test
    fun activeStoredFeature_withVisibleEditor_publishesPreview() {
        assertTrue(
            BatteryEditorLivePreviewPolicy.shouldPublish(
                storedEnabled = true,
                previewClientCount = 1
            )
        )
    }

    @Test
    fun disabledStoredFeature_neverPublishesPreview() {
        assertFalse(
            BatteryEditorLivePreviewPolicy.shouldPublish(
                storedEnabled = false,
                previewClientCount = 1
            )
        )
    }

    @Test
    fun editorWithoutVisibleClient_doesNotPublishPreview() {
        assertFalse(
            BatteryEditorLivePreviewPolicy.shouldPublish(
                storedEnabled = true,
                previewClientCount = 0
            )
        )
    }
}
