package com.asianmobile.emojibattery.shimeji.ads.ui.openads

internal enum class AppOpenPresentationStage {
    IDLE,
    WELCOME_BACK_COVER,
    FULLSCREEN_AD
}

internal data class AppOpenOverlayDirective(
    val isFullscreenAdShowing: Boolean,
    val hideActivityContent: Boolean
)

/** Keeps the branded cover, SDK overlay and host Activity rendering as separate concerns. */
internal object AppOpenOverlayPolicy {
    fun directive(stage: AppOpenPresentationStage): AppOpenOverlayDirective =
        when (stage) {
            AppOpenPresentationStage.IDLE,
            AppOpenPresentationStage.WELCOME_BACK_COVER -> AppOpenOverlayDirective(
                isFullscreenAdShowing = false,
                hideActivityContent = false
            )

            AppOpenPresentationStage.FULLSCREEN_AD -> AppOpenOverlayDirective(
                isFullscreenAdShowing = true,
                hideActivityContent = false
            )
        }
}
