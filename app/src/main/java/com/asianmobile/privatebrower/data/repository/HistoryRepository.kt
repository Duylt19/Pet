package com.asianmobile.privatebrower.data.repository

import com.asianmobile.privatebrower.data.database.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun observeAll(): Flow<List<HistoryEntity>>
    fun observeSearch(query: String): Flow<List<HistoryEntity>>

    /** Captured when navigation starts so a later clear can invalidate delayed page callbacks. */
    fun captureVisitGeneration(): Long

    suspend fun recordVisit(
        url: String,
        title: String,
        visitGeneration: Long,
        faviconUrl: String? = null
    )
    suspend fun restore(item: HistoryEntity)
    suspend fun deleteById(id: Long)
    suspend fun deleteAll()
}
