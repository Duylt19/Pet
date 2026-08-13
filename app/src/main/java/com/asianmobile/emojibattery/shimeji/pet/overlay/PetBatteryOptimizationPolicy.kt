package com.asianmobile.emojibattery.shimeji.pet.overlay

/**
 * Why the pet overlay was last killed, as far as the platform will say. Only the values that
 * change this decision are modelled; everything else collapses into [OTHER].
 */
enum class PetProcessKillKind {
    /** SIGKILL from outside the app: what a vendor power manager does. */
    SIGNALLED,

    /** The system reclaimed memory, or gave a reason it would not name. */
    SYSTEM_RECLAIM,

    /** The user swiped the app away or force-stopped it. Expected, not a symptom. */
    USER,

    /** A crash, an ANR, an update. Nothing a battery exemption would have prevented. */
    OTHER
}

/** Everything known about how this device treats the overlay, gathered at runtime. */
data class PetBackgroundRestrictionSignals(
    val isAlreadyIgnoringOptimization: Boolean = false,
    /** The user or system put the app in Settings' "Restricted" battery state. API 28+. */
    val isBackgroundRestricted: Boolean = false,
    /** The app sits in STANDBY_BUCKET_RESTRICTED, the harshest bucket. API 30+. */
    val isInRestrictedStandbyBucket: Boolean = false,
    /** How the overlay process died last, if the platform recorded it. API 30+. */
    val lastOverlayKill: PetProcessKillKind? = null,
    /** This ROM has its own auto-start allowlist, which the platform exemption does not touch. */
    val hasVendorPowerScreen: Boolean = false
)

/**
 * Whether asking this device for a battery-optimisation exemption can achieve anything.
 *
 * The exemption grants two things: network access and partial wake locks during Doze. The pet
 * overlay uses neither — it holds no wake lock, makes no request, and pauses rendering outright
 * on ACTION_SCREEN_OFF. Doze does not stop a foreground service either. So on a device that
 * leaves foreground services alone, the exemption changes nothing and the prompt is noise.
 *
 * What does stop the overlay is a restriction the platform reports or a process kill it records.
 * A manufacturer name and the existence of a vendor power screen are deliberately not treated as
 * evidence for this separate Android exemption. For example, Samsung's generic Battery screen is
 * not an Auto Start permission, and Android 9 by itself does not mean an app needs an exemption.
 */
object PetBatteryOptimizationPolicy {
    /**
     * A kill the user did not ask for, of a service that was supposed to keep running. This is
     * the one signal that proves this device needs the exemption rather than suggesting it.
     */
    fun isUnexpectedKill(kind: PetProcessKillKind?): Boolean = when (kind) {
        PetProcessKillKind.SIGNALLED, PetProcessKillKind.SYSTEM_RECLAIM -> true
        PetProcessKillKind.USER, PetProcessKillKind.OTHER, null -> false
    }

    /**
     * Whether measured platform evidence says the exemption may mean something on this device.
     * A grant alone is not evidence: a stock device manually exempted by the user stays hidden.
     * UI that only lists pending requests must additionally use [reasonFor], which becomes null
     * after grant.
     */
    fun isExemptionRelevant(signals: PetBackgroundRestrictionSignals): Boolean =
        signals.isBackgroundRestricted ||
            signals.isInRestrictedStandbyBucket ||
            isUnexpectedKill(signals.lastOverlayKill)

    /**
     * Why the row still needs acting on. Null once granted: there is nothing left to ask.
     *
     * Ordered by how much the signal is worth: what the platform states outright, then what it
     * recorded happening. No manufacturer fallback is used.
     */
    fun reasonFor(signals: PetBackgroundRestrictionSignals): PetExemptionReason? = when {
        signals.isAlreadyIgnoringOptimization -> null
        signals.isBackgroundRestricted -> PetExemptionReason.BACKGROUND_RESTRICTED
        isUnexpectedKill(signals.lastOverlayKill) -> PetExemptionReason.PREVIOUSLY_KILLED
        signals.isInRestrictedStandbyBucket -> PetExemptionReason.RESTRICTED_BUCKET
        else -> null
    }
}

/**
 * The vendor allowlist is a separate ask from the platform exemption: granting one does not grant
 * the other, so this stays offered even after the exemption is in place.
 */
fun PetBackgroundRestrictionSignals.shouldOfferVendorAllowlist(): Boolean = hasVendorPowerScreen

enum class PetExemptionReason {
    /** Settings has the app on "Restricted"; the overlay will be stopped. */
    BACKGROUND_RESTRICTED,

    /** This device already killed the overlay once. */
    PREVIOUSLY_KILLED,

    /** The app sits in the harshest standby bucket. */
    RESTRICTED_BUCKET
}
