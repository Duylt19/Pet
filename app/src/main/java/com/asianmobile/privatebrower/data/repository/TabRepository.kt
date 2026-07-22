package com.asianmobile.privatebrower.data.repository

import com.asianmobile.privatebrower.data.model.Tab
import kotlinx.coroutines.flow.Flow

interface TabRepository {
    fun observeNormalTabs(): Flow<List<Tab>>
    suspend fun insert(tab: Tab): Long
    suspend fun update(tab: Tab)
    suspend fun deleteById(id: Long)
    suspend fun deleteAllNormal()
    suspend fun countNormal(): Int
}
