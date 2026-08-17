package com.asianmobile.emojibattery.shimeji.ads.tracking

enum class AdFormat(val value: String) {
    NATIVE("native"),
    NATIVE_ITEM("native_item"),
    NATIVE_FULL("native_full"),
    BANNER("banner"),
    INTERSTITIAL("interstitial"),
    APP_OPEN("app_open"),
    REWARDED("rewarded"),
    UNKNOWN("unknown")
}

object AdPlacement {
    const val UNKNOWN = "unknown"
    const val NAVIGATION = "navigation"
    const val APP_OPEN = "app_open"
    const val REWARDED_DEFAULT = "rewarded_default"
}
