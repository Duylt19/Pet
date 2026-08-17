package com.asianmobile.emojibattery.shimeji.ads.ui.openads

internal enum class AppOpenPresentationStage {
    IDLE,
    WELCOME_BACK_COVER,
    FULLSCREEN_AD
}

/** Keeps the branded pre-ad cover separate from the actual fullscreen ad lifecycle. */
internal object AppOpenOverlayPolicy {
    fun shouldHideAppContent(stage: AppOpenPresentationStage): Boolean =
        stage == AppOpenPresentationStage.FULLSCREEN_AD
}
