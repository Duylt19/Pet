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
import com.asianmobile.emojibattery.shimeji.data.model.PetDisplayMode
import com.asianmobile.emojibattery.shimeji.data.model.PetPreferences
import com.asianmobile.emojibattery.shimeji.data.repository.OwnerPetCatalogRepository
import com.asianmobile.emojibattery.shimeji.data.repository.PetSettingsRepository
import com.asianmobile.emojibattery.shimeji.pet.pack.PetBitmapCache
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackRepository
import com.asianmobile.emojibattery.shimeji.pet.settings.PetSettingsPolicy
import com.asianmobile.emojibattery.shimeji.pet.speech.OwnerPetSpeechAnchorPolicy
import dagger.Lazy
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@AndroidEntryPoint
class PetOverlayService : Service() {
    @Inject lateinit var petPackRepositoryLazy: Lazy<PetPackRepository>
    @Inject lateinit var petBitmapCacheLazy: Lazy<PetBitmapCache>
    @Inject lateinit var petSettingsRepositoryLazy: Lazy<PetSettingsRepository>
    @Inject lateinit var ownerPetCatalogRepositoryLazy: Lazy<OwnerPetCatalogRepository>

    private val petPackRepository: PetPackRepository
        get() = petPackRepositoryLazy.get()
    private val petBitmapCache: PetBitmapCache
        get() = petBitmapCacheLazy.get()
    private val petSettingsRepository: PetSettingsRepository
        get() = petSettingsRepositoryLazy.get()
    private val ownerPetCatalogRepository: OwnerPetCatalogRepository
        get() = ownerPetCatalogRepositoryLazy.get()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val settingsPolicy = PetSettingsPolicy()
    private var overlayStartJob: Job? = null
    private var liveSettingsJob: Job? = null
    private var overlayController: PetOverlayController? = null
    private var sessionPositionResetRevisions: List<Int>? = null
    private var activeSessionSignature: PetOverlaySessionSignature? = null
    private var activeSessionMode: PetDisplayMode? = null
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
        // Every command delivered after startForegroundService() must complete the foreground
        // handshake before any early stop. A fast user toggle can move the process-local state
        // back to STOPPED before this callback runs; stopping first leaves Android waiting for
        // startForeground() and ends in ForegroundServiceDidNotStartInTimeException.
        try {
            promoteToForeground()
        } catch (error: RuntimeException) {
            Log.e(TAG, "Unable to promote pet overlay service", error)
            PetOverlayRuntime.updateRunning(false)
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_STOP) {
            PetOverlayRuntime.updateStopRequested()
            stopSelf()
            return START_NOT_STICKY
        }

        if (!PetOverlayRuntime.state.value.shouldStartService()) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!PetOverlay.canDraw(this)) {
            PetOverlayRuntime.updateRunning(false)
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
            if (preferences.runtimePetCount == 0) {
                PetOverlayRuntime.updateRunning(false)
                stopSelf()
                return
            }
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
            val assets = withContext(Dispatchers.Default) {
                createOverlayAssets(preferences, catalog)
            }
            if (!PetOverlayRuntime.state.value.shouldStartService()) {
                stopSelf()
                return
            }
            overlayController = createOverlayController(preferences, assets).also { controller ->
                controller.start()
                if (!getSystemService(PowerManager::class.java).isInteractive) {
                    controller.pauseRendering()
                }
            }
            if (!PetOverlayRuntime.state.value.shouldStartService()) {
                overlayController?.stop()
                overlayController = null
                stopSelf()
                return
            }
            activeSessionSignature = preferences.overlaySessionSignature()
            activeSessionMode = preferences.displayMode
            observeLiveSettings()
            PetOverlayRuntime.updateRunning(true, preferences.runtimePetCount)
        } catch (error: CancellationException) {
            throw error
        } catch (error: RuntimeException) {
            Log.e(TAG, "Unable to start pet overlay", error)
            overlayController?.stop()
            overlayController = null
            PetOverlayRuntime.updateRunning(false)
            stopSelf()
        }
    }

    private fun observeLiveSettings() {
        liveSettingsJob?.cancel()
        liveSettingsJob = serviceScope.launch {
            petSettingsRepository.preferences
                .collect { preferences ->
                    when (
                        PetOverlaySessionPolicy.resolveUpdate(
                            active = activeSessionSignature,
                            preferences = preferences
                        )
                    ) {
                        PetOverlaySessionUpdate.REBUILD -> {
                            restartOverlay(preferences)
                            return@collect
                        }

                        PetOverlaySessionUpdate.MIXED_ROSTER -> {
                            val controller = overlayController ?: return@collect
                            try {
                                val requestedAssets = withContext(Dispatchers.Default) {
                                    createOverlayAssets(
                                        preferences = preferences,
                                        catalog = ownerPetCatalogRepository.snapshot.value
                                    )
                                }
                                controller.updateMixedRoster(
                                    updatedPreferences = preferences,
                                    requestedAssets = requestedAssets
                                )
                                activeSessionSignature = preferences.overlaySessionSignature()
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: RuntimeException) {
                                Log.e(TAG, "Unable to reconcile running mixed pets", error)
                                restartOverlay(preferences)
                                return@collect
                            }
                        }

                        PetOverlaySessionUpdate.SWARM_RUNTIME -> {
                            val controller = overlayController ?: return@collect
                            try {
                                controller.updateSwarmPreferences(preferences)
                                activeSessionSignature = preferences.overlaySessionSignature()
                                PetOverlayRuntime.updateRunning(
                                    true,
                                    preferences.runtimePetCount
                                )
                            } catch (error: RuntimeException) {
                                Log.e(TAG, "Unable to update running pet swarm", error)
                                restartOverlay(preferences)
                            }
                            return@collect
                        }

                        PetOverlaySessionUpdate.NONE -> Unit
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
                    controller.updateVisibilitySettings(preferences.petSlots)
                    controller.resetPositions(resetSlots)
                    sessionPositionResetRevisions = preferences.positionResetRevisions
                    PetOverlayRuntime.updateRunning(true, preferences.runtimePetCount)
                }
        }
    }

    private suspend fun restartOverlay(preferences: PetPreferences) {
        if (preferences.runtimePetCount == 0) {
            stopSelf()
            return
        }
        try {
            val replacementPreferences = if (
                activeSessionMode == PetDisplayMode.MIXED &&
                preferences.displayMode == PetDisplayMode.MIXED &&
                activeSessionSignature?.mixedPetCount == preferences.petCount
            ) {
                preferences.copy(
                    lastPositions = overlayController?.currentPositions()
                        ?: preferences.lastPositions
                )
            } else {
                preferences
            }
            val replacementAssets = withContext(Dispatchers.Default) {
                createOverlayAssets(
                    preferences = replacementPreferences,
                    catalog = ownerPetCatalogRepository.snapshot.value
                )
            }
            if (!PetOverlayRuntime.state.value.shouldStartService()) return
            val replacement = createOverlayController(replacementPreferences, replacementAssets)
            overlayController?.stop()
            overlayController = replacement.also { controller ->
                controller.start()
                if (!getSystemService(PowerManager::class.java).isInteractive) {
                    controller.pauseRendering()
                }
            }
            sessionPositionResetRevisions = preferences.positionResetRevisions
            activeSessionSignature = preferences.overlaySessionSignature()
            activeSessionMode = preferences.displayMode
            PetOverlayRuntime.updateRunning(true, preferences.runtimePetCount)
        } catch (error: CancellationException) {
            throw error
        } catch (error: RuntimeException) {
            Log.e(TAG, "Unable to update running pet session", error)
            overlayController?.stop()
            overlayController = null
            stopSelf()
        }
    }

    private fun createOverlayController(
        preferences: PetPreferences,
        assets: List<PetOverlayAsset>
    ): PetOverlayController = PetOverlayController(
        context = this,
        assets = assets,
        preferences = preferences,
        performanceBudget = petSettingsRepository.performanceBudget
    )

    private fun createOverlayAssets(
        preferences: PetPreferences,
        catalog: OwnerPetCatalogSnapshot
    ): List<PetOverlayAsset> {
        val requestedPackKeys = when (preferences.displayMode) {
            PetDisplayMode.MIXED -> List(preferences.petCount) { slotIndex ->
                preferences.packKeyForSlot(slotIndex)
            }
            PetDisplayMode.SWARM -> List(preferences.swarm.count) {
                preferences.swarm.packKey
            }
        }
        val packs = requestedPackKeys.mapIndexed { slotIndex, packKey ->
            val requestedPack = petPackRepository.find(
                packKey
            ) ?: petPackRepository.selectedPackForSlot(slotIndex)
            OwnerPetSpeechAnchorPolicy.enrich(
                pack = requestedPack,
                catalog = catalog
            )
        }
        val visuals = packs.distinctBy { it.key }.associate { pack ->
            pack.key to petBitmapCache.prepare(pack)
        }
        return packs.map { pack ->
            PetOverlayAsset(
                pack = pack,
                visual = checkNotNull(visuals[pack.key])
            )
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        overlayController?.onBoundsChanged()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        unregisterScreenStateReceiver()
        val resetRevisions = sessionPositionResetRevisions
        if (resetRevisions != null && activeSessionMode == PetDisplayMode.MIXED) {
            overlayController?.stop()?.let { positions ->
                petSettingsRepository.updateLastPositions(positions, resetRevisions)
            }
        } else {
            overlayController?.stop()
        }
        overlayController = null
        sessionPositionResetRevisions = null
        activeSessionSignature = null
        activeSessionMode = null
        liveSettingsJob = null
        PetOverlayRuntime.updateRunning(false)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

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
