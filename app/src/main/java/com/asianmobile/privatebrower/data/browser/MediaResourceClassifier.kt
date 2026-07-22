package com.asianmobile.privatebrower.data.browser

import java.net.URI

/**
 * Classifies a media resource from its URL path and MIME type.
 *
 * Query parameters are deliberately excluded. Some CDNs put a manifest name in a query
 * parameter even when the requested resource is a direct media file. For example, Reddit uses
 * `video.mp4?m=DASHPlaylist.mpd`; treating the whole URL as a manifest sends MP4 bytes to the XML
 * parser and makes the download fail.
 */
internal enum class MediaResourceKind {
    DIRECT,
    HLS_MANIFEST,
    DASH_MANIFEST,
    UNKNOWN,
}

internal fun classifyMediaResource(url: String, mimeType: String?): MediaResourceKind {
    val path = mediaUrlPath(url).lowercase()
    val extension = mediaPathExtension(path)

    // A concrete file extension is stronger evidence than a stale or incorrectly inferred MIME.
    // This also lets failed downloads created by older builds retry through the direct-file path.
    when (extension) {
        in DIRECT_MEDIA_EXTENSIONS -> return MediaResourceKind.DIRECT
        "m3u8" -> return MediaResourceKind.HLS_MANIFEST
        "mpd" -> return MediaResourceKind.DASH_MANIFEST
    }

    // Some playlist endpoints append a path suffix after the manifest filename, for example
    // `playlist.m3u8/stream-plain`. Match only path boundaries, never query parameters.
    if (path.contains(".m3u8/")) return MediaResourceKind.HLS_MANIFEST
    if (path.contains(".mpd/")) return MediaResourceKind.DASH_MANIFEST

    val normalizedMime = mimeType?.substringBefore(';')?.trim()?.lowercase()
    return when {
        normalizedMime in HLS_MIME_TYPES -> MediaResourceKind.HLS_MANIFEST
        normalizedMime == "application/dash+xml" -> MediaResourceKind.DASH_MANIFEST
        normalizedMime?.startsWith("video/") == true -> MediaResourceKind.DIRECT
        else -> MediaResourceKind.UNKNOWN
    }
}

internal fun mediaPathExtension(urlOrPath: String): String {
    val path = if ('?' in urlOrPath || '#' in urlOrPath || "://" in urlOrPath) {
        mediaUrlPath(urlOrPath)
    } else {
        urlOrPath
    }
    return path.substringAfterLast('/').substringAfterLast('.', "").lowercase()
}

private fun mediaUrlPath(url: String): String =
    runCatching { URI(url).path }
        .getOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: url.substringBefore('?').substringBefore('#')

private val DIRECT_MEDIA_EXTENSIONS = setOf(
    "mp4", "m4v", "webm", "mov", "mkv", "flv", "avi", "ts",
)

private val HLS_MIME_TYPES = setOf(
    "application/vnd.apple.mpegurl",
    "application/x-mpegurl",
)
