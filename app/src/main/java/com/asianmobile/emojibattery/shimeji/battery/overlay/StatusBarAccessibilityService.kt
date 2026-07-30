@file:Suppress("DEPRECATION")

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
import android.graphics.Movie
import android.graphics.PixelFormat
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.asianmobile.emojibattery.shimeji.BuildConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry
import com.airbnb.lottie.LottieCompositionFactory
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
import kotlin.math.roundToInt

@AndroidEntryPoint
class StatusBarAccessibilityService : AccessibilityService() {
    @Inject lateinit var catalogRepository: BatteryCatalogRepository
    @Inject lateinit var settingsRepository: BatterySettingsRepository
    @Inject lateinit var editorPreviewSession: BatteryEditorPreviewSession

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windowManager: WindowManager
    private var overlayView: BatteryStatusBarView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var renderJob: Job? = null
    private var observeJob: Job? = null
    private var currentConfig = BatteryStatusConfig()
    private var currentBatteryTheme: BatteryThemeEntry? = null
    private var currentEmojiTheme: BatteryThemeEntry? = null
    private var currentPreviewFocus: BatteryStatusComponent? = null
    private var currentBackgroundPath: String? = null
    private var currentEmotionPath: String? = null
    private var currentAnimationPath: String? = null
    private var loadedAssetKey: String? = null
    private var emojiBitmap: Bitmap? = null
    private var batteryBitmap: Bitmap? = null
    private var backgroundBitmap: Bitmap? = null
    private var emotionBitmap: Bitmap? = null
    private var animatedAsset: BatteryAnimatedAsset? = null
    private var deviceState = BatteryDeviceState()
    private var level = 100
    private var charging = false
    private var receiverRegistered = false
    private var networkCallbackRegistered = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            scope.launch { refreshConnectivity() }
        }

        override fun onLost(network: Network) {
            scope.launch { refreshConnectivity() }
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            scope.launch { refreshConnectivity() }
        }
    }

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
                Intent.ACTION_AIRPLANE_MODE_CHANGED,
                AudioManager.RINGER_MODE_CHANGED_ACTION -> refreshDeviceState()
                WIFI_AP_STATE_CHANGED -> {
                    val state = intent.getIntExtra(WIFI_STATE_EXTRA, WIFI_AP_STATE_DISABLED)
                    deviceState = deviceState.copy(
                        hotspotEnabled = state == WIFI_AP_STATE_ENABLED
                    )
                }
            }
            render()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if (!receiverRegistered) registerSystemReceiver()
        registerNetworkCallback()
        refreshDeviceState()
        observeJob?.cancel()
        observeJob = scope.launch {
            combine(
                settingsRepository.config,
                catalogRepository.snapshot,
                editorPreviewSession.preview
            ) { storedConfig, catalog, preview ->
                val config = preview?.config ?: storedConfig
                BatteryOverlaySources(
                    config = config,
                    batteryTheme = catalog.themes.firstOrNull {
                        it.id == config.selectedBatteryThemeId
                    },
                    emojiTheme = catalog.themes.firstOrNull {
                        it.id == config.selectedEmojiThemeId
                    },
                    backgroundPath = catalog.backgrounds
                        .firstOrNull { it.id == config.backgroundDecorationId }
                        ?.assetPath,
                    emotionPath = catalog.emotions
                        .firstOrNull { it.id == config.emotionDecorationId }
                        ?.assetPath,
                    animationPath = catalog.animations
                        .firstOrNull { it.name == config.animationAssetName }
                        ?.assetPath,
                    previewFocus = preview?.focusedComponent
                )
            }.collect { sources ->
                currentConfig = sources.config
                currentBatteryTheme = sources.batteryTheme
                currentEmojiTheme = sources.emojiTheme
                currentPreviewFocus = sources.previewFocus
                currentBackgroundPath = sources.backgroundPath
                currentEmotionPath = sources.emotionPath
                currentAnimationPath = sources.animationPath
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
        if (networkCallbackRegistered) {
            try {
                connectivityManager().unregisterNetworkCallback(networkCallback)
            } catch (error: RuntimeException) {
                Log.w(TAG, "Network callback was already unregistered", error)
            }
            networkCallbackRegistered = false
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
        animatedAsset = null
        super.onDestroy()
    }

    private fun updateOverlay() {
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (!BuildConfig.BATTERY_STATUS_ENABLED ||
            !currentConfig.enabled ||
            !powerManager.isInteractive ||
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
        val assetKey = listOf(
            currentBatteryTheme?.id,
            currentEmojiTheme?.id,
            currentBackgroundPath,
            if (currentConfig.showEmotion) currentEmotionPath else null,
            if (currentConfig.showAnimation) currentAnimationPath else null
        ).joinToString("|")
        if (loadedAssetKey == assetKey) {
            view.render(
                currentConfig,
                previewDeviceState(),
                level,
                previewCharging(),
                emojiBitmap,
                batteryBitmap,
                backgroundBitmap,
                emotionBitmap,
                animatedAsset
            )
            return
        }
        renderJob?.cancel()
        renderJob = scope.launch {
            val decoded = withContext(Dispatchers.IO) {
                val emojiPath = catalogRepository.materializeAsset(currentEmojiTheme?.emojiPath)
                val batteryPath = catalogRepository.materializeAsset(
                    currentBatteryTheme?.batteryPath
                )
                val backgroundPath = catalogRepository.materializeAsset(currentBackgroundPath)
                val emotionPath = if (currentConfig.showEmotion) {
                    catalogRepository.materializeAsset(currentEmotionPath)
                } else {
                    null
                }
                val animationPath = if (currentConfig.showAnimation) {
                    catalogRepository.materializeAsset(currentAnimationPath)
                } else {
                    null
                }
                DecodedBatteryAssets(
                    emoji = decode(emojiPath),
                    battery = decode(batteryPath),
                    background = decode(backgroundPath),
                    emotion = decode(emotionPath),
                    animation = decodeAnimation(animationPath),
                    complete =
                        (currentEmojiTheme?.emojiPath == null || emojiPath != null) &&
                            (currentBatteryTheme?.batteryPath == null || batteryPath != null) &&
                            (currentBackgroundPath == null || backgroundPath != null) &&
                            (!currentConfig.showEmotion ||
                                currentEmotionPath == null ||
                                emotionPath != null) &&
                            (!currentConfig.showAnimation ||
                                currentAnimationPath == null ||
                                animationPath != null)
                )
            }
            val latestAssetKey = listOf(
                currentBatteryTheme?.id,
                currentEmojiTheme?.id,
                currentBackgroundPath,
                if (currentConfig.showEmotion) currentEmotionPath else null,
                if (currentConfig.showAnimation) currentAnimationPath else null
            ).joinToString("|")
            if (assetKey != latestAssetKey) {
                decoded.recycle()
                return@launch
            }
            if (!decoded.complete) {
                decoded.recycle()
                loadedAssetKey = null
                view.postDelayed(::render, ASSET_RETRY_DELAY_MS)
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
            animatedAsset = decoded.animation
            loadedAssetKey = assetKey
            view.render(
                currentConfig,
                previewDeviceState(),
                level,
                previewCharging(),
                emojiBitmap,
                batteryBitmap,
                backgroundBitmap,
                emotionBitmap,
                animatedAsset
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

    private fun previewDeviceState(): BatteryDeviceState = when (currentPreviewFocus) {
        BatteryStatusComponent.AIRPLANE -> deviceState.copy(
            airplaneMode = true,
            cellularConnected = false
        )
        BatteryStatusComponent.RINGER -> deviceState.copy(ringerMuted = true)
        BatteryStatusComponent.HOTSPOT -> deviceState.copy(hotspotEnabled = true)
        BatteryStatusComponent.CELLULAR -> deviceState.copy(
            cellularConnected = true,
            airplaneMode = false
        )
        BatteryStatusComponent.WIFI -> deviceState.copy(wifiConnected = true)
        else -> deviceState
    }

    private fun previewCharging(): Boolean =
        charging || currentPreviewFocus == BatteryStatusComponent.CHARGE

    private fun createLayoutParams(config: BatteryStatusConfig): WindowManager.LayoutParams {
        val density = resources.displayMetrics.density
        val barHeight = resolveBatteryStatusBarHeightPx(config.barHeightDp, density)
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
            y = 0
            title = getString(
                com.asianmobile.emojibattery.shimeji.R.string.battery_accessibility_service_label
            )
        }
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
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(WIFI_AP_STATE_CHANGED)
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
            } catch (error: RuntimeException) {
                Log.w(TAG, "Packaged battery asset is invalid", error)
                null
            }
        }
        return File(path).takeIf(File::isFile)?.let {
            BitmapFactory.decodeFile(it.absolutePath)
        }
    }

    private fun decodeAnimation(path: String?): BatteryAnimatedAsset? {
        if (path == null) return null
        return openPath(path) { input ->
            when {
                path.endsWith(".gif", ignoreCase = true) ->
                    BatteryAnimatedAsset(movie = Movie.decodeStream(input))
                path.endsWith(".json", ignoreCase = true) ->
                    BatteryAnimatedAsset(
                        lottieComposition = LottieCompositionFactory
                            .fromJsonInputStreamSync(input, path)
                            .value
                    )
                else -> null
            }
        }
    }

    private fun <T> openPath(path: String, block: (java.io.InputStream) -> T): T? {
        return try {
            if (path.startsWith(ANDROID_ASSET_URI_PREFIX)) {
                assets.open(path.removePrefix(ANDROID_ASSET_URI_PREFIX)).use(block)
            } else {
                File(path).takeIf(File::isFile)?.inputStream()?.buffered()?.use(block)
            }
        } catch (error: java.io.IOException) {
            Log.w(TAG, "Unable to decode Battery animation", error)
            null
        } catch (error: RuntimeException) {
            Log.w(TAG, "Battery animation is invalid", error)
            null
        }
    }

    private fun registerNetworkCallback() {
        if (networkCallbackRegistered) return
        try {
            connectivityManager().registerDefaultNetworkCallback(networkCallback)
            networkCallbackRegistered = true
        } catch (error: RuntimeException) {
            Log.w(TAG, "Unable to register network callback", error)
        }
    }

    private fun refreshConnectivity() {
        val manager = connectivityManager()
        val capabilities = manager.activeNetwork?.let(manager::getNetworkCapabilities)
        deviceState = deviceState.copy(
            wifiConnected = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true,
            cellularConnected =
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        )
        render()
    }

    private fun refreshDeviceState() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        deviceState = deviceState.copy(
            airplaneMode = Settings.Global.getInt(
                contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,
                0
            ) == 1,
            ringerMuted = audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL
        )
        refreshConnectivity()
    }

    private fun connectivityManager(): ConnectivityManager =
        getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

    private companion object {
        const val TAG = "BatteryStatusService"
        const val ANDROID_ASSET_URI_PREFIX = "file:///android_asset/"
        const val WIFI_AP_STATE_CHANGED = "android.net.wifi.WIFI_AP_STATE_CHANGED"
        const val WIFI_STATE_EXTRA = "wifi_state"
        const val WIFI_AP_STATE_DISABLED = 11
        const val WIFI_AP_STATE_ENABLED = 13
        const val ASSET_RETRY_DELAY_MS = 5_000L
    }
}

internal fun resolveBatteryStatusBarHeightPx(
    barHeightDp: Float,
    density: Float
): Int {
    if (!barHeightDp.isFinite() || !density.isFinite() || density <= 0f) return 1
    return (barHeightDp * density).roundToInt().coerceAtLeast(1)
}

private data class BatteryOverlaySources(
    val config: BatteryStatusConfig,
    val batteryTheme: BatteryThemeEntry?,
    val emojiTheme: BatteryThemeEntry?,
    val backgroundPath: String?,
    val emotionPath: String?,
    val animationPath: String?,
    val previewFocus: BatteryStatusComponent?
)

private data class DecodedBatteryAssets(
    val emoji: Bitmap?,
    val battery: Bitmap?,
    val background: Bitmap?,
    val emotion: Bitmap?,
    val animation: BatteryAnimatedAsset?,
    val complete: Boolean
) {
    fun recycle() {
        emoji?.recycle()
        battery?.recycle()
        background?.recycle()
        emotion?.recycle()
    }
}
