package com.asianmobile.privatebrower.data.util

import android.net.Uri
import com.asianmobile.privatebrower.data.model.SearchEngine

object UrlBuilder {
    private val URL_REGEX = Regex(
        "^(https?://)?([\\w-]+\\.)+[\\w-]+(/[\\w- ./?%&=#]*)?\$",
        RegexOption.IGNORE_CASE
    )

    fun buildUrl(input: String, engine: SearchEngine): String {
        val trimmed = input.trim()
        return when {
            trimmed.isBlank() -> engine.homeUrl
            isLikelyUrl(trimmed) -> normalizeUrl(trimmed)
            else -> engine.queryUrlTemplate.format(Uri.encode(trimmed))
        }
    }

    private fun isLikelyUrl(input: String): Boolean {
        if (input.contains(" ")) return false
        return URL_REGEX.matches(input) ||
            input.startsWith("http://") ||
            input.startsWith("https://") ||
            input.startsWith("about:") ||
            input.startsWith("javascript:") ||
            (input.contains(".") && !input.endsWith("."))
    }

    private fun normalizeUrl(input: String): String =
        if (input.startsWith("http://") || input.startsWith("https://")) input
        else "https://$input"
}
