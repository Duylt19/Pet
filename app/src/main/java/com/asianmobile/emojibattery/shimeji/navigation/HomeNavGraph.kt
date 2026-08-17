package com.asianmobile.emojibattery.shimeji.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.BatteryCatalogScreen
import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.BatteryCatalogViewModel
import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.CURRENT_BATTERY_STYLE_ID
import com.asianmobile.emojibattery.shimeji.ui.home.discover.DiscoverScreen
import com.asianmobile.emojibattery.shimeji.ui.home.shell.HomeShell
import com.asianmobile.emojibattery.shimeji.ui.home.shell.HomeTab
import com.asianmobile.emojibattery.shimeji.ui.pet.store.PetStoreScreen
import com.asianmobile.emojibattery.shimeji.ui.pet.store.PetStoreTab
import com.asianmobile.emojibattery.shimeji.ui.premium.StartPremiumIndexes
import com.asianmobile.emojibattery.shimeji.ui.settings.mine.SettingsScreen
import kotlinx.coroutines.delay

private const val HOME_BACK_HANDOFF_DELAY_MS = 250L

/**
 * Owns the only navigation surface shared by the four Home tabs.
 *
 * App destinations such as Search, My Pet and Battery Editor live in the root NavHost instead.
 * This mirrors an Activity boundary without creating multiple Android activities: Home keeps its
 * tab state and banner, while every app destination gets an independent composition/ViewModel
 * owner and therefore an independent ad lifecycle.
 */
@Composable
internal fun HomeRoute(
    homeEntry: NavBackStackEntry,
    onNavigateOutsideHome: (String) -> Unit,
    onNavigateToOverlayGrantPermissions: () -> Unit,
    onNavigateToAccessibilityHowToUse: () -> Unit,
    onHomeBack: () -> Unit,
    onDestinationChanged: (String?) -> Unit
) {
    val context = LocalContext.current
    val homeNavController = rememberNavController()
    val currentEntry by homeNavController.currentBackStackEntryAsState()
    val requestedTabValue by homeEntry.savedStateHandle
        .getStateFlow<String?>(Routes.HOME_TAB_REQUEST, null)
        .collectAsStateWithLifecycle()
    val requestedPetStoreTab by homeEntry.savedStateHandle
        .getStateFlow<String?>(Routes.PET_STORE_TAB_REQUEST, null)
        .collectAsStateWithLifecycle()
    val accessibilityResult = homeEntry.accessibilityHowToUseResult()
    val catalogViewModel = hiltViewModel<BatteryCatalogViewModel>(homeEntry)
    var canHandleHomeBack by remember(homeEntry.id) { mutableStateOf(false) }

    fun navigateToTab(tab: HomeTab, requestInterstitial: Boolean = true) {
        val route = routeForHomeTab(tab)
        if (currentEntry?.destination?.route == route) return
        val navigate = {
            homeNavController.safeNavigate(route, ignoreDebounce = true) {
                popUpTo(Routes.DISCOVER) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        if (requestInterstitial) {
            navigateWithAd(
                context = context,
                placement = navigationAdPlacement(route, NavigationAdDirection.TAB),
                onNavigate = navigate
            )
        } else {
            navigate()
        }
    }

    LaunchedEffect(requestedTabValue) {
        val requestedTab = homeTabFromNavigationValue(requestedTabValue)
            ?: return@LaunchedEffect
        // The root destination already requested its interstitial before handing this tab off.
        navigateToTab(requestedTab, requestInterstitial = false)
        homeEntry.savedStateHandle[Routes.HOME_TAB_REQUEST] = null
    }

    LaunchedEffect(homeEntry.id) {
        // A single physical Back can pop a root detail on key-down and finish on key-up after
        // Home is recomposed. Delay this callback across that hand-off so one press owns exactly
        // one destination. Normal Home Back behavior is unchanged after the short guard window.
        delay(HOME_BACK_HANDOFF_DELAY_MS)
        canHandleHomeBack = true
    }

    DisposableEffect(homeNavController, onDestinationChanged) {
        val listener = androidx.navigation.NavController.OnDestinationChangedListener {
                _, destination, _ ->
            onDestinationChanged(destination.route)
        }
        homeNavController.addOnDestinationChangedListener(listener)
        onDispose { homeNavController.removeOnDestinationChangedListener(listener) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_FFFFFF))
    ) {
        Box(modifier = Modifier.weight(1f)) {
            NavHost(
                navController = homeNavController,
                startDestination = Routes.DISCOVER,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) {
                homeTabs(
                    onNavigateToHomeTab = ::navigateToTab,
                    onNavigateOutsideHome = onNavigateOutsideHome,
                    onNavigateToOverlayGrantPermissions =
                        onNavigateToOverlayGrantPermissions,
                    accessibilityHowToUseResult = accessibilityResult,
                    onAccessibilityHowToUseResultConsumed =
                        homeEntry::consumeAccessibilityHowToUseResult,
                    onNavigateToAccessibilityHowToUse =
                        onNavigateToAccessibilityHowToUse,
                    requestedPetStoreTab = requestedPetStoreTab,
                    onRequestedPetStoreTabConsumed = {
                        homeEntry.savedStateHandle[Routes.PET_STORE_TAB_REQUEST] = null
                    },
                    catalogViewModel = catalogViewModel
                )
            }
        }

        HomeShell(
            selectedTab = homeTabForRoute(currentEntry?.destination?.route)
                ?: HomeTab.DISCOVER,
            onTabSelected = ::navigateToTab
        )
    }

    // Registered after the nested NavHost so every top-level tab behaves as the same app root.
    BackHandler(enabled = canHandleHomeBack, onBack = onHomeBack)
}

/** Declares only the four equivalent top-level destinations owned by [HomeRoute]. */
private fun NavGraphBuilder.homeTabs(
    onNavigateToHomeTab: (HomeTab) -> Unit,
    onNavigateOutsideHome: (String) -> Unit,
    onNavigateToOverlayGrantPermissions: () -> Unit,
    accessibilityHowToUseResult: Boolean?,
    onAccessibilityHowToUseResultConsumed: () -> Unit,
    onNavigateToAccessibilityHowToUse: () -> Unit,
    requestedPetStoreTab: String?,
    onRequestedPetStoreTabConsumed: () -> Unit,
    catalogViewModel: BatteryCatalogViewModel
) {
    composable(Routes.DISCOVER) {
        DiscoverScreen(
            onNavigateToSearch = { onNavigateOutsideHome(Routes.SEARCH) },
            onNavigateToPremium = {
                onNavigateOutsideHome(
                    "${Routes.PREMIUM}/${StartPremiumIndexes.IN_APP.name}"
                )
            },
            onNavigateToBattery = { onNavigateToHomeTab(HomeTab.BATTERY) },
            onNavigateToBatteryTroll = { onNavigateOutsideHome(Routes.BATTERY_TROLL) },
            onNavigateToPetStore = { onNavigateToHomeTab(HomeTab.PET_STORE) },
            onNavigateToMyPet = { onNavigateOutsideHome(Routes.MY_PET) },
            onNavigateToGrantPermissions = onNavigateToOverlayGrantPermissions,
            onOpenBatteryTheme = { themeId ->
                onNavigateOutsideHome(Routes.batteryEditor(themeId))
            },
            onOpenStatusBarTheme = { backgroundId ->
                onNavigateOutsideHome(
                    Routes.batteryEditor(
                        themeId = CURRENT_BATTERY_STYLE_ID,
                        backgroundId = backgroundId
                    )
                )
            },
            onCustomizeStatusBar = {
                onNavigateOutsideHome(Routes.batteryEditor(CURRENT_BATTERY_STYLE_ID))
            },
            accessibilityHowToUseResult = accessibilityHowToUseResult,
            onAccessibilityHowToUseResultConsumed =
                onAccessibilityHowToUseResultConsumed,
            onNavigateToAccessibilityHowToUse = onNavigateToAccessibilityHowToUse
        )
    }

    composable(Routes.BATTERY_CATALOG) {
        BatteryCatalogScreen(
            onSearch = { onNavigateOutsideHome(Routes.SEARCH) },
            onOpenCategory = { categoryId ->
                onNavigateOutsideHome(Routes.batteryCategory(categoryId))
            },
            onOpenTheme = { themeId ->
                onNavigateOutsideHome(Routes.batteryEditor(themeId))
            },
            onNavigateToPremium = {
                onNavigateOutsideHome(
                    "${Routes.PREMIUM}/${StartPremiumIndexes.IN_APP.name}"
                )
            },
            accessibilityHowToUseResult = accessibilityHowToUseResult,
            onAccessibilityHowToUseResultConsumed =
                onAccessibilityHowToUseResultConsumed,
            onNavigateToAccessibilityHowToUse = onNavigateToAccessibilityHowToUse,
            viewModel = catalogViewModel
        )
    }

    composable(Routes.PET_STORE) {
        PetStoreScreen(
            requestedTab = PetStoreTab.fromNavigationValue(requestedPetStoreTab),
            onRequestedTabConsumed = onRequestedPetStoreTabConsumed,
            onSearch = { onNavigateOutsideHome(Routes.SEARCH) },
            onPremium = {
                onNavigateOutsideHome(
                    "${Routes.PREMIUM}/${StartPremiumIndexes.IN_APP.name}"
                )
            },
            onViewPet = { onNavigateOutsideHome(Routes.MY_PET) },
            onNavigateToGrantPermissions = onNavigateToOverlayGrantPermissions
        )
    }

    composable(Routes.SETTINGS) {
        SettingsScreen(
            onSearch = { onNavigateOutsideHome(Routes.SEARCH) },
            onPremium = {
                onNavigateOutsideHome(
                    "${Routes.PREMIUM}/${StartPremiumIndexes.IN_APP.name}"
                )
            },
            onNavigateToLanguage = { onNavigateOutsideHome(Routes.LANGUAGE_SETTINGS) },
            onNavigateToMyPet = { onNavigateOutsideHome(Routes.MY_PET) },
            onNavigateToFavouriteRecent = {
                onNavigateOutsideHome(Routes.FAVOURITE_RECENT)
            },
            onNavigateToGrantPermissions = {
                onNavigateOutsideHome(Routes.GRANT_PERMISSIONS)
            },
            accessibilityHowToUseResult = accessibilityHowToUseResult,
            onAccessibilityHowToUseResultConsumed =
                onAccessibilityHowToUseResultConsumed,
            onNavigateToAccessibilityHowToUse = onNavigateToAccessibilityHowToUse
        )
    }
}
