package com.asianmobile.privatebrower.pet.overlay

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
import com.asianmobile.privatebrower.MainActivity
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.data.repository.PetSettingsRepository
import com.asianmobile.privatebrower.pet.pack.PetBitmapCache
import com.asianmobile.privatebrower.pet.pack.PetPackRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PetOverlayService : Service() {
    @Inject lateinit var petPackRepository: PetPackRepository
    @Inject lateinit var petBitmapCache: PetBitmapCache
    @Inject lateinit var petSettingsRepository: PetSettingsRepository

    private var overlayController: PetOverlayController? = null
    private var sessionPositionResetRevision: Int? = null
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

        if (overlayController == null) {
            try {
                val preferences = petSettingsRepository.preferences.value
                sessionPositionResetRevision = preferences.positionResetRevision
                val packs = List(preferences.petCount) { slotIndex ->
                    petPackRepository.selectedPackForSlot(slotIndex)
                }
                val visuals = packs.distinctBy { it.key }.associate { pack ->
                    pack.key to petBitmapCache.prepare(pack)
                }
                overlayController = PetOverlayController(
                    context = this,
                    assets = packs.map { pack ->
                        PetOverlayAsset(
                            pack = pack,
                            visual = checkNotNull(visuals[pack.key])
                        )
                    },
                    preferences = preferences,
                    performanceBudget = petSettingsRepository.performanceBudget
                ).also { controller ->
                    controller.start()
                    if (!getSystemService(PowerManager::class.java).isInteractive) {
                        controller.pauseRendering()
                    }
                }
                PetOverlayRuntime.updateRunning(true, preferences.petCount)
            } catch (error: RuntimeException) {
                Log.e(TAG, "Unable to start pet overlay", error)
                overlayController?.stop()
                overlayController = null
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        overlayController?.onBoundsChanged()
    }

    override fun onDestroy() {
        unregisterScreenStateReceiver()
        val resetRevision = sessionPositionResetRevision
        if (resetRevision != null) {
            overlayController?.stop()?.let { positions ->
                petSettingsRepository.updateLastPositions(positions, resetRevision)
            }
        } else {
            overlayController?.stop()
        }
        overlayController = null
        sessionPositionResetRevision = null
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
            "com.asianmobile.privatebrower.action.STOP_PET_OVERLAY"
        private const val NOTIFICATION_CHANNEL_ID = "pet_overlay"
        private const val NOTIFICATION_ID = 10_201
        private const val CONTENT_REQUEST_CODE = 10_202
        private const val STOP_REQUEST_CODE = 10_203
        private const val TAG = "PetOverlayService"

        internal fun startIntent(context: Context): Intent =
            Intent(context, PetOverlayService::class.java)
    }
}
