package com.asianmobile.privatebrower.data.repository.impl

import com.asianmobile.privatebrower.data.local.DataStoreManager
import com.asianmobile.privatebrower.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : PreferencesRepository {
    override val isLanguageCompleted: Flow<Boolean> = dataStoreManager.isLanguageCompleted
    override val isIntroCompleted: Flow<Boolean> = dataStoreManager.isIntroCompleted
    override val isPermissionCompleted: Flow<Boolean> = dataStoreManager.isPermissionCompleted
    override val isDefaultBrowserAccepted: Flow<Boolean> = dataStoreManager.isDefaultBrowserAccepted
    override val sessionCount: Flow<Int> = dataStoreManager.sessionCount

    override suspend fun setLanguageCompleted(v: Boolean) {
        dataStoreManager.saveLanguageCompleted(v)
    }

    override suspend fun setIntroCompleted(v: Boolean) {
        dataStoreManager.saveIntroCompleted(v)
    }

    override suspend fun setPermissionCompleted(v: Boolean) {
        dataStoreManager.savePermissionCompleted(v)
    }

    override suspend fun setDefaultBrowserAccepted(v: Boolean) {
        dataStoreManager.saveDefaultBrowserAccepted(v)
    }

    override suspend fun incrementSessionCount() {
        dataStoreManager.incrementSessionCount()
    }
}
