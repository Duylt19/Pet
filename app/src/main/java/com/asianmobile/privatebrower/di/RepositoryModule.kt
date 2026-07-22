package com.asianmobile.privatebrower.di

import com.asianmobile.privatebrower.data.repository.BookmarkRepository
import com.asianmobile.privatebrower.data.repository.HistoryRepository
import com.asianmobile.privatebrower.data.repository.PreferencesRepository
import com.asianmobile.privatebrower.data.repository.SearchEngineRepository
import com.asianmobile.privatebrower.data.repository.MediaStoreRepository
import com.asianmobile.privatebrower.data.repository.TabRepository
import com.asianmobile.privatebrower.data.repository.impl.BookmarkRepositoryImpl
import com.asianmobile.privatebrower.data.repository.impl.HistoryRepositoryImpl
import com.asianmobile.privatebrower.data.repository.impl.PreferencesRepositoryImpl
import com.asianmobile.privatebrower.data.repository.impl.SearchEngineRepositoryImpl
import com.asianmobile.privatebrower.data.repository.impl.MediaStoreRepositoryImpl
import com.asianmobile.privatebrower.data.repository.impl.TabRepositoryImpl
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
    abstract fun bindPreferencesRepository(
        impl: PreferencesRepositoryImpl
    ): PreferencesRepository

    @Binds
    @Singleton
    abstract fun bindTabRepository(
        impl: TabRepositoryImpl
    ): TabRepository

    @Binds
    @Singleton
    abstract fun bindSearchEngineRepository(
        impl: SearchEngineRepositoryImpl
    ): SearchEngineRepository

    @Binds
    @Singleton
    abstract fun bindBookmarkRepository(
        impl: BookmarkRepositoryImpl
    ): BookmarkRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(
        impl: HistoryRepositoryImpl
    ): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(
        impl: com.asianmobile.privatebrower.data.repository.impl.DownloadRepositoryImpl
    ): com.asianmobile.privatebrower.data.repository.DownloadRepository

    @Binds
    @Singleton
    abstract fun bindMediaStoreRepository(
        impl: MediaStoreRepositoryImpl
    ): MediaStoreRepository
}
