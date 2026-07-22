package com.asianmobile.privatebrower.data.browser

import java.net.URI

internal data class RedditMediaResource(
    val assetId: String,
    val fileName: String,
)

/** Reddit serves every rendition of one post below the same first path segment. */
internal fun redditMediaResource(url: String): RedditMediaResource? {
    val uri = runCatching { URI(url) }.getOrNull() ?: return null
    val host = uri.host?.lowercase().orEmpty()
    if (host != "v.redd.it" && host != "packaged-media.redd.it") return null

    val segments = uri.path.orEmpty().split('/').filter { it.isNotBlank() }
    val assetId = segments.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
    val fileName = segments.lastOrNull()?.lowercase().orEmpty()
    return RedditMediaResource(assetId = assetId, fileName = fileName)
}

internal fun isRedditPageUrl(url: String?): Boolean {
    val host = runCatching { URI(url.orEmpty()).host?.lowercase() }.getOrNull().orEmpty()
    return host == "reddit.com" || host.endsWith(".reddit.com")
}

internal fun isRedditAudioTrack(url: String): Boolean {
    val fileName = redditMediaResource(url)?.fileName ?: return false
    return fileName.startsWith("cmaf_audio_") || fileName.startsWith("dash_audio_")
}

internal fun isRedditMasterManifest(url: String): Boolean {
    val fileName = redditMediaResource(url)?.fileName ?: return false
    return fileName == "hlsplaylist.m3u8" || fileName == "dashplaylist.mpd"
}

internal fun isRedditComponentTrack(url: String): Boolean {
    val fileName = redditMediaResource(url)?.fileName ?: return false
    return fileName.startsWith("cmaf_") ||
        (fileName.startsWith("dash_") && fileName != "dashplaylist.mpd")
}
