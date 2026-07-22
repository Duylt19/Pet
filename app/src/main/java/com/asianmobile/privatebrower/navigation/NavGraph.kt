package com.asianmobile.privatebrower.navigation

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.asianmobile.privatebrower.ads.utils.SafeRemoteConfig
import com.asianmobile.privatebrower.ui.home.HomeScreen
import com.asianmobile.privatebrower.ui.home.settings.SettingsScreen
import com.asianmobile.privatebrower.ui.intro.IntroScreen
import com.asianmobile.privatebrower.ui.language.LanguageScreen
import com.asianmobile.privatebrower.ui.main.MainViewModel
import com.asianmobile.privatebrower.ui.permission.PermissionScreen
import com.asianmobile.privatebrower.ui.premium.PremiumScreen
import com.asianmobile.privatebrower.ui.premium.StartPremiumIndexes
import com.asianmobile.privatebrower.ui.splash.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val LANGUAGE = "language"
    const val LANGUAGE_SETTINGS = "language_settings"
    const val INTRO = "intro"
    const val PERMISSION = "permission"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val PREMIUM = "premium"

    // Legacy action keys emitted by the current Home UI. They intentionally have no
    // destination in the base navigation graph and can be replaced when Home is redesigned.
    const val BROWSER_WEBVIEW = "browser_webview"
    const val TAB_SELECTION = "tab_selection"
    const val BOOKMARKS_HISTORY = "bookmarks_history"
    const val MEDIA_PHOTOS = "media_photos"
    const val MEDIA_VIDEOS = "media_videos"
    const val MEDIA_AUDIO = "media_audio"
    const val MEDIA_DOCUMENTS = "media_documents"
}

@Composable
fun AppNavGraph(
    startDestination: String,
    nextScreenAfterSplash: String,
    viewModel: MainViewModel,
    onExitApp: () -> Unit,
    onDestinationChanged: (String?) -> Unit = {}
) {
    val context = LocalContext.current
    val navController = rememberNavController()

    fun navigateFromHome(route: String) {
        when (route) {
            Routes.SETTINGS -> navigateWithAd(context, route) {
                navController.safeNavigate(Routes.SETTINGS, ignoreDebounce = true)
            }

            Routes.LANGUAGE_SETTINGS -> navigateWithAd(context, route) {
                navController.safeNavigate(Routes.LANGUAGE_SETTINGS, ignoreDebounce = true)
            }

            Routes.PREMIUM -> navController.safeNavigate(
                "${Routes.PREMIUM}/${StartPremiumIndexes.IN_APP.name}",
                ignoreDebounce = true
            )
        }
    }

    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            onDestinationChanged(destination.route)
        }
    }

    val activity = context as? com.asianmobile.privatebrower.MainActivity
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val pendingHomeTab = activity?.pendingHomeTab

    LaunchedEffect(pendingHomeTab, currentRoute) {
        val startupRoutes = setOf(
            Routes.SPLASH,
            Routes.LANGUAGE,
            Routes.INTRO,
            Routes.PERMISSION
        )
        if (
            pendingHomeTab != null &&
            currentRoute != null &&
            currentRoute != Routes.HOME &&
            currentRoute !in startupRoutes &&
            !currentRoute.startsWith(Routes.PREMIUM)
        ) {
            navController.safeNavigate(Routes.HOME, ignoreDebounce = true) {
                popUpTo(Routes.HOME) { inclusive = false }
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(
                    viewModel = viewModel,
                    onNextScreen = {
                        navController.safeNavigate(nextScreenAfterSplash) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.LANGUAGE) {
                LanguageScreen(
                    onConfirm = {
                        viewModel.completeLanguage()
                        navigateWithAd(context, Routes.INTRO) {
                            navController.safeNavigate(Routes.INTRO, ignoreDebounce = true) {
                                popUpTo(Routes.LANGUAGE) { inclusive = true }
                            }
                            (context as? Activity)?.recreate()
                        }
                    }
                )
            }

            composable(Routes.LANGUAGE_SETTINGS) {
                LanguageScreen(
                    isSettings = true,
                    onConfirm = {
                        val intent = context.packageManager
                            .getLaunchIntentForPackage(context.packageName)
                            ?.apply {
                                putExtra("skip_splash", true)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            }
                        intent?.let(context::startActivity)
                        (context as? Activity)?.finish()
                    },
                    onBack = {
                        navigateWithAd(context, Routes.SETTINGS) {
                            navController.safePopBackStack(ignoreDebounce = true)
                        }
                    }
                )
            }

            composable(Routes.INTRO) {
                IntroScreen(
                    onFinish = {
                        viewModel.completeIntro()
                        if (SafeRemoteConfig.isShowPremiumOnboardingFirst()) {
                            navController.safeNavigate(
                                "${Routes.PREMIUM}/${StartPremiumIndexes.ONBOARDING_FIRST.name}",
                                ignoreDebounce = true
                            ) {
                                popUpTo(Routes.INTRO) { inclusive = true }
                            }
                        } else {
                            navigateWithAd(context, "after_intro") {
                                navController.safeNavigate(
                                    Routes.PERMISSION,
                                    ignoreDebounce = true
                                ) {
                                    popUpTo(Routes.INTRO) { inclusive = true }
                                }
                            }
                        }
                    }
                )
            }

            composable(Routes.PERMISSION) {
                val navigateHome = {
                    viewModel.completePermission()
                    navigateWithAd(context, Routes.HOME) {
                        navController.safeNavigate(Routes.HOME, ignoreDebounce = true) {
                            popUpTo(Routes.PERMISSION) { inclusive = true }
                        }
                    }
                }
                PermissionScreen(
                    onContinue = navigateHome,
                    onSkip = navigateHome
                )
            }

            composable(Routes.HOME) { backStackEntry ->
                val homeTab = activity?.pendingHomeTab
                LaunchedEffect(homeTab) {
                    if (homeTab != null) {
                        backStackEntry.savedStateHandle["targetTab"] = homeTab
                        activity.clearPendingHomeTab()
                    }
                }
                HomeScreen(
                    nextScreen = ::navigateFromHome,
                    savedStateHandle = backStackEntry.savedStateHandle,
                    onExitApp = onExitApp
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = {
                        navigateWithAd(context, Routes.HOME) {
                            navController.safePopBackStack(ignoreDebounce = true)
                        }
                    },
                    onNavigateToLanguage = {
                        navigateFromHome(Routes.LANGUAGE_SETTINGS)
                    }
                )
            }

            composable(
                route = "${Routes.PREMIUM}/{startByIndex}",
                arguments = listOf(
                    navArgument("startByIndex") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val startByIndex = backStackEntry.arguments
                    ?.getString("startByIndex")
                    ?.let { runCatching { StartPremiumIndexes.valueOf(it) }.getOrNull() }
                    ?: StartPremiumIndexes.IN_APP

                fun closePremium() {
                    when (startByIndex) {
                        StartPremiumIndexes.ONBOARDING_FIRST -> {
                            navController.safeNavigate(Routes.PERMISSION, ignoreDebounce = true) {
                                popUpTo(Routes.PREMIUM) { inclusive = true }
                            }
                        }

                        StartPremiumIndexes.SPLASH_RETURN -> {
                            navController.safeNavigate(Routes.HOME, ignoreDebounce = true) {
                                popUpTo(Routes.PREMIUM) { inclusive = true }
                            }
                        }

                        else -> navController.safePopBackStack(ignoreDebounce = true)
                    }
                }

                PremiumScreen(
                    startByIndex = startByIndex,
                    onClose = { closePremium() },
                    buyPremiumSuccess = { index ->
                        when (index) {
                            StartPremiumIndexes.ONBOARDING_FIRST,
                            StartPremiumIndexes.SPLASH_RETURN -> closePremium()

                            else -> {
                                val intent = context.packageManager
                                    .getLaunchIntentForPackage(context.packageName)
                                    ?.apply {
                                        putExtra("skip_splash", true)
                                        addFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        )
                                    }
                                intent?.let(context::startActivity)
                                (context as? Activity)?.finish()
                            }
                        }
                    }
                )
            }
        }
    }
}
