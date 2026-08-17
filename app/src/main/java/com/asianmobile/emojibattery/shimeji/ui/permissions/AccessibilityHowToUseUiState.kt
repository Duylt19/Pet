package com.asianmobile.emojibattery.shimeji.ui.permissions

enum class AccessibilityHowToStep {
    OPEN_INSTALLED_APPS,
    SELECT_EMOJI_BATTERY,
    ENABLE_SERVICE,
    CONFIRM_ACCESS
}

data class AccessibilityHowToUseUiState(
    val steps: List<AccessibilityHowToStep> = AccessibilityHowToStep.entries
)
