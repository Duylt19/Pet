package com.asianmobile.privatebrower.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.asianmobile.privatebrower.data.database.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY visited_at DESC LIMIT :limit OFFSET :offset")
    fun observePaged(limit: Int, offset: Int): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE title LIKE :q OR url LIKE :q ORDER BY visited_at DESC")
    fun observeSearch(q: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(h: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun deleteAll()

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Long)
}
