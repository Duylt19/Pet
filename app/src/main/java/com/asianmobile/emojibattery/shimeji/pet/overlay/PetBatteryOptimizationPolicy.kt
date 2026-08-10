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
    /** The vendor ships a power manager documented to kill foreground services. */
    val isAggressiveVendor: Boolean = false,
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
 * What does stop the overlay is a device that kills foreground services anyway. Rather than
 * guess that from the brand name, prefer evidence: a recorded kill of the overlay process, or a
 * restriction the platform will state outright. The vendor list is the last resort, for devices
 * too old to report either and for the first run before anything has been killed.
 */
object PetBatteryOptimizationPolicy {
    /**
     * Vendors documented as killing foreground services beyond what AOSP does. Sourced from the
     * dontkillmyapp vendor list; brands sharing one ROM are listed separately because
     * `Build.MANUFACTURER` reports the brand, not the ROM. This is a hint, not a measurement:
     * it goes stale as vendors change, so it only decides when nothing measured applies.
     */
    private val AGGRESSIVE_VENDORS = setOf(
        // MIUI / HyperOS
        "xiaomi", "redmi", "poco",
        // EMUI / MagicOS, plus PowerGenie
        "huawei", "honor",
        // ColorOS / OxygenOS
        "oppo", "realme", "oneplus",
        // Funtouch OS / OriginOS
        "vivo", "iqoo",
        // One UI puts unused apps to sleep
        "samsung",
        // Flyme
        "meizu",
        // Transsion: HiOS / XOS
        "tecno", "infinix", "itel",
        "asus", "letv", "leeco"
    )

    fun isAggressiveVendor(manufacturer: String): Boolean =
        manufacturer.trim().lowercase() in AGGRESSIVE_VENDORS

    /**
     * A kill the user did not ask for, of a service that was supposed to keep running. This is
     * the one signal that proves this device needs the exemption rather than suggesting it.
     */
    fun isUnexpectedKill(kind: PetProcessKillKind?): Boolean = when (kind) {
        PetProcessKillKind.SIGNALLED, PetProcessKillKind.SYSTEM_RECLAIM -> true
        PetProcessKillKind.USER, PetProcessKillKind.OTHER, null -> false
    }

    /**
     * Whether the exemption means anything on this device, granted or not. A device that needs
     * nothing never sees the row; a device that does keeps it, so the user can see the state and
     * revoke it rather than watching the row vanish the moment they grant it.
     */
    fun isExemptionRelevant(signals: PetBackgroundRestrictionSignals): Boolean =
        signals.isAlreadyIgnoringOptimization ||
            signals.isBackgroundRestricted ||
            signals.isInRestrictedStandbyBucket ||
            isUnexpectedKill(signals.lastOverlayKill) ||
            signals.isAggressiveVendor

    /** Why the row still needs acting on. Null once granted: there is nothing left to ask. */
    fun reasonFor(signals: PetBackgroundRestrictionSignals): PetExemptionReason? = when {
        signals.isAlreadyIgnoringOptimization -> null
        signals.isBackgroundRestricted -> PetExemptionReason.BACKGROUND_RESTRICTED
        isUnexpectedKill(signals.lastOverlayKill) -> PetExemptionReason.PREVIOUSLY_KILLED
        signals.isInRestrictedStandbyBucket -> PetExemptionReason.RESTRICTED_BUCKET
        signals.isAggressiveVendor -> PetExemptionReason.AGGRESSIVE_VENDOR
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
    RESTRICTED_BUCKET,

    /** No incident yet, but this vendor is known for them. */
    AGGRESSIVE_VENDOR
}
