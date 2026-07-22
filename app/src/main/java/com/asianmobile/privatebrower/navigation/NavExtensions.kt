package com.asianmobile.privatebrower.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import com.asianmobile.privatebrower.ads.ui.interstitial.InterstitialUtil
import com.asianmobile.privatebrower.ads.tracking.AdPlacement

private var lastNavigateTime = 0L
private const val NAVIGATION_DEBOUNCE_TIME = 500L

/**
 * Extension function for [NavController] to show an Interstitial Ad before navigating to a destination.
 */
fun navigateWithAd(
    context: Context,
    placement: String = AdPlacement.NAVIGATION,
    onNavigate: () -> Unit
) {
    val currentTime = System.currentTimeMillis()
    if (currentTime - lastNavigateTime < NAVIGATION_DEBOUNCE_TIME) {
        return // Prevent rapid double navigating
    }
    lastNavigateTime = currentTime

    context.findActivity()?.let { activity ->
        val interstitial = InterstitialUtil.getInstance()
        interstitial.openAd?.needShowOpenAds = false
        interstitial.showInterstitialAd(activity, placement) {
            onNavigate()
        }
    } ?: run {
        onNavigate()
    }
}

/**
 * Extension function for [NavController] to safely navigate with debounce and runCatching.
 */
fun NavController.safeNavigate(
    route: String,
    ignoreDebounce: Boolean = false,
    builder: NavOptionsBuilder.() -> Unit = {}
) {
    if (!ignoreDebounce) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastNavigateTime < NAVIGATION_DEBOUNCE_TIME) {
            return
        }
        lastNavigateTime = currentTime
    }

    runCatching {
        this.navigate(route, builder)
    }
}

/**
 * Extension function to find the current [Activity] from a [Context].
 */
private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

/**
 * Extension function for [NavController] to safely pop backstack only if there's a previous destination
 */
fun NavController.safePopBackStack(ignoreDebounce: Boolean = false) {
    if (!ignoreDebounce) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastNavigateTime < NAVIGATION_DEBOUNCE_TIME) {
            return
        }
        lastNavigateTime = currentTime
    }

    if (this.previousBackStackEntry != null) {
        runCatching {
            this.popBackStack()
        }
    }
}

/**
 * Extension function for [NavController] to safely pop backstack to a specific route
 */
fun NavController.safePopBackStack(
    route: String,
    inclusive: Boolean,
    ignoreDebounce: Boolean = false
) {
    if (!ignoreDebounce) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastNavigateTime < NAVIGATION_DEBOUNCE_TIME) {
            return
        }
        lastNavigateTime = currentTime
    }

    runCatching {
        this.popBackStack(route, inclusive)
    }
}
