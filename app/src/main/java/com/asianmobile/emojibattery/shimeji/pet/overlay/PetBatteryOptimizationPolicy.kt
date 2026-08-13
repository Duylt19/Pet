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
 * guess that from the brand name, prefer evidence: a recorded kill of the overlay process, a
 * restriction the platform will state outright, or a vendor power manager resolved on this very
 * device. The last one carries most of the weight below API 30, where the platform reports
 * neither kills nor standby buckets and the brand string is otherwise all there is. The vendor
 * list is the true last resort, for the ROMs that hide their power manager from `<queries>`.
 */
object PetBatteryOptimizationPolicy {
    /**
     * Vendors documented as killing foreground services beyond what AOSP does. Sourced from the
     * dontkillmyapp vendor list; brands sharing one ROM are listed separately because
     * `Build.MANUFACTURER` and `Build.BRAND` disagree about which one they report. This is a
     * hint, not a measurement: it goes stale as vendors change, so it only decides when nothing
     * measured applies.
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
        "asus", "letv", "leeco", "lemobile"
    )

    /** Anything that is not a letter or a digit, so a build string splits into brand words. */
    private val VENDOR_SEPARATORS = Regex("[^a-z0-9]+")

    /**
     * Whether either build string names a vendor on the list.
     *
     * Matched word by word rather than whole-string: several vendors put their legal entity in
     * `Build.MANUFACTURER` — Transsion reports `INFINIX MOBILITY LIMITED` and `TECNO MOBILE
     * LIMITED` — so an equality check silently misses exactly the ROMs this list exists for.
     * Both strings are read because the pair disagrees per vendor: MIUI reports `Xiaomi` as the
     * manufacturer and `Redmi`/`POCO` as the brand, and some ROMs invert that.
     */
    fun isAggressiveVendor(manufacturer: String, brand: String = ""): Boolean =
        manufacturer.namesAggressiveVendor() || brand.namesAggressiveVendor()

    private fun String.namesAggressiveVendor(): Boolean =
        lowercase().split(VENDOR_SEPARATORS).any { it in AGGRESSIVE_VENDORS }

    /**
     * A kill the user did not ask for, of a service that was supposed to keep running. This is
     * the one signal that proves this device needs the exemption rather than suggesting it.
     */
    fun isUnexpectedKill(kind: PetProcessKillKind?): Boolean = when (kind) {
        PetProcessKillKind.SIGNALLED, PetProcessKillKind.SYSTEM_RECLAIM -> true
        PetProcessKillKind.USER, PetProcessKillKind.OTHER, null -> false
    }

    /**
     * Whether the exemption means anything on this device, granted or not. UI that only lists
     * pending requests must additionally use [reasonFor], which becomes null after grant.
     */
    fun isExemptionRelevant(signals: PetBackgroundRestrictionSignals): Boolean =
        signals.isAlreadyIgnoringOptimization ||
            signals.isBackgroundRestricted ||
            signals.isInRestrictedStandbyBucket ||
            isUnexpectedKill(signals.lastOverlayKill) ||
            signals.hasVendorPowerScreen ||
            signals.isAggressiveVendor

    /**
     * Why the row still needs acting on. Null once granted: there is nothing left to ask.
     *
     * Ordered by how much the signal is worth: what the platform states outright, then what it
     * recorded happening, then what this device demonstrably ships, and the brand list last —
     * it is the only entry that is a guess.
     */
    fun reasonFor(signals: PetBackgroundRestrictionSignals): PetExemptionReason? = when {
        signals.isAlreadyIgnoringOptimization -> null
        signals.isBackgroundRestricted -> PetExemptionReason.BACKGROUND_RESTRICTED
        isUnexpectedKill(signals.lastOverlayKill) -> PetExemptionReason.PREVIOUSLY_KILLED
        signals.isInRestrictedStandbyBucket -> PetExemptionReason.RESTRICTED_BUCKET
        signals.hasVendorPowerScreen -> PetExemptionReason.VENDOR_POWER_MANAGER
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

    /**
     * This ROM ships its own power manager, resolved on the device rather than guessed from the
     * brand. A ROM that needed one at all is a ROM that stops background work by itself.
     */
    VENDOR_POWER_MANAGER,

    /** No incident yet, but this vendor is known for them. */
    AGGRESSIVE_VENDOR
}
