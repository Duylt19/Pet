package com.asianmobile.emojibattery.shimeji.battery.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Supplies real conditional system state to the embedded editor preview. */
@Singleton
class BatteryEditorSystemStateMonitor @Inject constructor(
    @ApplicationContext context: Context
) {
    private val appContext = context.applicationContext
    private val _state = MutableStateFlow(BatteryPreviewSystemState())
    val state: StateFlow<BatteryPreviewSystemState> = _state.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_BATTERY_CHANGED -> updatePower(intent)
                Intent.ACTION_AIRPLANE_MODE_CHANGED,
                AudioManager.RINGER_MODE_CHANGED_ACTION -> refreshDeviceState()
                WIFI_AP_STATE_CHANGED -> {
                    val hotspot = BatterySystemStatusPolicy.hotspot(
                        intent.getIntExtra(WIFI_STATE_EXTRA, WIFI_AP_STATE_DISABLED)
                    )
                    _state.value = _state.value.copy(
                        deviceState = _state.value.deviceState.copy(hotspot = hotspot)
                    )
                }
            }
        }
    }

    init {
        refreshDeviceState()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(WIFI_AP_STATE_CHANGED)
        }
        val sticky = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            appContext.registerReceiver(receiver, filter)
        }
        sticky?.takeIf { it.action == Intent.ACTION_BATTERY_CHANGED }?.let(::updatePower)
    }

    private fun refreshDeviceState() {
        val airplaneMode = runCatching {
            Settings.Global.getInt(
                appContext.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,
                0
            ) == 1
        }.getOrDefault(false)
        val ringer = runCatching {
            val audioManager = appContext.getSystemService(AudioManager::class.java)
            BatterySystemStatusPolicy.ringer(
                audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
            )
        }.getOrDefault(BatteryRingerState.NORMAL)
        _state.value = _state.value.copy(
            deviceState = _state.value.deviceState.copy(
                airplaneMode = airplaneMode,
                ringer = ringer
            )
        )
    }

    private fun updatePower(intent: Intent) {
        val rawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        _state.value = _state.value.copy(
            powerState = BatteryPowerState(
                level = (rawLevel * 100 / scale).coerceIn(0, 100),
                chargeState = BatterySystemStatusPolicy.charge(
                    intent.getIntExtra(
                        BatteryManager.EXTRA_STATUS,
                        BatteryManager.BATTERY_STATUS_UNKNOWN
                    )
                ),
                plugType = BatterySystemStatusPolicy.plug(
                    intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                ),
                present = intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true)
            )
        )
    }

    private companion object {
        const val WIFI_AP_STATE_CHANGED = "android.net.wifi.WIFI_AP_STATE_CHANGED"
        const val WIFI_STATE_EXTRA = "wifi_state"
        const val WIFI_AP_STATE_DISABLED = 11
    }
}
