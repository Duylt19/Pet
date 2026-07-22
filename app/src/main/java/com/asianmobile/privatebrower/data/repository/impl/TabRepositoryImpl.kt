package com.asianmobile.privatebrower.data.repository.impl

import com.asianmobile.privatebrower.data.database.dao.TabDao
import com.asianmobile.privatebrower.data.model.Tab
import com.asianmobile.privatebrower.data.model.toDomain
import com.asianmobile.privatebrower.data.model.toEntity
import com.asianmobile.privatebrower.data.repository.TabRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TabRepositoryImpl @Inject constructor(
    private val tabDao: TabDao
) : TabRepository {
    override fun observeNormalTabs(): Flow<List<Tab>> =
        tabDao.observeNormalTabs().map { entities -> entities.map { it.toDomain() } }

    override suspend fun insert(tab: Tab): Long = tabDao.insert(tab.toEntity())
    override suspend fun update(tab: Tab) = tabDao.update(tab.toEntity())
    override suspend fun deleteById(id: Long) = tabDao.deleteById(id)
    override suspend fun deleteAllNormal() = tabDao.deleteAllNormal()
    override suspend fun countNormal(): Int = tabDao.countNormal()
}
