package com.asianmobile.privatebrower.data.repository

import com.asianmobile.privatebrower.data.database.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun observeAll(): Flow<List<DownloadEntity>>
    fun observeActive(): Flow<List<DownloadEntity>>
    fun observeCompleted(limit: Int = 50): Flow<List<DownloadEntity>>
    suspend fun enqueue(
        fileName: String,
        url: String,
        path: String,
        mimeType: String,
        headers: Map<String, String> = emptyMap(),
        audioUrl: String = "",
        thumbnailUrl: String = ""
    ): Long
    suspend fun updateProgress(id: Long, status: String, bytes: Long)
    suspend fun cancel(id: Long)
    suspend fun delete(id: Long)
}
