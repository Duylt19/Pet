package com.asianmobile.emojibattery.shimeji.ui.app

import com.asianmobile.emojibattery.shimeji.navigation.Routes

/**
 * Keeps the first-run Permission screen available in source while it is temporarily excluded
 * from the active onboarding flow. Set this back to true when product wants the step restored.
 */
internal const val IS_FIRST_PERMISSION_ONBOARDING_ENABLED = false

internal fun destinationAfterIntro(): String =
    if (IS_FIRST_PERMISSION_ONBOARDING_ENABLED) Routes.PERMISSION else Routes.HOME_GRAPH
