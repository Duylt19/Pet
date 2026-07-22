package com.asianmobile.privatebrower.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.asianmobile.privatebrower.data.database.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY created_at DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('PENDING', 'RUNNING', 'PAUSED') ORDER BY created_at DESC")
    fun observeActive(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' ORDER BY completed_at DESC LIMIT :limit")
    fun observeCompleted(limit: Int = 50): Flow<List<DownloadEntity>>

    @Insert
    suspend fun insert(d: DownloadEntity): Long

    @Update
    suspend fun update(d: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM downloads WHERE status IN (:statuses)")
    suspend fun deleteByStatuses(statuses: List<String>)

    @Query("UPDATE downloads SET status = :status, downloaded_bytes = :bytes WHERE id = :id")
    suspend fun updateProgress(id: Long, status: String, bytes: Long)

    @Query("UPDATE downloads SET status = :status, error_message = NULL WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query(
        "UPDATE downloads SET status = 'PAUSED' " +
            "WHERE id = :id AND status IN ('RUNNING', 'PENDING')"
    )
    suspend fun markPausedIfActive(id: Long): Int

    @Query("UPDATE downloads SET status = 'FAILED', downloaded_bytes = :bytes, error_message = :message WHERE id = :id")
    suspend fun updateFailure(id: Long, bytes: Long, message: String)

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getById(id: Long): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE status IN ('RUNNING', 'PENDING')")
    suspend fun getResumable(): List<DownloadEntity>

    @Query("SELECT * FROM downloads WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT :limit")
    suspend fun getNextPending(limit: Int): List<DownloadEntity>

    @Query("SELECT COUNT(*) FROM downloads WHERE path = :path")
    suspend fun countByPath(path: String): Int

    @Query("UPDATE downloads SET size_bytes = :sizeBytes WHERE id = :id")
    suspend fun updateSize(id: Long, sizeBytes: Long)

    @Query("UPDATE downloads SET completed_at = :completedAt WHERE id = :id")
    suspend fun updateCompletedAt(id: Long, completedAt: Long)

    @Query("UPDATE downloads SET path = :path WHERE id = :id")
    suspend fun updatePath(id: Long, path: String)

    @Query(
        """UPDATE downloads SET
            file_name = :fileName,
            path = :path,
            size_bytes = :sizeBytes,
            downloaded_bytes = :sizeBytes,
            status = 'COMPLETED',
            error_message = NULL,
            completed_at = :completedAt
            WHERE id = :id"""
    )
    suspend fun markCompleted(
        id: Long,
        fileName: String,
        path: String,
        sizeBytes: Long,
        completedAt: Long
    )
}
