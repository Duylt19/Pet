package com.asianmobile.emojibattery.shimeji.ui.battery.editor

internal object BatteryEditorLivePreviewPolicy {
    fun shouldPublish(
        storedEnabled: Boolean,
        previewClientCount: Int
    ): Boolean = storedEnabled && previewClientCount > 0
}
