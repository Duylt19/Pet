package com.asianmobile.emojibattery.shimeji.navigation

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.BannerAd
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.NativeAdInternal
import com.asianmobile.emojibattery.shimeji.ads.config.BANNER_BATTERY_EDITOR_BOTTOM
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_BATTERY_CATEGORY
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_BATTERY_TROLL
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_BATTERY_EDITOR
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_CUSTOMIZE_STATUS_BAR
import com.asianmobile.emojibattery.shimeji.ads.utils.SafeRemoteConfig
import com.asianmobile.emojibattery.shimeji.ads.utils.AdOverlayState
import com.asianmobile.emojibattery.shimeji.ui.home.shell.HomeTab
import com.asianmobile.emojibattery.shimeji.ui.battery.favoriterecent.FavouriteRecentScreen
import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.BatteryCatalogViewModel
import com.asianmobile.emojibattery.shimeji.ui.battery.troll.BatteryTrollCustomizeScreen
import com.asianmobile.emojibattery.shimeji.ui.battery.troll.BatteryTrollScreen
import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.BatteryCategoryScreen
import com.asianmobile.emojibattery.shimeji.ui.battery.editor.BatteryEditorPage
import com.asianmobile.emojibattery.shimeji.ui.battery.editor.BatteryEditorScreen
import com.asianmobile.emojibattery.shimeji.ui.battery.editor.BATTERY_EDITOR_INITIAL_BACKGROUND_ID_ARG
import com.asianmobile.emojibattery.shimeji.ui.battery.editor.BatteryEditorViewModel
import com.asianmobile.emojibattery.shimeji.ui.battery.editor.isStatusOptionPage
import com.asianmobile.emojibattery.shimeji.ui.onboarding.intro.IntroScreen
import com.asianmobile.emojibattery.shimeji.ui.onboarding.language.LanguageScreen
import com.asianmobile.emojibattery.shimeji.ui.pet.store.PetStoreTab
import com.asianmobile.emojibattery.shimeji.ui.app.MainViewModel
import com.asianmobile.emojibattery.shimeji.ui.app.destinationAfterIntro
import com.asianmobile.emojibattery.shimeji.ui.settings.permissions.AccessibilityHowToUseScreen
import com.asianmobile.emojibattery.shimeji.ui.settings.permissions.GrantPermissionsScreen
import com.asianmobile.emojibattery.shimeji.ui.settings.permissions.GrantPermissionsTarget
import com.asianmobile.emojibattery.shimeji.ui.onboarding.permission.PermissionScreen
import com.asianmobile.emojibattery.shimeji.ui.premium.PremiumScreen
import com.asianmobile.emojibattery.shimeji.ui.pet.room.PetRoomScreen
import com.asianmobile.emojibattery.shimeji.ui.premium.StartPremiumIndexes
import com.asianmobile.emojibattery.shimeji.ui.onboarding.splash.SplashScreen
import com.asianmobile.emojibattery.shimeji.ui.search.SearchScreen

object Routes {
    const val SPLASH = "splash"
    const val LANGUAGE = "language"
    const val LANGUAGE_SETTINGS = "language_settings"
    const val INTRO = "intro"
    const val PERMISSION = "permission"
    const val HOME_GRAPH = "home_graph"
    const val HOME_TAB_REQUEST = "home_tab_request"
    const val DISCOVER = "discover"
    const val SEARCH = "search"
    const val FAVOURITE_RECENT = "favourite_recent"
    const val GRANT_PERMISSIONS = "grant_permissions"
    const val GRANT_PERMISSIONS_REQUIRED_TARGET = "requiredTarget"
    const val GRANT_PERMISSIONS_OVERLAY_TARGET = "overlay"
    const val ACCESSIBILITY_HOW_TO_USE = "accessibility_how_to_use"
    const val MY_PET = "my_pet"
    const val PET_STORE = "pet_store"
    const val PET_STORE_TAB_REQUEST = "pet_store_tab_request"
    const val SETTINGS = "settings"
    const val BATTERY_CATALOG = "battery_catalog"
    const val BATTERY_CATEGORY = "battery_category"
    const val BATTERY_EDITOR = "battery_editor"
    const val BATTERY_EDITOR_COMPONENT = "battery_editor_component"
    const val BATTERY_EDITOR_EMOTION_DETAIL = "battery_editor_emotion_detail"
    const val BATTERY_TROLL = "battery_troll"
    const val BATTERY_TROLL_CUSTOMIZE = "battery_troll_customize"
    const val PREMIUM = "premium"

    fun batteryEditor(themeId: Int, backgroundId: Int? = null): String =
        "$BATTERY_EDITOR/$themeId" + backgroundId?.let {
            "?$BATTERY_EDITOR_INITIAL_BACKGROUND_ID_ARG=$it"
        }.orEmpty()
    fun grantPermissionsForOverlay(): String =
        "$GRANT_PERMISSIONS?$GRANT_PERMISSIONS_REQUIRED_TARGET=" +
            GRANT_PERMISSIONS_OVERLAY_TARGET
    fun batteryCategory(categoryId: Int): String = "$BATTERY_CATEGORY/$categoryId"
    fun batteryTrollCustomize(trollId: Int): String = "$BATTERY_TROLL_CUSTOMIZE/$trollId"
    fun batteryEditorComponent(themeId: Int, page: String): String =
        "$BATTERY_EDITOR_COMPONENT/$themeId/$page"
    fun batteryEditorEmotionDetail(themeId: Int, groupKey: String): String =
        "$BATTERY_EDITOR_EMOTION_DETAIL/$themeId/$groupKey"
}

private const val ACCESSIBILITY_HOW_TO_USE_RESULT = "accessibility_how_to_use_result"

@Composable
internal fun NavBackStackEntry.accessibilityHowToUseResult(): Boolean? {
    val result by remember(this) {
        savedStateHandle.getStateFlow<Boolean?>(ACCESSIBILITY_HOW_TO_USE_RESULT, null)
    }.collectAsStateWithLifecycle()
    return result
}

internal fun NavBackStackEntry.consumeAccessibilityHowToUseResult() {
    savedStateHandle[ACCESSIBILITY_HOW_TO_USE_RESULT] = null
}

internal fun batteryEditorCollapsibleNativeScreenCode(route: String?, page: String?): String? =
    when {
        route?.startsWith("${Routes.BATTERY_EDITOR}/") == true ->
            SCREEN_CUSTOMIZE_STATUS_BAR

        route?.startsWith("${Routes.BATTERY_EDITOR_EMOTION_DETAIL}/") == true ->
            SCREEN_BATTERY_EDITOR

        (
            route?.startsWith("${Routes.BATTERY_EDITOR_COMPONENT}/") == true &&
                BatteryEditorPage.fromRoute(page)?.let { editorPage ->
                    editorPage == BatteryEditorPage.BATTERY_TEMPLATES ||
                        editorPage == BatteryEditorPage.EMOJI_TEMPLATES ||
                        editorPage == BatteryEditorPage.EMOJI ||
                        editorPage.isStatusOptionPage()
                } == true
            ) -> SCREEN_BATTERY_EDITOR

        else -> null
    }

internal fun showBatteryEditorCollapsibleNative(route: String?, page: String?): Boolean =
    batteryEditorCollapsibleNativeScreenCode(route, page) != null

internal fun destinationAdReloadKey(backStackEntryId: String?): Int =
    backStackEntryId
        ?.hashCode()
        ?.and(Int.MAX_VALUE)
        ?.coerceAtLeast(1)
        ?: 0

/**
 * Gives every non-Home destination an opaque, isolated surface and a destination-scoped ad slot.
 * The ad Composable inherits the destination's ViewModelStoreOwner, so a newly pushed screen
 * performs a new load instead of reusing the Home or previous screen holder.
 */
@Composable
private fun IsolatedDestination(
    bottomAd: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(com.asianmobile.emojibattery.shimeji.R.color.colors_FFFFFF))
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(
                    colorResource(com.asianmobile.emojibattery.shimeji.R.color.colors_FFFFFF)
                )
        ) {
            content()
        }
        bottomAd()
    }
}

@Composable
fun AppNavGraph(
    startDestination: String,
    nextScreenAfterSplash: String,
    viewModel: MainViewModel,
    onHomeBack: () -> Unit,
    onDestinationChanged: (String?) -> Unit = {}
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val hasRequestedNotificationPermission by
        viewModel.hasRequestedNotificationPermission.collectAsStateWithLifecycle()
    val isFullScreenAdShowing by AdOverlayState.isAdShowing.collectAsStateWithLifecycle()
    var hasLaunchedHomeNotificationPrompt by rememberSaveable { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    val currentRoute = currentBackStackEntry?.destination?.route

    LaunchedEffect(
        currentRoute,
        hasRequestedNotificationPermission,
        isFullScreenAdShowing,
        hasLaunchedHomeNotificationPrompt
    ) {
        val isNotificationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        if (
            !hasLaunchedHomeNotificationPrompt &&
            shouldRequestHomeNotificationPermission(
                sdkInt = Build.VERSION.SDK_INT,
                isGranted = isNotificationGranted,
                hasRequestedBefore = hasRequestedNotificationPermission,
                isHomeTopLevelVisible = currentRoute == Routes.HOME_GRAPH,
                isFullScreenAdShowing = isFullScreenAdShowing
            )
        ) {
            // Persist before launching so recreation or tab changes cannot show a second prompt.
            hasLaunchedHomeNotificationPrompt = true
            viewModel.markNotificationPermissionRequested()
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun navigateFromHome(route: String) {
        val destination = if (route == Routes.PREMIUM) {
            "${Routes.PREMIUM}/${StartPremiumIndexes.IN_APP.name}"
        } else {
            route
        }
        navController.safeNavigateWithAd(
            context = context,
            route = destination,
            placementRoute = route
        ) {
            launchSingleTop = true
        }
    }

    fun returnToHomeTab(tab: HomeTab, petStoreTab: PetStoreTab? = null) {
        val homeEntry = runCatching {
            navController.getBackStackEntry(Routes.HOME_GRAPH)
        }.getOrNull() ?: return
        val targetRoute = routeForHomeTab(tab)
        navigateWithAd(
            context = context,
            placement = navigationAdPlacement(targetRoute, NavigationAdDirection.TAB)
        ) {
            homeEntry.savedStateHandle[Routes.HOME_TAB_REQUEST] = tab.name
            if (petStoreTab != null) {
                homeEntry.savedStateHandle[Routes.PET_STORE_TAB_REQUEST] =
                    petStoreTab.navigationValue
            }
            navController.popBackStack(Routes.HOME_GRAPH, inclusive = false)
        }
    }

    fun navigateToAccessibilityHowToUse(source: NavBackStackEntry) {
        source.consumeAccessibilityHowToUseResult()
        navController.safeNavigateWithAd(context, Routes.ACCESSIBILITY_HOW_TO_USE) {
            launchSingleTop = true
        }
    }

    fun navigateToOverlayGrantPermissions() {
        navController.safeNavigateWithAd(
            context = context,
            route = Routes.grantPermissionsForOverlay(),
            placementRoute = Routes.GRANT_PERMISSIONS
        ) {
            launchSingleTop = true
        }
    }

    DisposableEffect(navController, onDestinationChanged) {
        val listener = androidx.navigation.NavController.OnDestinationChangedListener {
                _, destination, _ ->
            onDestinationChanged(destination.route)
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize(),
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
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
                        navigateWithAd(
                            context = context,
                            placement = navigationAdPlacement(
                                Routes.INTRO,
                                NavigationAdDirection.FORWARD
                            )
                        ) {
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
                        navigateWithAd(
                            context = context,
                            placement = navigationAdPlacement(
                                Routes.HOME_GRAPH,
                                NavigationAdDirection.FORWARD
                            )
                        ) {
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
                    },
                    onBack = {
                        navController.safePopBackStackWithAd(
                            context = context,
                            currentRoute = Routes.LANGUAGE_SETTINGS
                        )
                    }
                )
            }

            composable(Routes.INTRO) {
                IntroScreen(
                    onFinish = {
                        viewModel.completeIntro()
                        if (SafeRemoteConfig.isShowPremiumOnboardingFirst()) {
                            navController.safeNavigateWithAd(
                                context = context,
                                route = "${Routes.PREMIUM}/" +
                                    StartPremiumIndexes.ONBOARDING_FIRST.name,
                                placementRoute = Routes.PREMIUM
                            ) {
                                popUpTo(Routes.INTRO) { inclusive = true }
                            }
                        } else {
                            navController.safeNavigateWithAd(
                                context = context,
                                route = destinationAfterIntro(),
                                placementRoute = "after_intro"
                            ) {
                                popUpTo(Routes.INTRO) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Routes.PERMISSION) {
                val navigateHome = {
                    viewModel.completePermission()
                    // Preserve the existing ad placement key while entering the parent graph.
                    navController.safeNavigateWithAd(
                        context = context,
                        route = Routes.HOME_GRAPH,
                        placementRoute = Routes.DISCOVER
                    ) {
                        popUpTo(Routes.PERMISSION) { inclusive = true }
                    }
                }
                PermissionScreen(
                    onContinue = navigateHome,
                    onSkip = navigateHome,
                    onNavigateToGrantPermissions = ::navigateToOverlayGrantPermissions
                )
            }

            composable(Routes.HOME_GRAPH) { homeEntry ->
                HomeRoute(
                    homeEntry = homeEntry,
                    onNavigateOutsideHome = ::navigateFromHome,
                    onNavigateToOverlayGrantPermissions = ::navigateToOverlayGrantPermissions,
                    onNavigateToAccessibilityHowToUse = {
                        navigateToAccessibilityHowToUse(homeEntry)
                    },
                    onHomeBack = onHomeBack,
                    onDestinationChanged = onDestinationChanged
                )
            }

            composable(
                route = "${Routes.GRANT_PERMISSIONS}?" +
                    "${Routes.GRANT_PERMISSIONS_REQUIRED_TARGET}=" +
                    "{${Routes.GRANT_PERMISSIONS_REQUIRED_TARGET}}",
                arguments = listOf(
                    navArgument(Routes.GRANT_PERMISSIONS_REQUIRED_TARGET) {
                        type = NavType.StringType
                        defaultValue = GrantPermissionsTarget.ACCESSIBILITY.name.lowercase()
                    }
                )
            ) { backStackEntry ->
                val requiredTarget = when (
                    backStackEntry.arguments?.getString(
                        Routes.GRANT_PERMISSIONS_REQUIRED_TARGET
                    )
                ) {
                    Routes.GRANT_PERMISSIONS_OVERLAY_TARGET -> GrantPermissionsTarget.OVERLAY
                    else -> GrantPermissionsTarget.ACCESSIBILITY
                }
                GrantPermissionsScreen(
                    onNavigateBack = {
                        navController.safePopBackStackWithAd(
                            context = context,
                            currentRoute = Routes.GRANT_PERMISSIONS
                        )
                    },
                    onPermissionFlowCompleted = {
                        navController.safePopBackStack(ignoreDebounce = true)
                    },
                    requiredTarget = requiredTarget,
                    accessibilityHowToUseResult = backStackEntry.accessibilityHowToUseResult(),
                    onAccessibilityHowToUseResultConsumed =
                        backStackEntry::consumeAccessibilityHowToUseResult,
                    onNavigateToAccessibilityHowToUse = {
                        navigateToAccessibilityHowToUse(backStackEntry)
                    }
                )
            }

            composable(Routes.ACCESSIBILITY_HOW_TO_USE) {
                fun returnToSource(permissionGranted: Boolean, requestInterstitial: Boolean) {
                    val publishResult: () -> Unit = {
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            ACCESSIBILITY_HOW_TO_USE_RESULT,
                            permissionGranted
                        )
                        Unit
                    }
                    if (requestInterstitial) {
                        navController.safePopBackStackWithAd(
                            context = context,
                            currentRoute = Routes.ACCESSIBILITY_HOW_TO_USE,
                            onBeforePop = publishResult
                        )
                    } else {
                        publishResult()
                        navController.safePopBackStack(ignoreDebounce = true)
                    }
                }
                AccessibilityHowToUseScreen(
                    onNavigateBack = {
                        returnToSource(permissionGranted = false, requestInterstitial = true)
                    },
                    onPermissionGranted = {
                        returnToSource(permissionGranted = true, requestInterstitial = false)
                    }
                )
            }

            composable(Routes.SEARCH) {
                SearchScreen(
                    onCancel = {
                        navController.safePopBackStackWithAd(
                            context = context,
                            currentRoute = Routes.SEARCH
                        )
                    },
                    onOpenTheme = { themeId ->
                        navController.safeNavigateWithAd(
                            context = context,
                            route = Routes.batteryEditor(themeId),
                            placementRoute = Routes.BATTERY_EDITOR
                        )
                    },
                    onPremium = { navigateFromHome(Routes.PREMIUM) },
                    onViewPet = {
                        navController.safeNavigateWithAd(context, Routes.MY_PET)
                    },
                    onNavigateToGrantPermissions = ::navigateToOverlayGrantPermissions
                )
            }

            composable(Routes.FAVOURITE_RECENT) {
                FavouriteRecentScreen(
                    onBack = {
                        navController.safePopBackStackWithAd(
                            context = context,
                            currentRoute = Routes.FAVOURITE_RECENT
                        )
                    },
                    onPremium = { navigateFromHome(Routes.PREMIUM) },
                    onOpenTheme = { themeId ->
                        navController.safeNavigateWithAd(
                            context = context,
                            route = Routes.batteryEditor(themeId),
                            placementRoute = Routes.BATTERY_EDITOR
                        )
                    }
                )
            }

            composable(Routes.MY_PET) {
                fun openPetStore() {
                    // Leave the room before switching tabs. Food rewards stay inside My Pet Room.
                    returnToHomeTab(HomeTab.PET_STORE, PetStoreTab.PETS)
                }

                PetRoomScreen(
                    onNavigateBack = {
                        navController.safePopBackStackWithAd(
                            context = context,
                            currentRoute = Routes.MY_PET
                        )
                    },
                    onOpenPetStore = ::openPetStore,
                    onPremium = { navigateFromHome(Routes.PREMIUM) }
                )
            }

            composable(
                route = "${Routes.BATTERY_CATEGORY}/{categoryId}",
                arguments = listOf(navArgument("categoryId") { type = NavType.IntType })
            ) { backStackEntry ->
                val catalogEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Routes.HOME_GRAPH)
                }
                val catalogViewModel = hiltViewModel<BatteryCatalogViewModel>(catalogEntry)
                IsolatedDestination(
                    bottomAd = {
                        NativeAdInternal(
                            screenCode = SCREEN_BATTERY_CATEGORY,
                            reloadKey = destinationAdReloadKey(backStackEntry.id),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                ) {
                    BatteryCategoryScreen(
                        categoryId = backStackEntry.arguments?.getInt("categoryId") ?: 0,
                        onBack = {
                            navController.safePopBackStackWithAd(
                                context = context,
                                currentRoute = Routes.BATTERY_CATEGORY
                            )
                        },
                        onNavigateToPremium = {
                            navController.safeNavigateWithAd(
                                context = context,
                                route = "${Routes.PREMIUM}/" +
                                    StartPremiumIndexes.IN_APP.name,
                                placementRoute = Routes.PREMIUM
                            )
                        },
                        onOpenTheme = { themeId ->
                            navController.safeNavigateWithAd(
                                context = context,
                                route = Routes.batteryEditor(themeId),
                                placementRoute = Routes.BATTERY_EDITOR
                            )
                        },
                        accessibilityHowToUseResult =
                            backStackEntry.accessibilityHowToUseResult(),
                        onAccessibilityHowToUseResultConsumed =
                            backStackEntry::consumeAccessibilityHowToUseResult,
                        onNavigateToAccessibilityHowToUse = {
                            navigateToAccessibilityHowToUse(backStackEntry)
                        },
                        viewModel = catalogViewModel
                    )
                }
            }

            composable(
                route = "${Routes.BATTERY_EDITOR}/{themeId}?" +
                    "$BATTERY_EDITOR_INITIAL_BACKGROUND_ID_ARG=" +
                    "{$BATTERY_EDITOR_INITIAL_BACKGROUND_ID_ARG}",
                arguments = listOf(
                    navArgument("themeId") { type = NavType.IntType },
                    navArgument(BATTERY_EDITOR_INITIAL_BACKGROUND_ID_ARG) {
                        type = NavType.IntType
                        defaultValue = -1
                    }
                )
            ) { backStackEntry ->
                val themeId = backStackEntry.arguments?.getInt("themeId") ?: 0
                IsolatedDestination(
                    bottomAd = {
                        NativeAdInternal(
                            screenCode = SCREEN_CUSTOMIZE_STATUS_BAR,
                            reloadKey = destinationAdReloadKey(backStackEntry.id),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                ) {
                    BatteryEditorScreen(
                        page = BatteryEditorPage.OVERVIEW,
                        onBack = {
                            navController.safePopBackStackWithAd(
                                context = context,
                                currentRoute = Routes.BATTERY_EDITOR
                            )
                        },
                        onOpenPage = { page ->
                            navController.safeNavigateWithAd(
                                context = context,
                                route = Routes.batteryEditorComponent(themeId, page.name),
                                placementRoute = Routes.BATTERY_EDITOR_COMPONENT
                            )
                        },
                        onNavigateToPremium = {
                            navController.safeNavigateWithAd(
                                context = context,
                                route = "${Routes.PREMIUM}/" +
                                    StartPremiumIndexes.IN_APP.name,
                                placementRoute = Routes.PREMIUM
                            )
                        },
                        accessibilityHowToUseResult =
                            backStackEntry.accessibilityHowToUseResult(),
                        onAccessibilityHowToUseResultConsumed =
                            backStackEntry::consumeAccessibilityHowToUseResult,
                        onNavigateToAccessibilityHowToUse = {
                            navigateToAccessibilityHowToUse(backStackEntry)
                        }
                    )
                }
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
                val nativeScreenCode = batteryEditorCollapsibleNativeScreenCode(
                    backStackEntry.destination.route,
                    page.name
                )
                IsolatedDestination(
                    bottomAd = {
                        if (nativeScreenCode != null) {
                            NativeAdInternal(
                                screenCode = nativeScreenCode,
                                reloadKey = destinationAdReloadKey(backStackEntry.id),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            BannerAd(
                                modifier = Modifier.fillMaxWidth(),
                                adPosition = BANNER_BATTERY_EDITOR_BOTTOM
                            )
                        }
                    }
                ) {
                    BatteryEditorScreen(
                        page = page,
                        onBack = {
                            navController.safePopBackStackWithAd(
                                context = context,
                                currentRoute = Routes.BATTERY_EDITOR_COMPONENT
                            )
                        },
                        onNavigateToPremium = {
                            navController.safeNavigateWithAd(
                                context = context,
                                route = "${Routes.PREMIUM}/" +
                                    StartPremiumIndexes.IN_APP.name,
                                placementRoute = Routes.PREMIUM
                            )
                        },
                        onOpenEmotionGroup = { groupKey ->
                            val themeId = backStackEntry.arguments?.getInt("themeId") ?: 0
                            navController.safeNavigateWithAd(
                                context = context,
                                route = Routes.batteryEditorEmotionDetail(themeId, groupKey),
                                placementRoute = Routes.BATTERY_EDITOR_EMOTION_DETAIL
                            )
                        },
                        accessibilityHowToUseResult =
                            backStackEntry.accessibilityHowToUseResult(),
                        onAccessibilityHowToUseResultConsumed =
                            backStackEntry::consumeAccessibilityHowToUseResult,
                        onNavigateToAccessibilityHowToUse = {
                            navigateToAccessibilityHowToUse(backStackEntry)
                        },
                        viewModel = editorViewModel
                    )
                }
            }

            composable(
                route = "${Routes.BATTERY_EDITOR_EMOTION_DETAIL}/{themeId}/{groupKey}",
                arguments = listOf(
                    navArgument("themeId") { type = NavType.IntType },
                    navArgument("groupKey") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val themeId = backStackEntry.arguments?.getInt("themeId") ?: 0
                val overviewEntry = remember(backStackEntry, themeId) {
                    navController.getBackStackEntry(Routes.batteryEditor(themeId))
                }
                IsolatedDestination(
                    bottomAd = {
                        NativeAdInternal(
                            screenCode = SCREEN_BATTERY_EDITOR,
                            reloadKey = destinationAdReloadKey(backStackEntry.id),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                ) {
                    BatteryEditorScreen(
                        page = BatteryEditorPage.EMOTION_DETAIL,
                        emotionGroupKey = backStackEntry.arguments?.getString("groupKey"),
                        onBack = {
                            navController.safePopBackStackWithAd(
                                context = context,
                                currentRoute = Routes.BATTERY_EDITOR_EMOTION_DETAIL
                            )
                        },
                        onNavigateToPremium = {
                            navController.safeNavigateWithAd(
                                context = context,
                                route = "${Routes.PREMIUM}/" +
                                    StartPremiumIndexes.IN_APP.name,
                                placementRoute = Routes.PREMIUM
                            )
                        },
                        accessibilityHowToUseResult =
                            backStackEntry.accessibilityHowToUseResult(),
                        onAccessibilityHowToUseResultConsumed =
                            backStackEntry::consumeAccessibilityHowToUseResult,
                        onNavigateToAccessibilityHowToUse = {
                            navigateToAccessibilityHowToUse(backStackEntry)
                        },
                        viewModel = hiltViewModel<BatteryEditorViewModel>(overviewEntry)
                    )
                }
            }

            composable(Routes.BATTERY_TROLL) { backStackEntry ->
                IsolatedDestination(
                    bottomAd = {
                        NativeAdInternal(
                            screenCode = SCREEN_BATTERY_TROLL,
                            reloadKey = destinationAdReloadKey(backStackEntry.id),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                ) {
                    BatteryTrollScreen(
                        onNavigateBack = {
                            navController.safePopBackStackWithAd(
                                context = context,
                                currentRoute = Routes.BATTERY_TROLL
                            )
                        },
                        onNavigateToCustomize = { trollId ->
                            navController.safeNavigateWithAd(
                                context = context,
                                route = Routes.batteryTrollCustomize(trollId),
                                placementRoute = Routes.BATTERY_TROLL_CUSTOMIZE
                            )
                        },
                        onPremium = { navigateFromHome(Routes.PREMIUM) }
                    )
                }
            }

            composable(
                route = "${Routes.BATTERY_TROLL_CUSTOMIZE}/{trollId}",
                arguments = listOf(navArgument("trollId") { type = NavType.IntType })
            ) { backStackEntry ->
                IsolatedDestination(
                    bottomAd = {
                        BannerAd(
                            modifier = Modifier.fillMaxWidth(),
                            adPosition = BANNER_BATTERY_EDITOR_BOTTOM
                        )
                    }
                ) {
                    BatteryTrollCustomizeScreen(
                        onNavigateBack = {
                            navController.safePopBackStackWithAd(
                                context = context,
                                currentRoute = Routes.BATTERY_TROLL_CUSTOMIZE
                            )
                        },
                        accessibilityHowToUseResult =
                            backStackEntry.accessibilityHowToUseResult(),
                        onAccessibilityHowToUseResultConsumed =
                            backStackEntry::consumeAccessibilityHowToUseResult,
                        onNavigateToAccessibilityHowToUse = {
                            navigateToAccessibilityHowToUse(backStackEntry)
                        }
                    )
                }
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
                            navController.safeNavigate(
                                destinationAfterIntro(),
                                ignoreDebounce = true
                            ) {
                                popUpTo(Routes.PREMIUM) { inclusive = true }
                            }
                        }

                        StartPremiumIndexes.SPLASH_RETURN -> {
                            navController.safeNavigate(Routes.HOME_GRAPH, ignoreDebounce = true) {
                                popUpTo(Routes.PREMIUM) { inclusive = true }
                            }
                        }

                        else -> navController.safePopBackStack(ignoreDebounce = true)
                    }
                }

                PremiumScreen(
                    startByIndex = startByIndex,
                    onClose = {
                        navigateWithAd(
                            context = context,
                            placement = navigationAdPlacement(
                                Routes.PREMIUM,
                                NavigationAdDirection.BACK
                            ),
                            onNavigate = ::closePremium
                        )
                    },
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
