package com.asianmobile.emojibattery.shimeji.pet.overlay

/**
 * Whether asking this device for a battery-optimisation exemption can achieve anything.
 *
 * The exemption grants two things: network access and partial wake locks during Doze. The pet
 * overlay uses neither — it holds no wake lock, makes no request, and pauses rendering outright
 * on ACTION_SCREEN_OFF. Doze does not stop a foreground service either. So on stock Android the
 * exemption changes nothing for this app, and asking for it is a prompt the user gains nothing
 * from.
 *
 * It is worth asking on the manufacturers that kill foreground services with their own power
 * managers, which the platform exemption sometimes placates. Those devices are the reason this
 * row exists at all.
 */
object PetBatteryOptimizationPolicy {
    /**
     * Vendors documented as killing foreground services beyond what AOSP does. Sourced from the
     * dontkillmyapp vendor list; brands sharing one ROM are listed separately because
     * `Build.MANUFACTURER` reports the brand, not the ROM.
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
     * The row is shown only where the exemption can help and the user has not already granted
     * it, so a device that needs nothing never asks.
     */
    fun shouldOfferExemption(manufacturer: String, isAlreadyIgnoring: Boolean): Boolean =
        !isAlreadyIgnoring && isAggressiveVendor(manufacturer)
}
