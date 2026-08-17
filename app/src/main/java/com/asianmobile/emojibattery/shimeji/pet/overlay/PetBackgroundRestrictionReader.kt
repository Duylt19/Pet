package com.asianmobile.emojibattery.shimeji.pet.overlay

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads what the platform will say about this device restricting the pet overlay. Every call is
 * wrapped: these APIs are vendor-implemented and a ROM that throws must not take the screen with
 * it, it should just leave that signal unknown.
 */
@Singleton
class PetBackgroundRestrictionReader @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun read(): PetBackgroundRestrictionSignals = PetBackgroundRestrictionSignals(
        isAlreadyIgnoringOptimization = isIgnoringBatteryOptimization(),
        isBackgroundRestricted = isBackgroundRestricted(),
        isInRestrictedStandbyBucket = isInRestrictedStandbyBucket(),
        lastOverlayKill = lastOverlayKill(),
        hasVendorPowerScreen = vendorPowerIntent() != null
    )

    /**
     * The vendor's own auto-start screen, if this ROM has one. Resolved rather than assumed:
     * these components move between ROM versions, so an entry that no longer exists is skipped
     * instead of throwing when the user taps.
     */
    fun vendorPowerIntent(): Intent? = runCatching {
        PetVendorPowerSettings.CANDIDATES
            .asSequence()
            .map { screen ->
                Intent().setComponent(ComponentName(screen.packageName, screen.className))
            }
            .firstOrNull { intent ->
                context.packageManager.resolveActivity(intent, 0) != null
            }
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }.getOrNull()

    fun isIgnoringBatteryOptimization(): Boolean = runCatching {
        ContextCompat.getSystemService(context, PowerManager::class.java)
            ?.isIgnoringBatteryOptimizations(context.packageName) == true
    }.getOrDefault(false)

    private fun isBackgroundRestricted(): Boolean = runCatching {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            ContextCompat.getSystemService(context, ActivityManager::class.java)
                ?.isBackgroundRestricted == true
    }.getOrDefault(false)

    private fun isInRestrictedStandbyBucket(): Boolean = runCatching {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@runCatching false
        val bucket = ContextCompat.getSystemService(context, UsageStatsManager::class.java)
            ?.appStandbyBucket
            ?: return@runCatching false
        bucket >= STANDBY_BUCKET_RESTRICTED
    }.getOrDefault(false)

    /**
     * The most recent death of this app's process, classified. A process that died while it was
     * running a foreground service is the case worth acting on: the overlay was supposed to be
     * running and something ended it.
     *
     * Process-wide rather than pet-specific, so the battery bar reads it too — a force-stop takes
     * its Accessibility permission with it and this is the only record of who did it.
     */
    fun lastOverlayKill(): PetProcessKillKind? = runCatching {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@runCatching null
        val manager = ContextCompat.getSystemService(context, ActivityManager::class.java)
            ?: return@runCatching null
        manager.getHistoricalProcessExitReasons(context.packageName, 0, EXIT_HISTORY_LIMIT)
            .asSequence()
            .filter { it.isOverlayRelevant() }
            .map { it.toKillKind() }
            .firstOrNull { it != PetProcessKillKind.OTHER }
    }.getOrNull()

    /**
     * A SIGKILL from outside counts whatever the process was doing at the time: the vendor
     * pattern is to stop the service first and kill the process after, which records the death
     * at cached or service importance and would be filtered away by an importance test. Every
     * other reason has to have been at least foreground-service important, or it was not the
     * overlay that died.
     */
    private fun ApplicationExitInfo.isOverlayRelevant(): Boolean =
        reason == ApplicationExitInfo.REASON_SIGNALED ||
            importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE

    private fun ApplicationExitInfo.toKillKind(): PetProcessKillKind = when (reason) {
        // SIGKILL from outside the app. A vendor power manager leaves this trace.
        ApplicationExitInfo.REASON_SIGNALED -> PetProcessKillKind.SIGNALLED

        // The system took the process back and either said why, or declined to.
        ApplicationExitInfo.REASON_LOW_MEMORY,
        ApplicationExitInfo.REASON_OTHER,
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> PetProcessKillKind.SYSTEM_RECLAIM

        ApplicationExitInfo.REASON_USER_REQUESTED,
        ApplicationExitInfo.REASON_USER_STOPPED -> PetProcessKillKind.USER

        else -> PetProcessKillKind.OTHER
    }

    private companion object {
        /** UsageStatsManager.STANDBY_BUCKET_RESTRICTED, inlined: it is API 30 and hidden below. */
        const val STANDBY_BUCKET_RESTRICTED = 45

        /**
         * Deep enough that a run of crashes or updates cannot bury the one kill worth finding.
         * The records are already in the system's ring buffer, so asking for more costs a larger
         * parcel and nothing else.
         */
        const val EXIT_HISTORY_LIMIT = 15
    }
}
