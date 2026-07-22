# 05 — Data Model

Tài liệu schema dữ liệu local: **DataStore** (preferences) + **Room** (relational).

---

## 1. DataStore Preferences

File: `app/src/main/java/com/asianmobile/privatebrower/data/local/DataStoreManager.kt`

### Keys

| Key name (string) | Type | Mô tả | Default |
|--------------|------|-------|---------|
| `is_intro_completed` | Boolean | User đã xem xong onboarding intro | false |
| `is_language_completed` | Boolean | User đã chọn ngôn ngữ | false |
| `is_permission_completed` | Boolean | User đã grant permission | false |
| `pass_word_vault` | String | Vault password (kế thừa) | "" |
| `email_recovery_password` | String | Recovery email (kế thừa) | "" |
| `question_recovery_id` | String | Recovery question ID (kế thừa) | "" |
| `question_recovery_answer` | String | Recovery question answer (kế thừa) | "" |
| `is_dark_mode` | Boolean | Dark mode (v2) | false |
| `country_language` | String | Mã quốc gia (vd "VN", "US") | "" |
| `key_language` | String | Mã ngôn ngữ ISO (vd "en", "vi") | "" |
| `notifications_json` | String | Notifications JSON (kế thừa) | "" |
| `last_known_media_count` | Int | Last media count (kế thừa) | -1 |
| **`is_default_browser_prompted`** | Boolean | Đã hỏi user về set default | false |
| **`selected_search_engine`** | String | "google"/"bing"/"yahoo"/"duckduckgo"/"yandex"/"coccoc" | "google" |
| **`is_incognito_default`** | Boolean | Mở tab mới mặc định ở incognito | false |
| **`last_used_tab_id`** | Long | ID tab cuối user đang xem | -1L |
| **`session_count`** | Int | Số lần app được mở (cho premium splash return) | 0 |
| `runtime_permission_request_count_<permission>` | Int | Số lần app đã request từng runtime permission | 0 |

> App cũng dùng **SharedPreferences** `"language_cache"` (synchronous) cho startup language: keys `key_language`, `country_language`.

### Convention

```kotlin
@Singleton
class DataStoreManager @Inject constructor(@ApplicationContext context: Context) {
    private val Context.dataStore by preferencesDataStore(name = "settings")
    private val dataStore = context.dataStore

    val selectedSearchEngine: Flow<String> = dataStore.data.map { it[SELECTED_SEARCH_ENGINE] ?: "google" }

    suspend fun setSelectedSearchEngine(engine: String) {
        dataStore.edit { it[SELECTED_SEARCH_ENGINE] = engine }
    }
    // ...
}
```

---

## 2. Room Database

File: `app/src/main/java/com/asianmobile/privatebrower/data/database/PrivateBrowserDatabase.kt`

```kotlin
@Database(
    entities = [BookmarkEntity::class, HistoryEntity::class, TabEntity::class, DownloadEntity::class],
    version = 1,
    exportSchema = true,
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
```

`exportSchema = true` để track schema diff trong git (output JSON tại `app/schemas/`).

---

## 3. Entities

### 3.1. `BookmarkEntity`

```kotlin
@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["url"], unique = true)],
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val url: String,
    @ColumnInfo(name = "favicon_url") val faviconUrl: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)
```

### 3.2. `HistoryEntity`

```kotlin
@Entity(
    tableName = "history",
    indices = [Index(value = ["url"])],
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val url: String,
    @ColumnInfo(name = "favicon_url") val faviconUrl: String? = null,
    @ColumnInfo(name = "visited_at") val visitedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "visit_count") val visitCount: Int = 1,
)
```

**Upsert logic** (DAO): nếu tồn tại row với URL → update `visitedAt = now`, `visitCount = visitCount + 1`. Nếu không tồn tại → insert.

### 3.3. `TabEntity`

```kotlin
@Entity(tableName = "tabs")
data class TabEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val url: String,
    @ColumnInfo(name = "thumbnail_path") val thumbnailPath: String? = null,
    @ColumnInfo(name = "is_incognito") val isIncognito: Boolean = false,
    @ColumnInfo(name = "last_active_at") val lastActiveAt: Long = System.currentTimeMillis(),
    val position: Int = 0,
)
```

**Lưu ý:** Tab incognito **không** được lưu Room (mất khi app kill). Chỉ giữ in-memory trong `TabManager`. Xem [F02_INCOGNITO_MODE.md](features/F02_INCOGNITO_MODE.md).

### 3.4. `DownloadEntity`

```kotlin
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "file_name") val fileName: String,
    val url: String,
    val path: String,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long = 0,
    @ColumnInfo(name = "downloaded_bytes") val downloadedBytes: Long = 0,
    val status: String,   // PENDING / RUNNING / PAUSED / COMPLETED / FAILED
    @ColumnInfo(name = "error_message") val errorMessage: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "completed_at") val completedAt: Long? = null,
)

enum class DownloadStatus { PENDING, RUNNING, PAUSED, COMPLETED, FAILED }
```

---

## 4. DAOs

### BookmarkDao

```kotlin
@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY created_at DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE title LIKE :q OR url LIKE :q ORDER BY created_at DESC")
    fun observeSearch(q: String): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(b: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM bookmarks WHERE url = :url")
    suspend fun countByUrl(url: String): Int
}
```

### HistoryDao

```kotlin
@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY visited_at DESC LIMIT :limit OFFSET :offset")
    fun observePaged(limit: Int, offset: Int): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE title LIKE :q OR url LIKE :q ORDER BY visited_at DESC")
    fun observeSearch(q: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(h: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun deleteAll()

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Long)
}
```

### TabDao

```kotlin
@Dao
interface TabDao {
    @Query("SELECT * FROM tabs WHERE is_incognito = 0 ORDER BY position ASC")
    fun observeNormalTabs(): Flow<List<TabEntity>>

    @Insert suspend fun insert(t: TabEntity): Long
    @Update suspend fun update(t: TabEntity)
    @Query("DELETE FROM tabs WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("DELETE FROM tabs WHERE is_incognito = 0") suspend fun deleteAllNormal()
    @Query("SELECT COUNT(*) FROM tabs WHERE is_incognito = 0") suspend fun countNormal(): Int
}
```

### DownloadDao

```kotlin
@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY created_at DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('PENDING', 'RUNNING', 'PAUSED') ORDER BY created_at DESC")
    fun observeActive(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' ORDER BY completed_at DESC LIMIT :limit")
    fun observeCompleted(limit: Int = 50): Flow<List<DownloadEntity>>

    @Insert suspend fun insert(d: DownloadEntity): Long
    @Update suspend fun update(d: DownloadEntity)
    @Query("DELETE FROM downloads WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("UPDATE downloads SET status = :status, downloaded_bytes = :bytes WHERE id = :id")
    suspend fun updateProgress(id: Long, status: String, bytes: Long)
}
```

---

## 5. Domain Models (data/model/)

Khác Entity ở chỗ:
- Bỏ tiền tố `Entity`
- Status dùng enum thay vì String
- Có thể thêm computed fields (`displayDate`)
- Mapper: Extension function `toDomain()` và `toEntity()`

```kotlin
data class Bookmark(val id: Long, val title: String, val url: String, val faviconUrl: String?, val createdAt: Long)
data class HistoryItem(val id: Long, val title: String, val url: String, val faviconUrl: String?, val visitedAt: Long, val visitCount: Int)
data class Tab(val id: Long, val title: String, val url: String, val thumbnailPath: String?, val isIncognito: Boolean, val lastActiveAt: Long, val position: Int)
data class DownloadItem(val id: Long, val fileName: String, val url: String, val path: String, val mimeType: String, val sizeBytes: Long, val downloadedBytes: Long, val status: DownloadStatus, val errorMessage: String?, val createdAt: Long, val completedAt: Long?)

fun BookmarkEntity.toDomain() = Bookmark(id, title, url, faviconUrl, createdAt)
fun Bookmark.toEntity() = BookmarkEntity(id, title, url, faviconUrl, createdAt)
```

---

## 6. Repository Interfaces

File: `data/repository/`

```kotlin
interface BookmarkRepository {
    fun observeAll(): Flow<List<Bookmark>>
    suspend fun add(title: String, url: String, faviconUrl: String?): Boolean   // false nếu duplicate
    suspend fun delete(id: Long)
    suspend fun deleteAll()
}

interface HistoryRepository {
    fun observePaged(limit: Int, offset: Int = 0): Flow<List<HistoryItem>>
    suspend fun record(url: String, title: String, faviconUrl: String?)
    suspend fun clearAll()
}

interface TabRepository {
    fun observeNormalTabs(): Flow<List<Tab>>
    suspend fun addTab(url: String, title: String, isIncognito: Boolean): Long
    suspend fun updateTab(tab: Tab)
    suspend fun closeTab(id: Long)
    suspend fun closeAllNormal()
}

interface DownloadRepository {
    fun observeAll(): Flow<List<DownloadItem>>
    fun observeActive(): Flow<List<DownloadItem>>
    fun observeCompleted(): Flow<List<DownloadItem>>
    suspend fun enqueue(url: String, fileName: String, mimeType: String): Long
    suspend fun updateProgress(id: Long, status: DownloadStatus, bytes: Long)
    suspend fun cancel(id: Long)
}

interface SearchEngineRepository {
    fun observeCurrent(): Flow<SearchEngine>
    suspend fun setCurrent(engine: SearchEngine)
}

interface PreferencesRepository {
    val isLanguageCompleted: Flow<Boolean>
    val isIntroCompleted: Flow<Boolean>
    val isPermissionCompleted: Flow<Boolean>
    val isDefaultBrowserPrompted: Flow<Boolean>
    val sessionCount: Flow<Int>

    suspend fun setLanguageCompleted(v: Boolean)
    suspend fun setIntroCompleted(v: Boolean)
    suspend fun setPermissionCompleted(v: Boolean)
    suspend fun setDefaultBrowserPrompted(v: Boolean)
    suspend fun incrementSessionCount()
}
```

---

## 7. Migrations

V1 → V2 (khi có): thêm `Migration(1, 2)` trong `PrivateBrowserDatabase.Builder`.

```kotlin
Room.databaseBuilder(context, PrivateBrowserDatabase::class.java, PrivateBrowserDatabase.NAME)
    .addMigrations(MIGRATION_1_2)
    // KHÔNG dùng .fallbackToDestructiveMigration() ở release!
    .build()
```

Quy ước:
- Mỗi migration là 1 object riêng (`object MIGRATION_1_2 : Migration(1, 2) { ... }`)
- Test migration với `MigrationTestHelper` trong androidTest
- Schema JSON commit vào `app/schemas/`

---

## 8. Indexes & Performance

- `bookmarks.url` UNIQUE — tránh dedupe ở app layer
- `history.url` INDEX (non-unique) — query upsert nhanh
- `tabs.position` không index v1 (số lượng tab < 20)
- `downloads.status` không index v1 (số lượng < 100 thông thường)

Khi history > 10k rows: cân nhắc Paging 3 + index `visited_at DESC` (auto-created bởi ORDER BY).

---

## 9. File Storage (External to Room)

| Loại | Path | Lifecycle |
|------|------|-----------|
| Tab thumbnail | `context.cacheDir/tabs/<tab_id>.png` | Xoá khi close tab |
| Tab WebView state (`Bundle.saveState`) | `context.filesDir/tab_state/<tab_id>.dat` | Xoá khi close tab |
| Download files | `Environment.DIRECTORY_DOWNLOADS/PrivateBrowser/` (API 28-) hoặc MediaStore (API 29+) | User-managed |
| Favicon cache | Coil disk cache (auto) | LRU |

---

## 10. Backup & Privacy

- DataStore: **không** include trong Auto Backup (set `android:allowBackup="false"` hoặc whitelist) — chứa cờ onboarding, không nhạy cảm nhưng tránh backup
- Room DB: **không** include — chứa history riêng tư của user
- Manifest:
  ```xml
  <application
      android:allowBackup="false"
      android:fullBackupContent="false"
      ... />
  ```
