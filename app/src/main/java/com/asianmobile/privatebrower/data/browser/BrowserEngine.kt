package com.asianmobile.privatebrower.data.browser

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.MutableContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Message
import android.view.ContextThemeWrapper
import android.view.View
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.Profile
import androidx.webkit.ProfileStore
import androidx.webkit.UserAgentMetadata
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebStorageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.asianmobile.privatebrower.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume

data class BrowserWebViewHandle(
    val webView: WebView,
    val chromeClient: BrowserWebChromeClient
)

private class BrowserWebView(context: Context) : WebView(context) {
    val desktopModeBridge = DesktopModeBridge()
    private var desktopZoomRequestId = 0L

    fun updateDesktopMode(enabled: Boolean) {
        desktopModeBridge.update(enabled)
        desktopZoomRequestId++
    }

    fun requestMinimumDesktopZoom() {
        if (!desktopModeBridge.isDesktopMode()) return
        val requestId = ++desktopZoomRequestId

        fun applyMinimumZoom() {
            if (requestId != desktopZoomRequestId || !desktopModeBridge.isDesktopMode()) return
            runCatching { zoomBy(MINIMUM_DESKTOP_ZOOM_FACTOR) }
        }

        postVisualStateCallback(requestId, object : VisualStateCallback() {
            override fun onComplete(requestId: Long) {
                applyMinimumZoom()
            }
        })
        postDelayed(::applyMinimumZoom, DESKTOP_ZOOM_FALLBACK_DELAY_MS)
    }
}

class BrowserEngine @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val mediaCaptureServer: MediaCaptureServer
) {
    companion object {
        private const val INCOGNITO_PROFILE = "private_browser_incognito"
    }

    fun createWebView(
        isIncognito: Boolean,
        onProgress: (Int) -> Unit,
        onTitleChange: (String) -> Unit,
        onIconReceived: (Bitmap?) -> Unit,
        onPageStarted: (url: String) -> Unit,
        onPageFinished: (url: String, title: String) -> Unit,
        onVisitedHistoryUpdated: (url: String, title: String, isReload: Boolean) -> Unit,
        onCanGoBackForward: (back: Boolean, forward: Boolean) -> Unit,
        onVideoMetadata: (
            sourceUrl: String,
            posterUrl: String,
            pageTitle: String,
            pageUrl: String,
            isAdvertisement: Boolean,
            isPageContent: Boolean,
            isPrimaryPageMedia: Boolean
        ) -> Unit = { _, _, _, _, _, _, _ -> },
        onMseVideo: (captureId: String, mime: String, pageUrl: String) -> Unit = { _, _, _ -> },
        onCaptureEnd: (captureId: String) -> Unit = { },
        onResourceIntercepted: (WebResourceRequest) -> WebResourceResponse? = { null },
        onDownloadStart: (url: String, userAgent: String, contentDisposition: String, mimeType: String, contentLength: Long) -> Unit = { _, _, _, _, _ -> },
        onCreateWindow: (isUserGesture: Boolean, resultMsg: Message) -> Boolean = { _, _ -> false },
        onCloseWindow: (WebView) -> Unit = {},
        onRenderProcessGone: (WebView, RenderProcessGoneDetail) -> Boolean = { _, _ -> false }
    ): BrowserWebViewHandle {
        // A mutable wrapper lets the long-lived tab use the visible Activity while attached,
        // without retaining that Activity after the tab leaves composition.
        val webView = BrowserWebView(MutableContextWrapper(context.asBrowserThemedContext()))
        if (isIncognito && supportsProfileIsolation()) {
            WebViewCompat.setProfile(webView, INCOGNITO_PROFILE)
        }
        webView.settings.applyDefault(isIncognito)
        webView.addJavascriptInterface(
            webView.desktopModeBridge,
            DESKTOP_MODE_BRIDGE_NAME
        )
        webView.addJavascriptInterface(
            VideoMetadataBridge(onVideoMetadata),
            VIDEO_METADATA_BRIDGE_NAME
        )
        webView.addJavascriptInterface(
            MediaCaptureBridge(onMseVideo, onCaptureEnd),
            MEDIA_CAPTURE_BRIDGE_NAME
        )
        // Tier 3: hook MediaSource/appendBuffer at document-start (before the page's player
        // script runs) so we can capture the clean, de-obfuscated bytes it feeds the decoder.
        runCatching {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                WebViewCompat.addDocumentStartJavaScript(
                    webView,
                    DESKTOP_VIEWPORT_JS,
                    setOf("*")
                )
                val port = mediaCaptureServer.ensureStarted()
                val script = MSE_CAPTURE_JS
                    .replace("__PB_PORT__", port.toString())
                    .replace("__PB_TOKEN__", mediaCaptureServer.token)
                WebViewCompat.addDocumentStartJavaScript(webView, script, setOf("*"))
            }
        }
        webView.webViewClient = BrowserWebViewClient(
            onPageStarted = onPageStarted,
            onPageFinished = onPageFinished,
            onVisitedHistoryUpdated = onVisitedHistoryUpdated,
            onCanGoBackForward = onCanGoBackForward,
            onResourceIntercepted = onResourceIntercepted,
            renderProcessGoneHandler = onRenderProcessGone
        )
        val chromeClient = BrowserWebChromeClient(
            onProgress = onProgress,
            onTitleChange = onTitleChange,
            onIconReceived = onIconReceived,
            createWindowHandler = onCreateWindow,
            closeWindowHandler = onCloseWindow
        )
        webView.webChromeClient = chromeClient
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            onDownloadStart(url, userAgent, contentDisposition, mimeType, contentLength)
        }
        // Cookies and third-party cookies are required by federated login and bot checks.
        // On supported WebView providers, incognito uses a separate profile below.
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
        return BrowserWebViewHandle(webView, chromeClient)
    }

    internal fun getWebViewProfile(isIncognito: Boolean): Profile? {
        if (!supportsProfileIsolation()) return null
        val profileName = if (isIncognito) {
            INCOGNITO_PROFILE
        } else {
            Profile.DEFAULT_PROFILE_NAME
        }
        return ProfileStore.getInstance().getOrCreateProfile(profileName)
    }

    suspend fun clearProfileBrowsingData(
        isIncognito: Boolean,
        clearNetworkCacheFallback: Boolean = false
    ) {
        withContext(Dispatchers.Main.immediate) {
            val profile = getWebViewProfile(isIncognito)
            clearBrowsingData(
                cookieManager = profile?.cookieManager ?: CookieManager.getInstance(),
                webStorage = profile?.webStorage ?: WebStorage.getInstance(),
                geolocationPermissions = profile?.geolocationPermissions
                    ?: GeolocationPermissions.getInstance(),
                clearNetworkCacheFallback = clearNetworkCacheFallback
            )
        }
    }

    /** Clears the private profile in place so a new private tab cannot race profile deletion. */
    suspend fun clearIncognitoSessionData(): Boolean {
        return try {
            // Older WebView providers share one store between Normal and Private.
            clearProfileBrowsingData(
                isIncognito = supportsProfileIsolation(),
                clearNetworkCacheFallback = true
            )
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: RuntimeException) {
            false
        }
    }

    private suspend fun clearBrowsingData(
        cookieManager: CookieManager,
        webStorage: WebStorage,
        geolocationPermissions: GeolocationPermissions,
        clearNetworkCacheFallback: Boolean
    ) {
        val clearedWithCompatApi = if (
            WebViewFeature.isFeatureSupported(WebViewFeature.DELETE_BROWSING_DATA)
        ) {
            try {
                suspendCancellableCoroutine { continuation ->
                    WebStorageCompat.deleteBrowsingData(webStorage) {
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                }
                true
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: RuntimeException) {
                false
            }
        } else {
            false
        }

        if (!clearedWithCompatApi) {
            suspendCancellableCoroutine { continuation ->
                cookieManager.removeAllCookies {
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
            webStorage.deleteAllData()
            if (clearNetworkCacheFallback) {
                WebView(context.asBrowserThemedContext()).apply {
                    clearCache(true)
                    destroy()
                }
            }
        }

        cookieManager.flush()
        geolocationPermissions.clearAll()
    }

    fun supportsProfileIsolation(): Boolean =
        WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)
}

/**
 * Injected at page start on Meta sites. Makes the MSE player believe VP9/WebM is
 * unsupported so it requests H.264 (AVC) video renditions instead — those can be muxed
 * with the AAC audio track into a standard MP4 (VP9 cannot, on-device).
 */
private const val PREFER_H264_JS = """
(function(){
  try {
    var re = /vp0?9|vp8|webm/i;
    if (window.MediaSource && MediaSource.isTypeSupported) {
      var orig = MediaSource.isTypeSupported.bind(MediaSource);
      MediaSource.isTypeSupported = function(t){ return re.test(t||'') ? false : orig(t); };
    }
    if (window.HTMLMediaElement) {
      var cpt = HTMLMediaElement.prototype.canPlayType;
      HTMLMediaElement.prototype.canPlayType = function(t){ return re.test(t||'') ? '' : cpt.call(this, t); };
    }
  } catch(e){}
})();
"""

private const val VIDEO_METADATA_BRIDGE_NAME = "PrivateBrowserVideoBridge"

private const val DESKTOP_MODE_BRIDGE_NAME = "PrivateBrowserDesktopModeBridge"
private const val MINIMUM_DESKTOP_ZOOM_FACTOR = 0.02f
private const val DESKTOP_ZOOM_FALLBACK_DELAY_MS = 250L

private class DesktopModeBridge {
    @Volatile
    private var desktopMode = false

    fun update(enabled: Boolean) {
        desktopMode = enabled
    }

    @JavascriptInterface
    fun isDesktopMode(): Boolean = desktopMode
}

/**
 * Chrome's Desktop site mode uses a desktop-sized layout viewport in addition to a desktop UA.
 * Some responsive sites keep their mobile layout when WebView only enables wide viewport, so
 * keep the top-level document at 980 CSS pixels and guard against SPAs replacing the meta tag.
 */
private const val DESKTOP_VIEWPORT_JS = """
(function() {
  try {
    if (window.top !== window) return;
    var bridge = window.PrivateBrowserDesktopModeBridge;
    if (!bridge || !bridge.isDesktopMode || !bridge.isDesktopMode()) return;

    var desiredContent = 'width=980, user-scalable=yes';

    function forceDesktopViewport() {
      var root = document.documentElement;
      var head = document.head;
      if (!root || !head) return false;

      var metas = head.querySelectorAll('meta[name="viewport" i]');
      if (!metas.length) {
        var meta = document.createElement('meta');
        meta.setAttribute('name', 'viewport');
        meta.setAttribute('content', desiredContent);
        head.insertBefore(meta, head.firstChild);
        return true;
      }

      for (var i = 0; i < metas.length; i++) {
        if (metas[i].getAttribute('content') !== desiredContent) {
          metas[i].setAttribute('content', desiredContent);
        }
      }
      return true;
    }

    if (window.__pbDesktopViewportInstalled) {
      forceDesktopViewport();
      return;
    }
    window.__pbDesktopViewportInstalled = true;

    function installDesktopViewport() {
      var root = document.documentElement;
      if (!root) {
        setTimeout(installDesktopViewport, 0);
        return;
      }

      forceDesktopViewport();
      new MutationObserver(function() {
        forceDesktopViewport();
      }).observe(root, {
        childList: true,
        subtree: true,
        attributes: true,
        attributeFilter: ['name', 'content']
      });
    }

    installDesktopViewport();
  } catch (_) {}
})();
"""

/**
 * Collects poster/source metadata that is not available from WebResourceRequest. The bridge
 * exposes no browser or Android capability to page JavaScript; it only accepts strings.
 */
private class VideoMetadataBridge(
    private val onMetadata: (String, String, String, String, Boolean, Boolean, Boolean) -> Unit
) {
    @JavascriptInterface
    fun report(
        sourceUrl: String,
        posterUrl: String,
        pageTitle: String,
        pageUrl: String,
        isAdvertisement: Boolean,
        isPageContent: Boolean,
        isPrimaryPageMedia: Boolean
    ) {
        onMetadata(
            sourceUrl,
            posterUrl,
            pageTitle,
            pageUrl,
            isAdvertisement,
            isPageContent,
            isPrimaryPageMedia
        )
    }
}

private const val MEDIA_CAPTURE_BRIDGE_NAME = "PrivateBrowserCaptureBridge"

/**
 * Receives control signals (never the media bytes — those stream to the loopback capture
 * server) from the injected MSE hook: a capturable MSE/blob video appeared, or its stream
 * ended and the captured files are complete.
 */
private class MediaCaptureBridge(
    private val emitMseVideo: (captureId: String, mime: String, pageUrl: String) -> Unit,
    private val emitCaptureEnd: (captureId: String) -> Unit
) {
    @JavascriptInterface
    fun onMseVideo(captureId: String, mime: String, pageUrl: String) {
        emitMseVideo(captureId, mime, pageUrl)
    }

    @JavascriptInterface
    fun onCaptureEnd(captureId: String) {
        emitCaptureEnd(captureId)
    }
}

private const val VIDEO_METADATA_JS = """
(function() {
  try {
    var bridge = window.PrivateBrowserVideoBridge;
    if (!bridge || !bridge.report) return;

    function absoluteUrl(value) {
      if (!value) return '';
      try { return new URL(value, document.baseURI).href; } catch (_) { return ''; }
    }

    function pagePoster() {
      var meta = document.querySelector(
        'meta[property="og:image"],meta[name="twitter:image"],meta[property="twitter:image"]'
      );
      return absoluteUrl(meta && meta.content);
    }

    function pageTitle() {
      var meta = document.querySelector(
        'meta[property="og:title"],meta[name="twitter:title"],meta[property="twitter:title"]'
      );
      return cleanMediaTitle((meta && meta.content) || document.title);
    }

    // Media-looking URL check. Uses substring matching because manifest markers can sit
    // mid-path (e.g. ".m3u8/stream-plain"); the native side still filters out images and
    // non-video URLs, so a loose match here only risks a harmless extra report.
    function looksMedia(u) {
      if (!u) return false;
      var lu = ('' + u).toLowerCase();
      if (lu.indexOf('blob:') === 0) return false;
      var markers = ['.m3u8', '.mpd', '.mp4', '.m4v', '.webm', '.mov', '.mkv', 'mime_type=video'];
      for (var i = 0; i < markers.length; i++) { if (lu.indexOf(markers[i]) >= 0) return true; }
      return false;
    }

    function cleanMediaTitle(value) {
      var title = (value || '').toString().replace(/\s+/g, ' ').trim();
      if (!title || /^(play video|supports html5 video|advertisement|advertising|bấm xem(?: >>)?)$/i.test(title)) return '';
      return title.slice(0, 240);
    }

    // Find a title inside the closest semantic card. Walking from the video upward prevents a
    // heading from a sibling card (or the page's main article) from being assigned by accident.
    function titleForVideo(video) {
      var own = cleanMediaTitle(
        video.getAttribute('data-video-title') || video.getAttribute('data-title') ||
        video.getAttribute('aria-label') || video.getAttribute('title')
      );
      if (own) return own;

      var selectors = [
        '[data-video-title]', '[data-title]', 'h1,h2,h3,h4,h5,h6',
        '.video-title,.media-title,.title',
        '[class*="title-"],[class*="-title"],[class*="__title"],[class*="__name"]',
        'a[title]'
      ];
      var node = video.parentElement;
      for (var depth = 0; node && depth < 5; depth++, node = node.parentElement) {
        for (var i = 0; i < selectors.length; i++) {
          var candidate = node.querySelector(selectors[i]);
          var title = cleanMediaTitle(candidate && (
            candidate.getAttribute('data-video-title') || candidate.getAttribute('data-title') ||
            candidate.getAttribute('title') || candidate.innerText || candidate.textContent
          ));
          if (title) return title;
        }
        if (node.matches && node.matches('article,li,[role="listitem"]')) {
          var link = node.querySelector('a[href]');
          var linkTitle = cleanMediaTitle(link && (link.getAttribute('title') || link.innerText));
          if (linkTitle) return linkTitle;
        }
      }
      return '';
    }

    var seen = window.__pbSeenMedia || (window.__pbSeenMedia = {});
    function send(rawUrl, poster, mediaTitle, isPageContent, isPrimaryPageMedia) {
      var u = absoluteUrl(rawUrl);
      var evidenceRank = isPrimaryPageMedia ? 3 : (isPageContent ? 2 : 1);
      // Let later DOM/SEO evidence upgrade a URL first seen by the passive fetch/XHR hook.
      if (!u || (seen[u] || 0) >= evidenceRank) return;
      seen[u] = evidenceRank;
      try {
        bridge.report(
          u,
          absoluteUrl(poster) || (isPrimaryPageMedia ? pagePoster() : ''),
          cleanMediaTitle(mediaTitle) || (isPrimaryPageMedia ? pageTitle() : ''),
          location.href || '',
          false,
          !!isPageContent,
          !!isPrimaryPageMedia
        );
      } catch (_) {}
    }

    // Only use explicit, high-confidence markers. Broad checks such as an "ad" substring
    // would incorrectly reject normal containers like "header" or "shadow".
    function isAdvertisementVideo(video) {
      try {
        var title = (video.getAttribute('title') || '').toLowerCase();
        if (title === 'advertisement' || title === 'advertising') return true;
        if (video.hasAttribute('data-ad') || video.hasAttribute('data-ad-slot')) return true;

        var node = video;
        for (var depth = 0; node && depth < 7; depth++, node = node.parentElement) {
          if (node.hasAttribute &&
              (node.hasAttribute('data-ad') || node.hasAttribute('data-ad-slot'))) return true;
          var marker = ((node.id || '') + ' ' +
            (typeof node.className === 'string' ? node.className : '')).toLowerCase();
          if (/(^|[\s_-])(advertisement|advertising|video[_-]?ad|ad[_-]?(container|player|slot)|ima[_-]?ad|vast|pre[_-]?roll|mid[_-]?roll|post[_-]?roll|ads)([\s_-]|$)/.test(marker)) return true;
          // Common naming for silent autoplay video creatives used by banner ads.
          if (/(^|[\s_-])vid[_-]?bg([\s_-]|$)/.test(marker)) return true;
        }
      } catch (_) {}
      return false;
    }

    // 1) <video>/<source> elements. Report every HTTP source: currentSrc is often a blob
    // while the child <source> still contains the downloadable HLS manifest.
    function reportVideo(video) {
      var poster = absoluteUrl(video.poster);
      var mediaTitle = titleForVideo(video);
      var isAd = isAdvertisementVideo(video);
      var sources = [];
      [video.currentSrc, video.src].forEach(function(source) {
        var u = absoluteUrl(source);
        if (u && u.indexOf('blob:') !== 0 && sources.indexOf(u) < 0) sources.push(u);
      });
      video.querySelectorAll('source[src]').forEach(function(el) {
        var u = absoluteUrl(el.src || el.getAttribute('src'));
        if (u && u.indexOf('blob:') !== 0 && sources.indexOf(u) < 0) sources.push(u);
      });
      // A blob-only video still reports its context so native code can name an MSE capture.
      if (!sources.length) sources.push('');
      sources.forEach(function(source) {
        try {
          bridge.report(
            source,
            poster,
            mediaTitle,
            location.href || '',
            isAd,
            !isAd,
            false
          );
        } catch (_) {}
      });
    }

    // Reddit renders its player as a custom element. The actual <video> lives in a shadow tree,
    // but the host exposes the complete signed HLS URL, poster and post title as attributes.
    // `post-promoted` is also the reliable marker for promoted video ads in the feed.
    var redditSeen = window.__pbSeenRedditMedia || (window.__pbSeenRedditMedia = {});
    function reportRedditPlayer(player) {
      try {
        var source = absoluteUrl(player.getAttribute('src'));
        if (!source) return;
        var isAd = player.hasAttribute('post-promoted');
        var evidence = isAd ? 'ad' : 'content';
        if (redditSeen[source] === evidence) return;
        redditSeen[source] = evidence;
        bridge.report(
          source,
          absoluteUrl(player.getAttribute('poster')),
          cleanMediaTitle(player.getAttribute('post-title')),
          location.href || '',
          isAd,
          !isAd,
          false
        );
      } catch (_) {}
    }

    // 2) Social/SEO metadata that names the media file directly.
    function scanMeta() {
      // Always report page-level OpenGraph context separately. Native code only applies this
      // fallback when exactly one direct media family remains after ad filtering and HLS
      // consolidation, so a scrolling feed never gives every video the same article title.
      var pageMetadataKey = (location.href || '') + '|' + pageTitle() + '|' + pagePoster();
      if (window.__pbPageMetadataKey !== pageMetadataKey) {
        window.__pbPageMetadataKey = pageMetadataKey;
        try {
          bridge.report(
            '', pagePoster(), pageTitle(), location.href || '', false, false, true
          );
        } catch (_) {}
      }
      document.querySelectorAll(
        'meta[property="og:video"],meta[property="og:video:url"],' +
        'meta[property="og:video:secure_url"],meta[name="twitter:player:stream"]'
      ).forEach(function(m) { if (looksMedia(m.content)) send(m.content, '', '', true, true); });
      document.querySelectorAll('script[type="application/ld+json"]').forEach(function(s) {
        try {
          (function walk(o) {
            if (!o || typeof o !== 'object') return;
            if (typeof o.contentUrl === 'string' && looksMedia(o.contentUrl)) {
              var thumb = typeof o.thumbnailUrl === 'string' ? o.thumbnailUrl : '';
              send(o.contentUrl, thumb, o.name || o.headline || '', true, true);
            }
            for (var k in o) walk(o[k]);
          })(JSON.parse(s.textContent));
        } catch (_) {}
      });
    }

    // 3) Read the real source straight from common player objects.
    function scanPlayers() {
      try {
        if (window.jwplayer) {
          var p = window.jwplayer();
          if (p && p.getPlaylist) {
            (p.getPlaylist() || []).forEach(function(it) {
              var itemTitle = it && (it.title || it.name) || '';
              var itemPoster = it && (it.image || it.poster) || '';
              if (it && looksMedia(it.file)) send(it.file, itemPoster, itemTitle, true, false);
              (it && it.sources || []).forEach(function(sc) {
                if (looksMedia(sc.file)) send(sc.file, itemPoster, sc.label || itemTitle, true, false);
              });
            });
          }
        }
      } catch (_) {}
      try {
        if (window.videojs && videojs.getAllPlayers) {
          videojs.getAllPlayers().forEach(function(p) {
            var c = p && p.currentSrc && p.currentSrc();
            if (looksMedia(c)) send(c, '', '', true, false);
          });
        }
      } catch (_) {}
      // hls.js and dash.js commonly stash the instance on the <video> element or window.
      try {
        [window.hls, window.hlsPlayer].forEach(function(h) {
          if (h && looksMedia(h.url)) send(h.url, '', '', true, false);
        });
      } catch (_) {}
      try {
        [window.player, window.dashPlayer, window.dashjsPlayer].forEach(function(d) {
          if (d && d.getSource) { var s = d.getSource(); if (looksMedia(s)) send(s, '', '', true, false); }
        });
      } catch (_) {}
      // Shaka Player exposes the manifest URI it is currently playing.
      try {
        [window.player, window.shakaPlayer].forEach(function(s) {
          if (s && s.getAssetUri) { var u = s.getAssetUri(); if (looksMedia(u)) send(u, '', '', true, false); }
          else if (s && s.getManifestUri) { var m = s.getManifestUri(); if (looksMedia(m)) send(m, '', '', true, false); }
        });
      } catch (_) {}
    }

    // 4) Best-effort fetch/XHR hooks for media requests the player makes after load.
    if (!window.__pbNetHooked) {
      window.__pbNetHooked = 1;
      try {
        var of = window.fetch;
        if (of) window.fetch = function(input) {
          try {
            var u = (typeof input === 'string') ? input : (input && input.url);
            if (looksMedia(u)) send(u, '', '', false, false);
          } catch (_) {}
          return of.apply(this, arguments);
        };
      } catch (_) {}
      try {
        var oo = XMLHttpRequest.prototype.open;
        XMLHttpRequest.prototype.open = function(m, u) {
          try { if (looksMedia(u)) send(u, '', '', false, false); } catch (_) {}
          return oo.apply(this, arguments);
        };
      } catch (_) {}
    }

    function scanAll() {
      document.querySelectorAll('shreddit-player[src]').forEach(reportRedditPlayer);
      document.querySelectorAll('video').forEach(reportVideo);
      scanMeta();
      scanPlayers();
    }

    if (!window.__privateBrowserVideoMetadataObserver) {
      var scheduled = false;
      var scheduleScan = function() {
        if (scheduled) return;
        scheduled = true;
        setTimeout(function() { scheduled = false; scanAll(); }, 250);
      };
      window.__privateBrowserVideoMetadataObserver = new MutationObserver(scheduleScan);
      window.__privateBrowserVideoMetadataObserver.observe(document.documentElement, {
        subtree: true,
        childList: true,
        attributes: true,
        attributeFilter: ['src', 'poster', 'content', 'post-title', 'post-promoted']
      });
      document.addEventListener('loadedmetadata', function(event) {
        if (event.target && event.target.tagName === 'VIDEO') reportVideo(event.target);
      }, true);
      document.addEventListener('play', function(event) {
        if (event.target && event.target.tagName === 'VIDEO') { reportVideo(event.target); scanPlayers(); }
      }, true);
    }
    scanAll();
  } catch (_) {}
})();
"""

/**
 * Runs at document-start (before the page's player script). Wraps MediaSource/SourceBuffer so
 * every `appendBuffer` chunk — the clean bytes the site already de-obfuscated/decrypted for the
 * decoder — is streamed to the loopback capture server. Bytes never touch the JS bridge; only
 * small control signals do. Best-effort and fully wrapped in try/catch so it can't break a page.
 */
private const val MSE_CAPTURE_JS = """
(function() {
  try {
    // YouTube downloads are intentionally unsupported (Play Store policy; its VP9/AV1+Opus
    // MSE stream isn't MP4-muxable on-device). Don't capture its bytes at all — the native
    // side hides the FAB, so a capture here would only waste CPU/disk and never be consumed.
    var __h = (location.hostname || '').toLowerCase();
    if (/(^|\.)(youtube\.com|youtu\.be|youtube-nocookie\.com|googlevideo\.com)$/.test(__h)) return;
    if (window.__pbMseHooked) return;
    window.__pbMseHooked = 1;
    var BASE = 'http://127.0.0.1:__PB_PORT__/__PB_TOKEN__';
    function bridge() { return window.PrivateBrowserCaptureBridge; }

    var counter = 0;
    function newId() { counter += 1; return 'c' + counter + 'x' + Date.now(); }

    // Sequential POST queue per (capture, track) so bytes land in order.
    function poster(captureId, track) {
      var chain = Promise.resolve();
      return function(buf) {
        chain = chain.then(function() {
          return fetch(BASE + '/' + captureId + '/' + track, { method: 'POST', body: buf })
            .catch(function() {});
        });
      };
    }

    function endCapture(id) {
      if (!id || id.__ended) return;
      try { fetch(BASE + '/' + id + '/end', { method: 'POST' }).catch(function() {}); } catch (_) {}
      try { var b = bridge(); if (b && b.onCaptureEnd) b.onCaptureEnd(id); } catch (_) {}
    }

    var MS = window.MediaSource || window.WebKitMediaSource;
    if (!MS || !MS.prototype || !MS.prototype.addSourceBuffer) return;

    var origAdd = MS.prototype.addSourceBuffer;
    MS.prototype.addSourceBuffer = function(mime) {
      var sb = origAdd.apply(this, arguments);
      try {
        if (!this.__pbId) {
          this.__pbId = newId();
          var self = this;
          var finish = function() { endCapture(self.__pbId); };
          this.addEventListener('sourceended', finish);
          this.addEventListener('sourceclose', finish);
          try { var b = bridge(); if (b && b.onMseVideo) b.onMseVideo(this.__pbId, '' + (mime || ''), location.href || ''); } catch (_) {}
        }
        var lower = ('' + (mime || '')).toLowerCase();
        var track = lower.indexOf('audio') >= 0 ? 'a' : (lower.indexOf('video') >= 0 ? 'v' : 'm');
        sb.__pbPost = poster(this.__pbId, track);
      } catch (_) {}
      return sb;
    };

    var SB = window.SourceBuffer;
    if (SB && SB.prototype && SB.prototype.appendBuffer) {
      var origAppend = SB.prototype.appendBuffer;
      SB.prototype.appendBuffer = function(data) {
        try {
          if (this.__pbPost && data) {
            var copy;
            if (data.buffer) { copy = data.buffer.slice(data.byteOffset, data.byteOffset + data.byteLength); }
            else { copy = data.slice(0); }
            this.__pbPost(copy);
          }
        } catch (_) {}
        return origAppend.apply(this, arguments);
      };
    }
  } catch (_) {}
})();
"""

class BrowserWebViewClient(
    private val onPageStarted: (url: String) -> Unit,
    private val onPageFinished: (url: String, title: String) -> Unit,
    private val onVisitedHistoryUpdated: (url: String, title: String, isReload: Boolean) -> Unit,
    private val onCanGoBackForward: (back: Boolean, forward: Boolean) -> Unit,
    private val onResourceIntercepted: (WebResourceRequest) -> WebResourceResponse?,
    private val renderProcessGoneHandler: (WebView, RenderProcessGoneDetail) -> Boolean
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        val scheme = request.url.scheme ?: ""
        return when (scheme.lowercase()) {
            "http", "https", "", "about", "data", "javascript", "file" -> false
            "intent" -> handleIntent(view, url)
            // Custom app schemes (fb://, fb-messenger://, tg://, mailto/tel/sms, etc.):
            // try to open the relevant app and consume the navigation so the WebView
            // doesn't render an ERR_UNKNOWN_URL_SCHEME error page.
            else -> {
                if (request.isForMainFrame || request.hasGesture()) {
                    launchExternal(view.context, request.url)
                }
                true
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        val uri = Uri.parse(url)
        val scheme = uri.scheme ?: ""
        return when (scheme.lowercase()) {
            "http", "https", "", "about", "data", "javascript", "file" -> false
            "intent" -> handleIntent(view, url)
            else -> {
                launchExternal(view.context, uri)
                true
            }
        }
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        view.applyDesktopViewportFallback()
        // On Meta sites, tell the player VP9/WebM is unsupported so it serves H.264 (AVC)
        // renditions, which can be muxed with AAC into MP4 (VP9 cannot). Scoped to Meta
        // domains to avoid breaking VP9-only playback elsewhere.
        if (url.contains("instagram.", true) || url.contains("threads.", true) ||
            url.contains("facebook.", true) || url.contains("//fb.", true)) {
            try { view.evaluateJavascript(PREFER_H264_JS, null) } catch (_: Exception) {}
        }
        onPageStarted(url)
        onCanGoBackForward(view.canGoBack(), view.canGoForward())
    }

    override fun onPageFinished(view: WebView, url: String) {
        view.applyDesktopViewportFallback(zoomToMinimum = true)
        try { view.evaluateJavascript(VIDEO_METADATA_JS, null) } catch (_: Exception) {}
        onPageFinished(url, view.title ?: url)
        onCanGoBackForward(view.canGoBack(), view.canGoForward())
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
        super.doUpdateVisitedHistory(view, url, isReload)
        view.applyDesktopViewportFallback(zoomToMinimum = true)
        onVisitedHistoryUpdated(url, view.title ?: url, isReload)
        onCanGoBackForward(view.canGoBack(), view.canGoForward())
    }

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        return onResourceIntercepted(request)
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        // Cancel by default for security, conform to v1 spec
        handler.cancel()
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        return renderProcessGoneHandler(view, detail)
    }

    private fun handleIntent(view: WebView, url: String): Boolean {
        val intent = try {
            Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
        } catch (_: Exception) {
            return true
        }

        // Do not allow a website to target an explicit internal component.
        intent.component = null
        intent.selector = null
        intent.addCategory(Intent.CATEGORY_BROWSABLE)

        return try {
            launchIntent(view.context, intent)
            true
        } catch (_: Exception) {
            val fallbackUrl = intent.getStringExtra("browser_fallback_url")
            if (fallbackUrl?.let(::isHttpUrl) == true) {
                view.loadUrl(fallbackUrl)
            }
            true
        }
    }

    private fun launchExternal(context: Context, uri: Uri): Boolean {
        return try {
            launchIntent(context, Intent(Intent.ACTION_VIEW, uri).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            })
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun launchIntent(context: Context, intent: Intent) {
        if (!context.hasActivity()) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private tailrec fun Context.hasActivity(): Boolean {
        return when (this) {
            is android.app.Activity -> true
            is ContextWrapper -> {
                val base = baseContext
                if (base === this) false else base.hasActivity()
            }
            else -> false
        }
    }

    private fun isHttpUrl(url: String): Boolean {
        val scheme = runCatching { Uri.parse(url).scheme?.lowercase() }.getOrNull()
        return scheme == "http" || scheme == "https"
    }
}

class BrowserWebChromeClient(
    private val onProgress: (Int) -> Unit,
    private val onTitleChange: (String) -> Unit,
    private val onIconReceived: (Bitmap?) -> Unit,
    private val createWindowHandler: (isUserGesture: Boolean, resultMsg: Message) -> Boolean,
    private val closeWindowHandler: (WebView) -> Unit
) : WebChromeClient() {

    var fileChooserHandler: ((ValueCallback<Array<Uri>>, FileChooserParams) -> Boolean)? = null
    var permissionRequestHandler: ((PermissionRequest) -> Unit)? = null
    var permissionRequestCanceledHandler: ((PermissionRequest) -> Unit)? = null
    var geolocationPromptHandler: ((String, GeolocationPermissions.Callback) -> Unit)? = null
    var geolocationPromptHiddenHandler: (() -> Unit)? = null
    var showCustomViewHandler: ((View, CustomViewCallback) -> Unit)? = null
    var hideCustomViewHandler: (() -> Unit)? = null

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        onProgress(newProgress)
    }

    override fun onReceivedTitle(view: WebView, title: String) {
        onTitleChange(title)
    }

    override fun onReceivedIcon(view: WebView, icon: Bitmap) {
        onIconReceived(icon)
    }

    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message
    ): Boolean = createWindowHandler(isUserGesture, resultMsg)

    override fun onCloseWindow(window: WebView) {
        closeWindowHandler(window)
    }

    override fun onShowFileChooser(
        webView: WebView,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams
    ): Boolean {
        return fileChooserHandler?.invoke(filePathCallback, fileChooserParams) ?: false
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        permissionRequestHandler?.invoke(request) ?: request.deny()
    }

    override fun onPermissionRequestCanceled(request: PermissionRequest) {
        permissionRequestCanceledHandler?.invoke(request)
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String,
        callback: GeolocationPermissions.Callback
    ) {
        geolocationPromptHandler?.invoke(origin, callback) ?: callback.invoke(origin, false, false)
    }

    override fun onGeolocationPermissionsHidePrompt() {
        geolocationPromptHiddenHandler?.invoke()
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        showCustomViewHandler?.invoke(view, callback) ?: callback.onCustomViewHidden()
    }

    override fun onHideCustomView() {
        hideCustomViewHandler?.invoke()
    }

    fun clearUiHandlers() {
        fileChooserHandler = null
        permissionRequestHandler = null
        permissionRequestCanceledHandler = null
        geolocationPromptHandler = null
        geolocationPromptHiddenHandler = null
        showCustomViewHandler = null
        hideCustomViewHandler = null
    }
}

/**
 * WebView derives `prefers-color-scheme` from the Android theme of its Context.
 * Keep that theme stable before, during, and after attaching a long-lived tab.
 */
internal fun Context.asBrowserThemedContext(): Context =
    ContextThemeWrapper(this, R.style.Theme_PrivateBrowser)

fun WebSettings.applyDefault(isIncognito: Boolean) {
    javaScriptEnabled = true
    // DOM storage is required by bot checks and sign-in pages. Incognito isolates it in
    // a dedicated WebView profile when the installed provider supports multi-profile.
    domStorageEnabled = true
    setGeolocationEnabled(true)
    // Mobile pages must use the actual WebView width. Wide/overview viewport is enabled only when
    // the user explicitly requests Desktop site.
    useWideViewPort = false
    loadWithOverviewMode = false

    allowFileAccess = false
    allowContentAccess = false

    @Suppress("DEPRECATION")
    allowFileAccessFromFileURLs = false
    @Suppress("DEPRECATION")
    allowUniversalAccessFromFileURLs = false

    setSupportZoom(true)
    builtInZoomControls = true
    displayZoomControls = false
    loadsImagesAutomatically = true
    blockNetworkImage = false
    setSupportMultipleWindows(true)
    // Match modern popup blocking: user-initiated target=_blank/window.open is still handled.
    javaScriptCanOpenWindowsAutomatically = false

    mediaPlaybackRequiresUserGesture = false
    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        safeBrowsingEnabled = true
    }

    @Suppress("DEPRECATION")
    saveFormData = !isIncognito

    cacheMode = if (isIncognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
    // applyDesktopMode() replaces this with a provider-derived mobile or desktop UA before load.
    userAgentString = null
}

internal fun mobileUserAgent(defaultUserAgent: String): String {
    return defaultUserAgent
        .replace("; wv", "")
        .replace("Version/4.0 ", "")
}

internal fun desktopUserAgent(defaultUserAgent: String): String {
    return mobileUserAgent(defaultUserAgent)
        .replace(Regex("\\(Linux; Android[^)]*\\)"), "(X11; Linux x86_64)")
        .replace(" Mobile Safari/", " Safari/")
}

internal fun WebView.applyDesktopMode(enabled: Boolean) {
    (this as? BrowserWebView)?.updateDesktopMode(enabled)
    val providerUserAgent = WebSettings.getDefaultUserAgent(context)
    settings.userAgentString = if (enabled) {
        desktopUserAgent(providerUserAgent)
    } else {
        mobileUserAgent(providerUserAgent)
    }
    settings.useWideViewPort = enabled
    settings.loadWithOverviewMode = enabled
    settings.applyUserAgentMetadata(enabled)
    setInitialScale(0)
}

private fun WebView.applyDesktopViewportFallback(zoomToMinimum: Boolean = false) {
    val browserWebView = this as? BrowserWebView ?: return
    if (!browserWebView.desktopModeBridge.isDesktopMode()) return
    runCatching {
        evaluateJavascript(DESKTOP_VIEWPORT_JS) {
            if (zoomToMinimum) browserWebView.requestMinimumDesktopZoom()
        }
    }.onFailure {
        if (zoomToMinimum) browserWebView.requestMinimumDesktopZoom()
    }
}

private fun WebSettings.applyUserAgentMetadata(isDesktop: Boolean) {
    if (!WebViewFeature.isFeatureSupported(WebViewFeature.USER_AGENT_METADATA)) return

    // Modern sites can prioritize Client Hints over the legacy User-Agent string.
    runCatching {
        val metadata = UserAgentMetadata.Builder()
            .setMobile(!isDesktop)
            .apply {
                if (isDesktop) {
                    setPlatform("Linux")
                    setPlatformVersion("")
                    setArchitecture("x86")
                    setModel("")
                    setBitness(64)
                }
                if (
                    WebViewFeature.isFeatureSupported(
                        WebViewFeature.USER_AGENT_METADATA_FORM_FACTORS
                    )
                ) {
                    setFormFactors(
                        listOf(
                            if (isDesktop) {
                                UserAgentMetadata.FORM_FACTOR_DESKTOP
                            } else {
                                UserAgentMetadata.FORM_FACTOR_MOBILE
                            }
                        )
                    )
                }
            }
            .build()
        WebSettingsCompat.setUserAgentMetadata(this, metadata)
    }
}
