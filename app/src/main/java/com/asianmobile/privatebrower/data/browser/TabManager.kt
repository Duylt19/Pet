package com.asianmobile.privatebrower.data.browser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Message
import android.system.Os
import android.util.Log
import android.webkit.WebView
import com.asianmobile.privatebrower.data.download.DownloadStorage
import com.asianmobile.privatebrower.data.model.Tab
import com.asianmobile.privatebrower.data.repository.DownloadRepository
import com.asianmobile.privatebrower.data.repository.HistoryRepository
import com.asianmobile.privatebrower.data.repository.TabRepository
import com.asianmobile.privatebrower.service.DownloadForegroundService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class TabSession(
    val id: Long,
    val webView: WebView,
    val chromeClient: BrowserWebChromeClient,
    val isIncognito: Boolean,
    val url: StateFlow<String>,
    val title: StateFlow<String>,
    val progress: StateFlow<Int>,
    val isLoading: StateFlow<Boolean>,
    val canGoBack: StateFlow<Boolean>,
    val canGoForward: StateFlow<Boolean>,
    val favicon: StateFlow<Bitmap?>,
    val isDesktopMode: StateFlow<Boolean>,
    val openerTabId: Long? = null
)

data class TabPreview(
    val bitmap: Bitmap,
    val revision: Long
)

@Singleton
class TabManager @Inject constructor(
    private val tabRepository: TabRepository,
    private val historyRepository: HistoryRepository,
    private val browserEngine: BrowserEngine,
    private val videoSniffer: VideoSniffer,
    private val downloadRepository: DownloadRepository,
    private val mediaCaptureServer: MediaCaptureServer,
    @param:ApplicationContext private val context: Context
) {
    companion object {
        const val MAX_NORMAL_TABS = 10
        const val MAX_INCOGNITO_TABS = 5
        private const val MAX_THUMBNAIL_WIDTH_PX = 480
        private const val THUMBNAIL_JPEG_QUALITY = 82
        private const val THUMBNAIL_MIN_CHANNEL_RANGE = 12
        private const val THUMBNAIL_SAMPLE_COLUMNS = 16
        private const val THUMBNAIL_SAMPLE_ROWS = 24
        private const val THUMBNAIL_VISUAL_STATE_TIMEOUT_MS = 750L
        private const val SPA_HISTORY_SETTLE_DELAY_MS = 500L
        private const val PRIVATE_SESSION_PREFS = "private_browsing_session"
        private const val KEY_PRIVATE_SESSION_ACTIVE = "private_session_active"
        private const val TAG = "TabManager"
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val tabs = mutableMapOf<Long, TabSession>()
    private val desktopModeStates = mutableMapOf<Long, MutableStateFlow<Boolean>>()
    private val loadedTabIds = mutableSetOf<Long>()
    private val pendingReloadTabIds = mutableSetOf<Long>()
    private val tabCreationMutex = Mutex()
    private val thumbnailCaptureMutex = Mutex()
    private val tabUpdateMutex = Mutex()
    private val thumbnailCaptureJobs = mutableMapOf<Long, Job>()
    private val thumbnailCaptureRevisions = ConcurrentHashMap<Long, AtomicLong>()
    private val incognitoIdGenerator = AtomicLong(-System.currentTimeMillis())
    private val thumbnailVisualStateId = AtomicLong(0L)
    private val canGoBackStates = mutableMapOf<Long, MutableStateFlow<Boolean>>()
    private val canGoForwardStates = mutableMapOf<Long, MutableStateFlow<Boolean>>()
    private val privateSessionPreferences = context.getSharedPreferences(
        PRIVATE_SESSION_PREFS,
        Context.MODE_PRIVATE
    )

    private val _activeTabId = MutableStateFlow<Long?>(null)
    val activeTabId: StateFlow<Long?> = _activeTabId.asStateFlow()

    private val _allSessions = MutableStateFlow<List<TabSession>>(emptyList())
    val allSessions: StateFlow<List<TabSession>> = _allSessions.asStateFlow()

    private val _tabPreviews = MutableStateFlow<Map<Long, TabPreview>>(emptyMap())
    val tabPreviews: StateFlow<Map<Long, TabPreview>> = _tabPreviews.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)

    val activeSession: StateFlow<TabSession?> = _activeTabId.map { id ->
        id?.let { tabs[it] }
    }.stateIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private fun emitSessions() {
        _allSessions.value = tabs.values.toList()
    }

    init {
        coroutineScope.launch {
            try {
                // Private tabs are not restored; clear data left by an interrupted session.
                clearStaleThumbnailFiles()
                clearStalePrivateBrowsingSession()
                val savedTabs = tabRepository.observeNormalTabs().first()
                val restoredTabs = savedTabs.take(MAX_NORMAL_TABS)
                restoredTabs.forEachIndexed { index, tab ->
                    createTabSession(
                        tabId = tab.id,
                        url = tab.url,
                        isIncognito = false,
                        loadInitialUrl = index == restoredTabs.lastIndex
                    )
                }
                emitSessions()
                if (restoredTabs.isNotEmpty()) {
                    _activeTabId.value = restoredTabs.last().id
                }
            } finally {
                _isInitialized.value = true
            }
        }
    }

    suspend fun awaitInitialized() {
        _isInitialized.filter { it }.first()
    }

    private suspend fun clearStalePrivateBrowsingSession() {
        val hadPrivateSession = privateSessionPreferences.getBoolean(
            KEY_PRIVATE_SESSION_ACTIVE,
            false
        )
        if (!hadPrivateSession) return

        clearPrivateSessionData()
    }

    private suspend fun markPrivateSessionActive() {
        val saved = withContext(Dispatchers.IO) {
            privateSessionPreferences.edit()
                .putBoolean(KEY_PRIVATE_SESSION_ACTIVE, true)
                .commit()
        }
        if (!saved) {
            Log.w(TAG, "Could not persist private session marker")
        }
    }

    private suspend fun clearPrivateSessionData() {
        val profileIsolationSupported = browserEngine.supportsProfileIsolation()
        val cleared = browserEngine.clearIncognitoSessionData()
        if (!cleared) {
            Log.w(TAG, "Private browsing data cleanup will be retried next launch")
            return
        }

        withContext(Dispatchers.IO) {
            privateSessionPreferences.edit()
                .putBoolean(KEY_PRIVATE_SESSION_ACTIVE, false)
                .commit()
        }
        if (!profileIsolationSupported) {
            // The fallback store is shared, so normal pages must not continue with stale state.
            invalidateOpenTabsForReload(isIncognito = false)
        }
    }

    fun activeWebView(): WebView? = _activeTabId.value?.let { tabs[it]?.webView }

    suspend fun addTab(url: String, isIncognito: Boolean): Long? =
        tabCreationMutex.withLock {
            addTabInternal(
                url = url,
                isIncognito = isIncognito,
                loadInitialUrl = true,
                openerTabId = null
            )
        }

    /**
     * Returns the active tab, or creates exactly one when no live active tab exists.
     * The check and creation share the same mutex as [addTab] to prevent two callers from
     * observing an empty browser and creating duplicate starter tabs concurrently.
     */
    suspend fun ensureActiveTab(url: String, isIncognito: Boolean): Long? =
        tabCreationMutex.withLock {
            val activeTabId = _activeTabId.value?.takeIf(tabs::containsKey)
            if (activeTabId != null) {
                tabs[activeTabId]?.let(::loadOrReloadTabIfNeeded)
                activeTabId
            } else {
                addTabInternal(
                    url = url,
                    isIncognito = isIncognito,
                    loadInitialUrl = true,
                    openerTabId = null
                )
            }
        }

    private suspend fun addTabInternal(
        url: String,
        isIncognito: Boolean,
        loadInitialUrl: Boolean,
        openerTabId: Long?,
        initialDesktopMode: Boolean = false
    ): Long? {
        val tabCount = if (isIncognito) incognitoTabCount() else normalTabCount()
        val maxTabs = if (isIncognito) MAX_INCOGNITO_TABS else MAX_NORMAL_TABS
        if (tabCount >= maxTabs) return null

        if (isIncognito && tabCount == 0) {
            markPrivateSessionActive()
        }

        val tabId = if (isIncognito) {
            incognitoIdGenerator.decrementAndGet()
        } else {
            val position = tabRepository.countNormal()
            val tab = Tab(
                id = 0,
                title = url,
                url = url,
                thumbnailPath = null,
                isIncognito = false,
                lastActiveAt = System.currentTimeMillis(),
                position = position
            )
            tabRepository.insert(tab)
        }

        createTabSession(
            tabId = tabId,
            url = url,
            isIncognito = isIncognito,
            loadInitialUrl = loadInitialUrl,
            openerTabId = openerTabId,
            initialDesktopMode = initialDesktopMode
        )
        _activeTabId.value = tabId
        emitSessions()
        return tabId
    }

    fun switchTo(tabId: Long) {
        val session = tabs[tabId]
        if (session != null) {
            _activeTabId.value = tabId
            loadOrReloadTabIfNeeded(session)
            // Surface the tab's already-detected videos instead of wiping them, so returning
            // to a playing tab keeps its download FAB.
            videoSniffer.selectPage(tabs[tabId]?.url?.value)
        }
    }

    fun refreshActiveTabIfNeeded() {
        _activeTabId.value?.let(tabs::get)?.let(::loadOrReloadTabIfNeeded)
    }

    suspend fun closeTab(tabId: Long) {
        tabCreationMutex.withLock {
            closeTabLocked(tabId, clearIncognitoProfileWhenLastClosed = true)
        }
    }

    private suspend fun closeTabLocked(
        tabId: Long,
        clearIncognitoProfileWhenLastClosed: Boolean
    ) {
        thumbnailCaptureJobs.remove(tabId)?.cancelAndJoin()
        thumbnailCaptureRevisions.remove(tabId)
        val session = tabs.remove(tabId)
        desktopModeStates.remove(tabId)
        canGoBackStates.remove(tabId)
        canGoForwardStates.remove(tabId)
        loadedTabIds.remove(tabId)
        pendingReloadTabIds.remove(tabId)
        val shouldClearIncognitoProfile =
            clearIncognitoProfileWhenLastClosed &&
                session?.isIncognito == true &&
                tabs.values.none { it.isIncognito }
        session?.webView?.let { webView ->
            withContext(Dispatchers.Main.immediate) {
                webView.stopLoading()
                (webView.parent as? android.view.ViewGroup)?.removeView(webView)
                webView.destroy()
            }
        }
        if (session != null) {
            if (!session.isIncognito) {
                tabRepository.deleteById(tabId)
            }
            File(context.cacheDir, "tabs/$tabId.jpg").delete()
            File(context.cacheDir, "tabs/$tabId.png").delete()
        }
        if (shouldClearIncognitoProfile) {
            clearPrivateSessionData()
        }
        _tabPreviews.update { it - tabId }
        if (_activeTabId.value == tabId) {
            _activeTabId.value = session?.openerTabId?.takeIf(tabs::containsKey)
                ?: tabs.keys.lastOrNull()
        }
        emitSessions()
    }

    suspend fun closeAllInMode(isIncognito: Boolean) {
        tabCreationMutex.withLock {
            val toClose = tabs.filter { it.value.isIncognito == isIncognito }.keys.toList()
            toClose.forEach { tabId ->
                closeTabLocked(tabId, clearIncognitoProfileWhenLastClosed = true)
            }
        }
    }

    suspend fun endPrivateBrowsingSession() {
        awaitInitialized()
        tabCreationMutex.withLock {
            val privateTabIds = tabs
                .filterValues { it.isIncognito }
                .keys
                .toList()
            val shouldClearSession = privateTabIds.isNotEmpty() ||
                privateSessionPreferences.getBoolean(KEY_PRIVATE_SESSION_ACTIVE, false)
            privateTabIds.forEach { tabId ->
                closeTabLocked(
                    tabId = tabId,
                    clearIncognitoProfileWhenLastClosed = false
                )
            }
            if (shouldClearSession) {
                clearPrivateSessionData()
            }
        }
    }

    fun schedulePrivateBrowsingSessionEnd() {
        coroutineScope.launch {
            endPrivateBrowsingSession()
        }
    }

    fun clearNavigationHistory(isIncognito: Boolean?) {
        tabs.values
            .filter { isIncognito == null || it.isIncognito == isIncognito }
            .forEach { session ->
                session.webView.clearHistory()
                canGoBackStates[session.id]?.value = false
                canGoForwardStates[session.id]?.value = false
            }
    }

    /**
     * WebView's resource cache is application-wide, even when tabs use separate profiles.
     * Calling it once is sufficient and avoids pretending a scoped cache clear is possible.
     */
    fun clearWebViewCache() {
        val existingWebView = tabs.values.firstOrNull()?.webView
        if (existingWebView != null) {
            existingWebView.clearCache(true)
        } else {
            WebView(context.asBrowserThemedContext()).apply {
                clearCache(true)
                destroy()
            }
        }
    }

    /**
     * Keep each tab and its URL, but require a fresh navigation the next time it becomes visible.
     * Stopping current loads prevents background requests from immediately restoring cleared data.
     */
    fun invalidateOpenTabsForReload(isIncognito: Boolean? = null) {
        tabs.values
            .filter { isIncognito == null || it.isIncognito == isIncognito }
            .forEach { session ->
                session.webView.stopLoading()
                pendingReloadTabIds.add(session.id)
            }
    }

    private fun loadOrReloadTabIfNeeded(session: TabSession) {
        val requiresInitialLoad = loadedTabIds.add(session.id)
        val requiresFreshLoad = pendingReloadTabIds.remove(session.id)
        if (!requiresInitialLoad && !requiresFreshLoad) return

        session.webView.post {
            if (requiresInitialLoad) {
                session.webView.loadUrl(session.url.value)
            } else {
                session.webView.reload()
            }
        }
    }

    fun supportsProfileIsolation(): Boolean = browserEngine.supportsProfileIsolation()

    fun normalTabCount(): Int = tabs.values.count { !it.isIncognito }
    fun incognitoTabCount(): Int = tabs.values.count { it.isIncognito }

    fun setDesktopMode(tabId: Long, enabled: Boolean) {
        val session = tabs[tabId] ?: return
        val state = desktopModeStates[tabId] ?: return
        if (state.value == enabled) return

        state.value = enabled
        session.webView.post {
            session.webView.applyDesktopMode(enabled)
            session.webView.scrollTo(0, 0)
            session.webView.reload()
        }
    }

    private fun captureThumbnailBitmap(webView: WebView): Bitmap? {
        return try {
            val width = webView.width
            val height = webView.height
            if (width <= 0 || height <= 0) return null

            val scale = minOf(0.5f, MAX_THUMBNAIL_WIDTH_PX.toFloat() / width)
            val targetWidth = (width * scale).toInt().coerceAtLeast(1)
            val targetHeight = (height * scale).toInt().coerceAtLeast(1)
            Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.RGB_565).also { bitmap ->
                Canvas(bitmap).apply {
                    drawColor(Color.WHITE)
                    scale(scale, scale)
                    webView.draw(this)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun Bitmap.hasVisibleContent(): Boolean {
        if (width <= 1 || height <= 1) return false

        var minRed = 255
        var maxRed = 0
        var minGreen = 255
        var maxGreen = 0
        var minBlue = 255
        var maxBlue = 0

        repeat(THUMBNAIL_SAMPLE_ROWS) { row ->
            val y = row * (height - 1) / (THUMBNAIL_SAMPLE_ROWS - 1)
            repeat(THUMBNAIL_SAMPLE_COLUMNS) { column ->
                val x = column * (width - 1) / (THUMBNAIL_SAMPLE_COLUMNS - 1)
                val color = getPixel(x, y)
                val red = Color.red(color)
                val green = Color.green(color)
                val blue = Color.blue(color)
                minRed = minOf(minRed, red)
                maxRed = maxOf(maxRed, red)
                minGreen = minOf(minGreen, green)
                maxGreen = maxOf(maxGreen, green)
                minBlue = minOf(minBlue, blue)
                maxBlue = maxOf(maxBlue, blue)
            }
        }

        return maxRed - minRed >= THUMBNAIL_MIN_CHANNEL_RANGE ||
            maxGreen - minGreen >= THUMBNAIL_MIN_CHANNEL_RANGE ||
            maxBlue - minBlue >= THUMBNAIL_MIN_CHANNEL_RANGE
    }

    private suspend fun awaitThumbnailVisualState(webView: WebView) {
        withContext(Dispatchers.Main.immediate) {
            withTimeoutOrNull(THUMBNAIL_VISUAL_STATE_TIMEOUT_MS) {
                suspendCancellableCoroutine { continuation ->
                    runCatching {
                        webView.postVisualStateCallback(
                            thumbnailVisualStateId.incrementAndGet(),
                            object : WebView.VisualStateCallback() {
                                override fun onComplete(requestId: Long) {
                                    if (continuation.isActive) continuation.resume(Unit)
                                }
                            }
                        )
                    }.onFailure {
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                }
            }
        }
    }

    private fun nextThumbnailCaptureRevision(tabId: Long): Long =
        thumbnailCaptureRevisions
            .computeIfAbsent(tabId) { AtomicLong(0L) }
            .incrementAndGet()

    private fun isThumbnailCaptureCurrent(tabId: Long, revision: Long): Boolean =
        thumbnailCaptureRevisions[tabId]?.get() == revision

    private suspend fun persistThumbnail(
        tabId: Long,
        revision: Long,
        bitmap: Bitmap
    ): String? {
        return try {
            withContext(Dispatchers.IO) {
                val tabsDir = File(context.cacheDir, "tabs").apply { mkdirs() }
                val file = File(tabsDir, "$tabId.jpg")
                val tempFile = File.createTempFile("tab-$tabId-", ".jpg.tmp", tabsDir)
                try {
                    val saved = FileOutputStream(tempFile).use { out ->
                        val compressed = bitmap.compress(
                            Bitmap.CompressFormat.JPEG,
                            THUMBNAIL_JPEG_QUALITY,
                            out
                        )
                        if (compressed) {
                            out.flush()
                            out.fd.sync()
                        }
                        compressed
                    }
                    if (!saved) return@withContext null
                    if (!isThumbnailCaptureCurrent(tabId, revision)) return@withContext null

                    // The final path is replaced only after the JPEG is complete, so Coil never
                    // observes a partially-written thumbnail.
                    Os.rename(tempFile.absolutePath, file.absolutePath)
                    File(tabsDir, "$tabId.png").delete()
                    file.absolutePath
                } finally {
                    tempFile.delete()
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun publishTabPreview(tabId: Long, revision: Long, bitmap: Bitmap): Boolean {
        if (tabs[tabId] == null || !isThumbnailCaptureCurrent(tabId, revision)) return false
        _tabPreviews.update { previews ->
            previews + (tabId to TabPreview(bitmap = bitmap, revision = revision))
        }
        return true
    }

    private suspend fun captureThumbnailFile(
        tabId: Long,
        revision: Long,
        immediateBitmap: Bitmap? = null,
        previewAlreadyPublished: Boolean = false
    ): String? {
        val webView = withContext(Dispatchers.Main.immediate) {
            tabs[tabId]?.webView
        } ?: return null

        var capturedBitmap = immediateBitmap
        var previewOwnsBitmap = previewAlreadyPublished
        try {
            if (capturedBitmap == null) {
                awaitThumbnailVisualState(webView)
                if (!isThumbnailCaptureCurrent(tabId, revision)) return null
                capturedBitmap = withContext(Dispatchers.Main.immediate) {
                    if (tabs[tabId]?.webView === webView) {
                        captureThumbnailBitmap(webView)
                    } else {
                        null
                    }
                }
            }

            val bitmap = capturedBitmap ?: return null
            if (!previewOwnsBitmap) {
                if (!bitmap.hasVisibleContent() || !isThumbnailCaptureCurrent(tabId, revision)) {
                    return null
                }
                val published = withContext(Dispatchers.Main.immediate) {
                    if (tabs[tabId]?.webView === webView) {
                        publishTabPreview(tabId, revision, bitmap)
                    } else {
                        false
                    }
                }
                if (!published) return null
                previewOwnsBitmap = true
            }

            val thumbnail = thumbnailCaptureMutex.withLock {
                if (!isThumbnailCaptureCurrent(tabId, revision)) return@withLock null
                persistThumbnail(tabId, revision, bitmap)
            } ?: return null

            val tabIsStillCurrent = withContext(Dispatchers.Main.immediate) {
                tabs[tabId]?.webView === webView &&
                    isThumbnailCaptureCurrent(tabId, revision)
            }
            return thumbnail.takeIf { tabIsStillCurrent }
        } finally {
            if (!previewOwnsBitmap) {
                capturedBitmap?.takeUnless(Bitmap::isRecycled)?.recycle()
            }
        }
    }

    private fun scheduleThumbnailCapture(tabId: Long, captureCurrentFrame: Boolean) {
        coroutineScope.launch(Dispatchers.Main.immediate) {
            val session = tabs[tabId] ?: return@launch
            if (!captureCurrentFrame && thumbnailCaptureJobs[tabId]?.isActive == true) {
                return@launch
            }
            var immediateBitmap = if (captureCurrentFrame) {
                captureThumbnailBitmap(session.webView)
            } else {
                null
            }
            val revision = nextThumbnailCaptureRevision(tabId)

            thumbnailCaptureJobs.remove(tabId)?.cancel()
            val previewAlreadyPublished = immediateBitmap?.let { bitmap ->
                if (bitmap.hasVisibleContent() && publishTabPreview(tabId, revision, bitmap)) {
                    true
                } else {
                    bitmap.recycle()
                    immediateBitmap = null
                    false
                }
            } ?: false

            val captureJob = coroutineScope.launch(
                context = Dispatchers.Main.immediate,
                start = CoroutineStart.LAZY
            ) {
                val thumbnail = captureThumbnailFile(
                    tabId = tabId,
                    revision = revision,
                    immediateBitmap = immediateBitmap,
                    previewAlreadyPublished = previewAlreadyPublished
                )
                if (
                    thumbnail != null &&
                    isThumbnailCaptureCurrent(tabId, revision) &&
                    !session.isIncognito
                ) {
                    updateTabInDb(tabId, thumbnail = thumbnail)
                }
            }
            thumbnailCaptureJobs[tabId] = captureJob
            captureJob.invokeOnCompletion {
                coroutineScope.launch(Dispatchers.Main.immediate) {
                    if (thumbnailCaptureJobs[tabId] === captureJob) {
                        thumbnailCaptureJobs.remove(tabId)
                    }
                }
            }
            captureJob.start()
        }
    }

    /**
     * Capture the currently visible WebView frame before navigation. The bitmap is published to
     * memory before JPEG persistence suspends, so the Tabs screen can render it immediately.
     */
    fun requestThumbnailCapture(tabId: Long) {
        scheduleThumbnailCapture(tabId, captureCurrentFrame = true)
    }

    private fun requestThumbnailAfterVisualState(tabId: Long) {
        scheduleThumbnailCapture(tabId, captureCurrentFrame = false)
    }

    private fun clearStaleThumbnailFiles() {
        val tabsDir = File(context.cacheDir, "tabs")
        tabsDir.listFiles()?.forEach { file ->
            val tabId = file.name.substringBefore('.').toLongOrNull()
            if (file.name.endsWith(".tmp") || (tabId != null && tabId < 0L)) {
                file.delete()
            }
        }
    }

    private fun createTabSession(
        tabId: Long,
        url: String,
        isIncognito: Boolean,
        loadInitialUrl: Boolean = true,
        openerTabId: Long? = null,
        initialDesktopMode: Boolean = false
    ): TabSession {
        val progressFlow = MutableStateFlow(0)
        val titleFlow = MutableStateFlow("")
        val faviconFlow = MutableStateFlow<Bitmap?>(null)
        val urlFlow = MutableStateFlow(url)
        val isLoadingFlow = MutableStateFlow(false)
        val canGoBackFlow = MutableStateFlow(false)
        val canGoForwardFlow = MutableStateFlow(false)
        val desktopModeFlow = MutableStateFlow(initialDesktopMode)
        var spaNavigationVersion = 0L
        var navigationHistoryGeneration = historyRepository.captureVisitGeneration()
        desktopModeStates[tabId] = desktopModeFlow
        canGoBackStates[tabId] = canGoBackFlow
        canGoForwardStates[tabId] = canGoForwardFlow

        val webViewHandle = browserEngine.createWebView(
            isIncognito = isIncognito,
            onProgress = { progress -> progressFlow.value = progress },
            onTitleChange = { title ->
                titleFlow.value = title
                if (!isIncognito) {
                    coroutineScope.launch {
                        updateTabInDb(tabId, title = title)
                    }
                }
            },
            onIconReceived = { favicon -> faviconFlow.value = favicon },
            onPageStarted = { pageUrl ->
                pendingReloadTabIds.remove(tabId)
                spaNavigationVersion++
                navigationHistoryGeneration = historyRepository.captureVisitGeneration()
                isLoadingFlow.value = true
                urlFlow.value = pageUrl
                progressFlow.value = 0
                if (_activeTabId.value == tabId) {
                    videoSniffer.clearForPage(pageUrl)
                }
            },
            onPageFinished = { pageUrl, title ->
                spaNavigationVersion++
                isLoadingFlow.value = false
                urlFlow.value = pageUrl
                titleFlow.value = title
                val visitGeneration = navigationHistoryGeneration
                requestThumbnailAfterVisualState(tabId)
                coroutineScope.launch {
                    if (!isIncognito) {
                        historyRepository.recordVisit(
                            url = pageUrl,
                            title = title,
                            visitGeneration = visitGeneration
                        )
                        updateTabInDb(tabId, url = pageUrl, title = title)
                    }
                }
            },
            onVisitedHistoryUpdated = { pageUrl, pageTitle, _ ->
                val urlChanged = pageUrl.isNotBlank() && pageUrl != urlFlow.value
                if (urlChanged) {
                    val wasLoading = isLoadingFlow.value
                    urlFlow.value = pageUrl
                    if (pageTitle.isNotBlank()) {
                        titleFlow.value = pageTitle
                    }
                    if (_activeTabId.value == tabId) {
                        videoSniffer.selectPage(pageUrl)
                    }

                    if (!isIncognito) {
                        coroutineScope.launch {
                            updateTabInDb(
                                tabId = tabId,
                                url = pageUrl,
                                title = pageTitle.takeIf { it.isNotBlank() }
                            )
                        }
                    }

                    if (!wasLoading) {
                        val navigationVersion = ++spaNavigationVersion
                        val visitGeneration = historyRepository.captureVisitGeneration()
                        coroutineScope.launch {
                            delay(SPA_HISTORY_SETTLE_DELAY_MS)
                            if (
                                spaNavigationVersion == navigationVersion &&
                                urlFlow.value == pageUrl
                            ) {
                                val settledTitle = titleFlow.value.ifBlank {
                                    pageTitle.ifBlank { pageUrl }
                                }
                                requestThumbnailAfterVisualState(tabId)
                                if (!isIncognito) {
                                    historyRepository.recordVisit(
                                        url = pageUrl,
                                        title = settledTitle,
                                        visitGeneration = visitGeneration
                                    )
                                    updateTabInDb(
                                        tabId = tabId,
                                        url = pageUrl,
                                        title = settledTitle
                                    )
                                }
                            }
                        }
                    }
                }
            },
            onCanGoBackForward = { back, forward ->
                canGoBackFlow.value = back
                canGoForwardFlow.value = forward
            },
            onVideoMetadata = {
                    sourceUrl, posterUrl, pageTitle, pageUrl, isAdvertisement, isPageContent,
                    isPrimaryPageMedia ->
                videoSniffer.onVideoMetadata(
                    sourceUrl = sourceUrl,
                    posterUrl = posterUrl,
                    pageTitle = pageTitle,
                    pageUrl = pageUrl,
                    isAdvertisement = isAdvertisement,
                    isPageContent = isPageContent,
                    isPrimaryPageMedia = isPrimaryPageMedia
                )
            },
            onMseVideo = { captureId, mime, _ ->
                videoSniffer.onMseVideoDetected(captureId, mime, urlFlow.value)
            },
            onCaptureEnd = { /* completion is tracked by the capture server */ },
            onResourceIntercepted = { request ->
                // Sniff every tab, not just the active one: sites that open a pop-under ad on
                // play steal focus before the real media playlist is fetched. Videos are
                // bucketed per page in VideoSniffer, so a background tab can't pollute what the
                // foreground tab shows.
                videoSniffer.onResourceIntercepted(request, urlFlow.value)
                null
            },
            onDownloadStart = { url, userAgent, contentDisposition, mimeType, _ ->
                downloadUrl(
                    url = url,
                    referer = urlFlow.value,
                    suggestedName = guessFileName(url, contentDisposition, mimeType),
                    mimeType = mimeType,
                    userAgent = userAgent
                )
            },
            onCreateWindow = { isUserGesture, resultMsg ->
                createPopupTab(
                    sourceTabId = tabId,
                    isIncognito = isIncognito,
                    isUserGesture = isUserGesture,
                    resultMsg = resultMsg
                )
            },
            onCloseWindow = { window ->
                closeWindow(window)
            },
            onRenderProcessGone = { crashedWebView, _ ->
                recoverFromRendererCrash(tabId, crashedWebView)
            }
        )
        val webView = webViewHandle.webView
        webView.applyDesktopMode(initialDesktopMode)

        if (loadInitialUrl) {
            loadedTabIds.add(tabId)
            webView.post {
                webView.loadUrl(url)
            }
        }

        val session = TabSession(
            id = tabId,
            webView = webView,
            chromeClient = webViewHandle.chromeClient,
            isIncognito = isIncognito,
            url = urlFlow,
            title = titleFlow,
            progress = progressFlow,
            isLoading = isLoadingFlow,
            canGoBack = canGoBackFlow,
            canGoForward = canGoForwardFlow,
            favicon = faviconFlow,
            isDesktopMode = desktopModeFlow.asStateFlow(),
            openerTabId = openerTabId
        )
        tabs[tabId] = session
        return session
    }

    private fun createPopupTab(
        sourceTabId: Long,
        isIncognito: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message
    ): Boolean {
        if (!isUserGesture) return false
        val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
        val tabCount = if (isIncognito) incognitoTabCount() else normalTabCount()
        val maxTabs = if (isIncognito) MAX_INCOGNITO_TABS else MAX_NORMAL_TABS
        if (tabCount >= maxTabs) return false

        coroutineScope.launch {
            val popupTabId = tabCreationMutex.withLock {
                if (tabs[sourceTabId] == null) return@withLock null
                addTabInternal(
                    url = "about:blank",
                    isIncognito = isIncognito,
                    loadInitialUrl = false,
                    openerTabId = sourceTabId,
                    initialDesktopMode = tabs[sourceTabId]?.isDesktopMode?.value ?: false
                )
            } ?: return@launch
            val popupWebView = tabs[popupTabId]?.webView ?: return@launch
            transport.webView = popupWebView
            resultMsg.sendToTarget()
        }
        return true
    }

    private fun closeWindow(window: WebView) {
        val tabId = tabs.entries.firstOrNull { it.value.webView === window }?.key ?: return
        coroutineScope.launch {
            closeTab(tabId)
        }
    }

    private fun recoverFromRendererCrash(tabId: Long, crashedWebView: WebView): Boolean {
        val crashedSession = tabs[tabId] ?: return true
        if (crashedSession.webView !== crashedWebView) return true

        val wasActive = _activeTabId.value == tabId
        val url = crashedSession.url.value.ifBlank { "https://www.google.com" }
        val wasDesktopMode = crashedSession.isDesktopMode.value
        tabs.remove(tabId)
        desktopModeStates.remove(tabId)
        canGoBackStates.remove(tabId)
        canGoForwardStates.remove(tabId)
        loadedTabIds.remove(tabId)
        pendingReloadTabIds.remove(tabId)
        runCatching {
            (crashedWebView.parent as? android.view.ViewGroup)?.removeView(crashedWebView)
            crashedWebView.destroy()
        }
        createTabSession(
            tabId = tabId,
            url = url,
            isIncognito = crashedSession.isIncognito,
            openerTabId = crashedSession.openerTabId,
            initialDesktopMode = wasDesktopMode
        )
        emitSessions()
        if (wasActive) {
            _activeTabId.value = null
            _activeTabId.value = tabId
        }
        return true
    }

    private suspend fun updateTabInDb(
        tabId: Long,
        url: String? = null,
        title: String? = null,
        thumbnail: String? = null
    ) {
        tabUpdateMutex.withLock {
            val list = tabRepository.observeNormalTabs().first()
            val currentTab = list.find { it.id == tabId }
            if (currentTab != null) {
                tabRepository.update(
                    currentTab.copy(
                        url = url ?: currentTab.url,
                        title = title ?: currentTab.title,
                        thumbnailPath = thumbnail ?: currentTab.thumbnailPath,
                        lastActiveAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    /**
     * Download any URL through the app's download system (Downloads tab + notification).
     * Handles the three schemes a page can hand us: http(s) (streamed by OkHttp), data: (decoded
     * by the service) and blob: (read in-page via JS and streamed to the capture server, since
     * blob URLs aren't fetchable natively). Used by both the WebView download listener and the
     * long-press "save image/link" menu.
     */
    fun downloadUrl(
        url: String,
        referer: String?,
        suggestedName: String? = null,
        mimeType: String = "",
        userAgent: String = ""
    ) {
        coroutineScope.launch {
            if (url.startsWith("blob:", ignoreCase = true)) {
                startBlobCapture(url, suggestedName, referer)
            } else {
                enqueueDirect(url, referer, suggestedName, mimeType, userAgent)
            }
        }
    }

    private suspend fun enqueueDirect(
        url: String,
        referer: String?,
        suggestedName: String?,
        mimeType: String,
        userAgent: String
    ) {
        val fileName = suggestedName?.takeIf { it.isNotBlank() } ?: guessFileName(url, "", mimeType)
        val path = DownloadStorage.pendingPath(context, fileName)
        val headers = mutableMapOf<String, String>()
        // data: URLs carry their own bytes; only network downloads need session headers.
        if (!url.startsWith("data:", ignoreCase = true)) {
            android.webkit.CookieManager.getInstance().getCookie(url)?.takeIf { it.isNotBlank() }
                ?.let { headers["Cookie"] = it }
            if (userAgent.isNotBlank()) headers["User-Agent"] = userAgent
            referer?.takeIf { it.startsWith("http") }?.let { headers["Referer"] = it }
        }
        val id = downloadRepository.enqueue(
            fileName = fileName,
            url = url,
            path = path,
            mimeType = mimeType.ifBlank { "application/octet-stream" },
            headers = headers,
            thumbnailUrl = videoSniffer.thumbnailForPage(referer)
        )
        DownloadForegroundService.start(context, id)
    }

    /**
     * blob: URLs are only readable inside the page that created them, so inject JS to fetch the
     * blob and stream its bytes to the loopback capture server; the service then moves the file
     * into Downloads once complete.
     */
    private suspend fun startBlobCapture(blobUrl: String, suggestedName: String?, referer: String?) {
        val captureId = "blob${System.currentTimeMillis()}"
        val port = mediaCaptureServer.ensureStarted()
        val fileName = suggestedName?.takeIf { it.isNotBlank() }
            ?: "download_${System.currentTimeMillis()}"
        val path = DownloadStorage.pendingPath(context, fileName)
        val js = blobReaderJs(port, mediaCaptureServer.token, captureId, blobUrl)
        activeWebView()?.evaluateJavascript(js, null)
        val id = downloadRepository.enqueue(
            fileName = fileName,
            url = "blob-capture://$captureId",
            path = path,
            mimeType = "application/octet-stream",
            headers = emptyMap(),
            thumbnailUrl = videoSniffer.thumbnailForPage(referer)
        )
        DownloadForegroundService.start(context, id)
    }

    private fun blobReaderJs(port: Int, token: String, captureId: String, blobUrl: String): String {
        val base = "http://127.0.0.1:$port/$token/$captureId"
        val safeBlob = blobUrl.replace("\\", "\\\\").replace("'", "\\'")
        return """
            (function() {
              try {
                var base = '$base';
                fetch('$safeBlob').then(function(r){ return r.arrayBuffer(); })
                  .then(function(buf){ return fetch(base + '/f', { method:'POST', body: buf }); })
                  .then(function(){ fetch(base + '/end', { method:'POST' }); })
                  .catch(function(){ fetch(base + '/end', { method:'POST' }); });
              } catch (e) {}
            })();
        """.trimIndent()
    }

    /**
     * Resolve a download file name the way a real browser does: prefer the Content-Disposition
     * name (incl. RFC 5987 `filename*=UTF-8''…`), then fall back to AOSP's [URLUtil.guessFileName]
     * (which derives from the URL path and appends an extension from the mime type). The result
     * is sanitised of path separators / illegal characters.
     */
    private fun guessFileName(url: String, contentDisposition: String, mimeType: String): String {
        parseContentDispositionFilename(contentDisposition)?.let { return sanitizeFileName(it) }
        val guessed = try {
            android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType.ifBlank { null })
        } catch (_: Exception) {
            "download_${System.currentTimeMillis()}"
        }
        return sanitizeFileName(guessed)
    }

    private fun parseContentDispositionFilename(cd: String): String? {
        if (cd.isBlank()) return null
        // RFC 5987: filename*=UTF-8''<percent-encoded> (preferred, carries non-ASCII names).
        Regex("filename\\*\\s*=\\s*[^']*''([^;]+)", RegexOption.IGNORE_CASE)
            .find(cd)?.groupValues?.get(1)?.trim()?.let { encoded ->
                runCatching { java.net.URLDecoder.decode(encoded, "UTF-8") }
                    .getOrNull()?.takeIf { it.isNotBlank() }?.let { return it }
            }
        // Plain: filename="name.ext" or filename=name.ext
        Regex("filename\\s*=\\s*\"?([^\";]+)\"?", RegexOption.IGNORE_CASE)
            .find(cd)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
        return null
    }

    private fun sanitizeFileName(name: String): String =
        name.substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
            .trim()
            .ifBlank { "download_${System.currentTimeMillis()}" }
}
