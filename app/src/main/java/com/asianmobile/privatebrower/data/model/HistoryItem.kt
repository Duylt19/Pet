package com.asianmobile.privatebrower.data.model

import com.asianmobile.privatebrower.data.database.entity.HistoryEntity

data class HistoryItem(
    val id: Long,
    val title: String,
    val url: String,
    val faviconUrl: String?,
    val visitedAt: Long,
    val visitCount: Int
)

fun HistoryEntity.toDomain() = HistoryItem(id, title, url, faviconUrl, visitedAt, visitCount)
fun HistoryItem.toEntity() = HistoryEntity(id, title, url, faviconUrl, visitedAt, visitCount)
