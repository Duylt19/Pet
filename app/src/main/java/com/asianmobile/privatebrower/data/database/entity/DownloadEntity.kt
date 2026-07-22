package com.asianmobile.privatebrower.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "file_name") val fileName: String,
    val url: String,
    val path: String,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long = 0,
    @ColumnInfo(name = "downloaded_bytes") val downloadedBytes: Long = 0,
    val status: String,   // PENDING / RUNNING / PAUSED / COMPLETED / FAILED
    @ColumnInfo(name = "error_message") val errorMessage: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "completed_at") val completedAt: Long? = null,
    /** JSON-serialized request headers (Referer, cookies, etc.) needed for CDN auth */
    @ColumnInfo(name = "request_headers", defaultValue = "") val requestHeaders: String = "",
    /** Facebook DASH: separate audio-track URL to mux with [url] (video-only). Empty otherwise. */
    @ColumnInfo(name = "audio_url", defaultValue = "") val audioUrl: String = "",
    /** Page poster URL used while the media file itself is not yet available locally. */
    @ColumnInfo(name = "thumbnail_url", defaultValue = "") val thumbnailUrl: String = "",
)
