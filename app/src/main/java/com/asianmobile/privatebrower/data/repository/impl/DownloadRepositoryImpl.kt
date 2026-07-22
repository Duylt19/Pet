package com.asianmobile.privatebrower.data.repository.impl

import com.asianmobile.privatebrower.data.database.dao.DownloadDao
import com.asianmobile.privatebrower.data.database.entity.DownloadEntity
import com.asianmobile.privatebrower.data.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

class DownloadRepositoryImpl @Inject constructor(
    private val downloadDao: DownloadDao
) : DownloadRepository {

    override fun observeAll(): Flow<List<DownloadEntity>> = downloadDao.observeAll()

    override fun observeActive(): Flow<List<DownloadEntity>> = downloadDao.observeActive()

    override fun observeCompleted(limit: Int): Flow<List<DownloadEntity>> =
        downloadDao.observeCompleted(limit)

    override suspend fun enqueue(
        fileName: String,
        url: String,
        path: String,
        mimeType: String,
        headers: Map<String, String>,
        audioUrl: String,
        thumbnailUrl: String
    ): Long {
        // Serialize headers to JSON string
        val headersJson = if (headers.isNotEmpty()) {
            JSONObject(headers).toString()
        } else ""

        // Avoid overwriting an existing file or a concurrent pending download:
        // resolve a unique "name (1).ext" style file name.
        val (uniqueName, uniquePath) = resolveUnique(fileName, path)

        val entity = DownloadEntity(
            fileName = uniqueName,
            url = url,
            path = uniquePath,
            mimeType = mimeType,
            status = "PENDING",
            requestHeaders = headersJson,
            audioUrl = audioUrl,
            thumbnailUrl = thumbnailUrl
        )
        return downloadDao.insert(entity)
    }

    /**
     * Returns a (fileName, path) pair that collides neither with a file already on
     * disk nor with another download row pointing at the same path.
     */
    private suspend fun resolveUnique(fileName: String, path: String): Pair<String, String> {
        val target = File(path)
        val dir = target.parentFile
        val dotIndex = fileName.lastIndexOf('.')
        val base = if (dotIndex > 0) fileName.substring(0, dotIndex) else fileName
        val ext = if (dotIndex > 0) fileName.substring(dotIndex) else "" // includes leading "."

        var candidate = target
        var index = 1
        while (candidate.exists() || downloadDao.countByPath(candidate.absolutePath) > 0) {
            val newName = "$base ($index)$ext"
            candidate = if (dir != null) File(dir, newName) else File(newName)
            index++
        }
        return candidate.name to candidate.absolutePath
    }

    override suspend fun updateProgress(id: Long, status: String, bytes: Long) {
        downloadDao.updateProgress(id, status, bytes)
    }

    override suspend fun cancel(id: Long) {
        downloadDao.updateProgress(id, "FAILED", 0)
    }

    override suspend fun delete(id: Long) {
        downloadDao.deleteById(id)
    }
}
