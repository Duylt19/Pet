package com.asianmobile.emojibattery.shimeji.pet.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PetOverlayStartResult {
    START_REQUESTED,
    PERMISSION_REQUIRED,
    FAILED
}

enum class PetOverlayRuntimeState {
    STOPPED,
    STARTING,
    RUNNING;

    val isEnabled: Boolean
        get() = this != STOPPED
}

object PetOverlay {
    fun canDraw(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun permissionIntent(context: Context): Intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    )

    fun start(context: Context): PetOverlayStartResult {
        if (!canDraw(context)) return PetOverlayStartResult.PERMISSION_REQUIRED
        if (PetOverlayRuntime.state.value.isEnabled) {
            return PetOverlayStartResult.START_REQUESTED
        }
        PetOverlayRuntime.updateStartRequested()
        return runCatching {
            ContextCompat.startForegroundService(
                context,
                PetOverlayService.startIntent(context)
            )
            PetOverlayStartResult.START_REQUESTED
        }.getOrElse {
            PetOverlayRuntime.updateRunning(false)
            PetOverlayStartResult.FAILED
        }
    }

    fun stop(context: Context) {
        PetOverlayRuntime.updateStopRequested()
        context.stopService(Intent(context, PetOverlayService::class.java))
    }
}

object PetOverlayRuntime {
    private val handler = Handler(Looper.getMainLooper())
    private val startTimeout = Runnable {
        if (_state.value == PetOverlayRuntimeState.STARTING) updateRunning(false)
    }
    private val _state = MutableStateFlow(PetOverlayRuntimeState.STOPPED)
    val state: StateFlow<PetOverlayRuntimeState> = _state.asStateFlow()
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    private val _activePetCount = MutableStateFlow(0)
    val activePetCount: StateFlow<Int> = _activePetCount.asStateFlow()

    internal fun updateStartRequested() {
        _state.value = PetOverlayRuntimeState.STARTING
        handler.removeCallbacks(startTimeout)
        handler.postDelayed(startTimeout, START_TIMEOUT_MILLIS)
    }

    internal fun updateStopRequested() {
        handler.removeCallbacks(startTimeout)
        _state.value = PetOverlayRuntimeState.STOPPED
    }

    internal fun updateRunning(running: Boolean, petCount: Int = 0) {
        handler.removeCallbacks(startTimeout)
        if (running && !_state.value.isEnabled) return
        _isRunning.value = running
        _activePetCount.value = if (running) petCount.coerceAtLeast(0) else 0
        _state.value = if (running) {
            PetOverlayRuntimeState.RUNNING
        } else {
            PetOverlayRuntimeState.STOPPED
        }
    }

    private const val START_TIMEOUT_MILLIS = 20_000L
}

internal fun PetOverlayRuntimeState.shouldStartService(): Boolean = when (this) {
    PetOverlayRuntimeState.STARTING,
    PetOverlayRuntimeState.RUNNING -> true
    PetOverlayRuntimeState.STOPPED -> false
}
