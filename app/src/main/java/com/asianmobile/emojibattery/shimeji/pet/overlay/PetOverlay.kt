package com.asianmobile.emojibattery.shimeji.pet.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
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

object PetOverlay {
    fun canDraw(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun permissionIntent(context: Context): Intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    )

    fun start(context: Context): PetOverlayStartResult {
        if (!canDraw(context)) return PetOverlayStartResult.PERMISSION_REQUIRED
        return runCatching {
            ContextCompat.startForegroundService(
                context,
                PetOverlayService.startIntent(context)
            )
            PetOverlayStartResult.START_REQUESTED
        }.getOrDefault(PetOverlayStartResult.FAILED)
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, PetOverlayService::class.java))
    }
}

object PetOverlayRuntime {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    private val _activePetCount = MutableStateFlow(0)
    val activePetCount: StateFlow<Int> = _activePetCount.asStateFlow()

    internal fun updateRunning(running: Boolean, petCount: Int = 0) {
        _isRunning.value = running
        _activePetCount.value = if (running) petCount.coerceAtLeast(0) else 0
    }
}
