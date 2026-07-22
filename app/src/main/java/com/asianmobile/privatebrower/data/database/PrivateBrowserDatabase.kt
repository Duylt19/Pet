package com.asianmobile.privatebrower.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.asianmobile.privatebrower.data.database.dao.BookmarkDao
import com.asianmobile.privatebrower.data.database.dao.DownloadDao
import com.asianmobile.privatebrower.data.database.dao.HistoryDao
import com.asianmobile.privatebrower.data.database.dao.TabDao
import com.asianmobile.privatebrower.data.database.entity.BookmarkEntity
import com.asianmobile.privatebrower.data.database.entity.DownloadEntity
import com.asianmobile.privatebrower.data.database.entity.HistoryEntity
import com.asianmobile.privatebrower.data.database.entity.TabEntity

// DEV ONLY: keep version 1 while the schema is still changing. The development database
// is recreated on schema changes. Start maintaining migrations after version 2 is finalized.
@Database(
    entities = [
        BookmarkEntity::class,
        HistoryEntity::class,
        TabEntity::class,
        DownloadEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PrivateBrowserDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun tabDao(): TabDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        const val NAME = "private_browser.db"
    }
}
