package com.asianmobile.privatebrower.data.model

import com.asianmobile.privatebrower.data.database.entity.TabEntity

data class Tab(
    val id: Long,
    val title: String,
    val url: String,
    val thumbnailPath: String?,
    val isIncognito: Boolean,
    val lastActiveAt: Long,
    val position: Int
)

fun TabEntity.toDomain() = Tab(id, title, url, thumbnailPath, isIncognito, lastActiveAt, position)
fun Tab.toEntity() = TabEntity(id, title, url, thumbnailPath, isIncognito, lastActiveAt, position)
