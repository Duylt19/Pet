package com.asianmobile.privatebrower.ui.home.browsertab

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.asianmobile.privatebrower.R

/**
 * Popular site data for Home tab grid - Figma node 11280:2260.
 */
data class PopularSite(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val url: String
)

object PopularSites {
    val DEFAULTS = listOf(
        PopularSite(
            R.string.popular_site_google,
            R.drawable.ic_popular_site_google,
            "https://www.google.com"
        ),
        PopularSite(
            R.string.popular_site_gemini,
            R.drawable.ic_popular_site_gemini,
            "https://gemini.google.com"
        ),
        PopularSite(
            R.string.popular_site_chatgpt,
            R.drawable.ic_popular_site_chatgpt,
            "https://chatgpt.com"
        ),
        PopularSite(
            R.string.popular_site_telegram,
            R.drawable.ic_popular_site_telegram,
            "https://web.telegram.org"
        ),
        PopularSite(
            R.string.popular_site_x,
            R.drawable.ic_popular_site_x,
            "https://x.com"
        ),
        PopularSite(
            R.string.popular_site_whatsapp,
            R.drawable.ic_popular_site_whatsapp,
            "https://web.whatsapp.com"
        ),
        PopularSite(
            R.string.popular_site_reddit,
            R.drawable.ic_popular_site_reddit,
            "https://www.reddit.com"
        ),
        PopularSite(
            R.string.popular_site_pinterest,
            R.drawable.ic_popular_site_pinterest,
            "https://www.pinterest.com"
        )
    )
}
