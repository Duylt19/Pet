package com.asianmobile.privatebrower.navigation

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.asianmobile.privatebrower.ads.utils.SafeRemoteConfig
import com.asianmobile.privatebrower.ads.config.BANNER_BOOKMARKS_HISTORY_BOTTOM
import com.asianmobile.privatebrower.ads.config.BANNER_HOW_TO_DOWNLOAD_BOTTOM
import com.asianmobile.privatebrower.ads.ui.compose.BannerAd
import com.asianmobile.privatebrower.ui.home.HomeScreen
import com.asianmobile.privatebrower.ui.home.tabstab.TabSelectionScreen
import com.asianmobile.privatebrower.ui.home.settings.SettingsScreen
import com.asianmobile.privatebrower.ui.intro.IntroScreen
import com.asianmobile.privatebrower.ui.language.LanguageScreen
import com.asianmobile.privatebrower.ui.main.MainViewModel
import com.asianmobile.privatebrower.ui.permission.PermissionScreen
import com.asianmobile.privatebrower.ui.premium.PremiumScreen
import com.asianmobile.privatebrower.ui.premium.StartPremiumIndexes
import com.asianmobile.privatebrower.ui.splash.SplashScreen
import com.asianmobile.privatebrower.ui.browser.BrowserScreen
import com.asianmobile.privatebrower.ui.privacypolicy.PrivacyPolicyScreen
import com.asianmobile.privatebrower.ui.setdefault.SetDefaultBrowserScreen
import com.asianmobile.privatebrower.ui.home.filestab.MediaCategory
import com.asianmobile.privatebrower.ui.medialist.AudioScreen
import com.asianmobile.privatebrower.ui.medialist.DocumentScreen
import com.asianmobile.privatebrower.ui.medialist.PhotoScreen
import com.asianmobile.privatebrower.ui.medialist.VideoScreen
import com.asianmobile.privatebrower.ui.mediaviewer.MediaViewerRoute
import com.asianmobile.privatebrower.ui.mediaviewer.MediaViewerScreen
import com.asianmobile.privatebrower.ui.mediaviewer.MediaViewerSource
import com.asianmobile.privatebrower.ui.bookmarks.BookmarksHistorySection
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import com.asianmobile.privatebrower.utils.ScreenName
import com.asianmobile.privatebrower.utils.TrackScreenView

object Routes {
    const val SPLASH = "splash"
    const val INTRO = "intro"
    const val LANGUAGE = "language"
    const val LANGUAGE_SETTINGS = "language_settings"
    const val PERMISSION = "permission"
    const val HOME = "home"
    const val PREMIUM = "premium"
    const val SETTINGS = "settings"
    const val SET_DEFAULT_BROWSER = "set_default_browser"
    const val BROWSER_WEBVIEW = "browser_webview"
    const val TAB_SELECTION = "tab_selection"
    const val BOOKMARKS_HISTORY = "bookmarks_history"
    const val HISTORY = "$BOOKMARKS_HISTORY?section=history"
    const val PRIVACY_POLICY = "privacy_policy"
    const val MEDIA_PHOTOS = "media_photos"
    const val MEDIA_VIDEOS = "media_videos"
    const val MEDIA_AUDIO = "media_audio"
    const val MEDIA_DOCUMENTS = "media_documents"
    const val MEDIA_VIEWER = "media_viewer"
    const val HOW_TO_DOWNLOAD = "how_to_download"
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

    fun handleHomeNextScreen(type: String) {
        when {
            type == Routes.LANGUAGE_SETTINGS -> navigateWithAd(context, type) {
                navController.safeNavigate(
                    Routes.LANGUAGE_SETTINGS,
                    ignoreDebounce = true
                )
            }

            type == Routes.PREMIUM -> navController.safeNavigate(
                "${Routes.PREMIUM}/${StartPremiumIndexes.IN_APP.name}",
                ignoreDebounce = true
            )
            
            type == Routes.SETTINGS -> navigateWithAd(context, type) {
                navController.safeNavigate(Routes.SETTINGS, ignoreDebounce = true)
            }

            type.startsWith(Routes.TAB_SELECTION) -> navController.safeNavigate(
                type,
                ignoreDebounce = true
            )

            type.startsWith(Routes.BROWSER_WEBVIEW) -> navigateWithAd(
                context,
                Routes.BROWSER_WEBVIEW
            ) {
                navController.safeNavigate(type, ignoreDebounce = true)
            }

            type.startsWith(Routes.BOOKMARKS_HISTORY) -> navigateWithAd(
                context,
                Routes.BOOKMARKS_HISTORY
            ) {
                navController.safeNavigate(type, ignoreDebounce = true)
            }

            type.startsWith(Routes.PRIVACY_POLICY) -> navigateWithAd(
                context,
                Routes.PRIVACY_POLICY
            ) {
                navController.safeNavigate(type, ignoreDebounce = true)
            }

            type == Routes.MEDIA_PHOTOS ||
                type == Routes.MEDIA_VIDEOS ||
                type == Routes.MEDIA_AUDIO ||
                type == Routes.MEDIA_DOCUMENTS -> navigateWithAd(context, type) {
                navController.safeNavigate(type, ignoreDebounce = true)
            }

            type.startsWith(Routes.MEDIA_VIEWER) -> navController.safeNavigate(
                type,
                ignoreDebounce = true
            )

            type == Routes.HOW_TO_DOWNLOAD -> navigateWithAd(context, type) {
                navController.safeNavigate(type, ignoreDebounce = true)
            }
        }
    }

    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            onDestinationChanged(destination.route)
        }
    }

    // Deep link handling: consume pending URL from MainActivity
    val activity = context as? com.asianmobile.privatebrower.MainActivity
    val pendingUrl = activity?.pendingDeepLinkUrl
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    LaunchedEffect(pendingUrl, currentRoute) {
        pendingUrl?.let { url ->
            val startupRoutes = setOf(
                Routes.SPLASH,
                Routes.LANGUAGE,
                Routes.INTRO,
                Routes.PERMISSION,
                Routes.SET_DEFAULT_BROWSER
            )
            val canOpenBrowser = currentRoute != null &&
                currentRoute !in startupRoutes &&
                !currentRoute.startsWith(Routes.PREMIUM)
            if (canOpenBrowser) {
                val encodedUrl = Uri.encode(url)
                navController.safeNavigate(
                    "${Routes.BROWSER_WEBVIEW}?url=$encodedUrl",
                    ignoreDebounce = true
                )
                activity?.clearPendingDeepLink()
            }
        }
    }

    // Download notification tap while on a non-Home screen (Browser, Settings, …): return to
    // Home so its Downloads tab can be selected. The HOME composable consumes pendingHomeTab.
    val pendingHomeTab = activity?.pendingHomeTab
    LaunchedEffect(pendingHomeTab, currentRoute) {
        val startupRoutes = setOf(
            Routes.SPLASH, Routes.LANGUAGE, Routes.INTRO,
            Routes.PERMISSION, Routes.SET_DEFAULT_BROWSER
        )
        if (pendingHomeTab != null && currentRoute != null &&
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
                    viewModel,
                    skipLauncherAd = pendingUrl != null,
                    onNextScreen = {
                        navController.safeNavigate(nextScreenAfterSplash) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
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
                                    viewModel.getNextScreenAfterIntro(),
                                    ignoreDebounce = true
                                ) {
                                    popUpTo(Routes.INTRO) { inclusive = true }
                                }
                            }
                        }
                    }
                )
            }

            composable(
                route = "${Routes.PREMIUM}/{startByIndex}",
                arguments = listOf(navArgument("startByIndex") { type = NavType.StringType })
            ) { backStackEntry ->
                val indexStr = backStackEntry.arguments?.getString("startByIndex") ?: StartPremiumIndexes.IN_APP.name
                val startByIndex = runCatching { StartPremiumIndexes.valueOf(indexStr) }.getOrDefault(
                    StartPremiumIndexes.IN_APP)
                PremiumScreen(
                    startByIndex = startByIndex,
                    onClose = { index ->
                        navigateWithAd(context, "premium_close") {
                            when (index) {
                                StartPremiumIndexes.ONBOARDING_FIRST -> navController.safeNavigate(
                                    viewModel.getNextScreenAfterIntro(),
                                    ignoreDebounce = true
                                ) { popUpTo(Routes.PREMIUM) { inclusive = true } }

                                StartPremiumIndexes.SPLASH_RETURN -> navController.safeNavigate(
                                    Routes.HOME,
                                    ignoreDebounce = true
                                ) { popUpTo(Routes.PREMIUM) { inclusive = true } }

                                else -> navController.safePopBackStack(ignoreDebounce = true)
                            }
                        }
                    },
                    buyPremiumSuccess = { index ->
                        when (index) {
                            StartPremiumIndexes.ONBOARDING_FIRST -> navController.safeNavigate(
                                viewModel.getNextScreenAfterIntro(),
                                ignoreDebounce = true
                            ) { popUpTo(Routes.PREMIUM) { inclusive = true } }

                            StartPremiumIndexes.SPLASH_RETURN -> navController.safeNavigate(
                                Routes.HOME,
                                ignoreDebounce = true
                            ) { popUpTo(Routes.PREMIUM) { inclusive = true } }

                            else -> {
                                val intent =
                                    context.packageManager.getLaunchIntentForPackage(context.packageName)
                                intent?.putExtra("skip_splash", true)
                                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                context.startActivity(intent)
                                (context as? Activity)?.finish()
                            }
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
                        val intent =
                            context.packageManager.getLaunchIntentForPackage(context.packageName)
                        intent?.putExtra("skip_splash", true)
                        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        context.startActivity(intent)
                        (context as? Activity)?.finish()
                    },
                    onBack = {
                        navigateWithAd(context, Routes.SETTINGS) {
                            navController.safePopBackStack(ignoreDebounce = true)
                        }
                    }
                )
            }

            composable(Routes.PERMISSION) {
                PermissionScreen(
                    onContinue = {
                        viewModel.completePermission()
                        navigateWithAd(context, Routes.HOME) {
                            navController.safeNavigate(Routes.HOME, ignoreDebounce = true) {
                                popUpTo(
                                    Routes.PERMISSION
                                ) { inclusive = true }
                            }
                        }
                    },
                    onSkip = {
                        viewModel.completePermission()
                        navigateWithAd(context, Routes.HOME) {
                            navController.safeNavigate(Routes.HOME, ignoreDebounce = true) {
                                popUpTo(
                                    Routes.PERMISSION
                                ) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Routes.SET_DEFAULT_BROWSER) {
                SetDefaultBrowserScreen(
                    onCompleted = {
                        val nextRoute = viewModel.getNextScreenAfterDefaultBrowser()
                        navigateWithAd(context, nextRoute) {
                            navController.safeNavigate(nextRoute, ignoreDebounce = true) {
                                popUpTo(Routes.SET_DEFAULT_BROWSER) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Routes.HOME) { backStackEntry ->
                // Download notification tap → open the Downloads tab. Setting targetTab here
                // (keyed on the pending value) re-fires even when Home is already on screen.
                val homeTab = activity?.pendingHomeTab
                LaunchedEffect(homeTab) {
                    if (homeTab != null) {
                        backStackEntry.savedStateHandle["targetTab"] = homeTab
                        activity.clearPendingHomeTab()
                    }
                }
                HomeScreen(
                    nextScreen = { handleHomeNextScreen(it) },
                    savedStateHandle = backStackEntry.savedStateHandle,
                    onExitApp = onExitApp
                )
            }

            composable(
                route = "${Routes.TAB_SELECTION}?incognito={incognito}",
                arguments = listOf(
                    navArgument("incognito") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) {
                TabSelectionScreen(
                    onBack = { navController.safePopBackStack(ignoreDebounce = true) }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = {
                        navigateWithAd(context, Routes.HOME) {
                            navController.safePopBackStack(ignoreDebounce = true)
                        }
                    },
                    onNavigateToLanguage = { handleHomeNextScreen(Routes.LANGUAGE_SETTINGS) },
                    onNavigateToPrivacyPolicy = {
                        val url = Uri.encode(context.getString(com.asianmobile.privatebrower.R.string.privacy_policy_default_url))
                        handleHomeNextScreen("${Routes.PRIVACY_POLICY}?url=$url")
                    }
                )
            }

            composable(
                route = "${Routes.BROWSER_WEBVIEW}?url={url}&incognito={incognito}",
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("incognito") { type = NavType.BoolType; defaultValue = false }
                )
            ) {
                BrowserScreen(
                    onBack = { navController.safePopBackStack(ignoreDebounce = true) },
                    onNavigateToSettings = { handleHomeNextScreen(Routes.SETTINGS) },
                    onNavigateToTabs = {
                        navController.previousBackStackEntry?.savedStateHandle?.set("targetTab", 1)
                        navController.safePopBackStack(ignoreDebounce = true)
                    },
                    onNavigateToDownloads = {
                        navController.previousBackStackEntry?.savedStateHandle?.set("targetTab", 2)
                        navController.safePopBackStack(ignoreDebounce = true)
                    },
                    onNavigateToHistory = {
                        handleHomeNextScreen(Routes.HISTORY)
                    }
                )
            }

            composable(
                route = "${Routes.BOOKMARKS_HISTORY}?section={section}",
                arguments = listOf(
                    navArgument("section") {
                        type = NavType.StringType
                        defaultValue = "bookmarks"
                    }
                )
            ) { backStackEntry ->
                val initialSection = if (
                    backStackEntry.arguments?.getString("section") == "history"
                ) {
                    BookmarksHistorySection.HISTORY
                } else {
                    BookmarksHistorySection.BOOKMARKS
                }
                var isSearchModeActive by remember { mutableStateOf(false) }

                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        com.asianmobile.privatebrower.ui.bookmarks.BookmarksScreen(
                            initialSection = initialSection,
                            onNavigateToBrowser = { url ->
                                val route = "${Routes.BROWSER_WEBVIEW}?url=${Uri.encode(url)}&incognito=false"
                                navigateWithAd(context, Routes.BROWSER_WEBVIEW) {
                                    navController.safeNavigate(route, ignoreDebounce = true)
                                }
                            },
                            onBack = { navController.safePopBackStack(ignoreDebounce = true) },
                            onSearchModeChanged = { isSearchModeActive = it }
                        )
                    }
                    Box(
                        modifier = if (isSearchModeActive) {
                            Modifier.imePadding()
                        } else {
                            Modifier
                        }
                    ) {
                        BannerAd(
                            modifier = Modifier.fillMaxWidth(),
                            adPosition = BANNER_BOOKMARKS_HISTORY_BOTTOM
                        )
                    }
                }
            }

            composable(
                route = "${Routes.PRIVACY_POLICY}?url={url}",
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val policyUrl = backStackEntry.arguments?.getString("url").orEmpty()
                PrivacyPolicyScreen(
                    url = policyUrl,
                    onBack = {
                        navigateWithAd(context, Routes.SETTINGS) {
                            navController.safePopBackStack(ignoreDebounce = true)
                        }
                    }
                )
            }

            composable(
                route = "${Routes.MEDIA_PHOTOS}?type={type}",
                arguments = listOf(
                    navArgument("type") {
                        type = NavType.StringType
                        defaultValue = MediaCategory.IMAGES.name
                    }
                )
            ) { backStackEntry ->
                val mediaLibraryChanged by backStackEntry.savedStateHandle
                    .getStateFlow(MediaViewerRoute.RESULT_MEDIA_LIBRARY_CHANGED, false)
                    .collectAsStateWithLifecycle()
                PhotoScreen(
                    onOpenMedia = { item ->
                        navController.safeNavigate(
                            MediaViewerRoute.fromMedia(item),
                            ignoreDebounce = true
                        )
                    },
                    onBack = {
                        navigateWithAd(context, ScreenName.FILES_HOME.value) {
                            navController.safePopBackStack(ignoreDebounce = true)
                        }
                    },
                    mediaLibraryChanged = mediaLibraryChanged,
                    onMediaLibraryChangeConsumed = {
                        backStackEntry.savedStateHandle[
                            MediaViewerRoute.RESULT_MEDIA_LIBRARY_CHANGED
                        ] = false
                    }
                )
            }

            composable(
                route = "${Routes.MEDIA_VIDEOS}?type={type}",
                arguments = listOf(
                    navArgument("type") {
                        type = NavType.StringType
                        defaultValue = MediaCategory.VIDEO.name
                    }
                )
            ) { backStackEntry ->
                val mediaLibraryChanged by backStackEntry.savedStateHandle
                    .getStateFlow(MediaViewerRoute.RESULT_MEDIA_LIBRARY_CHANGED, false)
                    .collectAsStateWithLifecycle()
                VideoScreen(
                    onOpenMedia = { item ->
                        navController.safeNavigate(
                            MediaViewerRoute.fromMedia(item),
                            ignoreDebounce = true
                        )
                    },
                    onBack = {
                        navigateWithAd(context, ScreenName.FILES_HOME.value) {
                            navController.safePopBackStack(ignoreDebounce = true)
                        }
                    },
                    mediaLibraryChanged = mediaLibraryChanged,
                    onMediaLibraryChangeConsumed = {
                        backStackEntry.savedStateHandle[
                            MediaViewerRoute.RESULT_MEDIA_LIBRARY_CHANGED
                        ] = false
                    }
                )
            }

            composable(
                route = "${Routes.MEDIA_AUDIO}?type={type}",
                arguments = listOf(
                    navArgument("type") {
                        type = NavType.StringType
                        defaultValue = MediaCategory.MUSIC.name
                    }
                )
            ) { backStackEntry ->
                val mediaLibraryChanged by backStackEntry.savedStateHandle
                    .getStateFlow(MediaViewerRoute.RESULT_MEDIA_LIBRARY_CHANGED, false)
                    .collectAsStateWithLifecycle()
                AudioScreen(
                    onOpenMedia = { item ->
                        navController.safeNavigate(
                            MediaViewerRoute.fromMedia(item),
                            ignoreDebounce = true
                        )
                    },
                    onBack = {
                        navigateWithAd(context, ScreenName.FILES_HOME.value) {
                            navController.safePopBackStack(ignoreDebounce = true)
                        }
                    },
                    mediaLibraryChanged = mediaLibraryChanged,
                    onMediaLibraryChangeConsumed = {
                        backStackEntry.savedStateHandle[
                            MediaViewerRoute.RESULT_MEDIA_LIBRARY_CHANGED
                        ] = false
                    }
                )
            }

            composable(
                route = "${Routes.MEDIA_DOCUMENTS}?type={type}",
                arguments = listOf(
                    navArgument("type") {
                        type = NavType.StringType
                        defaultValue = MediaCategory.FILES.name
                    }
                )
            ) { backStackEntry ->
                val mediaLibraryChanged by backStackEntry.savedStateHandle
                    .getStateFlow(MediaViewerRoute.RESULT_MEDIA_LIBRARY_CHANGED, false)
                    .collectAsStateWithLifecycle()
                DocumentScreen(
                    onOpenMedia = { item ->
                        navController.safeNavigate(
                            MediaViewerRoute.fromMedia(item),
                            ignoreDebounce = true
                        )
                    },
                    onBack = {
                        navigateWithAd(context, ScreenName.FILES_HOME.value) {
                            navController.safePopBackStack(ignoreDebounce = true)
                        }
                    },
                    mediaLibraryChanged = mediaLibraryChanged,
                    onMediaLibraryChangeConsumed = {
                        backStackEntry.savedStateHandle[
                            MediaViewerRoute.RESULT_MEDIA_LIBRARY_CHANGED
                        ] = false
                    }
                )
            }

            composable(
                route = MediaViewerRoute.PATTERN,
                arguments = listOf(
                    navArgument("source") { type = NavType.StringType; defaultValue = "MEDIA_LIBRARY" },
                    navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("name") { type = NavType.StringType; defaultValue = "" },
                    navArgument("path") { type = NavType.StringType; defaultValue = "" },
                    navArgument("mediaUri") { type = NavType.StringType; defaultValue = "" },
                    navArgument("mimeType") { type = NavType.StringType; defaultValue = "" },
                    navArgument("sizeBytes") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("modifiedAt") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("durationMs") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("width") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("height") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("mediaSource") { type = NavType.StringType; defaultValue = "" },
                    navArgument("thumbnailUrl") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val mediaViewerSource = backStackEntry.arguments
                    ?.getString("source")
                    ?.let { runCatching { MediaViewerSource.valueOf(it) }.getOrNull() }
                MediaViewerScreen(
                    onBack = { navController.safePopBackStack(ignoreDebounce = true) },
                    onRemoved = {
                        if (mediaViewerSource == MediaViewerSource.MEDIA_LIBRARY) {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(MediaViewerRoute.RESULT_MEDIA_LIBRARY_CHANGED, true)
                        }
                        navController.safePopBackStack(ignoreDebounce = true)
                    }
                )
            }

            composable(Routes.HOW_TO_DOWNLOAD) {
                TrackScreenView(ScreenName.HOW_TO_DOWNLOAD)
                // TODO: Implement HowToDownloadScreen
                // For now, show a simple placeholder
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Text("How to Download - Coming Soon")
                    }
                    BannerAd(
                        modifier = Modifier.fillMaxWidth(),
                        adPosition = BANNER_HOW_TO_DOWNLOAD_BOTTOM
                    )
                }
            }
        } // end NavHost
    } // end BoxWithConstraints
}
