package com.asianmobile.privatebrower.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabs")
data class TabEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val url: String,
    @ColumnInfo(name = "thumbnail_path") val thumbnailPath: String? = null,
    @ColumnInfo(name = "is_incognito") val isIncognito: Boolean = false,
    @ColumnInfo(name = "last_active_at") val lastActiveAt: Long = System.currentTimeMillis(),
    val position: Int = 0,
)
