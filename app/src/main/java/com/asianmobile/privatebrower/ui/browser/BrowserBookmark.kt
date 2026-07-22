package com.asianmobile.privatebrower.ui.browser

import java.net.URI

internal fun isBookmarkableUrl(url: String): Boolean {
    val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return false
    return uri.scheme?.lowercase() in setOf("http", "https") && !uri.host.isNullOrBlank()
}

internal fun faviconUrlFor(url: String): String? {
    val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase()?.takeIf { it == "http" || it == "https" } ?: return null
    val host = uri.host ?: return null
    val port = if (uri.port >= 0) ":${uri.port}" else ""
    return "$scheme://$host$port/favicon.ico"
}
