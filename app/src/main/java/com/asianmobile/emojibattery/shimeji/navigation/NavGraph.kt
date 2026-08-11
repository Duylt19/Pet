package com.asianmobile.emojibattery.shimeji.navigation

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.BannerAd
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.AdType
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.NativeAdInternal
import com.asianmobile.emojibattery.shimeji.ads.config.BANNER_BATTERY_EDITOR_BOTTOM
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_HOME
import com.asianmobile.emojibattery.shimeji.ads.utils.SafeRemoteConfig
import com.asianmobile.emojibattery.shimeji.ui.shared.component.HomeBottomNavigation
import com.asianmobile.emojibattery.shimeji.ui.shared.component.HomeTab
import com.asianmobile.emojibattery.shimeji.ui.home.discover.DiscoverScreen
import com.asianmobile.emojibattery.shimeji.ui.battery.favoriterecent.FavouriteRecentScreen
import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.BatteryCatalogScreen
import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.BatteryCatalogViewModel
import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.BatteryCategoryScreen
import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.CURRENT_BATTERY_STYLE_ID
import com.asianmobile.emojibattery.shimeji.ui.battery.editor.BatteryEditorPage
import com.asianmobile.emojibattery.shimeji.ui.battery.editor.BatteryEditorScreen
import com.asianmobile.emojibattery.shimeji.ui.battery.editor.BatteryEditorViewModel
import com.asianmobile.emojibattery.shimeji.ui.battery.editor.isStatusOptionPage
import com.asianmobile.emojibattery.shimeji.ui.settings.mine.SettingsScreen
import com.asianmobile.emojibattery.shimeji.ui.onboarding.intro.IntroScreen
import com.asianmobile.emojibattery.shimeji.ui.onboarding.language.LanguageScreen
import com.asianmobile.emojibattery.shimeji.ui.app.MainViewModel
import com.asianmobile.emojibattery.shimeji.ui.settings.permissions.GrantPermissionsScreen
import com.asianmobile.emojibattery.shimeji.ui.onboarding.permission.PermissionScreen
import com.asianmobile.emojibattery.shimeji.ui.premium.PremiumScreen
import com.asianmobile.emojibattery.shimeji.ui.pet.room.PetRoomScreen
import com.asianmobile.emojibattery.shimeji.ui.pet.store.PetStoreScreen
import com.asianmobile.emojibattery.shimeji.ui.premium.StartPremiumIndexes
import com.asianmobile.emojibattery.shimeji.ui.onboarding.splash.SplashScreen
import com.asianmobile.emojibattery.shimeji.ui.search.SearchScreen

object Routes {
    const val SPLASH = "splash"
    const val LANGUAGE = "language"
    const val LANGUAGE_SETTINGS = "language_settings"
    const val INTRO = "intro"
    const val PERMISSION = "permission"
    const val HOME = "home"
    const val SEARCH = "search"
    const val FAVOURITE_RECENT = "favourite_recent"
    const val GRANT_PERMISSIONS = "grant_permissions"
    const val MY_PET = "my_pet"
    const val PET_STORE = "pet_store"
    const val SETTINGS = "settings"
    const val BATTERY_CATALOG = "battery_catalog"
    const val BATTERY_CATEGORY = "battery_category"
    const val BATTERY_EDITOR = "battery_editor"
    const val BATTERY_EDITOR_COMPONENT = "battery_editor_component"
    const val PREMIUM = "premium"

    fun batteryEditor(themeId: Int): String = "$BATTERY_EDITOR/$themeId"
    fun batteryCategory(categoryId: Int): String = "$BATTERY_CATEGORY/$categoryId"
    fun batteryEditorComponent(themeId: Int, page: String): String =
        "$BATTERY_EDITOR_COMPONENT/$themeId/$page"
}

private const val HOME_BOTTOM_BANNER_POSITION = "home_mode_bottom"

internal fun homeTabForRoute(route: String?): HomeTab? = when (route) {
    Routes.HOME -> HomeTab.DISCOVER
    Routes.BATTERY_CATALOG -> HomeTab.BATTERY
    Routes.PET_STORE -> HomeTab.PET_STORE
    Routes.SETTINGS -> HomeTab.MINE
    else -> null
}

internal fun routeForHomeTab(tab: HomeTab): String = when (tab) {
    HomeTab.DISCOVER -> Routes.HOME
    HomeTab.BATTERY -> Routes.BATTERY_CATALOG
    HomeTab.PET_STORE -> Routes.PET_STORE
    HomeTab.MINE -> Routes.SETTINGS
}

internal fun showHomeBottomBanner(route: String?): Boolean =
    homeTabForRoute(route) != null || route?.startsWith("${Routes.BATTERY_CATEGORY}/") == true

internal fun showBatteryEditorBottomBanner(route: String?): Boolean =
    route?.startsWith("${Routes.BATTERY_EDITOR}/") == true ||
        route?.startsWith("${Routes.BATTERY_EDITOR_COMPONENT}/") == true

internal fun showBatteryStatusOptionNative(route: String?, page: String?): Boolean =
    route?.startsWith("${Routes.BATTERY_EDITOR_COMPONENT}/") == true &&
        BatteryEditorPage.fromRoute(page)?.isStatusOptionPage() == true

@Composable
fun AppNavGraph(
    startDestination: String,
    nextScreenAfterSplash: String,
    viewModel: MainViewModel,
    onDestinationChanged: (String?) -> Unit = {}
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val selectedHomeTab = homeTabForRoute(currentBackStackEntry?.destination?.route)
    val shouldShowHomeBottomBanner = showHomeBottomBanner(
        currentBackStackEntry?.destination?.route
    )
    val currentRoute = currentBackStackEntry?.destination?.route
    val currentEditorPage = currentBackStackEntry?.arguments?.getString("page")
    val shouldShowBatteryStatusOptionNative = showBatteryStatusOptionNative(
        currentRoute,
        currentEditorPage
    )
    val shouldShowBatteryEditorBottomBanner =
        showBatteryEditorBottomBanner(currentRoute) && !shouldShowBatteryStatusOptionNative

    fun navigateToHomeTab(tab: HomeTab) {
        val route = routeForHomeTab(tab)
        if (currentBackStackEntry?.destination?.route == route) return

        val navigate = {
            navController.safeNavigate(route, ignoreDebounce = true) {
                popUpTo(Routes.HOME) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        if (tab == HomeTab.MINE) {
            navigateWithAd(context, route, navigate)
        } else {
            navigate()
        }
    }

    fun navigateFromHome(route: String) {
        when (route) {
            Routes.SETTINGS -> navigateToHomeTab(HomeTab.MINE)

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

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize()
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

            composable(Routes.HOME) {
                DiscoverScreen(
                    onNavigateToSearch = {
                        navController.safeNavigate(Routes.SEARCH, ignoreDebounce = true)
                    },
                    onNavigateToPremium = {
                        navigateFromHome(Routes.PREMIUM)
                    },
                    onNavigateToBattery = {
                        navigateToHomeTab(HomeTab.BATTERY)
                    },
                    onNavigateToPetStore = {
                        navigateToHomeTab(HomeTab.PET_STORE)
                    },
                    onOpenBatteryTheme = { themeId ->
                        navController.safeNavigate(
                            Routes.batteryEditor(themeId),
                            ignoreDebounce = true
                        )
                    },
                    onCustomizeStatusBar = {
                        navController.safeNavigate(
                            Routes.batteryEditor(CURRENT_BATTERY_STYLE_ID),
                            ignoreDebounce = true
                        )
                    }
                )
            }

            composable(Routes.GRANT_PERMISSIONS) {
                GrantPermissionsScreen(
                    onNavigateBack = { navController.safePopBackStack() }
                )
            }

            composable(Routes.SEARCH) {
                SearchScreen(
                    onCancel = { navController.safePopBackStack(ignoreDebounce = true) },
                    onOpenPetStore = {
                        navigateToHomeTab(HomeTab.PET_STORE)
                    },
                    onOpenTheme = { themeId ->
                        navController.safeNavigate(
                            Routes.batteryEditor(themeId),
                            ignoreDebounce = true
                        )
                    }
                )
            }

            composable(Routes.FAVOURITE_RECENT) {
                FavouriteRecentScreen(
                    onBack = { navController.safePopBackStack(ignoreDebounce = true) },
                    onPremium = { navigateFromHome(Routes.PREMIUM) },
                    onOpenTheme = { themeId ->
                        navController.safeNavigate(
                            Routes.batteryEditor(themeId),
                            ignoreDebounce = true
                        )
                    }
                )
            }

            composable(Routes.PET_STORE) {
                PetStoreScreen(
                    onSearch = {
                        navController.safeNavigate(Routes.SEARCH, ignoreDebounce = true)
                    },
                    onPremium = {
                        navigateFromHome(Routes.PREMIUM)
                    },
                    onViewPet = {
                        navController.safeNavigate(Routes.MY_PET, ignoreDebounce = true)
                    }
                )
            }

            composable(Routes.MY_PET) {
                PetRoomScreen(
                    onNavigateBack = { navController.safePopBackStack() },
                    onOpenPetStore = {
                        // Leave the room before switching tabs. navigateToHomeTab saves the
                        // current stack under `home`, so jumping straight from here would make
                        // a later Discover tap restore My Pet Room instead of Discover.
                        navController.safePopBackStack(ignoreDebounce = true)
                        navigateToHomeTab(HomeTab.PET_STORE)
                    }
                )
            }

            composable(Routes.BATTERY_CATALOG) {
                BatteryCatalogScreen(
                    onSearch = {
                        navController.safeNavigate(Routes.SEARCH, ignoreDebounce = true)
                    },
                    onOpenCategory = { categoryId ->
                        navController.safeNavigate(
                            Routes.batteryCategory(categoryId),
                            ignoreDebounce = true
                        ) { launchSingleTop = true }
                    },
                    onOpenTheme = { themeId ->
                        navController.safeNavigate(
                            Routes.batteryEditor(themeId),
                            ignoreDebounce = true
                        )
                    },
                    onNavigateToPremium = {
                        navController.safeNavigate(
                            "${Routes.PREMIUM}/${StartPremiumIndexes.IN_APP.name}",
                            ignoreDebounce = true
                        )
                    }
                )
            }

            composable(
                route = "${Routes.BATTERY_CATEGORY}/{categoryId}",
                arguments = listOf(navArgument("categoryId") { type = NavType.IntType })
            ) { backStackEntry ->
                val catalogEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Routes.BATTERY_CATALOG)
                }
                val catalogViewModel = hiltViewModel<BatteryCatalogViewModel>(catalogEntry)
                BatteryCategoryScreen(
                    categoryId = backStackEntry.arguments?.getInt("categoryId") ?: 0,
                    onBack = {
                        navController.safePopBackStack(ignoreDebounce = true)
                    },
                    onNavigateToPremium = {
                        navController.safeNavigate(
                            "${Routes.PREMIUM}/${StartPremiumIndexes.IN_APP.name}",
                            ignoreDebounce = true
                        )
                    },
                    onOpenTheme = { themeId ->
                        navController.safeNavigate(
                            Routes.batteryEditor(themeId),
                            ignoreDebounce = true
                        )
                    },
                    viewModel = catalogViewModel
                )
            }

            composable(
                route = "${Routes.BATTERY_EDITOR}/{themeId}",
                arguments = listOf(navArgument("themeId") { type = NavType.IntType })
            ) { backStackEntry ->
                val themeId = backStackEntry.arguments?.getInt("themeId") ?: 0
                BatteryEditorScreen(
                    page = BatteryEditorPage.OVERVIEW,
                    onBack = { navController.safePopBackStack(ignoreDebounce = true) },
                    onOpenPage = { page ->
                        navController.safeNavigate(
                            Routes.batteryEditorComponent(themeId, page.name),
                            ignoreDebounce = true
                        )
                    },
                    onNavigateToPremium = {
                        navController.safeNavigate(
                            "${Routes.PREMIUM}/${StartPremiumIndexes.IN_APP.name}",
                            ignoreDebounce = true
                        )
                    }
                )
            }

            composable(
                route = "${Routes.BATTERY_EDITOR_COMPONENT}/{themeId}/{page}",
                arguments = listOf(
                    navArgument("themeId") { type = NavType.IntType },
                    navArgument("page") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val page = BatteryEditorPage.fromRoute(
                    backStackEntry.arguments?.getString("page")
                ) ?: BatteryEditorPage.SIZE
                val overviewEntry = remember(backStackEntry) {
                    requireNotNull(navController.previousBackStackEntry) {
                        "Battery component editor must be opened from the battery overview"
                    }
                }
                val editorViewModel = hiltViewModel<BatteryEditorViewModel>(overviewEntry)
                BatteryEditorScreen(
                    page = page,
                    onBack = { navController.safePopBackStack(ignoreDebounce = true) },
                    onNavigateToPremium = {
                        navController.safeNavigate(
                            "${Routes.PREMIUM}/${StartPremiumIndexes.IN_APP.name}",
                            ignoreDebounce = true
                        )
                    },
                    viewModel = editorViewModel
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onSearch = {
                        navController.safeNavigate(Routes.SEARCH, ignoreDebounce = true)
                    },
                    onPremium = {
                        navigateFromHome(Routes.PREMIUM)
                    },
                    onNavigateToLanguage = {
                        navigateFromHome(Routes.LANGUAGE_SETTINGS)
                    },
                    onNavigateToMyPet = {
                        navController.safeNavigate(Routes.MY_PET, ignoreDebounce = true)
                    },
                    onNavigateToFavouriteRecent = {
                        navController.safeNavigate(Routes.FAVOURITE_RECENT, ignoreDebounce = true)
                    },
                    onOpenAppsHidden = {
                        // TODO(Mine): connect the app-exclusion picker when its product flow is defined.
                    },
                    onNavigateToGrantPermissions = {
                        navController.safeNavigate(
                            Routes.GRANT_PERMISSIONS,
                            ignoreDebounce = true
                        ) {
                            // ignoreDebounce turns the double-tap guard off, so without this a
                            // quick double tap stacks two identical screens to back out of.
                            launchSingleTop = true
                        }
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
        if (selectedHomeTab != null) {
            HomeBottomNavigation(
                selectedTab = selectedHomeTab,
                onTabSelected = ::navigateToHomeTab
            )
        }
        if (shouldShowBatteryStatusOptionNative) {
            NativeAdInternal(
                screenCode = SCREEN_HOME,
                instanceKey = "battery_status_option_collapsible",
                adTypeOverride = AdType.COLLAPSE_SMALL,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.navigationBarsPadding())
        } else if (shouldShowHomeBottomBanner || shouldShowBatteryEditorBottomBanner) {
            BannerAd(
                modifier = Modifier.fillMaxWidth(),
                adPosition = if (shouldShowBatteryEditorBottomBanner) {
                    BANNER_BATTERY_EDITOR_BOTTOM
                } else {
                    HOME_BOTTOM_BANNER_POSITION
                }
            )
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
