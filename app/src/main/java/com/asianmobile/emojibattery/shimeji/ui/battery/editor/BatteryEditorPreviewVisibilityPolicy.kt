package com.asianmobile.emojibattery.shimeji.ui.battery.editor

internal object BatteryEditorPreviewVisibilityPolicy {
    fun shouldShow(
        accessibilityEnabled: Boolean,
        statusBarEnabled: Boolean
    ): Boolean = !accessibilityEnabled || !statusBarEnabled
}
