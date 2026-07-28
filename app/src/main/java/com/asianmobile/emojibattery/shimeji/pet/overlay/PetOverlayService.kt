package com.asianmobile.emojibattery.shimeji.pet.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.asianmobile.emojibattery.shimeji.MainActivity
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogSnapshot
import com.asianmobile.emojibattery.shimeji.data.model.PetPreferences
import com.asianmobile.emojibattery.shimeji.data.repository.OwnerPetCatalogRepository
import com.asianmobile.emojibattery.shimeji.data.repository.PetSettingsRepository
import com.asianmobile.emojibattery.shimeji.pet.pack.PetBitmapCache
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackRepository
import com.asianmobile.emojibattery.shimeji.pet.settings.PetSettingsPolicy
import com.asianmobile.emojibattery.shimeji.pet.speech.OwnerPetSpeechAnchorPolicy
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@AndroidEntryPoint
class PetOverlayService : Service() {
    @Inject lateinit var petPackRepository: PetPackRepository
    @Inject lateinit var petBitmapCache: PetBitmapCache
    @Inject lateinit var petSettingsRepository: PetSettingsRepository
    @Inject lateinit var ownerPetCatalogRepository: OwnerPetCatalogRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val settingsPolicy = PetSettingsPolicy()
    private var overlayStartJob: Job? = null
    private var liveSettingsJob: Job? = null
    private var overlayController: PetOverlayController? = null
    private var sessionPositionResetRevisions: List<Int>? = null
    private var activeSessionSignature: PetSessionSignature? = null
    private var isScreenReceiverRegistered = false
    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> overlayController?.pauseRendering()
                Intent.ACTION_SCREEN_ON -> overlayController?.resumeRendering()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerScreenStateReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        promoteToForeground()
        if (!PetOverlay.canDraw(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (overlayController == null && overlayStartJob == null) {
            overlayStartJob = serviceScope.launch {
                startOverlay()
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun startOverlay() {
        try {
            val preferences = petSettingsRepository.preferences.value
            sessionPositionResetRevisions = preferences.positionResetRevisions
            val catalog = ownerPetCatalogRepository.snapshot.value.let { current ->
                if (!current.isLoading) {
                    current
                } else {
                    withTimeoutOrNull(CATALOG_WAIT_MILLIS) {
                        ownerPetCatalogRepository.snapshot.first { !it.isLoading }
                    } ?: ownerPetCatalogRepository.snapshot.value
                }
            }
            overlayController = createOverlayController(preferences, catalog).also { controller ->
                controller.start()
                if (!getSystemService(PowerManager::class.java).isInteractive) {
                    controller.pauseRendering()
                }
            }
            activeSessionSignature = preferences.sessionSignature()
            observeLiveSettings()
            PetOverlayRuntime.updateRunning(true, preferences.petCount)
        } catch (error: CancellationException) {
            throw error
        } catch (error: RuntimeException) {
            Log.e(TAG, "Unable to start pet overlay", error)
            overlayController?.stop()
            overlayController = null
            stopSelf()
        }
    }

    private fun observeLiveSettings() {
        liveSettingsJob?.cancel()
        liveSettingsJob = serviceScope.launch {
            petSettingsRepository.preferences
                .collect { preferences ->
                    if (activeSessionSignature != preferences.sessionSignature()) {
                        restartOverlay(preferences)
                        return@collect
                    }

                    val controller = overlayController ?: return@collect
                    val previousResetRevisions =
                        sessionPositionResetRevisions.orEmpty()
                    val resetSlots = settingsPolicy.changedPositionResetSlots(
                        previousRevisions = previousResetRevisions,
                        currentRevisions = preferences.positionResetRevisions,
                        petCount = preferences.petCount
                    )
                    controller.updateSizePercents(
                        preferences.petSlots.map { it.sizePercent }
                    )
                    controller.updateSpeedPercents(
                        preferences.petSlots.map { it.speedPercent }
                    )
                    controller.updateInteractionSettings(preferences.petSlots)
                    controller.updateSpeechSettings(preferences.petSlots)
                    controller.resetPositions(resetSlots)
                    sessionPositionResetRevisions = preferences.positionResetRevisions
                }
        }
    }

    private fun restartOverlay(preferences: PetPreferences) {
        try {
            val replacementPreferences = if (
                activeSessionSignature?.petCount == preferences.petCount
            ) {
                preferences.copy(
                    lastPositions = overlayController?.currentPositions()
                        ?: preferences.lastPositions
                )
            } else {
                preferences
            }
            val replacement = createOverlayController(
                preferences = replacementPreferences,
                catalog = ownerPetCatalogRepository.snapshot.value
            )
            overlayController?.stop()
            overlayController = replacement.also { controller ->
                controller.start()
                if (!getSystemService(PowerManager::class.java).isInteractive) {
                    controller.pauseRendering()
                }
            }
            sessionPositionResetRevisions = preferences.positionResetRevisions
            activeSessionSignature = preferences.sessionSignature()
            PetOverlayRuntime.updateRunning(true, preferences.petCount)
        } catch (error: RuntimeException) {
            Log.e(TAG, "Unable to update running pet session", error)
            overlayController?.stop()
            overlayController = null
            stopSelf()
        }
    }

    private fun createOverlayController(
        preferences: PetPreferences,
        catalog: OwnerPetCatalogSnapshot
    ): PetOverlayController {
        val packs = List(preferences.petCount) { slotIndex ->
            val requestedPack = petPackRepository.find(
                preferences.packKeyForSlot(slotIndex)
            ) ?: petPackRepository.selectedPackForSlot(slotIndex)
            OwnerPetSpeechAnchorPolicy.enrich(
                pack = requestedPack,
                catalog = catalog
            )
        }
        val visuals = packs.distinctBy { it.key }.associate { pack ->
            pack.key to petBitmapCache.prepare(pack)
        }
        return PetOverlayController(
            context = this,
            assets = packs.map { pack ->
                PetOverlayAsset(
                    pack = pack,
                    visual = checkNotNull(visuals[pack.key])
                )
            },
            preferences = preferences,
            performanceBudget = petSettingsRepository.performanceBudget
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        overlayController?.onBoundsChanged()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        unregisterScreenStateReceiver()
        val resetRevisions = sessionPositionResetRevisions
        if (resetRevisions != null) {
            overlayController?.stop()?.let { positions ->
                petSettingsRepository.updateLastPositions(positions, resetRevisions)
            }
        } else {
            overlayController?.stop()
        }
        overlayController = null
        sessionPositionResetRevisions = null
        activeSessionSignature = null
        liveSettingsJob = null
        PetOverlayRuntime.updateRunning(false)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private data class PetSessionSignature(
        val petCount: Int,
        val packKeys: List<String>
    )

    private fun PetPreferences.sessionSignature(): PetSessionSignature =
        PetSessionSignature(
            petCount = petCount,
            packKeys = selectedPackKeys.take(petCount)
        )

    private fun promoteToForeground() {
        val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            createNotification(),
            foregroundServiceType
        )
    }

    private fun createNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            CONTENT_REQUEST_CODE,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            STOP_REQUEST_CODE,
            Intent(this, PetOverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_pet)
            .setContentTitle(getString(R.string.pet_overlay_notification_title))
            .setContentText(getString(R.string.pet_overlay_notification_text))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                R.drawable.ic_notification_pet,
                getString(R.string.pet_overlay_notification_stop),
                stopIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.pet_overlay_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.pet_overlay_notification_channel_description)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun registerScreenStateReceiver() {
        if (isScreenReceiverRegistered) return
        ContextCompat.registerReceiver(
            this,
            screenStateReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        isScreenReceiverRegistered = true
    }

    private fun unregisterScreenStateReceiver() {
        if (!isScreenReceiverRegistered) return
        unregisterReceiver(screenStateReceiver)
        isScreenReceiverRegistered = false
    }

    companion object {
        internal const val ACTION_STOP =
            "com.asianmobile.emojibattery.shimeji.action.STOP_PET_OVERLAY"
        private const val NOTIFICATION_CHANNEL_ID = "pet_overlay"
        private const val NOTIFICATION_ID = 10_201
        private const val CONTENT_REQUEST_CODE = 10_202
        private const val STOP_REQUEST_CODE = 10_203
        private const val CATALOG_WAIT_MILLIS = 2_000L
        private const val TAG = "PetOverlayService"

        internal fun startIntent(context: Context): Intent =
            Intent(context, PetOverlayService::class.java)
    }
}
