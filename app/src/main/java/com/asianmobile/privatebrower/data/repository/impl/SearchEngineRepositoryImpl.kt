package com.asianmobile.privatebrower.data.repository.impl

import com.asianmobile.privatebrower.data.local.DataStoreManager
import com.asianmobile.privatebrower.data.model.SearchEngine
import com.asianmobile.privatebrower.data.repository.SearchEngineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SearchEngineRepositoryImpl @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : SearchEngineRepository {
    override fun observeCurrent(): Flow<SearchEngine> =
        dataStoreManager.selectedSearchEngine.map { SearchEngine.fromId(it) }

    override suspend fun setCurrent(engine: SearchEngine) {
        dataStoreManager.saveSelectedSearchEngine(engine.id)
    }
}
