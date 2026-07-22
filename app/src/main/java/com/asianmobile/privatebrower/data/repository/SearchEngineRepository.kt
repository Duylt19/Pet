package com.asianmobile.privatebrower.data.repository

import com.asianmobile.privatebrower.data.model.SearchEngine
import kotlinx.coroutines.flow.Flow

interface SearchEngineRepository {
    fun observeCurrent(): Flow<SearchEngine>
    suspend fun setCurrent(engine: SearchEngine)
}
