# S08 — Bookmarks / History

## Visual Reference

- Screenshot: [Screenshot_20260608-095406.png](../assets/screenshots/Screenshot_20260608-095406.png) (Bookmarks empty)

## Mục Đích

Hiển thị 2 tab: Bookmarks và History, với empty state khi rỗng, list khi có. Cho phép search, delete, navigate.

## Vị Trí Trong Navigation

- Route: `Routes.BOOKMARKS_HISTORY`
- Vào từ: Home drawer (Bookmarks/History), Home Browser tab (Bookmarks pill)
- Ra đến: back về Home; tap item → BROWSER_WEBVIEW

## Layout Breakdown

```
┌─────────────────────────────────────┐
│  ←     [Bookmarks]  History         │  <- top bar: back + segmented control
├─────────────────────────────────────┤
│  🔍 Search...                       │  <- optional search bar (v1 có thể skip)
├─────────────────────────────────────┤
│                                     │
│        ┌──────┐                     │
│        │ 📁  │  + + +              │  <- Empty illustration
│        └──────┘                     │
│                                     │
│      No bookmarks added yet         │
│                                     │
│  ─── or list ───                    │
│                                     │
│  Today                              │  <- date group header (History only)
│  ┌────────────────────────────────┐ │
│  │ 🌐  YouTube — youtube.com     │ │  <- favicon + title + url
│  │                          [×]   │ │     swipe to delete
│  └────────────────────────────────┘ │
│  ┌────────────────────────────────┐ │
│  │ 🌐  Google — google.com       │ │
│  └────────────────────────────────┘ │
│  Yesterday                          │
│  ┌────────────────────────────────┐ │
│  │ ...                            │ │
│  └────────────────────────────────┘ │
├─────────────────────────────────────┤
│  [Sticky banner ad]                 │
└─────────────────────────────────────┘
```

**Specs:**

- Top bar: AppHeaderBar với back arrow + SegmentedTabRow center 2 tab "Bookmarks"/"History"
- SegmentedTabRow:
  - Container pill `colors_F2F2F7`
  - Selected: bg white pill, text black bold
  - Unselected: text `colors_808080`
- Search bar (optional v1): pill input dưới top bar
- Empty state (Bookmarks): "No bookmarks added yet"
- Empty state (History): "No history yet"
- List items:
  - LazyColumn với section header sticky (History only)
  - Bookmark card nền `#212327`, radius 16px, padding 12px
  - Item row: favicon 40px, title 14px, URL 12px, star và more 20px
  - Native ad dạng item nằm sau bookmark thứ 2; nếu danh sách có ít hơn 2 item thì không chèn
  - Tap row → navigate BrowserWebView
- Banner ad sticky bottom

## States

| State | Display |
|-------|---------|
| Bookmarks tab empty | Empty illustration + "No bookmarks added yet" |
| Bookmarks tab có items | List items |
| History tab empty | Empty illustration + "No history yet" |
| History tab có items | Grouped list by date |
| Search active | Filtered list |
| Loading | Skeleton rows |

## ViewModel Contract

```kotlin
@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository,
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    data class UiState(
        val selectedTab: BookmarksTab = BookmarksTab.BOOKMARKS,
        val searchQuery: String = "",
        val bookmarks: List<Bookmark> = emptyList(),
        val historyGrouped: Map<String, List<HistoryItem>> = emptyMap(),
        val isLoading: Boolean = false,
    )

    val uiState: StateFlow<UiState>
    val navigateEvent: SharedFlow<String>

    fun onTabSelected(tab: BookmarksTab)
    fun onSearchQueryChanged(q: String)
    fun onItemClicked(url: String)
    fun onDeleteBookmark(id: Long)
    fun onDeleteHistoryItem(id: Long)
    fun onClearAllHistory()                  // optional menu action
}

enum class BookmarksTab { BOOKMARKS, HISTORY }
```

## Resources

```xml
<string name="bookmarks_title">Bookmarks</string>
<string name="bookmarks_segment_bookmarks">Bookmarks</string>
<string name="bookmarks_segment_history">History</string>
<string name="bookmarks_empty_message">No bookmarks added yet</string>
<string name="history_empty_message">No history yet</string>
<string name="bookmarks_search_placeholder">Search bookmarks and history</string>
<string name="history_group_today">Today</string>
<string name="history_group_yesterday">Yesterday</string>
<string name="bookmarks_swipe_delete">Delete</string>
<string name="bookmarks_delete_confirm">Delete this bookmark?</string>
<string name="history_clear_all_action">Clear All History</string>
```

Drawables:
- `ic_folder_empty.xml` (đã có)
- `ic_globe_fallback.xml` (favicon fallback)

## Ads

- Sticky banner bottom (`R.string.banner_id_bookmarks` hoặc dùng chung với home)
- Native item placement `SCREEN_BOOKMARKS`, điều khiển bằng `is_show_native_bookmarks`,
  hiển thị sau bookmark thứ 2 theo Figma `11113:1192`
- Interstitial khi enter screen (navigateWithAd từ Home)

## Edge Cases & Accessibility

- Item title rỗng → fallback dùng URL hostname
- URL ellipsis nếu > 50 chars
- Swipe to delete: undo snackbar 3s
- Long press → multi-select mode (v2)
- contentDescription cho favicon, delete button
- Empty state: animate folder illustration nhẹ
- Search debounce 300ms

## Acceptance Criteria

- [ ] Layout match screenshot #8 (Bookmarks empty)
- [ ] SegmentedTabRow switch 2 tab
- [ ] History group by date
- [ ] Tap item → BrowserWebView mở URL
- [ ] Delete bookmark/history hoạt động
- [ ] Empty state đúng cho 2 tab
- [ ] Search filter cả 2 tab

## Liên Quan

- [F04_BOOKMARKS_HISTORY.md](../features/F04_BOOKMARKS_HISTORY.md)
- [F09_CLEAR_HISTORY.md](../features/F09_CLEAR_HISTORY.md)
- [S07_BROWSER_WEBVIEW.md](S07_BROWSER_WEBVIEW.md)
