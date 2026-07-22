package com.asianmobile.privatebrower.ui.browser

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.privatebrower.ads.ui.interstitial.InterstitialUtil
import com.asianmobile.privatebrower.data.browser.DetectedVideo
import com.asianmobile.privatebrower.data.browser.TabManager
import com.asianmobile.privatebrower.data.browser.TabSession
import com.asianmobile.privatebrower.data.browser.VideoSniffer
import com.asianmobile.privatebrower.data.database.entity.BookmarkEntity
import com.asianmobile.privatebrower.data.download.DownloadStorage
import com.asianmobile.privatebrower.data.local.DataStoreManager
import com.asianmobile.privatebrower.data.repository.BookmarkRepository
import com.asianmobile.privatebrower.data.repository.DownloadRepository
import com.asianmobile.privatebrower.data.repository.SearchEngineRepository
import com.asianmobile.privatebrower.data.util.UrlBuilder
import com.asianmobile.privatebrower.service.DownloadForegroundService
import com.asianmobile.privatebrower.ui.permission.PermissionPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/** What the user long-pressed in the WebView: an image and/or a link URL. */
data class LinkContextInfo(val imageUrl: String?, val linkUrl: String?)

private data class ActiveBookmarkState(
    val url: String,
    val bookmark: BookmarkEntity?
)

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val tabManager: TabManager,
    private val bookmarkRepository: BookmarkRepository,
    private val videoSniffer: VideoSniffer,
    private val downloadRepository: DownloadRepository,
    private val dataStoreManager: DataStoreManager,
    private val searchEngineRepository: SearchEngineRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val requestedUrl = savedStateHandle.get<String>("url")
    private val initialIncognito = savedStateHandle.get<Boolean>("incognito") ?: false

    private val _showMoreMenu = MutableStateFlow(false)
    private val _showVideoSheet = MutableStateFlow(false)
    private val _showFindInPage = MutableStateFlow(false)
    private val _findInPageQuery = MutableStateFlow("")
    private val _findInPageCurrentMatch = MutableStateFlow(0)
    private val _findInPageTotalMatches = MutableStateFlow(0)
    private val _linkContextMenu = MutableStateFlow<LinkContextInfo?>(null)
    private val bookmarkMutex = Mutex()
    private val eventChannel = Channel<BrowserUiEvent>(Channel.BUFFERED)

    val events = eventChannel.receiveAsFlow()
    val activeSession = tabManager.activeSession

    suspend fun shouldOpenAppSettingsForWebPermission(
        permission: String,
        canShowRationale: Boolean
    ): Boolean = PermissionPolicy.shouldOpenAppSettings(
        requestCount = dataStoreManager.runtimePermissionRequestCount(permission),
        canShowRationale = canShowRationale
    )

    suspend fun markWebPermissionsRequested(permissions: Collection<String>) {
        dataStoreManager.markRuntimePermissionsRequested(permissions)
    }

    /** Set when the user long-presses an image or link; drives the save/copy context menu. */
    val linkContextMenu: StateFlow<LinkContextInfo?> = _linkContextMenu.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val activeBookmarkState = tabManager.activeSession.flatMapLatest { session ->
        if (session == null) {
            flowOf(ActiveBookmarkState(url = "", bookmark = null))
        } else {
            session.url.flatMapLatest { url ->
                if (!isBookmarkableUrl(url)) {
                    flowOf(ActiveBookmarkState(url = url, bookmark = null))
                } else {
                    bookmarkRepository.observeByUrl(url)
                        .map { bookmark -> ActiveBookmarkState(url = url, bookmark = bookmark) }
                        .catch { emit(ActiveBookmarkState(url = url, bookmark = null)) }
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<BrowserUiState> = combine(
        tabManager.activeSession.flatMapLatest { session ->
            if (session == null) {
                flowOf(BrowserUiState())
            } else {
                combine(
                    session.url,
                    session.title,
                    session.progress,
                    session.isLoading,
                    session.canGoBack,
                    session.canGoForward,
                    session.favicon,
                    session.isDesktopMode
                ) { flows ->
                    val url = flows[0] as String
                    val title = flows[1] as String
                    val progress = flows[2] as Int
                    val isLoading = flows[3] as Boolean
                    val canGoBack = flows[4] as Boolean
                    val canGoForward = flows[5] as Boolean
                    val favicon = flows[6] as Bitmap?
                    val isDesktopMode = flows[7] as Boolean
                    BrowserUiState(
                        url = url,
                        title = title,
                        isLoading = isLoading,
                        progress = progress,
                        canGoBack = canGoBack,
                        canGoForward = canGoForward,
                        isIncognito = session.isIncognito,
                        favicon = favicon,
                        isDesktopMode = isDesktopMode
                    )
                }
            }
        },
        _showMoreMenu,
        tabManager.allSessions,
        videoSniffer.detectedVideos,
        _showVideoSheet,
        _showFindInPage,
        _findInPageQuery,
        _findInPageCurrentMatch,
        _findInPageTotalMatches,
        activeBookmarkState
    ) { flows ->
        val state = flows[0] as BrowserUiState
        val showMenu = flows[1] as Boolean
        @Suppress("UNCHECKED_CAST")
        val sessions = flows[2] as List<TabSession>
        @Suppress("UNCHECKED_CAST")
        val videos = flows[3] as List<DetectedVideo>
        val showVideoSheet = flows[4] as Boolean
        val showFindInPage = flows[5] as Boolean
        val findInPageQuery = flows[6] as String
        val findInPageCurrentMatch = flows[7] as Int
        val findInPageTotalMatches = flows[8] as Int
        val bookmarkState = flows[9] as ActiveBookmarkState

        state.copy(
            showMoreMenu = showMenu,
            tabCount = sessions.count { it.isIncognito == state.isIncognito }.coerceAtLeast(1),
            isBookmarked = bookmarkState.url == state.url && bookmarkState.bookmark != null,
            canBookmark = isBookmarkableUrl(state.url),
            detectedVideos = videos,
            showVideoSheet = showVideoSheet,
            showFindInPage = showFindInPage,
            findInPageQuery = findInPageQuery,
            findInPageCurrentMatch = findInPageCurrentMatch,
            findInPageTotalMatches = findInPageTotalMatches
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BrowserUiState()
    )

    init {
        // Disable App Open Ad when browsing
        InterstitialUtil.getInstance().openAd?.needShowOpenAds = false

        viewModelScope.launch {
            tabManager.awaitInitialized()
            if (requestedUrl.isNullOrBlank()) {
                val homeUrl = searchEngineRepository.observeCurrent().first().homeUrl
                tabManager.ensureActiveTab(homeUrl, initialIncognito)
            } else {
                val newTabId = tabManager.addTab(requestedUrl, initialIncognito)
                if (newTabId == null) {
                    val webView = tabManager.activeWebView()
                    webView?.post {
                        webView.loadUrl(requestedUrl)
                    }
                }
            }
        }

    }

    fun refreshActiveTabIfNeeded() {
        tabManager.refreshActiveTabIfNeeded()
    }

    fun onMoreMenuClick() {
        if (_showMoreMenu.value) {
            dismissMoreMenu()
            return
        }
        hideFindInPage()
        _showVideoSheet.value = false
        _showMoreMenu.value = true
    }

    fun dismissMoreMenu() {
        _showMoreMenu.value = false
    }

    fun showVideoSheet() {
        dismissMoreMenu()
        _showVideoSheet.value = true
    }

    fun hideVideoSheet() {
        _showVideoSheet.value = false
    }

    fun downloadVideos(context: android.content.Context, videos: List<DetectedVideo>) {
        viewModelScope.launch {
            val activeUserAgent = tabManager.activeWebView()
                ?.settings
                ?.userAgentString
            for (video in videos) {
                // HLS/DASH: store the manifest URL; the engine merges segments and remuxes.
                // The finished artifact is an MP4/WebM/.ts, so name it .mp4 up front (the
                // service renames to the real container when it differs).
                val fileName = if (video.isHls || video.isDash) {
                    val base = video.displayName.substringBeforeLast('.', video.displayName)
                        .ifBlank { "video_${video.url.hashCode().toUInt()}" }
                    "$base.mp4"
                } else {
                    val name = video.displayName
                    val hasExpectedExtension = name.substringAfterLast('.', "")
                        .equals(video.fileExtension, ignoreCase = true)
                    if (hasExpectedExtension) name else "$name.${video.fileExtension}"
                }
                val path = DownloadStorage.pendingPath(context, fileName)
                // Keep the manifest marker so the service routes through the playlist/manifest
                // engine even when the source URL has no ".m3u8"/".mpd" extension. ProgressTab
                // derives the view mime from the completed file's extension.
                val mimeType = when {
                    video.isHls -> "application/vnd.apple.mpegurl"
                    video.isDash -> "application/dash+xml"
                    else -> video.mimeType ?: "video/${video.fileExtension}"
                }
                val id = downloadRepository.enqueue(
                    fileName = fileName,
                    url = video.url,
                    path = path,
                    mimeType = mimeType,
                    headers = video.headers.withUserAgentIfMissing(activeUserAgent),
                    audioUrl = video.audioUrl ?: "",
                    thumbnailUrl = video.thumbnailUrl.orEmpty()
                )
                DownloadForegroundService.start(context, id)
            }
            _showVideoSheet.value = false
        }
    }

    /** Show the save/copy menu for a long-pressed image and/or link (ignored if both empty). */
    fun onLongPressContent(imageUrl: String?, linkUrl: String?) {
        val image = imageUrl?.takeIf { it.isNotBlank() }
        val link = linkUrl?.takeIf { it.isNotBlank() }
        if (image == null && link == null) return
        _linkContextMenu.value = LinkContextInfo(image, link)
    }

    fun dismissLinkContextMenu() {
        _linkContextMenu.value = null
    }

    /** Download [url] (image, file, blob or data URI) through the app's download system. */
    fun saveUrl(url: String) {
        tabManager.downloadUrl(url, referer = activeSession.value?.url?.value)
        _linkContextMenu.value = null
    }

    fun goBack(): Boolean {
        val webView = tabManager.activeSession.value?.webView
        return if (webView != null && webView.canGoBack()) {
            webView.post { webView.goBack() }
            true
        } else {
            false
        }
    }

    fun goForward() {
        val webView = tabManager.activeSession.value?.webView
        if (webView != null && webView.canGoForward()) {
            webView.post { webView.goForward() }
        }
    }

    fun reload() {
        tabManager.activeSession.value?.webView?.apply {
            post { reload() }
        }
    }

    fun submitUrl(input: String) {
        viewModelScope.launch {
            val currentEngine = searchEngineRepository.observeCurrent().first()
            val targetUrl = UrlBuilder.buildUrl(input, currentEngine)
            tabManager.activeSession.value?.webView?.apply {
                post { loadUrl(targetUrl) }
            }
        }
    }

    fun toggleIncognito() {
        val currentSession = tabManager.activeSession.value ?: return
        val targetIncognito = !currentSession.isIncognito
        val currentUrl = currentSession.url.value
        viewModelScope.launch {
            val url = currentUrl.ifBlank {
                searchEngineRepository.observeCurrent().first().homeUrl
            }
            val newTabId = tabManager.addTab(url, targetIncognito)
            if (newTabId != null) {
                tabManager.closeTab(currentSession.id)
                dismissMoreMenu()
            } else {
                eventChannel.send(
                    BrowserUiEvent.TabLimitReached(
                        maxTabs = if (targetIncognito) {
                            TabManager.MAX_INCOGNITO_TABS
                        } else {
                            TabManager.MAX_NORMAL_TABS
                        },
                        isIncognito = targetIncognito
                    )
                )
            }
        }
    }

    fun newTab(url: String? = null, incognito: Boolean = false) {
        captureThumbnailBeforeLeaving()
        viewModelScope.launch {
            val targetUrl = url?.takeIf { it.isNotBlank() }
                ?: searchEngineRepository.observeCurrent().first().homeUrl
            val newTabId = tabManager.addTab(targetUrl, incognito)
            if (newTabId == null) {
                eventChannel.send(
                    BrowserUiEvent.TabLimitReached(
                        maxTabs = if (incognito) {
                            TabManager.MAX_INCOGNITO_TABS
                        } else {
                            TabManager.MAX_NORMAL_TABS
                        },
                        isIncognito = incognito
                    )
                )
            }
        }
    }

    fun toggleBookmark() {
        val session = tabManager.activeSession.value ?: return
        val url = session.url.value
        val title = session.title.value
        if (!isBookmarkableUrl(url)) {
            viewModelScope.launch { eventChannel.send(BrowserUiEvent.BookmarkUnavailable) }
            return
        }

        viewModelScope.launch {
            bookmarkMutex.withLock {
                try {
                    val existing = bookmarkRepository.findByUrl(url)
                    if (existing != null) {
                        bookmarkRepository.deleteById(existing.id)
                        eventChannel.send(BrowserUiEvent.BookmarkRemoved(existing))
                    } else {
                        val bookmark = BookmarkEntity(
                            title = title.ifBlank { url },
                            url = url,
                            faviconUrl = faviconUrlFor(url),
                            createdAt = System.currentTimeMillis()
                        )
                        val insertedId = bookmarkRepository.insert(bookmark)
                        if (insertedId >= 0) {
                            eventChannel.send(BrowserUiEvent.BookmarkAdded)
                        } else {
                            eventChannel.send(BrowserUiEvent.BookmarkOperationFailed)
                        }
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    eventChannel.send(BrowserUiEvent.BookmarkOperationFailed)
                }
            }
        }
    }

    fun restoreBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch {
            bookmarkMutex.withLock {
                try {
                    if (bookmarkRepository.findByUrl(bookmark.url) == null) {
                        bookmarkRepository.insert(bookmark.copy(id = 0L))
                    }
                    eventChannel.send(BrowserUiEvent.BookmarkRestored)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    eventChannel.send(BrowserUiEvent.BookmarkOperationFailed)
                }
            }
        }
    }

    // Desktop Mode
    fun toggleDesktopMode() {
        val session = tabManager.activeSession.value ?: return
        tabManager.setDesktopMode(session.id, !session.isDesktopMode.value)
    }

    // Find in Page
    fun showFindInPage() {
        _showFindInPage.value = true
        dismissMoreMenu()
    }

    fun hideFindInPage() {
        _showFindInPage.value = false
        _findInPageQuery.value = ""
        _findInPageCurrentMatch.value = 0
        _findInPageTotalMatches.value = 0
        tabManager.activeSession.value?.webView?.post {
            tabManager.activeSession.value?.webView?.clearMatches()
        }
    }

    fun updateFindInPageQuery(query: String) {
        _findInPageQuery.value = query
        if (query.isBlank()) {
            _findInPageCurrentMatch.value = 0
            _findInPageTotalMatches.value = 0
            tabManager.activeSession.value?.webView?.post {
                tabManager.activeSession.value?.webView?.clearMatches()
            }
            return
        }
        val webView = tabManager.activeSession.value?.webView ?: return
        webView.post {
            @Suppress("DEPRECATION")
            webView.findAllAsync(query)
        }
    }

    fun onFindResultReceived(activeMatchOrdinal: Int, numberOfMatches: Int) {
        _findInPageCurrentMatch.value = if (numberOfMatches > 0) activeMatchOrdinal + 1 else 0
        _findInPageTotalMatches.value = numberOfMatches
    }

    fun findNext() {
        tabManager.activeSession.value?.webView?.post {
            tabManager.activeSession.value?.webView?.findNext(true)
        }
    }

    fun findPrevious() {
        tabManager.activeSession.value?.webView?.post {
            tabManager.activeSession.value?.webView?.findNext(false)
        }
    }

    /**
     * Capture a fresh thumbnail of the active tab before leaving the browser screen.
     * This ensures the Tabs screen displays the latest page content, even if the user
     * navigated within the page without triggering onPageFinished (e.g., SPA, scroll, AJAX).
     */
    fun captureThumbnailBeforeLeaving() {
        val activeId = tabManager.activeTabId.value ?: return
        tabManager.requestThumbnailCapture(activeId)
    }

    override fun onCleared() {
        super.onCleared()
        // Re-enable App Open Ad
        InterstitialUtil.getInstance().openAd?.needShowOpenAds = true
    }
}

internal fun Map<String, String>.withUserAgentIfMissing(
    userAgent: String?
): Map<String, String> {
    if (userAgent.isNullOrBlank()) return this
    if (keys.any { it.equals("User-Agent", ignoreCase = true) }) return this
    return this + ("User-Agent" to userAgent)
}
