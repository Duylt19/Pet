package com.asianmobile.privatebrower.data.model

import com.asianmobile.privatebrower.data.database.entity.BookmarkEntity

data class Bookmark(
    val id: Long,
    val title: String,
    val url: String,
    val faviconUrl: String?,
    val createdAt: Long
)

fun BookmarkEntity.toDomain() = Bookmark(id, title, url, faviconUrl, createdAt)
fun Bookmark.toEntity() = BookmarkEntity(id, title, url, faviconUrl, createdAt)
