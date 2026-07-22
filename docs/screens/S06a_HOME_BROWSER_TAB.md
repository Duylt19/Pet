# S06a — Home Browser Tab

## Visual Reference

- Screenshot: [Screenshot_20260608-094940.png](../assets/screenshots/Screenshot_20260608-094940.png)

## Mục Đích

Tab "Home" trong bottom nav — entry point để search, mở quick access social shortcuts, vào bookmarks.

## Vị Trí Trong Navigation

- Render bên trong `HomeScreen` khi `selectedTab == 0`
- Không phải route riêng
- Action ra: BROWSER_WEBVIEW (search submit hoặc shortcut tap), BOOKMARKS_HISTORY (Bookmarks button)

## Layout Breakdown (top → bottom)

```
┌─────────────────────────────────────┐
│  [SearchBar with G icon]            │  <- Pill, height ~30sdp
│  [G] Search or type URL             │
├─────────────────────────────────────┤
│                                     │
│  [Bookmarks pill - purple gradient] │  <- Full width pill, "R" icon + "Bookmarks"
│                                     │
├─────────────────────────────────────┤
│  ┌────────────────────────────────┐ │
│  │ [Ad badge]                     │ │  <- Native ad card
│  │ Native ad content              │ │
│  │ [CTA button]                   │ │
│  └────────────────────────────────┘ │
├─────────────────────────────────────┤
│  [Fb]      [Ins]                    │  <- 2 columns shortcut grid
│  [Tic]     [Whats]                  │
│  [Tw]      [Vieo]                   │
│  [Thre]    [Daimo]                  │
│  ...                                │
└─────────────────────────────────────┘
```

**Specs:**

- Padding screen horizontal: `_12sdp`
- SearchBar:
  - Height ~30sdp
  - Background `colors_F2F2F7`
  - Pill shape
  - Leading: G icon (or current engine icon) 18sdp
  - Placeholder "Search or type URL" Body L `colors_808080`
  - IME action Search
- Spacer `_9sdp`
- Bookmarks pill button:
  - Background `colors_8B5CF6` (Bookmarks button purple) hoặc gradient
  - Height ~30sdp pill
  - "R" icon in white circle + "Bookmarks" white text Body L Bold center
- Spacer `_12sdp`
- Native ad card:
  - Height auto theo content
  - Layout: small native ad layout
- Spacer `_12sdp`
- Quick access grid (xem [F10](../features/F10_QUICK_ACCESS.md)):
  - 2 columns, gap `_9sdp` ngang + dọc
  - Mỗi item: pill height ~36sdp, icon 24sdp + label

## States

| State | Display |
|-------|---------|
| Default | SearchBar empty + Bookmarks + Ad + 8 shortcuts |
| Type search | SearchBar focused, keyboard hiện |
| Submit search empty | Mở engine home URL |
| Submit search có query | Mở BrowserWebView với search URL |
| Submit URL | Mở BrowserWebView direct |
| Tap microphone | Mở system speech recognizer, điền kết quả vào search query, chờ 1 giây rồi tự search |
| Speech service unavailable | Hiển thị thông báo, không crash hoặc im lặng |
| Tap shortcut | navigateWithAd → BrowserWebView với URL shortcut |
| Tap Bookmarks pill | navigateWithAd → BookmarksHistoryScreen |
| Ad load fail | Hide ad card (collapse height 0) |

## ViewModel Contract

```kotlin
@HiltViewModel
class BrowserHomeTabViewModel @Inject constructor(
    private val searchEngineRepository: SearchEngineRepository,
) : ViewModel() {

    data class UiState(
        val searchQuery: String = "",
        val searchEngine: SearchEngine = SearchEngine.GOOGLE,
        val shortcuts: List<QuickAccessShortcut> = QuickAccessShortcuts.DEFAULTS,
    )

    private val _navigateEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navigateEvent: SharedFlow<String> = _navigateEvent.asSharedFlow()

    val uiState: StateFlow<UiState>

    fun onSearchQueryChanged(q: String)
    fun onSearchSubmit()
    fun onShortcutClicked(shortcut: QuickAccessShortcut)
    fun onBookmarksClicked()
}
```

## Resources

```xml
<string name="home_search_placeholder_text">Search or type URL</string>
<string name="home_bookmarks_button_label">Bookmarks</string>

<!-- + 8 quick_access_*_label đã liệt kê F10 -->
```

Drawables:
- `ic_google_g.xml` + 5 search engine icons
- `ic_bookmarks_pill.xml` (chữ R trong circle)
- `ic_shortcut_*.xml` (8 shortcut icons)

## Ads

- Banner sticky inherit từ container (S06)
- Native ad card sau Bookmarks button — `R.string.native_id_home_card`

## Edge Cases & Accessibility

- SearchBar focus + back → bỏ focus
- Voice search dùng Activity Result API; kết quả đọc từ `RecognizerIntent.EXTRA_RESULTS`
- Speech result hiển thị trong input 1 giây trước khi tự submit; user sửa text hoặc submit thủ công sẽ hủy pending auto-search
- Android 11+ khai báo speech recognition trong manifest `queries`
- Không có recognition activity → hiển thị unavailable toast
- Submit search trong khi không có internet → WebView báo error page
- Shortcut URL bị block → WebView báo error
- Long press shortcut (v2) → menu Edit/Remove
- contentDescription cho SearchBar: "Search or type URL"
- contentDescription cho shortcut: "Open Facebook"

## Acceptance Criteria

- [ ] Layout match screenshot #2
- [ ] SearchBar functional với search/URL detection ([F07](../features/F07_SEARCH_ENGINE.md))
- [ ] Bookmarks pill tap → BookmarksHistory với ad
- [ ] 8 shortcut tap → BrowserWebView với URL đúng
- [ ] Native ad load đúng layout
- [ ] Premium ẩn ad

## Liên Quan

- [F07_SEARCH_ENGINE.md](../features/F07_SEARCH_ENGINE.md)
- [F10_QUICK_ACCESS.md](../features/F10_QUICK_ACCESS.md)
- [S07_BROWSER_WEBVIEW.md](S07_BROWSER_WEBVIEW.md)
- [S08_BOOKMARKS_HISTORY.md](S08_BOOKMARKS_HISTORY.md)
