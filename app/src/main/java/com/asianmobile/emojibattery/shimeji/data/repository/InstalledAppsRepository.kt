package com.asianmobile.emojibattery.shimeji.data.repository

import com.asianmobile.emojibattery.shimeji.data.model.InstalledApp

interface InstalledAppsRepository {
    suspend fun getLaunchableApps(): List<InstalledApp>
}
