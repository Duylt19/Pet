package com.asianmobile.privatebrower.data.repository.impl

import android.net.Uri
import com.asianmobile.privatebrower.data.database.dao.HistoryDao
import com.asianmobile.privatebrower.data.database.entity.HistoryEntity
import com.asianmobile.privatebrower.data.repository.HistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao
) : HistoryRepository {
    private val recordMutex = Mutex()
    private val visitGeneration = AtomicLong(0L)

    override fun observeAll(): Flow<List<HistoryEntity>> = historyDao.observePaged(200, 0)
    override fun observeSearch(query: String): Flow<List<HistoryEntity>> = historyDao.observeSearch(query)
    override fun captureVisitGeneration(): Long = visitGeneration.get()

    override suspend fun recordVisit(
        url: String,
        title: String,
        visitGeneration: Long,
        faviconUrl: String?
    ) {
        val normalizedUrl = normalizeHistoryUrl(url) ?: return
        withContext(Dispatchers.IO) {
            recordMutex.withLock {
                if (visitGeneration != this@HistoryRepositoryImpl.visitGeneration.get()) {
                    return@withLock
                }
                val now = System.currentTimeMillis()
                val existing = historyDao.findByUrl(normalizedUrl)
                historyDao.upsert(
                    existing?.copy(
                        title = title.ifBlank { normalizedUrl },
                        faviconUrl = faviconUrl ?: existing.faviconUrl,
                        visitedAt = now,
                        visitCount = existing.visitCount + 1
                    ) ?: HistoryEntity(
                        title = title.ifBlank { normalizedUrl },
                        url = normalizedUrl,
                        faviconUrl = faviconUrl,
                        visitedAt = now
                    )
                )
            }
        }
    }

    override suspend fun restore(item: HistoryEntity) = historyDao.upsert(item)
    override suspend fun deleteById(id: Long) = historyDao.deleteById(id)
    override suspend fun deleteAll() {
        withContext(Dispatchers.IO) {
            recordMutex.withLock {
                visitGeneration.incrementAndGet()
                historyDao.deleteAll()
            }
        }
    }

    private fun normalizeHistoryUrl(rawUrl: String): String? {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return null
        return runCatching {
            val uri = Uri.parse(trimmed)
            if (uri.scheme?.lowercase() !in HISTORY_SCHEMES || uri.host.isNullOrBlank()) {
                return null
            }
            uri.buildUpon().fragment(null).build().toString()
        }.getOrNull()
    }

    private companion object {
        val HISTORY_SCHEMES = setOf("http", "https")
    }
}
