package com.asianmobile.privatebrower.data.repository.impl

import com.asianmobile.privatebrower.data.database.dao.BookmarkDao
import com.asianmobile.privatebrower.data.database.entity.BookmarkEntity
import com.asianmobile.privatebrower.data.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BookmarkRepositoryImpl @Inject constructor(
    private val bookmarkDao: BookmarkDao
) : BookmarkRepository {
    override fun observeAll(): Flow<List<BookmarkEntity>> = bookmarkDao.observeAll()
    override fun observeSearch(query: String): Flow<List<BookmarkEntity>> = bookmarkDao.observeSearch(query)
    override fun observeByUrl(url: String): Flow<BookmarkEntity?> = bookmarkDao.observeByUrl(url)
    override suspend fun insert(entity: BookmarkEntity): Long = bookmarkDao.insert(entity)
    override suspend fun update(entity: BookmarkEntity) = bookmarkDao.update(entity)
    override suspend fun deleteById(id: Long) = bookmarkDao.deleteById(id)
    override suspend fun deleteAll() = bookmarkDao.deleteAll()
    override suspend fun countByUrl(url: String): Int = bookmarkDao.countByUrl(url)
    override suspend fun findByUrl(url: String): BookmarkEntity? = bookmarkDao.findByUrl(url)
}
