package com.asianmobile.emojibattery.shimeji.battery.overlay

import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BatteryEditorPreview(
    val ownerId: String,
    val config: BatteryStatusConfig,
    val focusedComponent: BatteryStatusComponent? = null
)

/**
 * Process-local bridge between the editor draft and the Accessibility overlay.
 *
 * DataStore remains the source of applied settings. A preview exists only while its editor owner
 * is visible and is discarded without persistence when that owner leaves.
 */
@Singleton
class BatteryEditorPreviewSession @Inject constructor() {
    private val _preview = MutableStateFlow<BatteryEditorPreview?>(null)
    val preview: StateFlow<BatteryEditorPreview?> = _preview.asStateFlow()

    fun start(ownerId: String, config: BatteryStatusConfig) {
        val focusedComponent = _preview.value
            ?.takeIf { it.ownerId == ownerId }
            ?.focusedComponent
        _preview.value = BatteryEditorPreview(
            ownerId = ownerId,
            config = config.copy(enabled = true),
            focusedComponent = focusedComponent
        )
    }

    fun update(
        ownerId: String,
        config: BatteryStatusConfig,
        focusedComponent: BatteryStatusComponent?
    ) {
        val current = _preview.value
        if (current != null && current.ownerId != ownerId) return
        _preview.value = BatteryEditorPreview(
            ownerId = ownerId,
            config = config.copy(enabled = true),
            focusedComponent = focusedComponent
        )
    }

    fun stop(ownerId: String) {
        if (_preview.value?.ownerId == ownerId) _preview.value = null
    }
}
