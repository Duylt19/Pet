package com.asianmobile.privatebrower.di

import com.asianmobile.privatebrower.data.repository.SearchEngineRepository
import com.asianmobile.privatebrower.data.repository.impl.SearchEngineRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSearchEngineRepository(
        impl: SearchEngineRepositoryImpl
    ): SearchEngineRepository
}
