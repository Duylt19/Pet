package com.asianmobile.emojibattery.shimeji.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.BatteryCatalogScreen
import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.CURRENT_BATTERY_STYLE_ID
import com.asianmobile.emojibattery.shimeji.ui.home.discover.DiscoverScreen
import com.asianmobile.emojibattery.shimeji.ui.home.shell.HomeTab
import com.asianmobile.emojibattery.shimeji.ui.pet.store.PetStoreScreen
import com.asianmobile.emojibattery.shimeji.ui.pet.store.PetStoreTab
import com.asianmobile.emojibattery.shimeji.ui.premium.StartPremiumIndexes
import com.asianmobile.emojibattery.shimeji.ui.settings.mine.SettingsScreen

/** Declares the four equivalent top-level destinations owned by the Home graph. */
internal fun NavGraphBuilder.homeGraph(
    navController: NavHostController,
    onNavigateToHomeTab: (HomeTab) -> Unit,
    onNavigateFromHome: (String) -> Unit,
    onNavigateToOverlayGrantPermissions: () -> Unit,
    onNavigateToAccessibilityHowToUse: (NavBackStackEntry) -> Unit
) {
    navigation(
        route = Routes.HOME_GRAPH,
        startDestination = Routes.DISCOVER
    ) {
        composable(Routes.DISCOVER) { backStackEntry ->
            DiscoverScreen(
                onNavigateToSearch = {
                    navController.safeNavigate(Routes.SEARCH, ignoreDebounce = true)
                },
                onNavigateToPremium = {
                    onNavigateFromHome(Routes.PREMIUM)
                },
                onNavigateToBattery = {
                    onNavigateToHomeTab(HomeTab.BATTERY)
                },
                onNavigateToBatteryTroll = {
                    navController.safeNavigate(Routes.BATTERY_TROLL, ignoreDebounce = true)
                },
                onNavigateToPetStore = {
                    onNavigateToHomeTab(HomeTab.PET_STORE)
                },
                onNavigateToMyPet = {
                    navController.safeNavigate(Routes.MY_PET, ignoreDebounce = true)
                },
                onNavigateToGrantPermissions = onNavigateToOverlayGrantPermissions,
                onOpenBatteryTheme = { themeId ->
                    navController.safeNavigate(
                        Routes.batteryEditor(themeId),
                        ignoreDebounce = true
                    )
                },
                onOpenStatusBarTheme = { backgroundId ->
                    navController.safeNavigate(
                        Routes.batteryEditor(
                            themeId = CURRENT_BATTERY_STYLE_ID,
                            backgroundId = backgroundId
                        ),
                        ignoreDebounce = true
                    )
                },
                onCustomizeStatusBar = {
                    navController.safeNavigate(
                        Routes.batteryEditor(CURRENT_BATTERY_STYLE_ID),
                        ignoreDebounce = true
                    )
                },
                accessibilityHowToUseResult = backStackEntry.accessibilityHowToUseResult(),
                onAccessibilityHowToUseResultConsumed =
                    backStackEntry::consumeAccessibilityHowToUseResult,
                onNavigateToAccessibilityHowToUse = {
                    onNavigateToAccessibilityHowToUse(backStackEntry)
                }
            )
        }

        composable(Routes.BATTERY_CATALOG) { backStackEntry ->
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
                },
                accessibilityHowToUseResult = backStackEntry.accessibilityHowToUseResult(),
                onAccessibilityHowToUseResultConsumed =
                    backStackEntry::consumeAccessibilityHowToUseResult,
                onNavigateToAccessibilityHowToUse = {
                    onNavigateToAccessibilityHowToUse(backStackEntry)
                }
            )
        }

        composable(Routes.PET_STORE) {
            val homeGraphEntry = navController.getBackStackEntry(Routes.HOME_GRAPH)
            val requestedTabValue by homeGraphEntry.savedStateHandle
                .getStateFlow<String?>(Routes.PET_STORE_TAB_REQUEST, null)
                .collectAsStateWithLifecycle()
            PetStoreScreen(
                requestedTab = PetStoreTab.fromNavigationValue(requestedTabValue),
                onRequestedTabConsumed = {
                    homeGraphEntry.savedStateHandle[Routes.PET_STORE_TAB_REQUEST] = null
                },
                onSearch = {
                    navController.safeNavigate(Routes.SEARCH, ignoreDebounce = true)
                },
                onPremium = {
                    onNavigateFromHome(Routes.PREMIUM)
                },
                onViewPet = {
                    navController.safeNavigate(Routes.MY_PET, ignoreDebounce = true)
                },
                onNavigateToGrantPermissions = onNavigateToOverlayGrantPermissions
            )
        }

        composable(Routes.SETTINGS) { backStackEntry ->
            SettingsScreen(
                onSearch = {
                    navController.safeNavigate(Routes.SEARCH, ignoreDebounce = true)
                },
                onPremium = {
                    onNavigateFromHome(Routes.PREMIUM)
                },
                onNavigateToLanguage = {
                    onNavigateFromHome(Routes.LANGUAGE_SETTINGS)
                },
                onNavigateToMyPet = {
                    navController.safeNavigate(Routes.MY_PET, ignoreDebounce = true)
                },
                onNavigateToFavouriteRecent = {
                    navController.safeNavigate(Routes.FAVOURITE_RECENT, ignoreDebounce = true)
                },
                onNavigateToGrantPermissions = {
                    navController.safeNavigate(
                        Routes.GRANT_PERMISSIONS,
                        ignoreDebounce = true
                    ) {
                        // The debounce guard is disabled, so keep the destination single-top.
                        launchSingleTop = true
                    }
                },
                accessibilityHowToUseResult = backStackEntry.accessibilityHowToUseResult(),
                onAccessibilityHowToUseResultConsumed =
                    backStackEntry::consumeAccessibilityHowToUseResult,
                onNavigateToAccessibilityHowToUse = {
                    onNavigateToAccessibilityHowToUse(backStackEntry)
                }
            )
        }
    }
}
