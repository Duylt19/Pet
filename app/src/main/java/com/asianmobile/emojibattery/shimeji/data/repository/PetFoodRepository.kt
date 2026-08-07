package com.asianmobile.emojibattery.shimeji.data.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * How many portions of each food the user holds. Pet Store grants portions through Rewarded;
 * My Pet Room consumes them when a pet is fed.
 */
interface PetFoodRepository {
    val inventory: StateFlow<Map<String, Int>>

    suspend fun grant(foodId: String, portions: Int = 1)

    /** Removes one portion; returns false when the user has none left. */
    suspend fun consume(foodId: String): Boolean
}
