package com.asianmobile.privatebrower.data.browser

import android.webkit.WebResourceRequest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class DetectedVideo(
    val url: String,
    val mimeType: String?,
    val refererUrl: String?,
    val headers: Map<String, String>,
    /** Facebook DASH: separate audio-only track URL to be muxed with [url] (video-only). */
    val audioUrl: String? = null,
    /** Facebook video_id, used to group/deduplicate the tracks of one video. */
    val facebookVideoId: String? = null,
    /** Overrides the derived display name (used for Facebook videos). */
    val customName: String? = null,
    /** Poster/preview image discovered from the page DOM or social metadata. */
    val thumbnailUrl: String? = null,
    /** Tier 3: MediaSource capture id. Non-null means the bytes are captured from the player. */
    val captureId: String? = null,
    /** True only when DOM/SEO/player evidence ties this URL to the page's actual content. */
    val isPageContent: Boolean = false
) {
    val isHls: Boolean
        get() = classifyMediaResource(url, mimeType) == MediaResourceKind.HLS_MANIFEST

    /** MPEG-DASH manifest — downloaded by parsing the .mpd and muxing the chosen tracks. */
    val isDash: Boolean
        get() = classifyMediaResource(url, mimeType) == MediaResourceKind.DASH_MANIFEST

    /** True when this video is produced by capturing the player's MediaSource stream. */
    val isCapture: Boolean get() = !captureId.isNullOrBlank()

    /** True when this is a Facebook DASH video that must mux a separate audio track. */
    val needsMux: Boolean get() = !audioUrl.isNullOrBlank()

    val displayName: String
        get() {
            customName?.let { return it }
            val path = try { java.net.URI(url).path } catch (_: Exception) { null }
            val fileName = path?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            return fileName ?: "video_${url.hashCode().toUInt()}"
        }

    val fileExtension: String
        get() {
            val pathExtension = mediaPathExtension(url)
            return when {
                mimeType == "video/mp4" || pathExtension == "mp4" || pathExtension == "m4v" -> "mp4"
                mimeType == "video/webm" || pathExtension == "webm" -> "webm"
                mimeType == "video/quicktime" || pathExtension == "mov" -> "mov"
                isHls -> "m3u8"
                pathExtension == "ts" -> "ts"
                pathExtension == "mkv" -> "mkv"
                pathExtension == "flv" -> "flv"
                pathExtension == "avi" -> "avi"
                else -> "video"
            }
        }
}

internal data class MediaDisplayMetadata(val posterUrl: String?, val title: String?)

internal fun Map<String, MediaDisplayMetadata>.withMediaMetadata(
    mediaKey: String,
    posterUrl: String?,
    title: String?
): Map<String, MediaDisplayMetadata> {
    val previous = this[mediaKey]
    return this + (
        mediaKey to MediaDisplayMetadata(
            posterUrl = posterUrl ?: previous?.posterUrl,
            title = title ?: previous?.title
        )
    )
}

/**
 * Detects downloadable videos from intercepted web requests.
 *
 * Per-platform handling and the playbook for adding new sites (incl. the pending X /
 * Twitter and Dailymotion work) live in docs/features/F05b_VIDEO_SITE_HANDLERS.md.
 */
@Singleton
class VideoSniffer @Inject constructor() {
    private val _detectedFlow = MutableSharedFlow<DetectedVideo>(extraBufferCapacity = 16)
    val detectedFlow: SharedFlow<DetectedVideo> = _detectedFlow.asSharedFlow()

    private val _detectedVideos = MutableStateFlow<List<DetectedVideo>>(emptyList())
    val detectedVideos: StateFlow<List<DetectedVideo>> = _detectedVideos.asStateFlow()

    private data class PageMetadata(
        val posterUrl: String?,
        val title: String?,
        /** Metadata is keyed per media family; one page can lazy-load many unrelated videos. */
        val contentMetadata: Map<String, MediaDisplayMetadata> = emptyMap(),
        /** Exact ad URLs and their families stay blocked if scrolling triggers them again. */
        val advertisementUrls: Set<String> = emptySet(),
        val advertisementMediaKeys: Set<String> = emptySet()
    )

    // Detected videos are bucketed per page so that opening another tab — e.g. a pop-under
    // ad that steals focus — never wipes the videos found on the page the user is actually
    // watching. Only the active page's bucket is surfaced via [detectedVideos].
    private val lock = Any()
    private val videosByPage = LinkedHashMap<String, List<DetectedVideo>>()

    @Volatile
    private var currentPageUrl: String? = null
    private val pageMetadata = mutableMapOf<String, PageMetadata>()

    /** Apply [transform] to [pageKey]'s videos; surface the result only if it's the active page. */
    private fun mutatePage(pageKey: String?, transform: (List<DetectedVideo>) -> List<DetectedVideo>) {
        if (pageKey.isNullOrBlank()) return
        val metadata = synchronized(pageMetadata) { pageMetadata[pageKey] }
        synchronized(lock) {
            val updated = transform(videosByPage[pageKey].orEmpty())
            videosByPage[pageKey] = updated
            // Bound memory: drop the oldest bucket (never the active page) past the cap.
            while (videosByPage.size > MAX_TRACKED_PAGES) {
                val eldest = videosByPage.keys.firstOrNull() ?: break
                if (eldest == currentPageUrl) break
                videosByPage.remove(eldest)
            }
            if (pageKey == currentPageUrl) {
                _detectedVideos.value = updated.forPresentation(metadata)
            }
        }
    }

    /**
     * Page-level OpenGraph metadata is only a safe fallback when filtering and consolidation
     * leave exactly one direct media family. Keep this enrichment out of [videosByPage]: a page
     * can lazy-load more videos while scrolling, and persisting the fallback would incorrectly
     * give every later item the first article's title and poster.
     */
    private fun List<DetectedVideo>.forPresentation(metadata: PageMetadata?): List<DetectedVideo> {
        val title = metadata?.title
        val poster = metadata?.posterUrl
        if (title == null && poster == null) return this

        val directFamilyKeys = asSequence()
            .filterNot { it.isCapture }
            .map { it.mediaIdentityKey() }
            .distinct()
            .toList()
        if (directFamilyKeys.size != 1) return this

        val onlyFamily = directFamilyKeys.single()
        return map { video ->
            if (!video.isCapture && video.mediaIdentityKey() == onlyFamily) {
                video.copy(
                    customName = video.customName ?: title,
                    thumbnailUrl = video.thumbnailUrl ?: poster,
                    isPageContent = true
                )
            } else video
        }.preferDirectOverCapture()
    }

    fun onResourceIntercepted(request: WebResourceRequest, pageUrl: String?) {
        val url = request.url.toString()
        val headers = request.requestHeaders

        // YouTube is intentionally not downloadable (Play Store policy, and its MSE-only
        // stream is codec-incompatible with our MP4 muxer anyway). Suppress detection for
        // both the page host and the media host so the download FAB never appears there.
        if (isDownloadBlockedHost(pageUrl) || isDownloadBlockedHost(url) ||
            isLikelyAdvertisementUrl(url)
        ) return

        // Meta CDNs (Facebook / Instagram / Threads) deliver video via the efg-tagged
        // URL. Handle them specially: DASH separate tracks are muxed, progressive files
        // are downloaded directly. (For IG/Threads we force H.264 progressive via JS.)
        if ((url.contains("fbcdn", ignoreCase = true) ||
                url.contains("cdninstagram", ignoreCase = true)) &&
            url.contains("efg=", ignoreCase = true)
        ) {
            handleMetaTrack(url, pageUrl, request.requestHeaders)
            return
        }

        // Strong signals from the browser engine itself: a <video> element's media
        // request carries Sec-Fetch-Dest: video (or an "Accept: video/*" header).
        // This catches progressive videos whose URL has no file extension.
        val secFetchDest = headers["Sec-Fetch-Dest"]?.trim()?.lowercase()
        val acceptsVideo = headers["Accept"]?.startsWith("video/", ignoreCase = true) == true
        val acceptsHls = headers["Accept"]?.contains("mpegurl", ignoreCase = true) == true
        val isMediaRequest = secFetchDest == "video" || acceptsVideo || acceptsHls

        var mime = guessMimeType(url, headers)
        if (mime == null && acceptsHls) mime = "application/vnd.apple.mpegurl"
        if (mime == null && isMediaRequest) mime = "video/mp4"

        // Reddit's player requests separate CMAF/DASH audio tracks. They are not playable videos
        // and must never be offered as standalone downloads; the master manifest includes them.
        if (isRedditAudioTrack(url)) return

        if (isVideoUrl(url, mime, isMediaRequest)) {
            val pageKey = pageUrl?.let(::canonicalPageUrl)
            val metadata = synchronized(pageMetadata) {
                pageKey?.let { pageMetadata[it] }
            }
            val identityKey = mediaIdentityKey(url, mime)
            val canonicalUrl = canonicalMediaUrl(url)
            if (metadata?.advertisementUrls?.contains(canonicalUrl) == true ||
                metadata?.advertisementMediaKeys?.contains(identityKey) == true
            ) return
            val mediaMetadata = metadata?.contentMetadata?.get(identityKey)
            val isPageContent = mediaMetadata != null
            val video = DetectedVideo(
                url = url,
                mimeType = mime,
                refererUrl = pageUrl,
                headers = request.requestHeaders.withReferer(pageUrl),
                customName = mediaMetadata?.title,
                thumbnailUrl = mediaMetadata?.posterUrl,
                isPageContent = isPageContent
            )
            _detectedFlow.tryEmit(video)
            if (com.asianmobile.privatebrower.BuildConfig.DEBUG) {
                android.util.Log.d(
                    "PBSniffer",
                    "detect mime=$mime hls=${video.isHls} mux=${video.needsMux} url=${url.take(140)}"
                )
            }

            mutatePage(pageKey) { current ->
                current.mergeVideo(video).dropPlaylistWrappers()
                    .consolidateSameVideo().preferDirectOverCapture()
            }
        }
    }

    /**
     * Merge page evidence with intercepted media URLs. Advertisement reports are tombstones:
     * they remove an already intercepted URL and prevent scroll/lazy-loading from adding it again.
     * Page title/poster are only copied to the matching confirmed media family, never every video
     * request that happened to share the same referer page.
     */
    fun onVideoMetadata(
        sourceUrl: String,
        posterUrl: String,
        pageTitle: String,
        pageUrl: String,
        isAdvertisement: Boolean = false,
        isPageContent: Boolean = true,
        isPrimaryPageMedia: Boolean = false
    ) {
        // YouTube downloads are intentionally unsupported — never surface its media (see
        // onResourceIntercepted).
        if (isDownloadBlockedHost(pageUrl) || isDownloadBlockedHost(sourceUrl)) return

        // Metadata is bucketed per page, so accept it from background tabs too (their videos
        // are stored under their own page key and only surfaced when that page is active).
        val pageKey = canonicalPageUrl(pageUrl)
        if (pageKey.isBlank()) return

        val source = sourceUrl.takeIf(::isHttpUrl)
        val sourceMime = source?.let { guessMimeType(it, emptyMap()) }
        val sourceIsVideo = source != null && isVideoUrl(source, sourceMime)
        val sourceKey = source?.takeIf { sourceIsVideo }?.let { mediaIdentityKey(it, sourceMime) }
        val canonicalSource = source?.let(::canonicalMediaUrl)

        if (isAdvertisement) {
            if (canonicalSource == null) return
            synchronized(pageMetadata) {
                val existing = pageMetadata[pageKey] ?: PageMetadata(null, null)
                pageMetadata[pageKey] = existing.copy(
                    advertisementUrls = existing.advertisementUrls + canonicalSource,
                    advertisementMediaKeys = sourceKey?.let {
                        existing.advertisementMediaKeys + it
                    } ?: existing.advertisementMediaKeys
                )
            }
            mutatePage(pageKey) { current ->
                current.removeMediaFamily(canonicalSource, sourceKey)
            }
            return
        }

        // Neither a passive fetch/XHR hook nor a generic player API may resurrect a URL that
        // its concrete DOM <video> node already confirmed as an advertisement.
        if (canonicalSource != null) {
            val isKnownAdvertisement = synchronized(pageMetadata) {
                val existing = pageMetadata[pageKey]
                canonicalSource in existing?.advertisementUrls.orEmpty() ||
                    (sourceKey != null && sourceKey in existing?.advertisementMediaKeys.orEmpty())
            }
            if (isKnownAdvertisement) return
        }

        val poster = posterUrl.takeIf(::isHttpUrl)
        val title = pageTitle.trim().takeIf { it.isNotEmpty() }?.let(::safePageTitle)
        val confirmedContent = isPageContent && sourceKey != null
        synchronized(pageMetadata) {
            val existing = pageMetadata[pageKey] ?: PageMetadata(null, null)
            val updatedContentMetadata = if (confirmedContent) {
                existing.contentMetadata.withMediaMetadata(
                    mediaKey = sourceKey!!,
                    posterUrl = poster,
                    title = title
                )
            } else existing.contentMetadata
            pageMetadata[pageKey] = existing.copy(
                posterUrl = poster.takeIf { isPrimaryPageMedia } ?: existing.posterUrl,
                title = title.takeIf { isPrimaryPageMedia } ?: existing.title,
                contentMetadata = updatedContentMetadata,
                advertisementUrls = existing.advertisementUrls,
                advertisementMediaKeys = existing.advertisementMediaKeys
            )
        }

        if (com.asianmobile.privatebrower.BuildConfig.DEBUG && source != null) {
            android.util.Log.d(
                "PBSniffer",
                "meta content=$confirmedContent source=${source.take(200)}"
            )
        }
        mutatePage(pageKey) { current ->
            val enriched = if (confirmedContent) {
                current.enrichMediaFamily(sourceKey!!, title, poster)
            } else current

            val merged = if (source != null && sourceIsVideo) {
                if (!isLikelyAdvertisementUrl(source)) {
                    enriched.mergeVideo(
                        DetectedVideo(
                            url = source,
                            mimeType = sourceMime,
                            refererUrl = pageUrl,
                            headers = emptyMap<String, String>().withReferer(pageUrl),
                            customName = title.takeIf { confirmedContent },
                            thumbnailUrl = poster.takeIf { confirmedContent },
                            isPageContent = confirmedContent
                        )
                    )
                } else enriched
            } else enriched
            merged.dropPlaylistWrappers().consolidateSameVideo().preferDirectOverCapture()
        }
    }

    fun thumbnailForPage(pageUrl: String?): String = synchronized(pageMetadata) {
        pageUrl?.let { pageMetadata[canonicalPageUrl(it)]?.posterUrl }.orEmpty()
    }

    /**
     * Surface a Tier 3 capturable video: the page drives playback through MediaSource, so its
     * real bytes are being streamed to the loopback capture server rather than fetched by URL.
     */
    fun onMseVideoDetected(captureId: String, mime: String, pageUrl: String?) {
        // YouTube downloads are intentionally unsupported (see onResourceIntercepted).
        // Reddit exposes a signed HLS master on <shreddit-player>; its MSE capture is only a
        // duplicate fallback and usually has no bytes yet when the download sheet is opened.
        if (isDownloadBlockedHost(pageUrl) || isRedditPageUrl(pageUrl)) return
        val pageKey = pageUrl?.let(::canonicalPageUrl) ?: return
        if (pageKey.isBlank() || captureId.isBlank()) return
        val metadata = synchronized(pageMetadata) { pageMetadata[pageKey] }
        val video = DetectedVideo(
            url = "$MSE_CAPTURE_SCHEME$captureId",
            mimeType = "video/mp4",
            refererUrl = pageUrl,
            headers = emptyMap(),
            captureId = captureId,
            customName = metadata?.title,
            thumbnailUrl = metadata?.posterUrl,
            isPageContent = true
        )
        _detectedFlow.tryEmit(video)
        mutatePage(pageKey) { current ->
            val next = if (current.any { it.captureId == captureId }) current else current + video
            next.preferDirectOverCapture()
        }
    }

    private data class FbTracks(
        var videoUrl: String? = null,
        var videoBitrate: Int = -1,
        var audioUrl: String? = null
    )

    // grouping key (video_id / xpv_asset_id) -> best video track + audio track so far
    private val fbTracks = mutableMapOf<String, FbTracks>()
    // keys already surfaced as a single progressive (combined) file — prefer over DASH
    private val progressiveKeys = mutableSetOf<String>()

    /**
     * Handle a Meta CDN (Facebook / Instagram / Threads) track URL. Decodes the base64
     * `efg` param to get the grouping id (video_id, else xpv_asset_id), vencode_tag and
     * bitrate. A "progressive" tag is a combined audio+video file → surfaced directly. A
     * DASH tag is a separate audio/video track → grouped by key and surfaced with
     * [DetectedVideo.audioUrl] so the downloader muxes them.
     */
    private fun handleMetaTrack(url: String, pageUrl: String?, requestHeaders: Map<String, String>) {
        val efg = Regex("[?&]efg=([^&]+)").find(url)?.groupValues?.get(1) ?: return
        val meta = try {
            val decoded = java.net.URLDecoder.decode(efg, "UTF-8")
            val bytes = android.util.Base64.decode(decoded, android.util.Base64.DEFAULT)
            org.json.JSONObject(String(bytes, Charsets.UTF_8))
        } catch (_: Exception) {
            return
        }
        val tag = meta.optString("vencode_tag")
        if (tag.isBlank()) return // not a media URL (e.g. image with efg)

        val videoId = meta.optLong("video_id", 0L)
        val assetId = meta.optLong("xpv_asset_id", 0L)
        val key = when {
            videoId != 0L -> "v$videoId"
            assetId != 0L -> "a$assetId"
            else -> return
        }
        val base = stripByteRange(url)
        val display = "video_${if (videoId != 0L) videoId else assetId}"
        val metadata = synchronized(pageMetadata) {
            pageUrl?.let { pageMetadata[canonicalPageUrl(it)] }
        }
        val headers = requestHeaders.withReferer(pageUrl)

        // Progressive = single file with both audio + video → download directly, no mux.
        if (tag.contains("progressive", ignoreCase = true)) {
            synchronized(fbTracks) {
                progressiveKeys.add(key)
                val video = DetectedVideo(
                    url = base, mimeType = "video/mp4", refererUrl = pageUrl,
                    headers = headers, facebookVideoId = key, customName = display,
                    thumbnailUrl = metadata?.posterUrl, isPageContent = true
                )
                mutatePage(pageUrl?.let(::canonicalPageUrl)) { current ->
                    current.filterNot { it.facebookVideoId == key } + video
                }
            }
            return
        }

        // DASH separate audio/video tracks → collect best video + audio, mux on download.
        val bitrate = meta.optInt("bitrate", 0)
        synchronized(fbTracks) {
            if (key in progressiveKeys) return // already have a combined file for this video
            val tracks = fbTracks.getOrPut(key) { FbTracks() }
            if (tag.contains("audio", ignoreCase = true)) {
                if (tracks.audioUrl == null) tracks.audioUrl = base
            } else if (bitrate > tracks.videoBitrate) {
                tracks.videoBitrate = bitrate
                tracks.videoUrl = base
            }
            val videoUrl = tracks.videoUrl ?: return
            val video = DetectedVideo(
                url = videoUrl, mimeType = "video/mp4", refererUrl = pageUrl,
                headers = headers, audioUrl = tracks.audioUrl,
                facebookVideoId = key, customName = display,
                thumbnailUrl = metadata?.posterUrl, isPageContent = true
            )
            mutatePage(pageUrl?.let(::canonicalPageUrl)) { current ->
                current.filterNot { it.facebookVideoId == key } + video
            }
        }
    }

    private fun stripByteRange(url: String): String =
        url.replace(Regex("&bytestart=\\d+"), "").replace(Regex("&byteend=\\d+"), "")

    /**
     * Reset detection for a page that is (re)loading its main frame. Only this page's bucket
     * is cleared — other tabs' detections are kept, so a page reload or a pop-under ad never
     * wipes videos found elsewhere.
     */
    fun clearForPage(pageUrl: String? = null) {
        val pageKey = pageUrl?.let(::canonicalPageUrl)
        synchronized(lock) {
            currentPageUrl = pageKey
            if (pageKey != null) videosByPage[pageKey] = emptyList()
            _detectedVideos.value = pageKey?.let { videosByPage[it] } ?: emptyList()
        }
        synchronized(pageMetadata) {
            pageKey?.let { pageMetadata[it] = PageMetadata(null, null) }
        }
        synchronized(fbTracks) { fbTracks.clear(); progressiveKeys.clear() }
    }

    /**
     * Switch the surfaced videos to those already detected on [pageUrl] (call on tab switch).
     * Unlike [clearForPage], no bucket is discarded — returning to a tab restores its FAB.
     */
    fun selectPage(pageUrl: String? = null) {
        val pageKey = pageUrl?.let(::canonicalPageUrl)
        val metadata = synchronized(pageMetadata) { pageKey?.let { pageMetadata[it] } }
        synchronized(lock) {
            currentPageUrl = pageKey
            _detectedVideos.value = pageKey?.let {
                videosByPage[it].orEmpty().forPresentation(metadata)
            } ?: emptyList()
        }
    }

    /** Clear all detected videos across every page. */
    fun clearAll() {
        synchronized(lock) {
            currentPageUrl = null
            videosByPage.clear()
            _detectedVideos.value = emptyList()
        }
        synchronized(pageMetadata) { pageMetadata.clear() }
        synchronized(fbTracks) { fbTracks.clear(); progressiveKeys.clear() }
    }

    private val IMAGE_PATTERN = Regex(
        """.*\.(jpg|jpeg|png|gif|webp|svg|ico|bmp)(\?.*)?$""",
        RegexOption.IGNORE_CASE
    )

    /**
     * True when [value]'s host belongs to a platform whose videos we intentionally do not
     * make downloadable (currently YouTube — Play Store policy, and its MSE stream isn't
     * MP4-muxable on-device). Matches the host and any subdomain; fails open on malformed URLs.
     */
    private fun isDownloadBlockedHost(value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        val host = runCatching { java.net.URI(value).host }.getOrNull()?.lowercase() ?: return false
        return BLOCKED_DOWNLOAD_HOSTS.any { host == it || host.endsWith(".$it") }
    }

    private fun isVideoUrl(url: String, mime: String?, isMediaRequest: Boolean = false): Boolean {
        // Exclude image URLs — never treat images as videos
        if (IMAGE_PATTERN.containsMatchIn(url)) return false
        if (mime?.startsWith("image/") == true) return false

        // Reject DASH/MSE ranged segments (e.g. Facebook bytestart/byteend). These are
        // partial, often video-only chunks that aren't playable on their own — the full
        // progressive URL is recovered separately via page scraping (addExtractedVideo).
        if (url.contains("bytestart=", ignoreCase = true) ||
            url.contains("byteend=", ignoreCase = true)) return false

        // HLS transport-stream segments are fetched by the playlist engine, never surfaced as
        // their own item. Filter any URL whose final extension is .ts — even when the file name
        // embeds ".mp4" (e.g. tuoitre's "name.mp4.720.0.ts").
        if (finalExtensionOf(url) == "ts") return false

        // HLS playlists are downloadable: the download engine fetches the playlist,
        // pulls every segment and merges them into a single file.
        val resourceKind = classifyMediaResource(url, mime)
        if (resourceKind == MediaResourceKind.HLS_MANIFEST) return true

        // MPEG-DASH manifests are downloadable: parse the .mpd, pick tracks, mux them.
        if (resourceKind == MediaResourceKind.DASH_MANIFEST) return true

        val urlMatches = VIDEO_PATTERN.containsMatchIn(url)
        val mimeMatches = mime?.startsWith("video/") == true
        // isMediaRequest covers extension-less progressive video URLs flagged by the
        // browser engine (Sec-Fetch-Dest: video).
        return urlMatches || mimeMatches || isMediaRequest
    }

    private fun guessMimeType(url: String, headers: Map<String, String>): String? {
        // If Content-Type header is available, use it directly
        headers["Content-Type"]?.let {
            val ct = it.substringBefore(";").trim()
            // Trust the header, but skip if it's an image
            if (ct.startsWith("image/")) return null
            return ct
        }

        // Check path part only (before query params)
        val path = try { java.net.URI(url).path ?: url } catch (_: Exception) { url }

        // Exclude image extensions explicitly
        if (IMAGE_PATTERN.containsMatchIn(path)) return null

        // The final extension is the source of truth: a resource named "name.mp4.json" is JSON
        // metadata, and "name.mp4.mobile.m3u8" is an HLS playlist — not a plain MP4.
        val finalExt = finalExtensionOf(url)
        if (finalExt in NON_MEDIA_EXTENSIONS) return null
        return when (finalExt) {
            "m3u8" -> "application/vnd.apple.mpegurl"
            "mpd" -> "application/dash+xml"
            "ts" -> "video/MP2T"
            "mp4", "m4v" -> "video/mp4"
            "webm" -> "video/webm"
            "mov" -> "video/quicktime"
            "mkv" -> "video/x-matroska"
            else -> when {
                // Manifest markers can sit mid-path (e.g. ".m3u8/stream-plain?t=…").
                path.contains(".m3u8", ignoreCase = true) -> "application/vnd.apple.mpegurl"
                path.contains(".mpd", ignoreCase = true) -> "application/dash+xml"
                path.contains(".mp4", ignoreCase = true) -> "video/mp4"
                path.contains(".webm", ignoreCase = true) -> "video/webm"
                path.contains(".mov", ignoreCase = true) -> "video/quicktime"
                // Extension-less progressive MP4 declared via a query param, e.g. TikTok's
                // /video/tos/.../?...&mime_type=video_mp4 (a combined audio+video file).
                url.contains("mime_type=video_mp4", ignoreCase = true) ||
                    url.contains("mime_type=video%2Fmp4", ignoreCase = true) -> "video/mp4"
                // Facebook CDN: only match if path contains "video" (not just fbcdn.net/v/)
                url.contains("video", ignoreCase = true) && url.contains("fbcdn", ignoreCase = true) -> "video/mp4"
                else -> null
            }
        }
    }

    /** The lowercased extension after the last dot of the last path segment (query/hash stripped). */
    private fun finalExtensionOf(url: String): String =
        mediaPathExtension(url)

    companion object {
        /** Synthetic URL scheme marking a [DetectedVideo] whose bytes come from MSE capture. */
        const val MSE_CAPTURE_SCHEME = "mse-capture://"
        private const val MAX_TRACKED_PAGES = 16

        /**
         * Final extensions that mark a resource as non-media (metadata/documents), so a URL like
         * "video.mp4.json" is rejected even though an earlier segment contains ".mp4".
         */
        private val NON_MEDIA_EXTENSIONS =
            setOf("json", "html", "htm", "php", "txt", "js", "css", "xml")

        /**
         * Hosts (and their subdomains) whose videos are intentionally not downloadable.
         * YouTube: Play Store policy forbids it, and its MSE/DASH VP9/AV1+Opus streams can't
         * be muxed to MP4 on-device — so a detected "download" only ever fails. `googlevideo`
         * / `ytimg` cover its media/CDN hosts when YouTube is embedded in another page.
         */
        private val BLOCKED_DOWNLOAD_HOSTS = setOf(
            "youtube.com",
            "youtu.be",
            "youtube-nocookie.com",
            "googlevideo.com",
            "ytimg.com"
        )
        private val VIDEO_PATTERN = Regex(
            ".+\\.(mp4|mov|webm|avi|mkv|flv)(\\?.*)?$",
            RegexOption.IGNORE_CASE
        )
    }
}

/**
 * When a page exposes a directly-downloadable video (a real HLS/MP4 URL) as well as an MSE
 * capture of the same playback, drop the capture entries — the direct URL is faster and doesn't
 * require playing the video through. Pure blob/MSE pages (no direct URL) keep their capture.
 */
internal fun List<DetectedVideo>.preferDirectOverCapture(): List<DetectedVideo> {
    if (none { it.isCapture } || none { !it.isCapture }) return this
    // A random MP4 advertisement must not evict the real MSE capture before the DOM has had
    // time to classify that advertisement. Only a confirmed page-content URL may win.
    if (none { !it.isCapture && it.isPageContent }) return this
    return filterNot { it.isCapture }
}

/**
 * Drop "wrapper" playlist entries. Some sites expose both a bare `X.m3u8` (which actually
 * returns an HTML player page) and the real playlist at `X.m3u8/stream-plain?t=…`. Keeping
 * both surfaces a decoy that always fails to download, so drop any entry whose URL (ignoring
 * the query string) is a strict path-prefix of another detected entry's URL.
 */
internal fun List<DetectedVideo>.dropPlaylistWrappers(): List<DetectedVideo> {
    if (size < 2) return this
    val bases = map { it.url.substringBefore('?') }
    return filterIndexed { index, _ ->
        val base = bases[index]
        bases.none { other -> other != base && other.startsWith("$base/") }
    }
}

/**
 * Collapse the many URLs a site may expose for one logical video into a single item. CDNs often
 * serve the same clip as a progressive file, an HLS master, and several per-quality HLS variants
 * whose names all embed the original media file (e.g. tuoitre's `name.mp4`, `name.mp4.mobile.m3u8`,
 * `name.mp4.index.480.m3u8`). Group by that embedded media name and keep the best single
 * representation: a progressive file, else the HLS master, else any variant.
 *
 * Entries with no recognizable embedded media name are keyed by their own URL, so genuinely
 * distinct videos (different names/ids) are never merged.
 */
internal fun List<DetectedVideo>.consolidateSameVideo(): List<DetectedVideo> {
    if (size < 2) return this
    val groups = LinkedHashMap<String, MutableList<DetectedVideo>>()
    for (video in this) groups.getOrPut(video.mediaIdentityKey()) { mutableListOf() }.add(video)
    return groups.values.map { group ->
        if (group.size == 1) group[0] else group.consolidatedRepresentative()
    }
}

/**
 * Stable identity for one logical video. It deliberately ignores the delivery container and
 * rendition suffix, so `clip.m3u8`, `clip_720p.m3u8` and `clip.mp4` form one family. The host is
 * retained to prevent two unrelated CDNs with the same filename from being merged.
 */
internal fun DetectedVideo.mediaIdentityKey(): String = mediaIdentityKey(
    url = url,
    mimeType = mimeType,
    facebookVideoId = facebookVideoId,
    captureId = captureId
)

internal fun mediaIdentityKey(
    url: String,
    mimeType: String?,
    facebookVideoId: String? = null,
    captureId: String? = null
): String {
    if (!facebookVideoId.isNullOrBlank()) return "facebook:$facebookVideoId"
    if (!captureId.isNullOrBlank() || url.startsWith(VideoSniffer.MSE_CAPTURE_SCHEME)) return url
    redditMediaResource(url)?.let { return "reddit:${it.assetId}" }

    val uri = runCatching { java.net.URI(url) }.getOrNull()
    val cleanPath = (uri?.path ?: url.substringBefore('?').substringBefore('#')).lowercase()
    val segment = cleanPath.substringAfterLast('/').trim()
    if (segment.isBlank() || MEDIA_CONTAINER_EXTENSIONS.none { segment.contains(it) }) {
        // Extension-less media endpoints can encode the actual video id in the query. Keep the
        // full URL in that case rather than risk combining two different clips.
        return canonicalMediaUrl(url)
    }

    var stem = segment
    do {
        val before = stem
        MEDIA_CONTAINER_EXTENSIONS.forEach { extension ->
            if (stem.endsWith(extension)) stem = stem.dropLast(extension.length)
        }
        RENDITION_IDENTITY_SUFFIXES.forEach { pattern -> stem = stem.replace(pattern, "") }
    } while (stem != before)

    if (stem.isBlank()) return canonicalMediaUrl(url)
    val host = uri?.host?.lowercase().orEmpty()
    val parentScope = cleanPath.substringBeforeLast('/', "")
        .split('/')
        .filter { it.isNotBlank() && !isDeliveryDirectorySegment(it) }
        .joinToString("/")
    val identityStem = if (isGenericPlaylistStem(stem, mimeType)) "playlist" else stem
    return "media:$host|$parentScope|$identityStem"
}

/** Prefer a progressive file, then a master manifest, then a fixed-quality rendition. */
private fun DetectedVideo.representationRank(): Int = when {
    isCapture -> 100
    isRedditMasterManifest(url) -> 600
    isRedditComponentTrack(url) -> 150
    !isHls && !isDash -> 400
    isGenericHlsMasterManifest(url, mimeType) -> 360
    hasQualityRenditionSuffix() -> 200
    isHls -> 320
    else -> 300
}

private fun isGenericHlsMasterManifest(url: String, mimeType: String?): Boolean {
    if (classifyMediaResource(url, mimeType) != MediaResourceKind.HLS_MANIFEST) return false
    val stem = url.substringBefore('?').substringBefore('#').substringAfterLast('/')
        .removeSuffix(".m3u8")
    return GENERIC_HLS_MASTER_STEMS.matches(stem)
}

private fun isGenericPlaylistStem(stem: String, mimeType: String?): Boolean =
    classifyMediaResource("https://local.invalid/$stem.m3u8", mimeType) ==
        MediaResourceKind.HLS_MANIFEST && GENERIC_PLAYLIST_STEMS.matches(stem)

private fun isDeliveryDirectorySegment(segment: String): Boolean {
    val value = segment.lowercase()
    if (value in DELIVERY_DIRECTORY_SEGMENTS) return true
    if (value == "vne") return true
    val qualities = value.trim(',').split(',').filter { it.isNotBlank() }
    return qualities.isNotEmpty() && qualities.all { QUALITY_DIRECTORY_SEGMENT.matches(it) }
}

private fun DetectedVideo.hasQualityRenditionSuffix(): Boolean {
    val segment = url.substringBefore('?').substringBefore('#').substringAfterLast('/')
    val withoutContainer = MEDIA_CONTAINER_EXTENSIONS.fold(segment.lowercase()) { value, ext ->
        if (value.endsWith(ext)) value.dropLast(ext.length) else value
    }
    return QUALITY_RENDITION_SUFFIXES.any { it.containsMatchIn(withoutContainer) }
}

private fun List<DetectedVideo>.consolidatedRepresentative(): DetectedVideo {
    val preferred = maxByOrNull { it.representationRank() }!!
    val contentEvidence = firstOrNull { it.isPageContent }
    return preferred.copy(
        mimeType = preferred.mimeType ?: firstNotNullOfOrNull { it.mimeType },
        refererUrl = preferred.refererUrl ?: firstNotNullOfOrNull { it.refererUrl },
        headers = fold(emptyMap<String, String>()) { headers, video -> headers + video.headers },
        audioUrl = preferred.audioUrl ?: firstNotNullOfOrNull { it.audioUrl },
        facebookVideoId = preferred.facebookVideoId ?: firstNotNullOfOrNull { it.facebookVideoId },
        customName = preferred.customName ?: contentEvidence?.customName
            ?: firstNotNullOfOrNull { it.customName },
        thumbnailUrl = preferred.thumbnailUrl ?: contentEvidence?.thumbnailUrl
            ?: firstNotNullOfOrNull { it.thumbnailUrl },
        isPageContent = any { it.isPageContent }
    )
}

private val MEDIA_CONTAINER_EXTENSIONS = listOf(
    ".m3u8", ".mpd", ".mp4", ".m4v", ".webm", ".mov", ".mkv", ".flv", ".avi"
)
private val RENDITION_IDENTITY_SUFFIXES = listOf(
    Regex("""(?i)(?:[._-]index)?[._-]\d{3,4}p?$"""),
    Regex("""(?i)[._-]\d{2,4}x\d{2,4}$"""),
    Regex("""(?i)[._-](?:mobile|master|playlist|manifest|adaptive|chunklist|hls|dash|fhd|uhd|hd|sd|high|medium|low)$""")
)
private val QUALITY_RENDITION_SUFFIXES = listOf(
    Regex("""(?i)(?:[._-]index)?[._-]\d{3,4}p?$"""),
    Regex("""(?i)[._-]\d{2,4}x\d{2,4}$""")
)
private val DELIVERY_DIRECTORY_SEGMENTS = setOf(".hls", "hls", ".dash", "dash")
private val QUALITY_DIRECTORY_SEGMENT = Regex("""(?i)\d{3,4}p""")
private val GENERIC_HLS_MASTER_STEMS = Regex("""(?i)(?:master|playlist|manifest)""")
private val GENERIC_PLAYLIST_STEMS =
    Regex("""(?i)(?:master|playlist|manifest|index(?:[._-]v\d+)?(?:[._-]a\d+)?)""")

internal fun List<DetectedVideo>.mergeVideo(incoming: DetectedVideo): List<DetectedVideo> {
    val index = indexOfFirst { it.url == incoming.url }
    if (index < 0) return this + incoming
    val existing = this[index]
    val merged = existing.copy(
        mimeType = existing.mimeType ?: incoming.mimeType,
        refererUrl = existing.refererUrl ?: incoming.refererUrl,
        headers = existing.headers + incoming.headers,
        audioUrl = existing.audioUrl ?: incoming.audioUrl,
        facebookVideoId = existing.facebookVideoId ?: incoming.facebookVideoId,
        customName = existing.customName ?: incoming.customName,
        thumbnailUrl = existing.thumbnailUrl ?: incoming.thumbnailUrl,
        isPageContent = existing.isPageContent || incoming.isPageContent
    )
    return toMutableList().apply { this[index] = merged }
}

internal fun List<DetectedVideo>.enrichMediaFamily(
    mediaKey: String,
    title: String?,
    posterUrl: String?
): List<DetectedVideo> = map { video ->
    if (video.mediaIdentityKey() == mediaKey) {
        video.copy(
            customName = video.customName ?: title,
            thumbnailUrl = video.thumbnailUrl ?: posterUrl,
            isPageContent = true
        )
    } else video
}

internal fun List<DetectedVideo>.removeMediaFamily(
    canonicalUrl: String,
    mediaKey: String?
): List<DetectedVideo> = filterNot { video ->
    canonicalMediaUrl(video.url) == canonicalUrl ||
        (mediaKey != null && video.mediaIdentityKey() == mediaKey)
}

private fun Map<String, String>.withReferer(pageUrl: String?): Map<String, String> = buildMap {
    putAll(this@withReferer)
    if (!pageUrl.isNullOrBlank() && keys.none { it.equals("Referer", ignoreCase = true) }) {
        put("Referer", pageUrl)
    }
}

private fun isHttpUrl(value: String): Boolean =
    value.startsWith("https://", ignoreCase = true) || value.startsWith("http://", ignoreCase = true)

internal fun canonicalMediaUrl(value: String): String = value.substringBefore('#')

/** Only hosts dedicated to ad delivery are blocked natively; page-specific creatives are
 * classified through their DOM video node to avoid false positives on normal content URLs. */
internal fun isLikelyAdvertisementUrl(value: String): Boolean {
    val host = runCatching { java.net.URI(value).host }.getOrNull()?.lowercase() ?: return false
    return ADVERTISEMENT_HOSTS.any { host == it || host.endsWith(".$it") }
}

private val ADVERTISEMENT_HOSTS = setOf(
    "doubleclick.net",
    "googlesyndication.com",
    "googleadservices.com",
    "imasdk.googleapis.com",
    // VNExpress banner/video creatives observed on-device. These are dedicated ad/log hosts,
    // not the article video CDN (which is served from vnecdn.net).
    "ds.eclick.vn",
    "logging.admicro.vn"
)

private fun canonicalPageUrl(value: String): String = value.substringBefore('#')

private fun safePageTitle(value: String): String = value
    .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()
    .take(96)
