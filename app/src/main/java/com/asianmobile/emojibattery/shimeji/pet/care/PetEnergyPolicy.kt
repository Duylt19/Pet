package com.asianmobile.emojibattery.shimeji.pet.care

enum class PetEnergyLevel {
    GOOD,
    MEDIUM,
    LOW
}

/**
 * Energy drains while the app is closed, so it is stored as a value plus the moment it was
 * written and resolved on read. That keeps the drain honest without a background timer.
 */
object PetEnergyPolicy {
    const val MAX_ENERGY = 100
    const val DECAY_PERCENT_PER_MINUTE = 1
    private const val MILLIS_PER_MINUTE = 60_000L

    /** Energy of a pet that has never been fed: a full bowl on adoption. */
    const val INITIAL_ENERGY = MAX_ENERGY

    fun currentEnergy(storedPercent: Int, updatedAtMillis: Long, nowMillis: Long): Int {
        val stored = storedPercent.coerceIn(0, MAX_ENERGY)
        if (updatedAtMillis <= 0L || nowMillis <= updatedAtMillis) return stored
        val elapsedMinutes = (nowMillis - updatedAtMillis) / MILLIS_PER_MINUTE
        val drained = elapsedMinutes * DECAY_PERCENT_PER_MINUTE
        // A pet that has been away for weeks is empty, not negative.
        if (drained >= stored) return 0
        return (stored - drained).toInt()
    }

    fun afterFeeding(currentPercent: Int, foodEnergy: Int): Int =
        (currentPercent.coerceIn(0, MAX_ENERGY) + foodEnergy.coerceAtLeast(0))
            .coerceAtMost(MAX_ENERGY)

    /**
     * Figma shows green at full, yellow at 60 and red at 10, so the bands sit either side of
     * those samples.
     */
    fun level(percent: Int): PetEnergyLevel = when {
        percent >= GOOD_THRESHOLD -> PetEnergyLevel.GOOD
        percent >= MEDIUM_THRESHOLD -> PetEnergyLevel.MEDIUM
        else -> PetEnergyLevel.LOW
    }

    fun isMax(percent: Int): Boolean = percent >= MAX_ENERGY

    /** Bar fill as a fraction, never fully empty so the rounded cap stays visible. */
    fun fillFraction(percent: Int): Float =
        (percent.coerceIn(0, MAX_ENERGY) / MAX_ENERGY.toFloat())
            .coerceAtLeast(MIN_VISIBLE_FRACTION)

    private const val GOOD_THRESHOLD = 70
    private const val MEDIUM_THRESHOLD = 30
    private const val MIN_VISIBLE_FRACTION = 0.06f
}
