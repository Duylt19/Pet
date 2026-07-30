package com.asianmobile.emojibattery.shimeji.battery.overlay

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusDisplayMode
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry
import com.asianmobile.emojibattery.shimeji.data.repository.BatteryCatalogRepository
import com.asianmobile.emojibattery.shimeji.data.repository.BatterySettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class StatusBarAccessibilityService : AccessibilityService() {
    @Inject lateinit var catalogRepository: BatteryCatalogRepository
    @Inject lateinit var settingsRepository: BatterySettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windowManager: WindowManager
    private var overlayView: BatteryStatusBarView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var renderJob: Job? = null
    private var observeJob: Job? = null
    private var currentConfig = BatteryStatusConfig()
    private var currentTheme: BatteryThemeEntry? = null
    private var currentBackgroundPath: String? = null
    private var currentEmotionPath: String? = null
    private var loadedAssetKey: String? = null
    private var emojiBitmap: Bitmap? = null
    private var batteryBitmap: Bitmap? = null
    private var backgroundBitmap: Bitmap? = null
    private var emotionBitmap: Bitmap? = null
    private var level = 100
    private var charging = false
    private var receiverRegistered = false

    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    val rawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                    level = (rawLevel * 100 / scale).coerceIn(0, 100)
                    charging = intent.getIntExtra(
                        BatteryManager.EXTRA_STATUS,
                        BatteryManager.BATTERY_STATUS_UNKNOWN
                    ) in setOf(
                        BatteryManager.BATTERY_STATUS_CHARGING,
                        BatteryManager.BATTERY_STATUS_FULL
                    )
                }
                Intent.ACTION_SCREEN_OFF,
                Intent.ACTION_SCREEN_ON,
                Intent.ACTION_USER_PRESENT -> {
                    updateOverlay()
                    return
                }
            }
            render()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if (!receiverRegistered) registerSystemReceiver()
        observeJob?.cancel()
        observeJob = scope.launch {
            combine(settingsRepository.config, catalogRepository.snapshot) { config, catalog ->
                BatteryOverlaySources(
                    config = config,
                    theme = catalog.themes.firstOrNull { it.id == config.selectedThemeId },
                    backgroundPath = catalog.backgrounds
                        .firstOrNull { it.id == config.backgroundDecorationId }
                        ?.assetPath,
                    emotionPath = catalog.emotions
                        .firstOrNull { it.id == config.emotionDecorationId }
                        ?.assetPath
                )
            }.collect { sources ->
                currentConfig = sources.config
                currentTheme = sources.theme
                currentBackgroundPath = sources.backgroundPath
                currentEmotionPath = sources.emotionPath
                updateOverlay()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateOverlay()
    }

    override fun onDestroy() {
        removeOverlay()
        observeJob?.cancel()
        if (receiverRegistered) {
            try {
                unregisterReceiver(systemReceiver)
            } catch (error: IllegalArgumentException) {
                Log.w(TAG, "Battery receiver was already unregistered", error)
            }
            receiverRegistered = false
        }
        scope.cancel()
        emojiBitmap?.recycle()
        batteryBitmap?.recycle()
        backgroundBitmap?.recycle()
        emotionBitmap?.recycle()
        emojiBitmap = null
        batteryBitmap = null
        backgroundBitmap = null
        emotionBitmap = null
        super.onDestroy()
    }

    private fun updateOverlay() {
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        if (!currentConfig.enabled ||
            keyguardManager.isKeyguardLocked ||
            resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT
        ) {
            removeOverlay()
            return
        }
        val view = overlayView ?: run {
            val newView = BatteryStatusBarView(this)
            val newParams = createLayoutParams(currentConfig)
            try {
                windowManager.addView(newView, newParams)
            } catch (error: RuntimeException) {
                Log.e(TAG, "Unable to attach battery accessibility overlay", error)
                return
            }
            overlayView = newView
            layoutParams = newParams
            newView
        }
        val params = layoutParams ?: return
        val updated = createLayoutParams(currentConfig)
        params.height = updated.height
        params.y = updated.y
        try {
            windowManager.updateViewLayout(view, params)
        } catch (error: RuntimeException) {
            Log.e(TAG, "Unable to update battery accessibility overlay", error)
            removeOverlay()
            return
        }
        render()
    }

    private fun render() {
        val view = overlayView ?: return
        val theme = currentTheme
        val assetKey = listOf(
            theme?.id,
            currentBackgroundPath,
            if (currentConfig.showEmotion) currentEmotionPath else null
        ).joinToString("|")
        if (loadedAssetKey == assetKey) {
            view.render(
                currentConfig,
                level,
                charging,
                emojiBitmap,
                batteryBitmap,
                backgroundBitmap,
                emotionBitmap
            )
            return
        }
        renderJob?.cancel()
        renderJob = scope.launch {
            val decoded = withContext(Dispatchers.IO) {
                DecodedBatteryAssets(
                    emoji = decode(theme?.emojiPath),
                    battery = decode(theme?.batteryPath),
                    background = decode(currentBackgroundPath),
                    emotion = if (currentConfig.showEmotion) {
                        decode(currentEmotionPath)
                    } else {
                        null
                    }
                )
            }
            val latestAssetKey = listOf(
                currentTheme?.id,
                currentBackgroundPath,
                if (currentConfig.showEmotion) currentEmotionPath else null
            ).joinToString("|")
            if (assetKey != latestAssetKey) {
                decoded.recycle()
                return@launch
            }
            emojiBitmap?.recycle()
            batteryBitmap?.recycle()
            backgroundBitmap?.recycle()
            emotionBitmap?.recycle()
            emojiBitmap = decoded.emoji
            batteryBitmap = decoded.battery
            backgroundBitmap = decoded.background
            emotionBitmap = decoded.emotion
            loadedAssetKey = assetKey
            view.render(
                currentConfig,
                level,
                charging,
                emojiBitmap,
                batteryBitmap,
                backgroundBitmap,
                emotionBitmap
            )
        }
    }

    private fun removeOverlay() {
        renderJob?.cancel()
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (error: IllegalArgumentException) {
                Log.w(TAG, "Battery overlay was already detached", error)
            }
        }
        overlayView = null
        layoutParams = null
    }

    private fun createLayoutParams(config: BatteryStatusConfig): WindowManager.LayoutParams {
        val density = resources.displayMetrics.density
        val statusHeight = statusBarHeight()
        val barHeight = (config.barHeightDp * density).toInt().coerceAtLeast(statusHeight)
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            barHeight,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            y = if (config.displayMode == BatteryStatusDisplayMode.BELOW_SYSTEM_BAR) {
                statusHeight
            } else {
                0
            }
            title = getString(
                com.asianmobile.emojibattery.shimeji.R.string.battery_accessibility_service_label
            )
        }
    }

    private fun statusBarHeight(): Int {
        val identifier = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (identifier != 0) resources.getDimensionPixelSize(identifier) else 0
    }

    private fun registerSystemReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        val sticky = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(systemReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(systemReceiver, filter)
        }
        receiverRegistered = true
        sticky?.let { systemReceiver.onReceive(this, it) }
    }

    private fun decode(path: String?): Bitmap? {
        if (path == null) return null
        if (path.startsWith(ANDROID_ASSET_URI_PREFIX)) {
            val assetPath = path.removePrefix(ANDROID_ASSET_URI_PREFIX)
            return try {
                assets.open(assetPath).use(BitmapFactory::decodeStream)
            } catch (error: java.io.IOException) {
                Log.w(TAG, "Unable to decode packaged battery asset", error)
                null
            }
        }
        return File(path).takeIf(File::isFile)?.let {
            BitmapFactory.decodeFile(it.absolutePath)
        }
    }

    private companion object {
        const val TAG = "BatteryStatusService"
        const val ANDROID_ASSET_URI_PREFIX = "file:///android_asset/"
    }
}

private data class BatteryOverlaySources(
    val config: BatteryStatusConfig,
    val theme: BatteryThemeEntry?,
    val backgroundPath: String?,
    val emotionPath: String?
)

private data class DecodedBatteryAssets(
    val emoji: Bitmap?,
    val battery: Bitmap?,
    val background: Bitmap?,
    val emotion: Bitmap?
) {
    fun recycle() {
        emoji?.recycle()
        battery?.recycle()
        background?.recycle()
        emotion?.recycle()
    }
}
