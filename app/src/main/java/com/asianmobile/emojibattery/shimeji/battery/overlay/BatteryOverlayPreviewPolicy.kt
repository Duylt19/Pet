package com.asianmobile.emojibattery.shimeji.battery.overlay

import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig

internal data class BatteryOverlayPreviewSource(
    val config: BatteryStatusConfig,
    val focusedComponent: BatteryStatusComponent?
)

/**
 * Keeps the persisted activation state authoritative while an editor draft is visible.
 *
 * The editor may replace the visual properties of an already active status bar, but opening the
 * editor must never turn a disabled status bar into an Accessibility overlay.
 */
internal fun resolveBatteryOverlayPreviewSource(
    storedConfig: BatteryStatusConfig,
    preview: BatteryEditorPreview?
): BatteryOverlayPreviewSource {
    if (!storedConfig.enabled || preview == null) {
        return BatteryOverlayPreviewSource(
            config = storedConfig,
            focusedComponent = null
        )
    }
    return BatteryOverlayPreviewSource(
        config = preview.config.copy(enabled = true),
        focusedComponent = preview.focusedComponent
    )
}
