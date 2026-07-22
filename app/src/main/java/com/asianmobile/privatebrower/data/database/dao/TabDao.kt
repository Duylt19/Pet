package com.asianmobile.privatebrower.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.asianmobile.privatebrower.data.database.entity.TabEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TabDao {
    @Query("SELECT * FROM tabs WHERE is_incognito = 0 ORDER BY position ASC")
    fun observeNormalTabs(): Flow<List<TabEntity>>

    @Insert
    suspend fun insert(t: TabEntity): Long

    @Update
    suspend fun update(t: TabEntity)

    @Query("DELETE FROM tabs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM tabs WHERE is_incognito = 0")
    suspend fun deleteAllNormal()

    @Query("SELECT COUNT(*) FROM tabs WHERE is_incognito = 0")
    suspend fun countNormal(): Int
}
