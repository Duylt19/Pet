package com.asianmobile.emojibattery.shimeji.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
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
import com.asianmobile.emojibattery.shimeji.ads.utils.SafeRemoteConfig
import com.asianmobile.emojibattery.shimeji.ui.component.HomeBottomNavigation
import com.asianmobile.emojibattery.shimeji.ui.component.HomeTab
import com.asianmobile.emojibattery.shimeji.ui.discover.DiscoverScreen
import com.asianmobile.emojibattery.shimeji.ui.favoriterecent.FavouriteRecentScreen
import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.BatteryCatalogScreen
import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.CURRENT_BATTERY_STYLE_ID
import com.asianmobile.emojibattery.shimeji.ui.battery.editor.BatteryEditorPage
import com.asianmobile.emojibattery.shimeji.ui.battery.editor.BatteryEditorScreen
import com.asianmobile.emojibattery.shimeji.ui.battery.editor.BatteryEditorViewModel
import com.asianmobile.emojibattery.shimeji.ui.catalog.PetCatalogScreen
import com.asianmobile.emojibattery.shimeji.ui.catalog.PetCatalogTarget
import com.asianmobile.emojibattery.shimeji.ui.catalog.PetDetailScreen
import com.asianmobile.emojibattery.shimeji.ui.home.settings.PetCustomizationScreen
import com.asianmobile.emojibattery.shimeji.ui.home.settings.SettingsScreen
import com.asianmobile.emojibattery.shimeji.ui.home.swarm.SwarmCustomizationScreen
import com.asianmobile.emojibattery.shimeji.ui.intro.IntroScreen
import com.asianmobile.emojibattery.shimeji.ui.language.LanguageScreen
import com.asianmobile.emojibattery.shimeji.ui.main.MainViewModel
import com.asianmobile.emojibattery.shimeji.ui.permission.PermissionScreen
import com.asianmobile.emojibattery.shimeji.ui.premium.PremiumScreen
import com.asianmobile.emojibattery.shimeji.ui.petroom.PetRoomScreen
import com.asianmobile.emojibattery.shimeji.ui.petstore.PetStoreScreen
import com.asianmobile.emojibattery.shimeji.ui.premium.StartPremiumIndexes
import com.asianmobile.emojibattery.shimeji.ui.splash.SplashScreen
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
    const val MY_PET = "my_pet"
    const val PET_CATALOG = "pet_catalog"
    const val PET_STORE = "pet_store"
    const val PET_DETAIL = "pet_detail"
    const val PET_CUSTOMIZATION = "pet_customization"
    const val SWARM_CUSTOMIZATION = "swarm_customization"
    const val SETTINGS = "settings"
    const val BATTERY_CATALOG = "battery_catalog"
    const val BATTERY_EDITOR = "battery_editor"
    const val BATTERY_EDITOR_COMPONENT = "battery_editor_component"
    const val PREMIUM = "premium"

    fun petCatalog(
        target: PetCatalogTarget,
        slotIndex: Int = 0
    ): String = "$PET_CATALOG/${target.name}/$slotIndex"
    fun petDetail(
        target: PetCatalogTarget,
        slotIndex: Int,
        packKey: String
    ): String = "$PET_DETAIL/${target.name}/$slotIndex/${Uri.encode(packKey)}"
    fun petCustomization(slotIndex: Int): String = "$PET_CUSTOMIZATION/$slotIndex"
    fun batteryEditor(themeId: Int): String = "$BATTERY_EDITOR/$themeId"
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
                    onNavigateToMyPet = {
                        navController.safeNavigate(Routes.MY_PET, ignoreDebounce = true)
                    },
                    onNavigateToPetStore = {
                        navigateToHomeTab(HomeTab.PET_STORE)
                    },
                    onOpenPet = { packKey ->
                        navController.safeNavigate(
                            Routes.petDetail(PetCatalogTarget.MIXED, 0, packKey),
                            ignoreDebounce = true
                        )
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

            composable(Routes.SEARCH) {
                SearchScreen(
                    onCancel = { navController.safePopBackStack(ignoreDebounce = true) },
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

            composable(
                route = "${Routes.PET_CATALOG}/{target}/{slotIndex}",
                arguments = listOf(
                    navArgument("target") { type = NavType.StringType },
                    navArgument("slotIndex") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val slotIndex = backStackEntry.arguments?.getInt("slotIndex") ?: 0
                val target = backStackEntry.arguments?.getString("target")
                    ?.let { encoded ->
                        PetCatalogTarget.entries.firstOrNull { it.name == encoded }
                    }
                    ?: PetCatalogTarget.MIXED
                PetCatalogScreen(
                    onBack = { navController.safePopBackStack(ignoreDebounce = true) },
                    onNavigateToPremium = {
                        navController.safeNavigate(
                            "${Routes.PREMIUM}/${StartPremiumIndexes.IN_APP.name}",
                            ignoreDebounce = true
                        )
                    },
                    onOpenPack = { packKey ->
                        navController.safeNavigate(
                            Routes.petDetail(target, slotIndex, packKey),
                            ignoreDebounce = true
                        )
                    }
                )
            }

            composable(
                route = "${Routes.PET_DETAIL}/{target}/{slotIndex}/{packKey}",
                arguments = listOf(
                    navArgument("target") { type = NavType.StringType },
                    navArgument("slotIndex") { type = NavType.IntType },
                    navArgument("packKey") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                PetDetailScreen(
                    packKey = backStackEntry.arguments?.getString("packKey").orEmpty(),
                    onBack = { navController.safePopBackStack(ignoreDebounce = true) }
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
                    }
                )
            }

            composable(
                route = "${Routes.PET_CUSTOMIZATION}/{slotIndex}",
                arguments = listOf(navArgument("slotIndex") { type = NavType.IntType })
            ) {
                PetCustomizationScreen(
                    onBack = {
                        navController.safePopBackStack(ignoreDebounce = true)
                    },
                    onChangeCharacter = { slotIndex ->
                        navController.safeNavigate(
                            Routes.petCatalog(PetCatalogTarget.MIXED, slotIndex),
                            ignoreDebounce = true
                        )
                    },
                    onPetRemoved = {
                        navController.safePopBackStack(ignoreDebounce = true)
                    }
                )
            }

            composable(Routes.SWARM_CUSTOMIZATION) {
                SwarmCustomizationScreen(
                    onBack = {
                        navController.safePopBackStack(ignoreDebounce = true)
                    },
                    onChangeCharacter = {
                        navController.safeNavigate(
                            Routes.petCatalog(PetCatalogTarget.SWARM),
                            ignoreDebounce = true
                        )
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
            BannerAd(
                modifier = Modifier.fillMaxWidth(),
                adPosition = HOME_BOTTOM_BANNER_POSITION
            )
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
