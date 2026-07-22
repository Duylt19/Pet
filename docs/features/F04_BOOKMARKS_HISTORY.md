# F04 — Bookmarks & History

> **Trang thai hien tai:** Man Bookmarks/History dung segmented control. Route tu Browser
> mo san segment History; Home mo san Bookmarks. History duoc ghi duy nhat tai `TabManager`
> singleton, khong con collect trong tung `BrowserViewModel`, de tranh ghi trung khi co nhieu
> Browser destination trong back stack.

## 1. Entities

Xem [05_DATA_MODEL.md](../05_DATA_MODEL.md) — `BookmarkEntity`, `HistoryEntity`.

---

## 2. Repository

### BookmarkRepositoryImpl

```kotlin
class BookmarkRepositoryImpl @Inject constructor(
    private val bookmarkDao: BookmarkDao,
) : BookmarkRepository {
    override fun observeAll() = bookmarkDao.observeAll()
    override fun observeByUrl(url: String) = bookmarkDao.observeByUrl(url)
    override suspend fun findByUrl(url: String) = bookmarkDao.findByUrl(url)
    override suspend fun insert(entity: BookmarkEntity) = bookmarkDao.insert(entity)
    override suspend fun deleteById(id: Long) = bookmarkDao.deleteById(id)
}
```

### HistoryRepositoryImpl

```kotlin
class HistoryRepositoryImpl @Inject constructor(
    private val dao: HistoryDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : HistoryRepository {

    override fun observePaged(limit: Int, offset: Int) =
        dao.observePaged(limit, offset).map { list -> list.map { it.toDomain() } }

    override suspend fun recordVisit(url: String, title: String, faviconUrl: String?) = withContext(io) {
        val existing = dao.findByUrl(url)
        if (existing != null) {
            dao.upsert(existing.copy(
                title = title,
                faviconUrl = faviconUrl ?: existing.faviconUrl,
                visitedAt = System.currentTimeMillis(),
                visitCount = existing.visitCount + 1,
            ))
        } else {
            dao.upsert(HistoryEntity(title = title, url = url, faviconUrl = faviconUrl))
        }
    }

    override suspend fun clearAll() = withContext(io) { dao.deleteAll() }
}
```

---

## 3. Use Cases (optional layer)

V1 có thể skip UseCase, gọi repository trực tiếp từ ViewModel. Dùng UseCase nếu logic phức tạp cần share:

```kotlin
class AddBookmarkUseCase @Inject constructor(
    private val bookmarkRepository: BookmarkRepository,
) {
    suspend operator fun invoke(url: String, title: String, faviconUrl: String?): AddBookmarkResult {
        if (url.isBlank()) return AddBookmarkResult.InvalidUrl
        val added = bookmarkRepository.add(title.ifBlank { url }, url, faviconUrl)
        return if (added) AddBookmarkResult.Added else AddBookmarkResult.Duplicate
    }
}

sealed class AddBookmarkResult {
    object Added : AddBookmarkResult()
    object Duplicate : AddBookmarkResult()
    object InvalidUrl : AddBookmarkResult()
}
```

---

## 4. UI Screen: Bookmarks/History

File: `ui/bookmarks/BookmarksScreen.kt`. Xem chi tiết: [S08_BOOKMARKS_HISTORY.md](../screens/S08_BOOKMARKS_HISTORY.md).

### UiState

```kotlin
data class BookmarksUiState(
    val selectedTab: BookmarksTab = BookmarksTab.BOOKMARKS,
    val searchQuery: String = "",
    val bookmarks: List<Bookmark> = emptyList(),
    val history: List<HistoryItem> = emptyList(),
    val groupedHistory: Map<String, List<HistoryItem>> = emptyMap(),  // grouped by date label
)

enum class BookmarksTab { BOOKMARKS, HISTORY }
```

### History Grouping

```kotlin
fun groupHistory(items: List<HistoryItem>): Map<String, List<HistoryItem>> {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    return items.groupBy { item ->
        val date = Instant.ofEpochMilli(item.visitedAt).atZone(ZoneId.systemDefault()).toLocalDate()
        when (date) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        }
    }
}
```

---

## 5. Toggle Bookmark Từ BrowserWebView

Trong menu của BrowserWebView (S07):

```kotlin
private val activeBookmarkState = activeSession.flatMapLatest { session ->
    session?.url?.flatMapLatest { url ->
        bookmarkRepository.observeByUrl(url)
    } ?: flowOf(null)
}

fun toggleBookmark() = viewModelScope.launch {
    bookmarkMutex.withLock {
        val existing = bookmarkRepository.findByUrl(currentUrl)
        if (existing == null) {
            bookmarkRepository.insert(currentPageBookmark())
            events.send(BrowserUiEvent.BookmarkAdded)
        } else {
            bookmarkRepository.deleteById(existing.id)
            events.send(BrowserUiEvent.BookmarkRemoved(existing))
        }
    }
}
```

---

## 6. Record History

Tu dong trong `TabManager.onPageFinished` (xem [F01](F01_BROWSER_CORE.md) section 6). Quy tac:
- ❌ Không record nếu incognito
- ❌ Khong record cac scheme khac `http` va `https`
- ✅ Bo fragment (`#xxx`) truoc khi upsert
- ✅ Record ca khi user da chuyen sang tab khac truoc luc page finish
- ✅ Mutex bao ve `findByUrl + upsert` de khong mat `visitCount` khi event den gan nhau

---

## 7. Search

`HistoryDao.observeSearch("%$query%")` — LIKE wildcards. Áp dụng cho cả bookmarks.

ViewModel:
```kotlin
fun onSearchQueryChanged(q: String) {
    _uiState.update { it.copy(searchQuery = q) }
    // trigger debounce search
}

@OptIn(FlowPreview::class)
private fun observeSearch() {
    viewModelScope.launch {
        _uiState.map { it.searchQuery }
            .debounce(300)
            .distinctUntilChanged()
            .collectLatest { q ->
                val pattern = if (q.isBlank()) "%" else "%$q%"
                // re-collect bookmarks / history with pattern
            }
    }
}
```

---

## 8. Edge Cases

| Trường hợp | Xử lý |
|-----------|-------|
| URL duplicate/race khi add bookmark | UNIQUE constraint + mutex; không tạo bản ghi trùng |
| Đổi URL hoặc active tab | `observeByUrl()` đổi collector và cập nhật icon |
| Bỏ bookmark nhầm | Snackbar "Undo" chèn lại bookmark đã xóa |
| `about:`, `data:`, error page | Disable bookmark action |
| URL rất dài (> 2000 chars) | Truncate hiển thị, lưu nguyên |
| Title rỗng | Fallback dùng URL hostname |
| History > 10k rows | V1 không paging — load 200 mới nhất. V2 dùng Paging 3 |
| Clear history khi đang xem trang | Vẫn ok, không ảnh hưởng tab đang mở |
| Bookmark URL bị invalid khi tap (404) | WebView load lỗi → show error page |

---

## 9. Performance

- Index trên `url` cho cả 2 table
- Flow collect with `flowOn(io)` để tránh main thread query
- LazyColumn key = `item.id`

---

## 10. Liên Quan

- [F01_BROWSER_CORE.md](F01_BROWSER_CORE.md) — gọi `record()` trong onPageFinished
- [F09_CLEAR_HISTORY.md](F09_CLEAR_HISTORY.md) — clear all history
- [S08_BOOKMARKS_HISTORY.md](../screens/S08_BOOKMARKS_HISTORY.md)
- [S07_BROWSER_WEBVIEW.md](../screens/S07_BROWSER_WEBVIEW.md) — add bookmark trigger
