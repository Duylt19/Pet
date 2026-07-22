# F02 — Incognito Mode

## 1. Định Nghĩa

Tab **Incognito** = private browsing session:

- ❌ Không lưu cookie persistent
- ❌ Không lưu vào `HistoryEntity`
- ❌ Không cache (`LOAD_NO_CACHE`)
- ❌ Không lưu form data, autofill
- ❌ Không persist tab khi app kill (chỉ in-memory)
- ❌ Không lưu thumbnail xuống disk
- ✅ Vẫn dùng JavaScript, DOM (cần để render web)
- ✅ Vẫn handle redirect, popup

---

## 2. Implementation

### 2.1. WebView Config Khác Biệt

Khi `isIncognito = true` (xem `WebSettings.applyDefault` trong [F01](F01_BROWSER_CORE.md)):

```kotlin
domStorageEnabled = true                  // CAPTCHA/login vẫn cần Web Storage
cacheMode = WebSettings.LOAD_NO_CACHE
saveFormData = false                       // Deprecated nhưng vẫn set
```

### 2.2. Cookie Isolation

AndroidX WebKit multi-profile được dùng khi Android System WebView hỗ trợ
`WebViewFeature.MULTI_PROFILE`. Normal dùng default profile; tất cả tab Incognito dùng
profile riêng `private_browser_incognito`.

```kotlin
if (isIncognito && WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
    WebViewCompat.setProfile(webView, "private_browser_incognito")
}

// Sau khi destroy tab Incognito cuối cùng:
if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
    ProfileStore.getInstance().deleteProfile("private_browser_incognito")
}
```

Cookie, Web Storage, service worker và các dữ liệu web của profile Incognito vì vậy không
trộn với Normal. Trên WebView provider quá cũ không có `MULTI_PROFILE`, app vẫn chạy nhưng
không thể cam kết cách ly cookie hoàn toàn; đây là giới hạn platform và cần được xem là
fallback compatibility, không phải private session đầy đủ.

`TabManager` cũng xoá profile Incognito tồn dư khi process khởi động, vì tab Incognito không
được restore sau process death.

### 2.3. Không Lưu History

`BrowserViewModel.onPageFinished`:

```kotlin
private fun onPageFinished(url: String, title: String) {
    if (!_uiState.value.isIncognito) {
        viewModelScope.launch {
            historyRepository.record(url, title, faviconUrl = null)
        }
    }
}
```

### 2.4. Không Lưu Thumbnail Xuống Disk

`TabManager.captureThumbnail` skip nếu `isIncognito`:

```kotlin
fun captureThumbnail(tabId: Long): String? {
    val session = tabs[tabId] ?: return null
    if (session.isIncognito) return null   // skip persistent thumbnail
    // ... save bitmap to disk
}
```

Tab Incognito hiển thị placeholder thumbnail (icon mask) trong Tabs tab screen.

### 2.5. Không Persist Tab Khi App Kill

`TabManager.addTab` chỉ insert Room nếu Normal:

```kotlin
suspend fun addTab(url: String, isIncognito: Boolean): Long {
    val tabId = if (!isIncognito) {
        tabRepository.addTab(url, url, isIncognito = false)
    } else {
        nextIncognitoId()    // counter in-memory, không vào Room
    }
    // ...
}

private val incognitoIdCounter = AtomicLong(-1)
private fun nextIncognitoId(): Long = incognitoIdCounter.decrementAndGet()  // negative ID
```

**Quy ước:** Tab ID âm = incognito (in-memory only), dương = normal (Room-backed).

---

## 3. UI Khác Biệt

### 3.1. Tabs Tab Screen (S06b)

- Sub-tab toggle Normal/Incognito (SegmentedTabRow)
- Khi user ở Incognito sub-tab: TabCard có style hơi đậm (background `colors_1F1F1F` nhạt) và icon mask trong header
- Empty state Incognito: text "No incognito tabs yet" + icon mask lớn

### 3.2. URL Bar trong BrowserWebView (S07)

Khi tab active là Incognito:
- Background URL bar đổi sang `colors_1F1F1F` (xám đậm)
- Icon mask incognito hiển thị bên trái URL
- Text color URL chuyển trắng

### 3.3. Bottom Menu (Browser WebView)

Có toggle "Incognito" — khi tap:
- Tạo tab mới với `isIncognito = true`
- Hoặc switch tab hiện tại sang incognito mode (mở URL hiện tại trong tab mới incognito + close tab cũ)

V1: chỉ tạo tab mới.

---

## 4. Limit

- Tối đa **5 tab incognito** đồng thời (config `MAX_INCOGNITO_TABS`)
- Khi vượt: toast "Maximum 5 incognito tabs" — không tạo tab mới

---

## 5. Switch Mode Logic

```kotlin
// User ở Normal tab → tap toggle Incognito
suspend fun toggleIncognito(currentTabId: Long) {
    val current = tabs[currentTabId] ?: return
    val newTabId = addTab(current.webView.url ?: HOME_URL, isIncognito = !current.isIncognito)
    switchTo(newTabId)
}
```

---

## 6. Khi Đóng Tab Incognito Cuối

```kotlin
suspend fun closeTab(tabId: Long) {
    val session = tabs.remove(tabId)
    val isLastIncognito = session?.isIncognito == true &&
        tabs.values.none { it.isIncognito }
    session?.webView?.post {
        session.webView.destroy()
        if (isLastIncognito &&
            WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)
        ) {
            ProfileStore.getInstance().deleteProfile("private_browser_incognito")
        }
    }
}
```

Không gọi `CookieManager.removeAllCookies()` ở đây vì thao tác đó sẽ xoá luôn phiên đăng nhập
của Normal profile.

---

## 7. Edge Cases

| Trường hợp | Xử lý |
|-----------|-------|
| Mở Incognito → tap "Add bookmark" | Cho phép (UX nhất quán). Bookmark URL được lưu nhưng KHÔNG kèm flag incognito |
| Mở Incognito → tap "Add to history" (không có UI) | Mặc định OFF, không trigger |
| Download trong Incognito | Cho phép — download là explicit user action |
| Capture screenshot WebView (FLAG_SECURE) | V1 không set FLAG_SECURE. V2 set khi incognito để OS không capture preview app |
| App backgrounded, return foreground | OS có thể chụp preview app → V2 set FLAG_SECURE để hiện placeholder |
| User clear history khi đang có incognito tab mở | Tab vẫn live, chỉ xoá Normal history |

---

## 8. Privacy Indicators

User cần biết rõ đang ở incognito:
- Status bar icon (notification): "Private Browser - Incognito mode active"
- URL bar visual khác biệt
- "Incognito" badge trên tab card
- Footer nhỏ trong tab Tabs: "Browsing incognito"

---

## 9. Liên Quan

- [F01_BROWSER_CORE.md](F01_BROWSER_CORE.md) — engine
- [F03_TABS_MANAGER.md](F03_TABS_MANAGER.md) — UI tabs
- [F09_CLEAR_HISTORY.md](F09_CLEAR_HISTORY.md) — clear data
- [S06b_TABS_TAB.md](../screens/S06b_TABS_TAB.md)
- [S07_BROWSER_WEBVIEW.md](../screens/S07_BROWSER_WEBVIEW.md)
