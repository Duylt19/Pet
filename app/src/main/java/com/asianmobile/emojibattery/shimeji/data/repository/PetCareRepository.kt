package com.asianmobile.emojibattery.shimeji.data.repository

import kotlinx.coroutines.flow.StateFlow

/** Energy stored for one pet, resolved against the clock by `PetEnergyPolicy`. */
data class PetEnergyRecord(
    val percent: Int,
    val updatedAtMillis: Long
)

/** Per-pet care state: how full a pet is and when the user adopted it. */
interface PetCareRepository {
    val energy: StateFlow<Map<Int, PetEnergyRecord>>

    val adoptedAtMillis: StateFlow<Map<Int, Long>>

    /** Stores the fed energy together with the moment it was written. */
    suspend fun setEnergy(petId: Int, percent: Int, atMillis: Long)

    /** Records an adoption date once; a later call for the same pet is ignored. */
    suspend fun rememberAdoption(petId: Int, atMillis: Long)
}
