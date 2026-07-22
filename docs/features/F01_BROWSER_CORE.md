# F01 — Browser Core

WebView engine, lifecycle, multi-tab quản lý.

---

## 1. Mục Tiêu

Một engine WebView ổn định, cấu hình chuẩn, dùng chung cho:
- Tab Normal (lưu cookie, history)
- Tab Incognito (xem [F02_INCOGNITO_MODE.md](F02_INCOGNITO_MODE.md))
- Privacy Policy screen (xem [S14](../screens/S14_PRIVACY_POLICY.md))

### Trạng thái implementation hiện tại (2026-07-10)

- Dùng User-Agent thật của Android System WebView; không đóng đinh phiên bản Chrome.
- Bật JavaScript, DOM storage, third-party cookies và Safe Browsing để tương thích CAPTCHA/OAuth.
- `target="_blank"`/`window.open()` tạo tab mới; `window.close()` đóng tab popup và trở về tab nguồn.
- Hỗ trợ file picker, camera/microphone, geolocation và full-screen custom view.
- Website permission luôn qua xác nhận theo origin rồi mới xin Android runtime permission.
- `intent://` có browser fallback; custom scheme chỉ mở ngoài app từ main-frame/user gesture.
- Renderer WebView chết được tạo lại thay vì làm crash toàn ứng dụng.
- WebView được `onResume()`/`onPause()` theo lifecycle và không giữ tham chiếu Activity khi rời UI.

---

## 2. Class Chính

### 2.1. `BrowserEngine`

File: `app/src/main/java/com/asianmobile/privatebrower/data/browser/BrowserEngine.kt`

Factory tạo WebView được cấu hình sẵn:

```kotlin
class BrowserEngine @Inject constructor(@ApplicationContext private val context: Context) {

    fun createWebView(
        isIncognito: Boolean,
        onProgress: (Int) -> Unit,
        onTitleChange: (String) -> Unit,
        onIconReceived: (Bitmap?) -> Unit,
        onPageStarted: (url: String) -> Unit,
        onPageFinished: (url: String, title: String) -> Unit,
        onCanGoBackForward: (back: Boolean, forward: Boolean) -> Unit,
        onResourceIntercepted: (WebResourceRequest) -> WebResourceResponse? = { null },
    ): BrowserWebViewHandle {
        val webView = WebView(MutableContextWrapper(context))
        if (isIncognito && WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
            WebViewCompat.setProfile(webView, INCOGNITO_PROFILE)
        }
        webView.settings.applyDefault(isIncognito)
        // Attach BrowserWebViewClient + BrowserWebChromeClient callbacks.
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        return BrowserWebViewHandle(webView, chromeClient)
    }
}
```

### 2.2. `BrowserWebViewClient`

```kotlin
class BrowserWebViewClient(
    private val onPageStarted: (url: String) -> Unit,
    private val onPageFinished: (url: String, title: String) -> Unit,
    private val onCanGoBackForward: (back: Boolean, forward: Boolean) -> Unit,
    private val onResourceIntercepted: (WebResourceRequest) -> WebResourceResponse?,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        return when (request.url.scheme) {
            "intent" -> handleIntent(view.context, url)   // intent:// → external
            "mailto", "tel", "sms" -> launchExternal(view.context, request.url)
            else -> false   // load trong WebView
        }
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        onPageStarted(url)
        onCanGoBackForward(view.canGoBack(), view.canGoForward())
    }

    override fun onPageFinished(view: WebView, url: String) {
        onPageFinished(url, view.title ?: url)
        onCanGoBackForward(view.canGoBack(), view.canGoForward())
    }

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        return onResourceIntercepted(request)
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        // Show dialog confirm proceed / cancel — KHÔNG auto handler.proceed()
        // V1 cancel mặc định
        handler.cancel()
    }
}
```

### 2.3. `BrowserWebChromeClient`

```kotlin
class BrowserWebChromeClient(
    private val onProgress: (Int) -> Unit,
    private val onTitleChange: (String) -> Unit,
    private val onIconReceived: (Bitmap?) -> Unit,
    private val createWindowHandler: (Boolean, Message) -> Boolean,
    private val closeWindowHandler: (WebView) -> Unit,
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView, newProgress: Int) { onProgress(newProgress) }
    override fun onReceivedTitle(view: WebView, title: String) { onTitleChange(title) }
    override fun onReceivedIcon(view: WebView, icon: Bitmap) { onIconReceived(icon) }

    override fun onShowFileChooser(
        webView: WebView, filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: FileChooserParams,
    ): Boolean {
        return fileChooserHandler?.invoke(filePathCallback, fileChooserParams) ?: false
    }

    // onCreateWindow/onCloseWindow: nối popup với TabManager.
    // onPermissionRequest: camera/microphone/protected media.
    // onGeolocationPermissionsShowPrompt: quyền vị trí theo origin.
    // onShowCustomView/onHideCustomView: video full-screen.
}
```

### 2.4. `WebSettings.applyDefault(isIncognito)` (extension)

```kotlin
fun WebSettings.applyDefault(isIncognito: Boolean) {
    javaScriptEnabled = true
    domStorageEnabled = true          // CAPTCHA/login cần local/session storage
    useWideViewPort = true
    loadWithOverviewMode = true

    allowFileAccess = false
    allowContentAccess = false
    allowFileAccessFromFileURLs = false
    allowUniversalAccessFromFileURLs = false

    builtInZoomControls = true
    displayZoomControls = false
    setSupportMultipleWindows(true)
    javaScriptCanOpenWindowsAutomatically = false // chỉ popup từ user gesture

    mediaPlaybackRequiresUserGesture = false
    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

    cacheMode = if (isIncognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
    userAgentString = null            // UA thật của WebView provider hiện tại
}
```

---

## 3. State Model

```kotlin
data class BrowserUiState(
    val currentUrl: String = "",
    val title: String = "",
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isIncognito: Boolean = false,
    val faviconBitmap: Bitmap? = null,
    val detectedVideos: List<DetectedVideo> = emptyList(),
)
```

---

## 4. Multi-Tab Manager

### `TabManager.kt`

File: `app/src/main/java/com/asianmobile/privatebrower/data/browser/TabManager.kt`

```kotlin
@Singleton
class TabManager @Inject constructor(
    private val tabRepository: TabRepository,
    private val browserEngine: BrowserEngine,
    @ApplicationContext private val context: Context,
) {
    private val tabs = mutableMapOf<Long, TabSession>()

    private val _activeTabId = MutableStateFlow<Long?>(null)
    val activeTabId: StateFlow<Long?> = _activeTabId.asStateFlow()

    fun activeWebView(): WebView? = _activeTabId.value?.let { tabs[it]?.webView }

    suspend fun addTab(url: String, isIncognito: Boolean): Long {
        val tabId = tabRepository.addTab(url = url, title = url, isIncognito = isIncognito)
        val webView = browserEngine.createWebView(isIncognito = isIncognito, ...)
        webView.loadUrl(url)
        tabs[tabId] = TabSession(tabId, webView, isIncognito)
        _activeTabId.value = tabId
        return tabId
    }

    fun switchTo(tabId: Long) { _activeTabId.value = tabId }

    suspend fun closeTab(tabId: Long) {
        tabs.remove(tabId)?.webView?.apply {
            stopLoading()
            destroy()
        }
        tabRepository.closeTab(tabId)
        if (_activeTabId.value == tabId) {
            _activeTabId.value = tabs.keys.lastOrNull()
        }
    }

    fun captureThumbnail(tabId: Long): String? {
        val webView = tabs[tabId]?.webView ?: return null
        val bitmap = Bitmap.createBitmap(webView.width.coerceAtLeast(1), webView.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        Canvas(bitmap).also { webView.draw(it) }
        val file = File(context.cacheDir, "tabs/$tabId.png").apply { parentFile?.mkdirs() }
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 80, it) }
        return file.absolutePath
    }
}

private data class TabSession(val id: Long, val webView: WebView, val isIncognito: Boolean)
```

### Quy tắc

- 1 tab = 1 WebView instance, không reuse
- Tab Normal lưu Room; Tab Incognito chỉ in-memory
- Khi app process die: WebView state mất (Android không serialize được toàn bộ DOM). Restore từ URL trong Room — load lại.
- Memory limit: 10 tab Normal + 5 tab Incognito. Khi vượt: alert user "Too many tabs" hoặc evict oldest (config).

---

## 5. Lifecycle Hooks

Trong `BrowserWebViewScreen`:

```kotlin
DisposableEffect(tabId) {
    val webView = tabManager.tabs[tabId]?.webView
    webView?.onResume()
    onDispose {
        webView?.onPause()
    }
}
```

Khi composable rời composition (Home → Bookmarks): `onPause` để dừng JS/audio.

---

## 6. Video Sniffer Hook

WebViewClient.shouldInterceptRequest callback → kiểm tra MIME type:

```kotlin
override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
    val url = request.url.toString()
    val isVideo = url.matches(VIDEO_REGEX) ||
        request.requestHeaders["Accept"]?.contains("video/") == true
    if (isVideo) {
        videoSniffer.onDetected(DetectedVideo(url, refererUrl = view.url))
    }
    return null  // không intercept, để default load
}

private val VIDEO_REGEX = Regex(".+\\.(mp4|m3u8|ts|mov|avi|mkv)(\\?.*)?$", RegexOption.IGNORE_CASE)
```

Chi tiết: [F05_DOWNLOAD_MANAGER.md](F05_DOWNLOAD_MANAGER.md).

---

## 7. Edge Cases

| Trường hợp | Xử lý |
|-----------|-------|
| URL malformed | `URLUtil.guessUrl()` fix nhẹ; nếu fail → search engine |
| HTTPS error | Show dialog confirm proceed (v2). V1: cancel |
| `intent://` URLs | Parse + mở app ngoài; nếu không có app thì load `browser_fallback_url` |
| File upload | `onShowFileChooser` mở SAF picker |
| Permission request (camera/mic) | Hỏi theo origin, sau đó xin Android runtime permission |
| Geolocation request | Hỏi theo origin, chỉ hoạt động trên secure origin |
| Popup new window (`window.open`) | `onCreateWindow` → mở tab mới với URL |
| Tab khi process restart | WebView state mất, load lại URL từ Room |
| Tab quá nhiều | Alert + evict oldest by `lastActiveAt` |
| OOM khi nhiều tab | Throttle thumbnail capture, downscale bitmap 50% |

---

## 8. DI Module

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object BrowserModule {
    @Provides @Singleton fun provideBrowserEngine(@ApplicationContext c: Context) = BrowserEngine(c)
    @Provides @Singleton fun provideTabManager(
        tabRepository: TabRepository,
        browserEngine: BrowserEngine,
        @ApplicationContext c: Context,
    ) = TabManager(tabRepository, browserEngine, c)
}
```

---

## 9. Liên Quan

- [F02_INCOGNITO_MODE.md](F02_INCOGNITO_MODE.md) — incognito specifics
- [F03_TABS_MANAGER.md](F03_TABS_MANAGER.md) — UI tab management
- [F05_DOWNLOAD_MANAGER.md](F05_DOWNLOAD_MANAGER.md) — video sniffing
- [S07_BROWSER_WEBVIEW.md](../screens/S07_BROWSER_WEBVIEW.md) — screen using this engine
