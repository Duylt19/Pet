# S07 — Browser WebView

## Visual Reference

- Screenshot: chưa có demo trực tiếp; inference từ behavior chuẩn của browser apps

## Mục Đích

Màn hình full-screen WebView render trang web. Chứa URL bar, progress bar, navigation controls, menu actions.

## Vị Trí Trong Navigation

- Route: `Routes.BROWSER_WEBVIEW?url=<encoded>&incognito=<bool>`
- Vào từ: Home Browser tab (search/shortcut), Tabs tab (tap card/+), Bookmarks (tap row), Quick Access shortcut, Deep link (Intent.VIEW)
- Ra đến: back → tab Home; tap menu → popup neo trên toolbar hoặc Bookmarks/Settings
- Back behavior:
  - Nếu `webView.canGoBack()` → goBack
  - Nếu không → safePopBackStack → Home

## Layout Breakdown

```
┌─────────────────────────────────────┐
│  [⌂] [shield] google.com             │  <- URL bar (top)
│  ▰▰▰▰▰▰▰▰▰▱  (80%)                │  <- LinearProgressIndicator
├─────────────────────────────────────┤
│                                     │
│                                     │
│        [WebView fill rest]          │
│                                     │
│                                     │
│                                     │
│                                     │
│              [👁 badge]              │  <- Floating badge khi sniff video
│                                     │
├─────────────────────────────────────┤
│  [←] [→] [bookmark] [tabs] [⋮]     │  <- Bottom toolbar
└─────────────────────────────────────┘
```

**Specs:**

- URL bar:
  - Height `_36sdp` (~48dp)
  - Background `colors_F2F2F7`
  - Left: back button 18sdp icon (disabled khi !canGoBack)
  - Forward button 18sdp (disabled khi !canGoForward)
  - Reload/Stop button: icon swap theo isLoading
  - URL display: pill background white, click → editable
  - Right: menu 3-dot vertical 18sdp
- Khi incognito: URL bar background `colors_1F1F1F`, text white, hiển thị icon mask trái URL
- LinearProgressIndicator height 2dp, color primary, alpha 0.6 cho background
- WebView: chiếm fill rest, không có padding
- Floating sniff badge:
  - Position bottom-right 16dp offset
  - Circle 56dp, color primary, icon download
  - Badge nhỏ số count (nếu > 1 video detected)

### Anchored More Popup (khi tap 3-dot)

```
┌─────────────────────────────────────┐
│ [←] [→] [★] [↓] [↻]               │
│  New Tab                            │
│  New Incognito Tab                  │
│  History                            │
│  Add/Remove Bookmark                │
│  Downloads                          │
│  Find in Page                       │
│  Share                              │
│  Desktop Site                 [✓]   │
│  Settings                           │
│  Help & Feedback                    │
└─────────────────────────────────────┘
```

## States

| State | Display |
|-------|---------|
| Loading | Progress bar visible, reload icon = stop |
| Idle | Progress hidden, reload icon = refresh |
| canGoBack/Forward | Buttons enabled/disabled |
| URL đã lưu | Bookmark icon filled + menu hiện "Remove bookmark" |
| URL chưa lưu | Bookmark icon outline + menu hiện "Add bookmark" |
| URL nội bộ/không hợp lệ | Bookmark action disabled |
| More menu | Neo vào nút ba chấm, dismiss khi tap ngoài/Back/chọn action |
| Incognito | Dark URL bar, mask icon |
| Sniff video detected | Floating badge visible |
| Error page | WebView render system error |
| SSL warning | Dialog confirm (v2: proceed/cancel) |
| URL editing | Pill expand → TextField focus, keyboard show |

## ViewModel Contract

```kotlin
@HiltViewModel
class BrowserWebViewViewModel @Inject constructor(
    private val tabManager: TabManager,
    private val historyRepository: HistoryRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val searchEngineRepository: SearchEngineRepository,
    private val videoSniffer: VideoSniffer,
    @SavedStateHandle private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    data class UiState(
        val tabId: Long = 0,
        val currentUrl: String = "",
        val displayUrl: String = "",
        val title: String = "",
        val progress: Int = 0,
        val isLoading: Boolean = false,
        val canGoBack: Boolean = false,
        val canGoForward: Boolean = false,
        val isIncognito: Boolean = false,
        val detectedVideos: List<DetectedVideo> = emptyList(),
        val isEditingUrl: Boolean = false,
        val urlInputText: String = "",
        val showMoreMenu: Boolean = false,
        val isBookmarked: Boolean = false,
        val canBookmark: Boolean = false,
        val isDesktopMode: Boolean = false,
    )

    val uiState: StateFlow<UiState>

    fun onBackClicked()
    fun onForwardClicked()
    fun onReloadOrStopClicked()
    fun onUrlBarClicked()              // enter edit mode
    fun onUrlInputChanged(text: String)
    fun onUrlSubmitted()
    fun onMoreMenuClick()
    fun dismissMoreMenu()
    fun toggleBookmark()
    fun restoreBookmark(bookmark: BookmarkEntity)
    fun onShare(context: Context)
    fun onChangeSearchEngine()         // open picker sheet
    fun onNewTab()
    fun onNewIncognitoTab()
    fun onSettingsClicked()
    fun onSniffBadgeClicked()          // open download sheet
    fun onDownloadVideo(video: DetectedVideo)
}
```

## Resources

```xml
<string name="browser_add_bookmark">Add bookmark</string>
<string name="browser_remove_bookmark">Remove bookmark</string>
<string name="browser_menu_share">Share page</string>
<string name="browser_menu_reload">Reload</string>
<string name="browser_menu_change_engine">Change search engine</string>
<string name="browser_menu_new_tab">New tab</string>
<string name="browser_menu_new_incognito_tab">New incognito tab</string>
<string name="browser_menu_settings">Settings</string>
<string name="browser_url_placeholder">Enter URL or search</string>
<string name="browser_video_detected_title">Video detected</string>
<string name="browser_video_detected_action">Tap to download</string>
<string name="browser_bookmark_added">Bookmark added</string>
<string name="browser_bookmark_removed">Bookmark removed</string>
<string name="browser_bookmark_undo">Undo</string>
<string name="browser_share_subject">Check this out</string>
```

Drawables:
- `ic_arrow_back`, `ic_forward`, `ic_refresh`, `ic_stop`, `ic_more_vert`
- `ic_lock_secure`, `ic_lock_warning` (cho HTTPS/HTTP indicator)
- `ic_incognito_mask`
- `ic_download_arrow` (sniff badge)

## Ads

- Banner: KHÔNG hiển thị (full screen content)
- Interstitial: show khi back về Home (`navigateWithAd`)
- OpenAd: OFF khi screen visible (`needShowOpenAds = false`)
- Native: KHÔNG

## Edge Cases & Accessibility

- WebView load fail (no internet) → render error page (WebView default behavior)
- SSL error: dialog (v2)
- Form input upload → SAF picker
- Permission camera/mic/vị trí: hỏi theo website origin, sau đó xin runtime permission
- `target="_blank"`/`window.open`: mở tab mới; `window.close`: quay về tab nguồn
- Full-screen video/custom view: overlay toàn màn hình, Back thoát full-screen trước
- URL > 80 chars: ellipsis middle (show domain + ... + path tail)
- Khi incognito: status bar dark + icon mask
- Touch back button hardware: same logic như button onBack
- Bookmark state collect trực tiếp từ Room theo URL của active tab; đổi URL/tab phải cập nhật icon ngay
- Bỏ bookmark hiển thị Snackbar Undo; thao tác nhanh được serialize để tránh add/remove race
- Desktop Site là state riêng từng tab; popup/window mới kế thừa mode của tab nguồn
- Vượt giới hạn normal/incognito tab phải báo Snackbar, không fail im lặng
- Deep link HTTP(S) bỏ qua launcher/App Open ad để trang đích không bị quảng cáo che khi mở từ app khác
- WebView khi background → onPause; khi resume → onResume
- contentDescription cho mọi icon
- Long URL bar: scroll horizontal
- Khi sniff video: TalkBack announce "Video detected, tap to download"

## Acceptance Criteria

- [ ] Load URL → render đúng page
- [ ] Progress bar update real-time
- [ ] Back button: webView.canGoBack → goBack; else pop
- [ ] URL bar edit + submit → load new URL
- [x] More popup neo đúng nút, dismiss/scroll tốt trên màn hình nhỏ
- [x] Bookmark icon phản ứng theo URL/Room; add/remove/undo hoạt động
- [x] Share → system share sheet, có fallback khi không có app xử lý
- [x] Desktop mode và tab count tách đúng theo từng tab/mode
- [ ] Incognito mode visual khác biệt
- [ ] Sniff badge khi MP4 detected
- [ ] OpenAd OFF khi screen active
- [ ] Interstitial khi back về Home
- [x] CAPTCHA (reCAPTCHA checkbox + image challenge) render và tương tác được
- [x] Google sign-in page load bằng User-Agent WebView hiện tại
- [x] Popup login/link `target="_blank"` mở tab mới và đóng đúng tab
- [x] File upload mở system picker
- [x] Website permission camera/mic/vị trí có xác nhận theo origin

## Liên Quan

- [F01_BROWSER_CORE.md](../features/F01_BROWSER_CORE.md)
- [F02_INCOGNITO_MODE.md](../features/F02_INCOGNITO_MODE.md)
- [F04_BOOKMARKS_HISTORY.md](../features/F04_BOOKMARKS_HISTORY.md)
- [F05_DOWNLOAD_MANAGER.md](../features/F05_DOWNLOAD_MANAGER.md)
- [F07_SEARCH_ENGINE.md](../features/F07_SEARCH_ENGINE.md)
