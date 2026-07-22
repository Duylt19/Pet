package com.asianmobile.privatebrower.di

import android.content.Context
import androidx.room.Room
import com.asianmobile.privatebrower.data.local.DataStoreManager
import com.asianmobile.privatebrower.data.database.PrivateBrowserDatabase
import com.asianmobile.privatebrower.data.database.dao.BookmarkDao
import com.asianmobile.privatebrower.data.database.dao.DownloadDao
import com.asianmobile.privatebrower.data.database.dao.HistoryDao
import com.asianmobile.privatebrower.data.database.dao.TabDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDataStoreManager(@ApplicationContext context: Context): DataStoreManager {
        return DataStoreManager(context)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PrivateBrowserDatabase {
        return Room.databaseBuilder(
            context,
            PrivateBrowserDatabase::class.java,
            PrivateBrowserDatabase.NAME
        )
            // DEV ONLY: schema changes recreate the local DB while version 1 is unstable.
            // Replace this with explicit migrations after version 2 becomes the baseline.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()
    }

    @Provides
    @Singleton
    fun provideBookmarkDao(db: PrivateBrowserDatabase): BookmarkDao {
        return db.bookmarkDao()
    }

    @Provides
    @Singleton
    fun provideHistoryDao(db: PrivateBrowserDatabase): HistoryDao {
        return db.historyDao()
    }

    @Provides
    @Singleton
    fun provideTabDao(db: PrivateBrowserDatabase): TabDao {
        return db.tabDao()
    }

    @Provides
    @Singleton
    fun provideDownloadDao(db: PrivateBrowserDatabase): DownloadDao {
        return db.downloadDao()
    }
}
