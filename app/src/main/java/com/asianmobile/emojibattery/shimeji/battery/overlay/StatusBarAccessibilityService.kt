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
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusDisplayMode
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
            combine(settingsRepository.config, catalogRepository.snapshot) { config, catalog ->
                BatteryOverlaySources(
                    config = config,
                    theme = catalog.themes.firstOrNull { it.id == config.selectedThemeId },
                    backgroundPath = catalog.backgrounds
                        .firstOrNull { it.id == config.backgroundDecorationId }
                        ?.assetPath,
                    emotionPath = catalog.emotions
                        .firstOrNull { it.id == config.emotionDecorationId }
                        ?.assetPath,
                    animationPath = catalog.animations
                        .firstOrNull { it.name == config.animationAssetName }
                        ?.assetPath
                )
            }.collect { sources ->
                currentConfig = sources.config
                currentTheme = sources.theme
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
            if (currentConfig.showEmotion) currentEmotionPath else null,
            if (currentConfig.showAnimation) currentAnimationPath else null
        ).joinToString("|")
        if (loadedAssetKey == assetKey) {
            view.render(
                currentConfig,
                deviceState,
                level,
                charging,
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
                DecodedBatteryAssets(
                    emoji = decode(theme?.emojiPath),
                    battery = decode(theme?.batteryPath),
                    background = decode(currentBackgroundPath),
                    emotion = if (currentConfig.showEmotion) {
                        decode(currentEmotionPath)
                    } else {
                        null
                    },
                    animation = if (currentConfig.showAnimation) {
                        decodeAnimation(currentAnimationPath)
                    } else null
                )
            }
            val latestAssetKey = listOf(
                currentTheme?.id,
                currentBackgroundPath,
                if (currentConfig.showEmotion) currentEmotionPath else null,
                if (currentConfig.showAnimation) currentAnimationPath else null
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
            animatedAsset = decoded.animation
            loadedAssetKey = assetKey
            view.render(
                currentConfig,
                deviceState,
                level,
                charging,
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
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true,
            signalLevel = if (
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
            ) 4 else 0
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
    }
}

private data class BatteryOverlaySources(
    val config: BatteryStatusConfig,
    val theme: BatteryThemeEntry?,
    val backgroundPath: String?,
    val emotionPath: String?,
    val animationPath: String?
)

private data class DecodedBatteryAssets(
    val emoji: Bitmap?,
    val battery: Bitmap?,
    val background: Bitmap?,
    val emotion: Bitmap?,
    val animation: BatteryAnimatedAsset?
) {
    fun recycle() {
        emoji?.recycle()
        battery?.recycle()
        background?.recycle()
        emotion?.recycle()
    }
}
