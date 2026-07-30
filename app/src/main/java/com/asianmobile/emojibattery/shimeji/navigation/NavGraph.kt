package com.asianmobile.emojibattery.shimeji.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.asianmobile.emojibattery.shimeji.ads.utils.SafeRemoteConfig
import com.asianmobile.emojibattery.shimeji.ui.home.HomeScreen
import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.BatteryCatalogScreen
import com.asianmobile.emojibattery.shimeji.ui.battery.editor.BatteryEditorScreen
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
import com.asianmobile.emojibattery.shimeji.ui.premium.StartPremiumIndexes
import com.asianmobile.emojibattery.shimeji.ui.splash.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val LANGUAGE = "language"
    const val LANGUAGE_SETTINGS = "language_settings"
    const val INTRO = "intro"
    const val PERMISSION = "permission"
    const val HOME = "home"
    const val PET_CATALOG = "pet_catalog"
    const val PET_DETAIL = "pet_detail"
    const val PET_CUSTOMIZATION = "pet_customization"
    const val SWARM_CUSTOMIZATION = "swarm_customization"
    const val SETTINGS = "settings"
    const val BATTERY_CATALOG = "battery_catalog"
    const val BATTERY_EDITOR = "battery_editor"
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

            composable(Routes.HOME) {
                HomeScreen(
                    onNavigateToCatalog = { target, slotIndex ->
                        navController.safeNavigate(
                            Routes.petCatalog(target, slotIndex),
                            ignoreDebounce = true
                        )
                    },
                    onNavigateToBattery = {
                        navController.safeNavigate(
                            Routes.BATTERY_CATALOG,
                            ignoreDebounce = true
                        )
                    },
                    onNavigateToSettings = {
                        navigateFromHome(Routes.SETTINGS)
                    },
                    onNavigateToPremium = {
                        navigateFromHome(Routes.PREMIUM)
                    },
                    onNavigateToSwarmCustomization = {
                        navController.safeNavigate(
                            Routes.SWARM_CUSTOMIZATION,
                            ignoreDebounce = true
                        )
                    }
                )
            }

            composable(Routes.BATTERY_CATALOG) {
                BatteryCatalogScreen(
                    onBack = { navController.safePopBackStack(ignoreDebounce = true) },
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
            ) {
                BatteryEditorScreen(
                    onBack = { navController.safePopBackStack(ignoreDebounce = true) }
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
                    onBack = {
                        navigateWithAd(context, Routes.HOME) {
                            navController.safePopBackStack(ignoreDebounce = true)
                        }
                    },
                    onNavigateToLanguage = {
                        navigateFromHome(Routes.LANGUAGE_SETTINGS)
                    },
                    onNavigateToPetCustomization = { slotIndex ->
                        navController.safeNavigate(
                            Routes.petCustomization(slotIndex),
                            ignoreDebounce = true
                        )
                    },
                    onAddPet = { slotIndex ->
                        navController.safeNavigate(
                            Routes.petCatalog(PetCatalogTarget.MIXED, slotIndex),
                            ignoreDebounce = true
                        )
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
}
