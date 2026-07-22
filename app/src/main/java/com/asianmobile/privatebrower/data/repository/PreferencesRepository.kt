package com.asianmobile.privatebrower.data.repository

import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val isLanguageCompleted: Flow<Boolean>
    val isIntroCompleted: Flow<Boolean>
    val isPermissionCompleted: Flow<Boolean>
    val isDefaultBrowserAccepted: Flow<Boolean>
    val sessionCount: Flow<Int>

    suspend fun setLanguageCompleted(v: Boolean)
    suspend fun setIntroCompleted(v: Boolean)
    suspend fun setPermissionCompleted(v: Boolean)
    suspend fun setDefaultBrowserAccepted(v: Boolean)
    suspend fun incrementSessionCount()
}
