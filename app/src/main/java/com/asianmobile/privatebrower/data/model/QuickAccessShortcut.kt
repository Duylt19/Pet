package com.asianmobile.privatebrower.data.model

import com.asianmobile.privatebrower.R

data class QuickAccessShortcut(
    val id: String,
    val labelRes: Int,
    val iconRes: Int,
    val url: String,
    val brandColorHex: String
)

object QuickAccessShortcuts {
    val DEFAULTS = listOf(
        QuickAccessShortcut("fb", R.string.quick_access_fb_label, R.drawable.ic_shortcut_facebook, "https://m.facebook.com/watch/", "#1877F2"),
        QuickAccessShortcut("ins", R.string.quick_access_ins_label, R.drawable.ic_shortcut_instagram, "https://www.instagram.com/explore/", ""), // Instagram uses gradient
        QuickAccessShortcut("tic", R.string.quick_access_tic_label, R.drawable.ic_shortcut_tiktok, "https://www.tiktok.com/foryou", "#000000"),
        QuickAccessShortcut("whats", R.string.quick_access_whats_label, R.drawable.ic_shortcut_whatsapp, "https://web.whatsapp.com", "#25D366"),
        QuickAccessShortcut("tw", R.string.quick_access_tw_label, R.drawable.ic_shortcut_x, "https://x.com", "#000000"),
        QuickAccessShortcut("vieo", R.string.quick_access_vieo_label, R.drawable.ic_shortcut_vimeo, "https://vimeo.com", "#1AB7EA"),
        QuickAccessShortcut("thre", R.string.quick_access_thre_label, R.drawable.ic_shortcut_threads, "https://www.threads.net", "#000000"),
        QuickAccessShortcut("daimo", R.string.quick_access_daimo_label, R.drawable.ic_shortcut_dailymotion, "https://www.dailymotion.com", "#0066DC")
    )
}
