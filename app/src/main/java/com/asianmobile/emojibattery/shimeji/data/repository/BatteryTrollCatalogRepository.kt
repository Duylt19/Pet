package com.asianmobile.emojibattery.shimeji.data.repository

import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollCatalogSnapshot
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollEntry
import kotlinx.coroutines.flow.StateFlow

interface BatteryTrollCatalogRepository {
    val snapshot: StateFlow<BatteryTrollCatalogSnapshot>

    suspend fun refresh()

    fun findTroll(trollId: Int): BatteryTrollEntry?

    /**
     * Downloads (or reuses) the verified local copy of a catalog-relative asset path and returns
     * its absolute file path, or `null` when the asset is unknown or fails verification.
     */
    suspend fun materializeAsset(path: String?): String?
}
