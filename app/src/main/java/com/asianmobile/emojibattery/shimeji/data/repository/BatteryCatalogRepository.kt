package com.asianmobile.emojibattery.shimeji.data.repository

import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogSnapshot
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry
import kotlinx.coroutines.flow.StateFlow

interface BatteryCatalogRepository {
    val snapshot: StateFlow<BatteryCatalogSnapshot>

    suspend fun refresh()

    fun findTheme(themeId: Int): BatteryThemeEntry?

    suspend fun materializeAsset(path: String?): String?
}
