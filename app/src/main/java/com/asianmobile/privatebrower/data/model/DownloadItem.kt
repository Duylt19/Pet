package com.asianmobile.privatebrower.data.model

import com.asianmobile.privatebrower.data.database.entity.DownloadEntity

data class DownloadItem(
    val id: Long,
    val fileName: String,
    val url: String,
    val path: String,
    val mimeType: String,
    val sizeBytes: Long,
    val downloadedBytes: Long,
    val status: DownloadStatus,
    val errorMessage: String?,
    val createdAt: Long,
    val completedAt: Long?,
    val thumbnailUrl: String = ""
)

fun DownloadEntity.toDomain() = DownloadItem(
    id = id,
    fileName = fileName,
    url = url,
    path = path,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    downloadedBytes = downloadedBytes,
    status = runCatching { DownloadStatus.valueOf(status) }.getOrDefault(DownloadStatus.FAILED),
    errorMessage = errorMessage,
    createdAt = createdAt,
    completedAt = completedAt,
    thumbnailUrl = thumbnailUrl
)

fun DownloadItem.toEntity() = DownloadEntity(
    id = id,
    fileName = fileName,
    url = url,
    path = path,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    downloadedBytes = downloadedBytes,
    status = status.name,
    errorMessage = errorMessage,
    createdAt = createdAt,
    completedAt = completedAt,
    thumbnailUrl = thumbnailUrl
)
