package com.asianmobile.emojibattery.shimeji.ui.battery.catalog

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ads.ui.rewarded.RewardedAdResult
import com.asianmobile.emojibattery.shimeji.ads.ui.rewarded.RewardedVideoAds
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryAccessibility
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntitlement
import com.asianmobile.emojibattery.shimeji.ui.component.GrantPermissionDialog
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView

@Composable
fun BatteryCatalogScreen(
    onSearch: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onOpenCategory: (Int) -> Unit,
    onOpenTheme: (Int) -> Unit,
    viewModel: BatteryCatalogViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TrackScreenView(ScreenName.BATTERY_CATALOG)
    BatteryCatalogFlowHost(
        state = state,
        viewModel = viewModel,
        onOpenTheme = onOpenTheme,
        onNavigateToPremium = onNavigateToPremium
    ) {
        BatteryCatalogContent(
            state = state,
            onSearch = onSearch,
            onPremium = onNavigateToPremium,
            onBatteryToggle = viewModel::onBatteryToggle,
            onCustomizeStatusBar = viewModel::requestCurrentStyle,
            onOpenCategory = onOpenCategory,
            onFavorite = viewModel::toggleFavorite,
            onTheme = viewModel::requestTheme,
            onRetry = viewModel::refresh
        )
    }
}

@Composable
internal fun BatteryCatalogFlowHost(
    state: BatteryCatalogUiState,
    viewModel: BatteryCatalogViewModel,
    onOpenTheme: (Int) -> Unit,
    onNavigateToPremium: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var pendingAccessibilityThemeId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showAccessibilityDisclosure by rememberSaveable { mutableStateOf(false) }

    val continueToEditorIfAllowed = {
        val pendingThemeId = pendingAccessibilityThemeId
        if (pendingThemeId != null && BatteryAccessibility.isEnabled(context)) {
            pendingAccessibilityThemeId = null
            showAccessibilityDisclosure = false
            onOpenTheme(pendingThemeId)
        }
    }
    val accessibilityLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshAccessibility()
        continueToEditorIfAllowed()
    }
    val requiresRewardAd = !state.isPremium && state.themes.any { theme ->
        theme.assetsReady &&
            theme.entitlement == BatteryThemeEntitlement.PREMIUM &&
            theme.id !in state.rewardUnlockedThemeIds
    }

    LaunchedEffect(context, requiresRewardAd) {
        if (requiresRewardAd) {
            RewardedVideoAds.getInstance().loadRewardedVideo(context.applicationContext)
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BatteryCatalogEffect.OpenTheme -> {
                    if (BatteryAccessibility.isEnabled(context)) {
                        onOpenTheme(effect.themeId)
                    } else {
                        pendingAccessibilityThemeId = effect.themeId
                        showAccessibilityDisclosure = true
                    }
                }

                BatteryCatalogEffect.ShowRewardedAd -> {
                    val activity = context as? Activity
                    if (activity == null) {
                        viewModel.onRewardResult(RewardedAdResult.UNAVAILABLE.shouldContinueFlow)
                    } else {
                        RewardedVideoAds.getInstance().showRewardedAd(activity) { result ->
                            viewModel.onRewardResult(result.shouldContinueFlow)
                        }
                    }
                }

                BatteryCatalogEffect.RequestBatteryAccessibility -> {
                    pendingAccessibilityThemeId = null
                    showAccessibilityDisclosure = true
                }
            }
        }
    }
    DisposableEffect(lifecycleOwner, pendingAccessibilityThemeId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshEntitlement()
                viewModel.refreshAccessibility()
                continueToEditorIfAllowed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    content()

    if (showAccessibilityDisclosure) {
        GrantPermissionDialog(
            onGrantPermission = {
                showAccessibilityDisclosure = false
                accessibilityLauncher.launch(BatteryAccessibility.settingsIntent())
            },
            onMaybeLater = {
                showAccessibilityDisclosure = false
                pendingAccessibilityThemeId = null
                viewModel.cancelPendingBatteryEnable()
            }
        )
    }

    val pendingTheme = state.themes.firstOrNull { it.id == state.pendingUnlockThemeId }
    if (pendingTheme != null) {
        BatteryRewardUnlockSheet(
            theme = pendingTheme,
            isLoading = state.isRewardInProgress,
            rewardNotEarned = state.message == BatteryCatalogMessage.REWARD_NOT_EARNED,
            onDismiss = viewModel::dismissUnlockDialog,
            onWatchReward = viewModel::requestRewardUnlock,
            onPremium = onNavigateToPremium
        )
    } else if (state.message == BatteryCatalogMessage.THEME_UNAVAILABLE) {
        AlertDialog(
            onDismissRequest = viewModel::clearMessage,
            title = { Text(stringResource(R.string.battery_theme_unavailable_title)) },
            text = { Text(stringResource(R.string.battery_theme_unavailable_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::clearMessage) {
                    Text(stringResource(R.string.common_done))
                }
            }
        )
    }
}
