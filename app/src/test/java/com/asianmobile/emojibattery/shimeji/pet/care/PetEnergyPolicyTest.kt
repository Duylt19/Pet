package com.asianmobile.emojibattery.shimeji.pet.care

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetEnergyPolicyTest {
    @Test
    fun `drains one percent per minute`() {
        val energy = PetEnergyPolicy.currentEnergy(
            storedPercent = 100,
            updatedAtMillis = 0L + MINUTE,
            nowMillis = MINUTE + 25 * MINUTE
        )

        assertEquals(75, energy)
    }

    @Test
    fun `keeps the stored value within the first minute`() {
        val energy = PetEnergyPolicy.currentEnergy(
            storedPercent = 60,
            updatedAtMillis = MINUTE,
            nowMillis = MINUTE + 59_000L
        )

        assertEquals(60, energy)
    }

    @Test
    fun `an unwritten energy record drains from adoption time`() {
        val energy = PetEnergyPolicy.resolvedEnergy(
            storedPercent = null,
            updatedAtMillis = null,
            adoptedAtMillis = MINUTE,
            nowMillis = MINUTE + 2 * MINUTE
        )

        assertEquals(98, energy)
    }

    @Test
    fun `empties rather than going negative after a long absence`() {
        val energy = PetEnergyPolicy.currentEnergy(
            storedPercent = 40,
            updatedAtMillis = MINUTE,
            nowMillis = MINUTE + 30L * 24L * 60L * MINUTE
        )

        assertEquals(0, energy)
    }

    @Test
    fun `treats a pet that was never written as untouched`() {
        assertEquals(
            100,
            PetEnergyPolicy.currentEnergy(
                storedPercent = 100,
                updatedAtMillis = 0L,
                nowMillis = 999L * MINUTE
            )
        )
    }

    @Test
    fun `ignores a clock that moved backwards`() {
        val energy = PetEnergyPolicy.currentEnergy(
            storedPercent = 50,
            updatedAtMillis = 10L * MINUTE,
            nowMillis = 2L * MINUTE
        )

        assertEquals(50, energy)
    }

    @Test
    fun `feeding never exceeds a full bowl`() {
        assertEquals(100, PetEnergyPolicy.afterFeeding(currentPercent = 90, foodEnergy = 25))
        assertEquals(55, PetEnergyPolicy.afterFeeding(currentPercent = 30, foodEnergy = 25))
    }

    @Test
    fun `matches the three bands Figma shows`() {
        assertEquals(PetEnergyLevel.GOOD, PetEnergyPolicy.level(100))
        assertEquals(PetEnergyLevel.MEDIUM, PetEnergyPolicy.level(60))
        assertEquals(PetEnergyLevel.LOW, PetEnergyPolicy.level(10))
    }

    @Test
    fun `only a full bowl reads as Max`() {
        assertTrue(PetEnergyPolicy.isMax(100))
        assertFalse(PetEnergyPolicy.isMax(99))
    }

    @Test
    fun `an empty bar still shows its rounded cap`() {
        assertTrue(PetEnergyPolicy.fillFraction(0) > 0f)
        assertEquals(1f, PetEnergyPolicy.fillFraction(100), 0.0001f)
    }

    private companion object {
        const val MINUTE = 60_000L
    }
}
