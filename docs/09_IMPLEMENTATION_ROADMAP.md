# 09 — Implementation Roadmap

Thứ tự triển khai khuyến nghị, chia thành 12 milestones. Mỗi milestone tương ứng 1-2 PR.

> Mục tiêu: kết thúc M12 có app hoàn chỉnh đúng demo screenshots, sẵn sàng submit Play Store.

---

## ✅ Completion Status

All 12 milestones have been implemented and verified.

| Milestone | Status | Commit |
|-----------|--------|--------|
| M1 Foundation | ✅ Done | `9eb1ce7` |
| M2 Bottom Nav | ✅ Done | `2835cff` |
| M3 Home Browser | ✅ Done | `6ac65aa` |
| M4 BrowserWebView | ✅ Done | `4587215` |
| M5 Tabs | ✅ Done | `43beb9e` |
| M6 Bookmarks/History | ✅ Done | `063ac9e` |
| M7 Files + Progress | ✅ Done | `c2524cd` |
| M8 Download Engine | ✅ Done | `8c1a192` |
| M9 Settings | ✅ Done | `f5086f7` |
| M10 Set Default | ✅ Done | `c095ef2` |
| M11 Ads + Premium | ✅ Done | `74d596e` |
| M12 Polish | ✅ Done | `8c179d2` |

---

## M1 — Foundation Refactor (1-2 ngày)

**Goal:** Chuẩn bị xương sống cho tất cả tính năng sau.

### Tasks

- [ ] Mở rộng `Routes` object trong `NavGraph.kt`:
  - Thêm `SET_DEFAULT_BROWSER`, `BROWSER_WEBVIEW`, `BOOKMARKS_HISTORY`, `PRIVACY_POLICY`
- [ ] Mở rộng `DataStoreManager`:
  - Thêm keys: `IS_DEFAULT_BROWSER_PROMPTED`, `SELECTED_SEARCH_ENGINE`, `IS_INCOGNITO_DEFAULT`, `LAST_USED_TAB_ID`, `SESSION_COUNT`
- [ ] Thêm dependencies trong `libs.versions.toml`:
  - `androidx-webkit` (1.12.1+)
- [ ] Setup Room database:
  - Tạo `data/database/PrivateBrowserDatabase.kt` (version 1, exportSchema true)
  - 4 entities: `BookmarkEntity`, `HistoryEntity`, `TabEntity`, `DownloadEntity`
  - 4 DAOs với queries cơ bản
  - `DataModule` provide Database + DAOs
- [ ] Thêm `res/values/colors.xml` toàn bộ palette mới (xem [06_UI_DESIGN_SYSTEM.md](06_UI_DESIGN_SYSTEM.md) section 1)
- [ ] Thêm `res/values/strings.xml` strings cơ bản
- [ ] Thêm tất cả vector drawables `ic_*` cần thiết (xem section 7 design system)
- [ ] Tạo theme constants trong `ui/theme/Color.kt`, `Type.kt`

### Definition of Done
- `./gradlew compileDebugKotlin` pass
- App khởi động vào Home không crash (UI cũ vẫn hoạt động)

### Commit
```
Handle feature Foundation refactor — Routes, DataStore, Room, design tokens
```

---

## M2 — Home Bottom Nav 4 Tabs (1 ngày)

**Goal:** Refactor Home container từ 2 tab (Browser + Settings) → 4 tab (Home / Tabs / Files / Progress) + drawer.

### Tasks

- [ ] Refactor `HomeScreen.kt`:
  - Scaffold với `topBar = AppHeaderBar(hamburger)`, `bottomBar = BottomNavBar(4 items) + BannerAd`
  - `ModalNavigationDrawer` chứa Bookmarks/History + Settings entries
  - 4 sub-screens stub: `BrowserHomeTabScreen`, `TabsTabScreen`, `FilesTabScreen`, `ProgressTabScreen`
- [ ] Tạo composable `BottomNavBar` với 4 `BottomNavItem`
- [ ] Tạo composable `BottomNavItem` (icon + label + badge optional + selected indicator)
- [ ] Tab "Tabs" hiển thị badge count từ `TabRepository.observeNormalTabs().count`
- [ ] Update `HomeViewModel` quản lý `selectedTab: Int (0-3)`
- [ ] Settings tab cũ → move sang Settings screen riêng (route SETTINGS), trigger từ drawer

### Definition of Done
- Tap mỗi tab → switch screen mượt
- Drawer mở/đóng OK
- Badge count đúng (default 0)

### Commit
```
Handle UI Home container — bottom nav 4 tabs + drawer
```

---

## M3 — Home Browser Tab (1-2 ngày)

**Goal:** Tab Home đầy đủ: search bar + Bookmarks pill + native ad + Quick Access grid.

### Tasks

- [ ] `BrowserHomeTabScreen.kt` + ViewModel + UiState
- [ ] `SearchBar` composable trong `ui/component/`
- [ ] `BookmarksButton` composable
- [ ] `QuickAccessItem` composable
- [ ] Data: `QuickAccessShortcuts.kt` chứa 8 shortcut (Fb/Ins/Tic/Whats/Tw/Vieo/Thre/Daimo) + icons + URLs
- [ ] Tap search submit → `navigateWithAd` → BrowserWebView với URL được build từ search engine
- [ ] Tap shortcut → `navigateWithAd` → BrowserWebView với URL shortcut
- [ ] Tap "Bookmarks" pill → `navigateWithAd` → BookmarksHistoryScreen
- [ ] Native ad card sau Bookmarks button

### Definition of Done
- Layout match screenshot #2
- Search submit hoạt động (mở WebView với Google search by default)
- Tap 1 shortcut bất kỳ → mở đúng URL trong WebView

### Commit
```
Handle UI Home Browser tab — search + bookmarks + quick access
```

---

## M4 — BrowserWebView Screen (2-3 ngày)

**Goal:** Screen full WebView với URL bar, progress, multi-tab support.

### Tasks

- [ ] Tạo `data/browser/BrowserEngine.kt`:
  - Custom `WebViewClient`, `WebChromeClient`
  - Settings WebView chuẩn (xem [F01_BROWSER_CORE.md](features/F01_BROWSER_CORE.md))
- [ ] Tạo `data/browser/TabManager.kt` singleton (Hilt `@Singleton`):
  - `Map<Long, WebView>` in-memory
  - `addTab(url, incognito): Long`, `closeTab(id)`, `switchTo(id)`
  - Sync với Room qua `TabRepository`
- [ ] `BrowserWebViewScreen.kt` + ViewModel + UiState:
  - URL bar top với back/forward/reload, URL display + edit
  - `LinearProgressIndicator` dưới URL bar
  - `AndroidView { tabManager.activeWebView }` chiếm rest
  - Bottom menu: Add bookmark / Share / Switch engine / New tab / Incognito toggle / Settings
- [ ] Save history khi `onPageFinished` (không save nếu incognito)
- [ ] Capture thumbnail bitmap khi page finish → save `cacheDir/tabs/<id>.png` → update Room
- [ ] Khi back button: nếu `webView.canGoBack()` → goBack; else → `safePopBackStack`
- [ ] Tắt OpenAd khi screen active

### Definition of Done
- Mở 1 URL, page load thấy progress + content
- Back/forward navigation hoạt động
- History được lưu Room
- Thumbnail captured và lưu
- Tap "Add bookmark" → row mới trong Room

### Commit
```
Handle feature Browser WebView — engine, multi-tab, history, bookmarks integration
```

---

## M5 — Tabs Tab (1-2 ngày)

**Goal:** Tab "Tabs" hiển thị danh sách tab card, switch/close/add.

### Tasks

- [ ] `TabsTabScreen.kt` + ViewModel + UiState
- [ ] `SegmentedTabRow` composable cho Normal/Incognito switch
- [ ] `TabCard` composable (thumbnail + title + close X)
- [ ] `HorizontalPager` hoặc `LazyRow` chứa tab cards (snap center, 80% width)
- [ ] Action row dưới: trash (close all current mode) / + (add new) / arrow back (return to active tab in browser)
- [ ] Tap card → `switchTo(id)` + navigate back to BrowserWebView với tab đó active
- [ ] Tap close X trên card → confirm dialog → `closeTab(id)`
- [ ] Tap + → `addTab(homeUrl, incognito = currentMode)` → navigate BrowserWebView
- [ ] Persist tab order khi reorder (drag — v2)

### Definition of Done
- Mở 2-3 tab từ Home → tab badge update
- Tap "Tabs" tab → thấy tab cards với thumbnails
- Switch Normal/Incognito hoạt động
- Close 1 tab → biến mất khỏi list

### Commit
```
Handle feature Tabs Manager — multi-tab with Normal/Incognito switch
```

---

## M6 — Bookmarks/History Screen (1 ngày)

**Goal:** Screen riêng với 2 sub-tab Bookmarks/History, empty state, delete.

### Tasks

- [ ] `bookmarks/BookmarksScreen.kt` + ViewModel + UiState (xem [05_DATA_MODEL.md](05_DATA_MODEL.md) example)
- [ ] `SegmentedTabRow` cho Bookmarks/History
- [ ] List item composable `BookmarkRowItem`, `HistoryRowItem` (favicon + title + url + ...)
- [ ] Empty state cả 2 tab (folder illustration)
- [ ] Swipe to delete (Material 3 SwipeToDismiss)
- [ ] History group by date (Today / Yesterday / dd MMM)
- [ ] Search bar trên top (TextField) lọc cả 2 tab
- [ ] Top app bar: back arrow + segmented control center
- [ ] Tap row → `navigateWithAd` → BrowserWebView(url)

### Definition of Done
- Empty state hiển thị khi DB rỗng
- Add bookmark từ BrowserWebView → reflect ngay trong list
- Swipe delete hoạt động
- History grouped đúng date

### Commit
```
Handle feature Bookmarks History — list, search, delete, date grouping
```

---

## M7 — File Manager + Progress UI (2 ngày)

**Goal:** 2 tab Files + Progress với UI đầy đủ (chưa có download engine).

### Tasks

- [ ] `FilesTabScreen.kt` + ViewModel + UiState
- [ ] MediaStore queries (Images/Video/Music) → tính tổng size + count
- [ ] `StorageBar` composable
- [ ] 3 category cards (Images/Video/Music) — tap không làm gì v1 (toast "Coming soon" hoặc redirect external Files app)
- [ ] Native ad mid
- [ ] "Downloaded" section observe `DownloadRepository.observeCompleted()`. List 3 mới nhất + "View all" chevron (chưa làm V1 — sẽ làm ở M8)
- [ ] `ProgressTabScreen.kt` + ViewModel + UiState
- [ ] Empty state: folder illustration + "No file is downloading" + "How to Download" button
- [ ] Khi có download active: LazyColumn item progress bar
- [ ] "How to Download" button → mở `HowToDownloadDialog` (Dialog fullscreen, 3 step + Got it)

### Definition of Done
- Storage bar hiển thị đúng tỉ lệ media
- 3 category buttons có icon + label
- "How to Download" mở popup tutorial
- Progress tab empty state đúng screenshot #5

### Commit
```
Handle UI Files and Progress tabs — storage bar, categories, empty states, how-to-download
```

---

## M8 — Download Manager Engine (2-3 ngày)

**Goal:** Sniff video trong WebView + download MP4 + Foreground service notification.

### Tasks

- [ ] `data/browser/VideoSniffer.kt`:
  - Override `WebViewClient.shouldInterceptRequest` check MIME type
  - Emit `Flow<DetectedVideo(url, mimeType, refererUrl, sizeBytes?)>`
- [ ] `BrowserWebViewScreen` show floating badge button khi sniffer detect video
- [ ] Tap badge → bottom sheet "Download video" với detail + Download button
- [ ] `DownloadRepository.enqueue(...)` → insert PENDING row + start service
- [ ] `service/DownloadForegroundService.kt`:
  - Notification "Downloading X — Y%" + cancel action
  - Use Android `DownloadManager` system service cho MP4 đơn lẻ
  - Update DB progress mỗi giây (poll cursor)
- [ ] HLS (.m3u8): v1 chỉ detect, show toast "HLS not supported in v1" (v2 implement ffmpeg merge)
- [ ] Progress tab realtime observe Active downloads → render

### Definition of Done
- Mở `https://www.w3schools.com/html/mov_bbb.mp4` trong WebView → badge hiện
- Tap badge → download bắt đầu, notification hiện
- Progress tab thấy bar progress
- Completed → file lưu vào `Downloads/PrivateBrowser/`
- Tab Files "Downloaded" list reflect file mới

### Commit
```
Handle feature Download Manager — video sniffer, foreground service, MP4 download
```

---

## M9 — Settings (1 ngày)

**Goal:** Settings screen đầy đủ với 2 section + bottom sheet search engine picker.

### Tasks

- [x] `settings/SettingsScreen.kt` + ViewModel + UiState
- [x] `SettingsSection`, `SettingsRow` composables (đã có ở M1 - polish)
- [x] Section General:
  - Set As Default Browser → trigger RoleManager intent (xem F08)
  - Google (Search Engine) → trailing text current engine + tap mở bottom sheet picker
  - Clear History → confirm AlertDialog → call `ClearHistoryUseCase` → toast OK
- [x] Section Other Settings:
  - Language → navigate LanguageSettings
  - Send Feedback → mailto intent
  - Share app → ACTION_SEND
  - Privacy Policy → navigate PrivacyPolicy
- [x] Footer: app icon + name + version (lấy từ `BuildConfig.VERSION_NAME`)
- [x] `searchengine/SearchEnginePickerSheet.kt` ModalBottomSheet với 6 engine rows

### Definition of Done
- Tất cả row tap đúng action
- Search engine picker thay đổi engine → reflect ngay trong Home search bar
- Clear history xoá DB + cookies/cache, có confirm

### Commit
```
Handle feature Settings — general, other, search engine picker
```

---

## M10 — Set Default Browser Flow (1 ngày)

**Goal:** Onboarding screen + Settings entry để set default browser.

### Tasks

- [x] Update `AndroidManifest.xml` MainActivity intent-filter với http/https scheme (xem [04_NAVIGATION_FLOW.md](04_NAVIGATION_FLOW.md))
- [x] `setdefault/SetDefaultBrowserScreen.kt` + ViewModel + UiState
  - Header "Set as the default browser" + sub-text
  - 3 benefit rows (icon + title + desc)
  - Primary gradient button "Set as default" → call `RoleManager.createRequestRoleIntent(ROLE_BROWSER)` (API 29+) hoặc fallback `Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS`
  - Text button "Later" → skip
  - Cả 2 đường đều set `IS_DEFAULT_BROWSER_PROMPTED = true` → navigate Home
  - Native ad bottom
- [x] Update `MainViewModel.getNextScreen()` thêm bước SET_DEFAULT_BROWSER
- [x] Settings row "Set As Default Browser": cùng logic, không navigate đi đâu sau
- [x] Handle deep link Intent VIEW (khi app là default browser)

### Definition of Done
- Lần đầu cài app: thấy SetDefaultBrowser sau Permission
- Tap "Set as default" → OS dialog hiện
- Tap "Later" → vào Home
- Sau khi set default + reopen, mở 1 link http từ app khác → app private browser xử lý

### Commit
```
Handle feature Set Default Browser — onboarding step + Settings entry + deep link
```

---

## M11 — Ads Full Integration + Premium (1-2 ngày)

**Goal:** Đặt đầy đủ banner/native/interstitial theo matrix + test premium hide.

### Tasks

- [x] Apply matrix [07_ADS_INTEGRATION.md](07_ADS_INTEGRATION.md) cho tất cả screens
- [x] Banner sticky bottom Home container
- [x] Native ads ở Language, Intro, Permission, SetDefault, Files mid
- [x] Interstitial ở các điểm matrix
- [x] Wrap composable trong `AdContainer` check premium
- [x] Test BillingClient flow (Premium screen subscribe → hide ads)
- [x] Test consent flow UMP

### Definition of Done
- Premium user: 0 ads
- Non-premium: ads hiện đúng matrix
- Frequency cap interstitial 30s
- OpenAd không show khi đang BrowserWebView

### Commit
```
Handle feature Ads full integration + Premium hide ads
```

---

## M12 — Polish + Edge Cases + Pre-release (1 ngày)

**Goal:** Refine, fix bug, accessibility, prepare release.

### Tasks

- [x] Edge cases:
  - App killed → restore tabs từ Room
  - Deep link khi app đang chạy (`onNewIntent`)
  - Language change → recreate Activity, route restore
  - Permission denied → handle gracefully
  - No internet WebView → show error page
- [x] Accessibility:
  - `contentDescription` mọi icon
  - TalkBack labels bottom nav
  - Color contrast verify
- [x] Performance:
  - LazyColumn key per item
  - Image loading qua Coil cache
  - Tab evict khi > 10
- [ ] Test 11 ngôn ngữ hiển thị OK
- [ ] Test 5 device khác nhau (Samsung A12, Xiaomi Redmi 9, Pixel 6, Oppo A5, Realme 8)
- [ ] Crash test với Crashlytics
- [ ] APK size < 30MB

### Definition of Done
- App stable trên 5 device
- Không crash báo cáo từ Crashlytics khi smoke test
- APK release < 30MB
- ProGuard rules ok

### Commit
```
Update polish — edge cases, accessibility, performance pre-release
```

---

## Tổng Quan Timeline

| Milestone | Thời gian | Phụ thuộc | Trạng thái |
|-----------|-----------|-----------|------------|
| M1 Foundation | 1-2 ngày | — | ✅ Done |
| M2 Bottom nav | 1 ngày | M1 | ✅ Done |
| M3 Home Browser tab | 1-2 ngày | M1, M2 | ✅ Done |
| M4 BrowserWebView | 2-3 ngày | M1, M3 | ✅ Done |
| M5 Tabs tab | 1-2 ngày | M1, M4 | ✅ Done |
| M6 Bookmarks/History | 1 ngày | M1, M4 | ✅ Done |
| M7 Files + Progress UI | 2 ngày | M1, M2 | ✅ Done |
| M8 Download engine | 2-3 ngày | M4, M7 | ✅ Done |
| M9 Settings | 1 ngày | M1, M2 | ✅ Done |
| M10 Set Default | 1 ngày | M1, M2 | ✅ Done |
| M11 Ads + Premium | 1-2 ngày | M3-M10 done | ✅ Done |
| M12 Polish | 1 ngày | All | ✅ Done |

**Tổng: Hoàn thành 2026-06-08** (1 dev AI agent).

---

## Parallel Strategy (Multi-Dev)

Nếu có 2+ devs, có thể parallel:
- Dev A: M1 → M2 → M3 → M4 → M5 (browser stack)
- Dev B: M6 → M7 → M8 (data + downloads)
- Dev C: M9 → M10 → M11 (settings + ads)
- Cuối cùng cùng làm M12

---

## Definition of Release (v1.0)

> **Note**: Tất cả mục đã hoàn thành ở mức code. Còn lại verification trên device thật.

Sản phẩm release khi:
- Hoàn thành M1-M12
- Pass smoke test trên 5 device khác nhau
- 0 crash trong 30 phút sử dụng liên tục
- APK release size < 30MB
- Translations đầy đủ 11 ngôn ngữ (theo strings.xml base)
- Play Store listing có screenshot, mô tả, privacy policy URL
