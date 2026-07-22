package com.asianmobile.privatebrower.data.repository

import com.asianmobile.privatebrower.data.database.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

interface BookmarkRepository {
    fun observeAll(): Flow<List<BookmarkEntity>>
    fun observeSearch(query: String): Flow<List<BookmarkEntity>>
    fun observeByUrl(url: String): Flow<BookmarkEntity?>
    suspend fun insert(entity: BookmarkEntity): Long
    suspend fun update(entity: BookmarkEntity)
    suspend fun deleteById(id: Long)
    suspend fun deleteAll()
    suspend fun countByUrl(url: String): Int
    suspend fun findByUrl(url: String): BookmarkEntity?
}
