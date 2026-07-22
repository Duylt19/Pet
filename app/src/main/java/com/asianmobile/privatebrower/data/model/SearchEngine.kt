package com.asianmobile.privatebrower.data.model

import androidx.annotation.DrawableRes
import com.asianmobile.privatebrower.R

enum class SearchEngine(
    val id: String,
    val displayName: String,
    @param:DrawableRes val iconRes: Int,
    val queryUrlTemplate: String,
    val homeUrl: String
) {
    GOOGLE("google", "Google", R.drawable.ic_google_g, "https://www.google.com/search?q=%s", "https://www.google.com"),
    BING("bing", "Bing", R.drawable.ic_bing_b, "https://www.bing.com/search?q=%s", "https://www.bing.com"),
    YAHOO("yahoo", "Yahoo", R.drawable.ic_yahoo_y, "https://search.yahoo.com/search?p=%s", "https://www.yahoo.com"),
    DUCKDUCKGO("duckduckgo", "DuckDuckGo", R.drawable.ic_duckduckgo, "https://duckduckgo.com/?q=%s", "https://duckduckgo.com"),
    YANDEX("yandex", "Yandex", R.drawable.ic_yandex, "https://yandex.com/search/?text=%s", "https://yandex.com"),
    COC_COC("coccoc", "Coc Coc", R.drawable.ic_coccoc, "https://coccoc.com/search?query=%s", "https://coccoc.com");

    companion object {
        fun fromId(id: String): SearchEngine = values().firstOrNull { it.id == id } ?: GOOGLE
    }
}
