package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import android.app.Activity
import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ads.ui.rewarded.RewardedAdResult
import com.asianmobile.emojibattery.shimeji.ads.ui.rewarded.RewardedVideoAds
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryAccessibility
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryConnectivityState
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryHotspotState
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryRingerState
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryStatusComponent
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryStatusLayoutItem
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryStatusLayoutPolicy
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatterySystemStatusPolicy
import com.asianmobile.emojibattery.shimeji.data.model.BUILT_IN_BATTERY_CATEGORY_ID
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDecorationEntry
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDataType
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDateFont
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDateFormat
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntitlement
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry
import com.asianmobile.emojibattery.shimeji.data.model.MAX_BATTERY_STATUS_ICON_STYLE_INDEX
import com.asianmobile.emojibattery.shimeji.ui.component.AppSwitch
import com.asianmobile.emojibattery.shimeji.ui.component.CutePetTopBar
import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.BatteryRewardUnlockSheet
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private const val ITEM_LOADING_INDICATOR_DELAY_MS = 180L
private val WIFI_ICON_STYLES = (1..MAX_BATTERY_STATUS_ICON_STYLE_INDEX).map { style ->
    listOf(
        BatterySystemStatusPolicy.wifiIcon(BatteryConnectivityState.CONNECTED, style)
    )
}
private val SIGNAL_ICON_STYLES = (1..MAX_BATTERY_STATUS_ICON_STYLE_INDEX).map { style ->
    listOf(
        BatterySystemStatusPolicy.cellularIcon(BatteryConnectivityState.CONNECTED, style)
    )
}
private val AIRPLANE_ICON_STYLES = (1..MAX_BATTERY_STATUS_ICON_STYLE_INDEX).map { style ->
    listOf(BatterySystemStatusPolicy.airplaneIcon(style))
}
private val HOTSPOT_ICON_STYLES = (1..MAX_BATTERY_STATUS_ICON_STYLE_INDEX).map { style ->
    listOfNotNull(
        BatterySystemStatusPolicy.hotspotIcon(BatteryHotspotState.ENABLED, style)
    )
}
private val RINGER_ICON_STYLES = (1..MAX_BATTERY_STATUS_ICON_STYLE_INDEX).map { style ->
    listOfNotNull(
        BatterySystemStatusPolicy.ringerIcon(BatteryRingerState.VIBRATE, style),
        BatterySystemStatusPolicy.ringerIcon(BatteryRingerState.SILENT, style)
    )
}

internal enum class BatteryEditorPage {
    OVERVIEW,
    SIZE,
    APPEARANCE,
    EMOJI,
    BATTERY,
    ANIMATION,
    WIFI,
    DATA,
    SIGNAL,
    AIRPLANE,
    HOTSPOT,
    RINGER,
    CHARGE,
    DATE_TIME;

    companion object {
        fun fromRoute(value: String?): BatteryEditorPage? =
            entries.firstOrNull { page ->
                page != OVERVIEW && page.name == value
            }
    }
}

@Composable
internal fun BatteryEditorScreen(
    page: BatteryEditorPage = BatteryEditorPage.OVERVIEW,
    onBack: () -> Unit,
    onOpenPage: (BatteryEditorPage) -> Unit = {},
    onNavigateToPremium: () -> Unit,
    viewModel: BatteryEditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showDisclosure by remember { mutableStateOf(false) }
    var showDiscardConfirmation by rememberSaveable { mutableStateOf(false) }
    val requestBack = {
        if (page == BatteryEditorPage.OVERVIEW && state.hasUnsavedChanges) {
            showDiscardConfirmation = true
        } else {
            onBack()
        }
    }
    var accessibilityEnabled by remember {
        mutableStateOf(BatteryAccessibility.isEnabled(context))
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        accessibilityEnabled = BatteryAccessibility.isEnabled(context)
        if (accessibilityEnabled) viewModel.apply()
    }
    val requiresRewardAd = !state.isPremium && state.themes.any { theme ->
        theme.assetsReady &&
            theme.entitlement == BatteryThemeEntitlement.PREMIUM &&
            theme.id !in state.config.rewardUnlockedThemeIds
    }

    TrackScreenView(page.analyticsScreen())
    LaunchedEffect(context, requiresRewardAd) {
        if (requiresRewardAd) {
            RewardedVideoAds.getInstance().loadRewardedVideo(context.applicationContext)
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                BatteryEditorEffect.ShowRewardedAd -> {
                    val activity = context as? Activity
                    if (activity == null) {
                        viewModel.onRewardResult(
                            RewardedAdResult.UNAVAILABLE.shouldContinueFlow
                        )
                    } else {
                        RewardedVideoAds.getInstance().showRewardedAd(activity) { result ->
                            viewModel.onRewardResult(result.shouldContinueFlow)
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(page) {
        viewModel.setPreviewComponent(page.previewComponent())
    }
    BackHandler(
        enabled = page == BatteryEditorPage.OVERVIEW && state.hasUnsavedChanges,
        onBack = requestBack
    )
    DisposableEffect(viewModel) {
        viewModel.startPreview()
        onDispose { viewModel.stopPreview() }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityEnabled = BatteryAccessibility.isEnabled(context)
                viewModel.refreshEntitlement()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BatteryEditorContent(
        state = state,
        page = page,
        accessibilityEnabled = accessibilityEnabled,
        onBack = requestBack,
        onDone = onBack,
        onOpenPage = onOpenPage,
        onShowTime = viewModel::setShowTime,
        onShowPercentage = viewModel::setShowPercentage,
        onBarHeight = viewModel::setBarHeight,
        onEmojiSize = viewModel::setEmojiSize,
        onBatterySize = viewModel::setBatterySize,
        onBackgroundColor = viewModel::setBackgroundColor,
        onForegroundColor = viewModel::setForegroundColor,
        onBackgroundDecoration = viewModel::setBackgroundDecoration,
        onShowEmotion = viewModel::setShowEmotion,
        onEmotionDecoration = viewModel::setEmotionDecoration,
        onSelectTheme = viewModel::requestTheme,
        onConfig = viewModel::setConfig,
        onApply = {
            if (accessibilityEnabled) viewModel.apply() else showDisclosure = true
        },
        onDisable = viewModel::disable
    )

    val pendingTheme = state.themes.firstOrNull {
        it.id == state.pendingSelection?.themeId
    }
    if (pendingTheme != null) {
        BatteryRewardUnlockSheet(
            theme = pendingTheme,
            isLoading = state.isRewardInProgress,
            rewardNotEarned = state.message == BatteryEditorMessage.REWARD_NOT_EARNED,
            onDismiss = viewModel::dismissUnlockDialog,
            onWatchReward = viewModel::requestRewardUnlock,
            onPremium = onNavigateToPremium
        )
    } else if (
        state.message == BatteryEditorMessage.THEME_UNAVAILABLE ||
        state.message == BatteryEditorMessage.ASSET_DOWNLOAD_FAILED
    ) {
        val assetDownloadFailed =
            state.message == BatteryEditorMessage.ASSET_DOWNLOAD_FAILED
        AlertDialog(
            onDismissRequest = viewModel::clearMessage,
            title = {
                Text(
                    stringResource(
                        if (assetDownloadFailed) {
                            R.string.battery_asset_download_failed_title
                        } else {
                            R.string.battery_theme_unavailable_title
                        }
                    )
                )
            },
            text = {
                Text(
                    stringResource(
                        if (assetDownloadFailed) {
                            R.string.battery_asset_download_failed_message
                        } else {
                            R.string.battery_theme_unavailable_message
                        }
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::clearMessage) {
                    Text(stringResource(R.string.common_done))
                }
            }
        )
    }

    if (showDisclosure) {
        AlertDialog(
            onDismissRequest = { showDisclosure = false },
            title = { Text(stringResource(R.string.battery_accessibility_title)) },
            text = { Text(stringResource(R.string.battery_accessibility_disclosure)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDisclosure = false
                        (context as? Activity)?.let {
                            launcher.launch(BatteryAccessibility.settingsIntent())
                        }
                    }
                ) {
                    Text(stringResource(R.string.battery_open_accessibility_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisclosure = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text(stringResource(R.string.battery_discard_title)) },
            text = { Text(stringResource(R.string.battery_discard_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirmation = false
                        viewModel.discardDraft()
                        onBack()
                    }
                ) {
                    Text(stringResource(R.string.battery_discard_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmation = false }) {
                    Text(stringResource(R.string.battery_discard_keep_editing))
                }
            }
        )
    }
}

@Composable
private fun BatteryEditorContent(
    state: BatteryEditorUiState,
    page: BatteryEditorPage,
    accessibilityEnabled: Boolean,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onOpenPage: (BatteryEditorPage) -> Unit,
    onShowTime: (Boolean) -> Unit,
    onShowPercentage: (Boolean) -> Unit,
    onBarHeight: (Float) -> Unit,
    onEmojiSize: (Float) -> Unit,
    onBatterySize: (Float) -> Unit,
    onBackgroundColor: (Int) -> Unit,
    onForegroundColor: (Int) -> Unit,
    onBackgroundDecoration: (Int) -> Unit,
    onShowEmotion: (Boolean) -> Unit,
    onEmotionDecoration: (Int) -> Unit,
    onSelectTheme: (BatteryThemeEntry, BatteryThemeComponent) -> Unit,
    onConfig: (BatteryStatusConfig) -> Unit,
    onApply: () -> Unit,
    onDisable: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_FFF9F4))
            .navigationBarsPadding()
    ) {
        CutePetTopBar(
            title = editorPageTitle(page),
            onBack = onBack,
            trailing = {
                if (page != BatteryEditorPage.OVERVIEW) {
                    TextButton(onClick = onDone) {
                        Text(
                            text = stringResource(R.string.common_done),
                            color = colorResource(R.color.colors_12B890),
                            fontFamily = FontFamily(Font(R.font.inter_semibold))
                        )
                    }
                }
            }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = dimensionResource(SdpR.dimen._16sdp))
        ) {
            when (page) {
                BatteryEditorPage.OVERVIEW -> OverviewEditor(
                    state = state,
                    onOpenPage = onOpenPage,
                    onShowTime = onShowTime,
                    onShowPercentage = onShowPercentage,
                    onSelectTheme = onSelectTheme
                )
                BatteryEditorPage.SIZE -> SizeEditor(
                    state = state,
                    onBarHeight = onBarHeight,
                    onEmojiSize = onEmojiSize,
                    onBatterySize = onBatterySize,
                    onConfig = onConfig
                )
                BatteryEditorPage.APPEARANCE -> AppearanceEditor(
                    state = state,
                    onBackgroundColor = onBackgroundColor,
                    onForegroundColor = onForegroundColor,
                    onBackgroundDecoration = onBackgroundDecoration
                )
                BatteryEditorPage.EMOJI -> EmojiEditor(
                    state = state,
                    onShowEmotion = onShowEmotion,
                    onEmotionDecoration = onEmotionDecoration
                )
                BatteryEditorPage.BATTERY -> BatteryComponentEditor(
                    state = state,
                    onShowPercentage = onShowPercentage,
                    onBatterySize = onBatterySize,
                    onConfig = onConfig
                )
                BatteryEditorPage.ANIMATION -> AnimationEditor(state, onConfig)
                BatteryEditorPage.WIFI -> StatusComponentEditor(
                    state.config,
                    state.config.wifiSizeDp,
                    state.config.wifiColorArgb,
                    state.config.wifiIconStyleIndex,
                    WIFI_ICON_STYLES,
                    { onConfig(state.config.copy(wifiSizeDp = it)) },
                    { onConfig(state.config.copy(wifiColorArgb = it)) },
                    { onConfig(state.config.copy(wifiIconStyleIndex = it)) }
                )
                BatteryEditorPage.DATA -> DataEditor(state.config, onConfig)
                BatteryEditorPage.SIGNAL -> StatusComponentEditor(
                    state.config,
                    state.config.signalSizeDp,
                    state.config.signalColorArgb,
                    state.config.signalIconStyleIndex,
                    SIGNAL_ICON_STYLES,
                    { onConfig(state.config.copy(signalSizeDp = it)) },
                    { onConfig(state.config.copy(signalColorArgb = it)) },
                    { onConfig(state.config.copy(signalIconStyleIndex = it)) }
                )
                BatteryEditorPage.AIRPLANE -> StatusComponentEditor(
                    state.config,
                    state.config.airplaneSizeDp,
                    state.config.airplaneColorArgb,
                    state.config.airplaneIconStyleIndex,
                    AIRPLANE_ICON_STYLES,
                    { onConfig(state.config.copy(airplaneSizeDp = it)) },
                    { onConfig(state.config.copy(airplaneColorArgb = it)) },
                    { onConfig(state.config.copy(airplaneIconStyleIndex = it)) }
                )
                BatteryEditorPage.HOTSPOT -> StatusComponentEditor(
                    state.config,
                    state.config.hotspotSizeDp,
                    state.config.hotspotColorArgb,
                    state.config.hotspotIconStyleIndex,
                    HOTSPOT_ICON_STYLES,
                    { onConfig(state.config.copy(hotspotSizeDp = it)) },
                    { onConfig(state.config.copy(hotspotColorArgb = it)) },
                    { onConfig(state.config.copy(hotspotIconStyleIndex = it)) }
                )
                BatteryEditorPage.RINGER -> StatusComponentEditor(
                    state.config,
                    state.config.ringerSizeDp,
                    state.config.ringerColorArgb,
                    state.config.ringerIconStyleIndex,
                    RINGER_ICON_STYLES,
                    { onConfig(state.config.copy(ringerSizeDp = it)) },
                    { onConfig(state.config.copy(ringerColorArgb = it)) },
                    { onConfig(state.config.copy(ringerIconStyleIndex = it)) }
                )
                BatteryEditorPage.CHARGE -> ChargeEditor(state.config, onConfig)
                BatteryEditorPage.DATE_TIME -> DateTimeEditor(state.config, onConfig)
            }
            if (page == BatteryEditorPage.OVERVIEW) {
                Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
                AccessibilityState(accessibilityEnabled)
                Spacer(Modifier.height(dimensionResource(SdpR.dimen._14sdp)))
            } else {
                Spacer(Modifier.height(dimensionResource(SdpR.dimen._24sdp)))
            }
        }
        if (page == BatteryEditorPage.OVERVIEW) {
            ApplyFooter(
                enabled = state.isThemeAvailable,
                selectionInProgress = state.assetSelectionInProgress != null,
                isApplied = state.config.enabled,
                onApply = onApply,
                onDisable = onDisable
            )
        }
    }
}

@Composable
private fun editorPageTitle(page: BatteryEditorPage): String = when (page) {
    BatteryEditorPage.OVERVIEW -> stringResource(R.string.battery_editor_title)
    BatteryEditorPage.SIZE -> stringResource(R.string.battery_editor_size_title)
    BatteryEditorPage.APPEARANCE -> stringResource(R.string.battery_editor_appearance_title)
    BatteryEditorPage.EMOJI -> stringResource(R.string.battery_editor_emoji_title)
    BatteryEditorPage.BATTERY -> stringResource(R.string.battery_editor_battery_title)
    BatteryEditorPage.ANIMATION -> stringResource(R.string.battery_component_animation)
    BatteryEditorPage.WIFI -> stringResource(R.string.battery_component_wifi)
    BatteryEditorPage.DATA -> stringResource(R.string.battery_component_data)
    BatteryEditorPage.SIGNAL -> stringResource(R.string.battery_component_signal)
    BatteryEditorPage.AIRPLANE -> stringResource(R.string.battery_component_airplane)
    BatteryEditorPage.HOTSPOT -> stringResource(R.string.battery_component_hotspot)
    BatteryEditorPage.RINGER -> stringResource(R.string.battery_component_ringer)
    BatteryEditorPage.CHARGE -> stringResource(R.string.battery_component_charge)
    BatteryEditorPage.DATE_TIME -> stringResource(R.string.battery_component_date)
}

@Composable
private fun OverviewEditor(
    state: BatteryEditorUiState,
    onOpenPage: (BatteryEditorPage) -> Unit,
    onShowTime: (Boolean) -> Unit,
    onShowPercentage: (Boolean) -> Unit,
    onSelectTheme: (BatteryThemeEntry, BatteryThemeComponent) -> Unit
) {
    ThemeComponentPicker(
        title = stringResource(R.string.battery_editor_pet_picker),
        component = BatteryThemeComponent.EMOJI,
        state = state,
        selectedThemeId = state.config.selectedEmojiThemeId,
        onSelectTheme = onSelectTheme
    )
    Spacer(Modifier.height(dimensionResource(SdpR.dimen._16sdp)))
    ThemeComponentPicker(
        title = stringResource(R.string.battery_editor_battery_picker),
        component = BatteryThemeComponent.BATTERY,
        state = state,
        selectedThemeId = state.config.selectedBatteryThemeId,
        onSelectTheme = onSelectTheme
    )
    Spacer(Modifier.height(dimensionResource(SdpR.dimen._16sdp)))
    Text(
        text = stringResource(R.string.battery_editor_overview_hint),
        color = colorResource(R.color.colors_776D84),
        fontFamily = FontFamily(Font(R.font.inter_regular)),
        fontSize = dimensionResource(SspR.dimen._9ssp).value.sp
    )
    Spacer(Modifier.height(dimensionResource(SdpR.dimen._14sdp)))
    EditorSectionTitle(stringResource(R.string.battery_editor_quick_controls))
    Spacer(Modifier.height(dimensionResource(SdpR.dimen._8sdp)))
    EditorCard {
        ToggleRow(
            label = stringResource(R.string.battery_show_time),
            checked = state.config.showTime,
            onChecked = onShowTime
        )
        HorizontalDivider(color = colorResource(R.color.colors_E9DFEF))
        ToggleRow(
            label = stringResource(R.string.battery_show_percentage),
            checked = state.config.showPercentage,
            onChecked = onShowPercentage
        )
    }
    Spacer(Modifier.height(dimensionResource(SdpR.dimen._16sdp)))
    EditorSectionTitle(stringResource(R.string.battery_editor_customize))
    Spacer(Modifier.height(dimensionResource(SdpR.dimen._8sdp)))
    EditorCard {
        EditorNavigationRow(
            iconRes = R.drawable.ic_pet_size,
            title = stringResource(R.string.battery_editor_size_title),
            summary = stringResource(
                R.string.battery_editor_size_summary,
                state.config.barHeightDp.toInt(),
                state.config.emojiSizeDp.toInt(),
                state.config.batterySizeDp.toInt()
            ),
            onClick = { onOpenPage(BatteryEditorPage.SIZE) }
        )
        EditorDivider()
        EditorNavigationRow(
            iconRes = R.drawable.ic_menu_settings,
            title = stringResource(R.string.battery_editor_appearance_title),
            summary = selectedBackgroundName(state),
            onClick = { onOpenPage(BatteryEditorPage.APPEARANCE) }
        )
        EditorDivider()
        EditorNavigationRow(
            iconRes = R.drawable.ic_notification_pet,
            title = stringResource(R.string.battery_editor_emoji_title),
            summary = if (state.config.showEmotion) {
                stringResource(R.string.battery_editor_component_on)
            } else {
                stringResource(R.string.battery_editor_component_off)
            },
            onClick = { onOpenPage(BatteryEditorPage.EMOJI) }
        )
        EditorDivider()
        EditorNavigationRow(
            iconRes = R.drawable.ic_battery_status,
            title = stringResource(R.string.battery_editor_battery_title),
            summary = if (state.config.showPercentage) {
                stringResource(R.string.battery_editor_percentage_on)
            } else {
                stringResource(R.string.battery_editor_percentage_off)
            },
            onClick = { onOpenPage(BatteryEditorPage.BATTERY) }
        )
    }
    Spacer(Modifier.height(dimensionResource(SdpR.dimen._16sdp)))
    EditorSectionTitle(stringResource(R.string.battery_editor_more_components))
    Spacer(Modifier.height(dimensionResource(SdpR.dimen._4sdp)))
    Text(
        text = stringResource(R.string.battery_editor_more_components_hint),
        color = colorResource(R.color.colors_776D84),
        fontSize = dimensionResource(SspR.dimen._8ssp).value.sp
    )
    Spacer(Modifier.height(dimensionResource(SdpR.dimen._8sdp)))
    StatusComponentsGrid(onOpenPage)
}

@Composable
private fun ThemeComponentPicker(
    title: String,
    component: BatteryThemeComponent,
    state: BatteryEditorUiState,
    selectedThemeId: Int,
    onSelectTheme: (BatteryThemeEntry, BatteryThemeComponent) -> Unit
) {
    val initialCategoryId = state.themes
        .firstOrNull { it.id == selectedThemeId }
        ?.categoryId
    var selectedCategoryId by rememberSaveable(component.name) {
        mutableStateOf<Int?>(null)
    }
    LaunchedEffect(initialCategoryId) {
        if (initialCategoryId != null) selectedCategoryId = initialCategoryId
    }
    val visibleThemes = state.themes.filter { theme ->
        selectedCategoryId == null || theme.categoryId == selectedCategoryId
    }
    EditorSectionTitle(title)
    Spacer(Modifier.height(dimensionResource(SdpR.dimen._8sdp)))
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._7sdp))
    ) {
        item {
            ThemeCategoryChip(
                label = stringResource(R.string.battery_catalog_all),
                selected = selectedCategoryId == null,
                enabled = state.assetSelectionInProgress == null,
                onClick = { selectedCategoryId = null }
            )
        }
        items(state.categories, key = { it.id }) { category ->
            ThemeCategoryChip(
                label = if (category.id == BUILT_IN_BATTERY_CATEGORY_ID) {
                    stringResource(R.string.battery_builtin_category)
                } else {
                    category.name
                },
                selected = selectedCategoryId == category.id,
                enabled = state.assetSelectionInProgress == null,
                onClick = { selectedCategoryId = category.id }
            )
        }
    }
    Spacer(Modifier.height(dimensionResource(SdpR.dimen._8sdp)))
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._8sdp))
    ) {
        items(visibleThemes, key = { it.id }) { theme ->
            val locked = theme.entitlement == BatteryThemeEntitlement.PREMIUM &&
                !state.isPremium &&
                theme.id !in state.config.rewardUnlockedThemeIds
            ThemeComponentOption(
                theme = theme,
                component = component,
                selected = theme.id == selectedThemeId,
                locked = locked,
                loading = state.assetSelectionInProgress ==
                    BatteryEditorThemeSelection(theme.id, component),
                enabled = state.assetSelectionInProgress == null,
                onClick = { onSelectTheme(theme, component) }
            )
        }
    }
}

@Composable
private fun ThemeCategoryChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = label,
        color = colorResource(
            if (selected) R.color.colors_FFFFFF else R.color.colors_776D84
        ),
        fontFamily = FontFamily(Font(R.font.inter_semibold)),
        fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._14sdp)))
            .background(
                colorResource(
                    if (selected) R.color.colors_12B890 else R.color.colors_FFFFFF
                )
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                horizontal = dimensionResource(SdpR.dimen._11sdp),
                vertical = dimensionResource(SdpR.dimen._7sdp)
            )
    )
}

@Composable
private fun ThemeComponentOption(
    theme: BatteryThemeEntry,
    component: BatteryThemeComponent,
    selected: Boolean,
    locked: Boolean,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val assetPath = when (component) {
        BatteryThemeComponent.EMOJI -> theme.emojiPath
        BatteryThemeComponent.BATTERY -> theme.batteryPath
    }
    val loadingLabel = stringResource(R.string.battery_asset_loading)
    var showLoadingOverlay by remember(theme.id, component) { mutableStateOf(false) }
    LaunchedEffect(loading) {
        if (loading) {
            delay(ITEM_LOADING_INDICATOR_DELAY_MS)
            showLoadingOverlay = true
        } else {
            showLoadingOverlay = false
        }
    }
    Box(
        modifier = Modifier
            .width(dimensionResource(SdpR.dimen._86sdp))
            .semantics {
                if (loading) stateDescription = loadingLabel
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._14sdp)))
                .background(colorResource(R.color.colors_FFFFFF))
                .border(
                    width = dimensionResource(
                        if (selected) SdpR.dimen._2sdp else SdpR.dimen._1sdp
                    ),
                    color = colorResource(
                        if (selected) R.color.colors_12B890 else R.color.colors_C8C8C9
                    ),
                    shape = RoundedCornerShape(dimensionResource(SdpR.dimen._14sdp))
                )
                .clickable(enabled = enabled, onClick = onClick)
                .padding(dimensionResource(SdpR.dimen._5sdp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(SdpR.dimen._58sdp)),
                contentAlignment = Alignment.Center
            ) {
                if (assetPath != null) {
                    SubcomposeAsyncImage(
                        model = assetPath,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(dimensionResource(SdpR.dimen._48sdp))
                    ) {
                        if (painter.state is AsyncImagePainter.State.Success) {
                            SubcomposeAsyncImageContent()
                        } else {
                            ThemeComponentPlaceholder(component)
                        }
                    }
                } else {
                    ThemeComponentPlaceholder(component)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp)))
                    .background(colorResource(R.color.colors_12B890))
                    .padding(vertical = dimensionResource(SdpR.dimen._4sdp)),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (locked) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = colorResource(R.color.colors_FFFFFF),
                        modifier = Modifier.size(dimensionResource(SdpR.dimen._12sdp))
                    )
                    Spacer(Modifier.width(dimensionResource(SdpR.dimen._3sdp)))
                }
                Text(
                    text = stringResource(
                        when {
                            locked -> R.string.battery_component_unlock
                            selected -> R.string.battery_component_selected
                            else -> R.string.battery_component_select
                        }
                    ),
                    color = colorResource(R.color.colors_FFFFFF),
                    fontFamily = FontFamily(Font(R.font.inter_semibold)),
                    fontSize = dimensionResource(SspR.dimen._7ssp).value.sp,
                    maxLines = 1
                )
            }
        }
        if (showLoadingOverlay) {
            Column(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._14sdp)))
                    .background(colorResource(R.color.colors_FFFFFF).copy(alpha = 0.88f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = colorResource(R.color.colors_12B890),
                    strokeWidth = dimensionResource(SdpR.dimen._2sdp),
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._20sdp))
                )
                Spacer(Modifier.height(dimensionResource(SdpR.dimen._4sdp)))
                Text(
                    text = loadingLabel,
                    color = colorResource(R.color.colors_12B890),
                    fontFamily = FontFamily(Font(R.font.inter_semibold)),
                    fontSize = dimensionResource(SspR.dimen._7ssp).value.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ThemeComponentPlaceholder(component: BatteryThemeComponent) {
    @DrawableRes val fallbackIcon = if (component == BatteryThemeComponent.BATTERY) {
        R.drawable.ic_battery_status
    } else {
        R.drawable.ic_notification_pet
    }
    Image(
        painter = painterResource(fallbackIcon),
        contentDescription = null,
        modifier = Modifier.size(dimensionResource(SdpR.dimen._32sdp))
    )
}

@Composable
private fun selectedBackgroundName(state: BatteryEditorUiState): String {
    if (state.config.backgroundDecorationId == 0) {
        return stringResource(R.string.battery_background_solid)
    }
    return state.backgrounds
        .firstOrNull { it.id == state.config.backgroundDecorationId }
        ?.name
        ?: stringResource(R.string.battery_background_style)
}

@Composable
private fun SizeEditor(
    state: BatteryEditorUiState,
    onBarHeight: (Float) -> Unit,
    onEmojiSize: (Float) -> Unit,
    onBatterySize: (Float) -> Unit,
    onConfig: (BatteryStatusConfig) -> Unit
) {
    EditorPageHint(stringResource(R.string.battery_editor_size_hint))
    EditorCard {
        EditorSlider(
            label = stringResource(R.string.battery_bar_height),
            value = state.config.barHeightDp,
            range = state.barHeightRange.minimumDp..state.barHeightRange.maximumDp,
            onValue = onBarHeight
        )
        EditorSlider(
            label = stringResource(R.string.battery_left_padding),
            value = state.config.leftPaddingDp,
            range = 0f..32f,
            onValue = { onConfig(state.config.copy(leftPaddingDp = it)) }
        )
        EditorSlider(
            label = stringResource(R.string.battery_right_padding),
            value = state.config.rightPaddingDp,
            range = 0f..32f,
            onValue = { onConfig(state.config.copy(rightPaddingDp = it)) }
        )
        EditorSlider(
            label = stringResource(R.string.battery_emoji_size),
            value = state.config.emojiSizeDp,
            range = 12f..36f,
            onValue = onEmojiSize
        )
        EditorSlider(
            label = stringResource(R.string.battery_icon_size),
            value = state.config.batterySizeDp,
            range = 20f..48f,
            onValue = onBatterySize
        )
    }
}

@Composable
private fun AppearanceEditor(
    state: BatteryEditorUiState,
    onBackgroundColor: (Int) -> Unit,
    onForegroundColor: (Int) -> Unit,
    onBackgroundDecoration: (Int) -> Unit
) {
    EditorPageHint(stringResource(R.string.battery_editor_appearance_hint))
    EditorCard {
        ColorPalette(
            label = stringResource(R.string.battery_background_color),
            selected = state.config.backgroundColorArgb,
            onColor = onBackgroundColor
        )
        DecorationPicker(
            label = stringResource(R.string.battery_background_style),
            decorations = state.backgrounds,
            selectedId = state.config.backgroundDecorationId,
            includeNone = true,
            onSelect = onBackgroundDecoration
        )
        ColorPalette(
            label = stringResource(R.string.battery_foreground_color),
            selected = state.config.foregroundColorArgb,
            onColor = onForegroundColor
        )
    }
}

@Composable
private fun EmojiEditor(
    state: BatteryEditorUiState,
    onShowEmotion: (Boolean) -> Unit,
    onEmotionDecoration: (Int) -> Unit
) {
    EditorPageHint(stringResource(R.string.battery_editor_emoji_hint))
    EditorCard {
        ToggleRow(
            label = stringResource(R.string.battery_show_emotion),
            checked = state.config.showEmotion,
            onChecked = onShowEmotion
        )
        if (state.config.showEmotion) {
            DecorationPicker(
                label = stringResource(R.string.battery_emotion_style),
                decorations = state.emotions,
                selectedId = state.config.emotionDecorationId,
                includeNone = false,
                onSelect = onEmotionDecoration
            )
        }
    }
}

@Composable
private fun BatteryComponentEditor(
    state: BatteryEditorUiState,
    onShowPercentage: (Boolean) -> Unit,
    onBatterySize: (Float) -> Unit,
    onConfig: (BatteryStatusConfig) -> Unit
) {
    EditorPageHint(stringResource(R.string.battery_editor_battery_hint))
    EditorCard {
        ToggleRow(
            label = stringResource(R.string.battery_show_percentage),
            checked = state.config.showPercentage,
            onChecked = onShowPercentage
        )
        EditorSlider(
            label = stringResource(R.string.battery_icon_size),
            value = state.config.batterySizeDp,
            range = 20f..48f,
            onValue = onBatterySize
        )
        EditorSlider(
            label = stringResource(R.string.battery_percentage_size),
            value = state.config.percentSizeDp,
            range = 10f..32f,
            onValue = { onConfig(state.config.copy(percentSizeDp = it)) }
        )
        ColorPalette(
            label = stringResource(R.string.battery_percentage_color),
            selected = state.config.percentColorArgb,
            onColor = { onConfig(state.config.copy(percentColorArgb = it)) }
        )
    }
}

@Composable
private fun StatusComponentEditor(
    config: BatteryStatusConfig,
    size: Float,
    color: Int,
    selectedStyleIndex: Int,
    iconStyles: List<List<String>>,
    onSize: (Float) -> Unit,
    onColor: (Int) -> Unit,
    onStyle: (Int) -> Unit
) {
    EditorPageHint(stringResource(R.string.battery_status_component_hint))
    EditorCard {
        StatusIconStylePicker(
            selected = selectedStyleIndex,
            color = color,
            iconStyles = iconStyles,
            onSelected = onStyle
        )
        EditorSlider(
            label = stringResource(R.string.battery_status_icon_size),
            value = size,
            range = 8f..32f,
            onValue = onSize
        )
        ColorPalette(
            label = stringResource(R.string.battery_status_icon_color),
            selected = color,
            onColor = onColor
        )
        Text(
            text = stringResource(
                R.string.battery_status_uses_device_state,
                config.barHeightDp.toInt()
            ),
            color = colorResource(R.color.colors_776D84),
            fontSize = dimensionResource(SspR.dimen._8ssp).value.sp
        )
    }
}

@Composable
private fun AnimationEditor(
    state: BatteryEditorUiState,
    onConfig: (BatteryStatusConfig) -> Unit
) {
    val config = state.config
    EditorPageHint(stringResource(R.string.battery_animation_hint))
    EditorCard {
        ToggleRow(
            label = stringResource(R.string.battery_animation_show),
            checked = config.showAnimation,
            onChecked = { onConfig(config.copy(showAnimation = it)) }
        )
        EditorSlider(
            label = stringResource(R.string.battery_animation_size),
            value = config.animationSizeDp,
            range = 12f..36f,
            onValue = { onConfig(config.copy(animationSizeDp = it)) }
        )
        if (state.animations.isNotEmpty()) {
            Text(
                text = stringResource(R.string.battery_animation_style),
                color = colorResource(R.color.colors_776D84),
                fontSize = dimensionResource(SspR.dimen._9ssp).value.sp
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(SdpR.dimen._8sdp)
                ),
                modifier = Modifier.padding(vertical = dimensionResource(SdpR.dimen._8sdp))
            ) {
                items(state.animations, key = { it.id }) { animation ->
                    DecorationOption(
                        selected = config.animationAssetName == animation.name,
                        contentDescription = animation.name,
                        onClick = {
                            onConfig(config.copy(animationAssetName = animation.name))
                        }
                    ) {
                        AsyncImage(
                            model = animation.assetPath,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            text = animation.id.toString(),
                            color = Color(config.foregroundColorArgb),
                            fontSize = 9.sp,
                            modifier = Modifier.align(Alignment.BottomEnd)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DataEditor(
    config: BatteryStatusConfig,
    onConfig: (BatteryStatusConfig) -> Unit
) {
    EditorPageHint(stringResource(R.string.battery_data_hint))
    EditorCard {
        StatusTypePicker(
            label = stringResource(R.string.battery_data_type),
            values = BatteryDataType.entries.map { it.label },
            selected = config.dataType.label,
            onSelected = { label ->
                val type = BatteryDataType.entries.first { it.label == label }
                onConfig(config.copy(dataType = type))
            }
        )
        EditorSlider(
            label = stringResource(R.string.battery_status_icon_size),
            value = config.dataSizeDp,
            range = 8f..32f,
            onValue = { onConfig(config.copy(dataSizeDp = it)) }
        )
        ColorPalette(
            label = stringResource(R.string.battery_status_icon_color),
            selected = config.dataColorArgb,
            onColor = { onConfig(config.copy(dataColorArgb = it)) }
        )
    }
}

@Composable
private fun ChargeEditor(
    config: BatteryStatusConfig,
    onConfig: (BatteryStatusConfig) -> Unit
) {
    EditorPageHint(stringResource(R.string.battery_charge_hint))
    EditorCard {
        ChargeStylePicker(
            selected = config.chargeIconIndex,
            color = config.chargeColorArgb,
            onSelected = { onConfig(config.copy(chargeIconIndex = it)) }
        )
        EditorSlider(
            label = stringResource(R.string.battery_status_icon_size),
            value = config.chargeSizeDp,
            range = 8f..32f,
            onValue = { onConfig(config.copy(chargeSizeDp = it)) }
        )
        ColorPalette(
            label = stringResource(R.string.battery_status_icon_color),
            selected = config.chargeColorArgb,
            onColor = { onConfig(config.copy(chargeColorArgb = it)) }
        )
    }
}

@Composable
@SuppressLint("DiscouragedApi")
private fun ChargeStylePicker(
    selected: Int,
    color: Int,
    onSelected: (Int) -> Unit
) {
    StatusIconStylePicker(
        selected = selected,
        color = color,
        iconStyles = (1..12).map { index ->
            listOf("charge_%02d".format(index))
        },
        label = stringResource(R.string.battery_charge_style),
        onSelected = onSelected
    )
}

@Composable
@SuppressLint("DiscouragedApi")
private fun StatusIconStylePicker(
    selected: Int,
    color: Int,
    iconStyles: List<List<String>>,
    label: String? = null,
    onSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    Text(
        text = label ?: stringResource(R.string.battery_status_icon_style),
        color = colorResource(R.color.colors_776D84),
        fontFamily = FontFamily(Font(R.font.inter_medium)),
        fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
        modifier = Modifier.padding(top = dimensionResource(SdpR.dimen._8sdp))
    )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._8sdp)),
        modifier = Modifier.padding(vertical = dimensionResource(SdpR.dimen._7sdp))
    ) {
        items(iconStyles.indices.toList()) { stylePosition ->
            val styleIndex = stylePosition + 1
            val resourceIds = remember(iconStyles, stylePosition, resources) {
                iconStyles[stylePosition].map { resourceName ->
                    resources.getIdentifier(
                        resourceName,
                        "drawable",
                        context.packageName
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(dimensionResource(SdpR.dimen._54sdp))
                    .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp)))
                    .background(colorResource(R.color.colors_FFFFFF))
                    .border(
                        width = dimensionResource(
                            if (selected == styleIndex) {
                                SdpR.dimen._3sdp
                            } else {
                                SdpR.dimen._1sdp
                            }
                        ),
                        color = colorResource(
                            if (selected == styleIndex) {
                                R.color.colors_12B890
                            } else {
                                R.color.colors_E9DFEF
                            }
                        ),
                        shape = RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp))
                    )
                    .clickable { onSelected(styleIndex) },
                contentAlignment = Alignment.Center
            ) {
                if (resourceIds.any { it != 0 }) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(
                            dimensionResource(SdpR.dimen._2sdp)
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        resourceIds.filter { it != 0 }.forEach { resourceId ->
                            Icon(
                                painter = painterResource(resourceId),
                                contentDescription = styleIndex.toString(),
                                tint = Color(color),
                                modifier = Modifier.size(
                                    dimensionResource(
                                        if (resourceIds.size > 1) {
                                            SdpR.dimen._20sdp
                                        } else {
                                            SdpR.dimen._28sdp
                                        }
                                    )
                                )
                            )
                        }
                    }
                } else {
                    Text(styleIndex.toString(), color = Color(color))
                }
            }
        }
    }
}

@Composable
private fun DateTimeEditor(
    config: BatteryStatusConfig,
    onConfig: (BatteryStatusConfig) -> Unit
) {
    EditorPageHint(stringResource(R.string.battery_date_hint))
    EditorCard {
        ToggleRow(
            label = stringResource(R.string.battery_date_show),
            checked = config.showDateTime,
            onChecked = { onConfig(config.copy(showDateTime = it)) }
        )
        EditorSlider(
            label = stringResource(R.string.battery_date_size),
            value = config.dateTimeSizeDp,
            range = 8f..32f,
            onValue = { onConfig(config.copy(dateTimeSizeDp = it)) }
        )
        ColorPalette(
            label = stringResource(R.string.battery_date_color),
            selected = config.dateTimeColorArgb,
            onColor = { onConfig(config.copy(dateTimeColorArgb = it)) }
        )
        StatusTypePicker(
            label = stringResource(R.string.battery_date_format),
            values = BatteryDateFormat.entries.map { it.pattern },
            selected = config.dateFormat.pattern,
            onSelected = { value ->
                onConfig(
                    config.copy(
                        dateFormat = BatteryDateFormat.entries.first { it.pattern == value }
                    )
                )
            }
        )
        StatusTypePicker(
            label = stringResource(R.string.battery_date_font),
            values = BatteryDateFont.entries.map { it.displayName },
            selected = config.dateTimeFont.displayName,
            onSelected = { value ->
                onConfig(
                    config.copy(
                        dateTimeFont = BatteryDateFont.entries.first {
                            it.displayName == value
                        }
                    )
                )
            }
        )
    }
}

@Composable
private fun StatusTypePicker(
    label: String,
    values: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Text(
        text = label,
        color = colorResource(R.color.colors_776D84),
        fontFamily = FontFamily(Font(R.font.inter_medium)),
        fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
        modifier = Modifier.padding(top = dimensionResource(SdpR.dimen._8sdp))
    )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._8sdp)),
        modifier = Modifier.padding(vertical = dimensionResource(SdpR.dimen._7sdp))
    ) {
        items(values) { value ->
            val active = value == selected
            Text(
                text = value,
                color = colorResource(
                    if (active) R.color.colors_FFFFFF else R.color.colors_2F2440
                ),
                fontFamily = FontFamily(Font(R.font.inter_semibold)),
                fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp)))
                    .background(
                        colorResource(
                            if (active) R.color.colors_12B890 else R.color.colors_E0F7F1
                        )
                    )
                    .clickable { onSelected(value) }
                    .padding(
                        horizontal = dimensionResource(SdpR.dimen._12sdp),
                        vertical = dimensionResource(SdpR.dimen._8sdp)
                    )
            )
        }
    }
}

@Composable
private fun EditorPageHint(text: String) {
    Text(
        text = text,
        color = colorResource(R.color.colors_776D84),
        fontFamily = FontFamily(Font(R.font.inter_regular)),
        fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
        modifier = Modifier.padding(bottom = dimensionResource(SdpR.dimen._10sdp))
    )
}

@Composable
private fun EditorSectionTitle(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(
                    width = dimensionResource(SdpR.dimen._4sdp),
                    height = dimensionResource(SdpR.dimen._20sdp)
                )
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._3sdp)))
                .background(colorResource(R.color.colors_12B890))
        )
        Spacer(Modifier.width(dimensionResource(SdpR.dimen._8sdp)))
        Text(
            text = title,
            color = colorResource(R.color.colors_2F2440),
            fontFamily = FontFamily(Font(R.font.inter_bold)),
            fontWeight = FontWeight.Bold,
            fontSize = dimensionResource(SspR.dimen._13ssp).value.sp
        )
    }
}

@Composable
private fun EditorCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.colors_FFFFFF)),
        shape = RoundedCornerShape(dimensionResource(SdpR.dimen._16sdp)),
        border = BorderStroke(
            dimensionResource(SdpR.dimen._1sdp),
            colorResource(R.color.colors_E9DFEF)
        )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = dimensionResource(SdpR.dimen._14sdp),
                vertical = dimensionResource(SdpR.dimen._8sdp)
            ),
            content = { content() }
        )
    }
}

@Composable
private fun EditorNavigationRow(
    @DrawableRes iconRes: Int,
    title: String,
    summary: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._10sdp)))
            .clickable(onClick = onClick)
            .padding(vertical = dimensionResource(SdpR.dimen._10sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._38sdp))
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._10sdp)))
                .background(colorResource(R.color.colors_E0F7F1)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = colorResource(R.color.colors_12B890),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._22sdp))
            )
        }
        Spacer(Modifier.width(dimensionResource(SdpR.dimen._10sdp)))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colorResource(R.color.colors_2F2440),
                fontFamily = FontFamily(Font(R.font.inter_semibold)),
                fontSize = dimensionResource(SspR.dimen._10ssp).value.sp
            )
            Text(
                text = summary,
                color = colorResource(R.color.colors_776D84),
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = title,
            tint = colorResource(R.color.colors_12B890),
            modifier = Modifier.size(dimensionResource(SdpR.dimen._18sdp))
        )
    }
}

@Composable
private fun EditorDivider() {
    HorizontalDivider(
        color = colorResource(R.color.colors_E9DFEF),
        modifier = Modifier.padding(start = dimensionResource(SdpR.dimen._48sdp))
    )
}

@Composable
private fun StatusComponentsGrid(onOpenPage: (BatteryEditorPage) -> Unit) {
    val components = listOf(
        StatusComponentDestination(
            R.string.battery_component_animation,
            BatteryEditorPage.ANIMATION,
            "ic_notification_pet"
        ),
        StatusComponentDestination(
            R.string.battery_component_wifi,
            BatteryEditorPage.WIFI,
            "ic_status_wifi_solid"
        ),
        StatusComponentDestination(
            R.string.battery_component_data,
            BatteryEditorPage.DATA,
            "ic_data"
        ),
        StatusComponentDestination(
            R.string.battery_component_signal,
            BatteryEditorPage.SIGNAL,
            "ic_status_signal_rounded"
        ),
        StatusComponentDestination(
            R.string.battery_component_airplane,
            BatteryEditorPage.AIRPLANE,
            "ic_status_airplane_classic"
        ),
        StatusComponentDestination(
            R.string.battery_component_hotspot,
            BatteryEditorPage.HOTSPOT,
            "ic_status_hotspot_orbit"
        ),
        StatusComponentDestination(
            R.string.battery_component_ringer,
            BatteryEditorPage.RINGER,
            "ic_status_silent_bell_outline"
        ),
        StatusComponentDestination(
            R.string.battery_component_charge,
            BatteryEditorPage.CHARGE,
            "ic_charge"
        ),
        StatusComponentDestination(
            R.string.battery_component_date,
            BatteryEditorPage.DATE_TIME,
            "ic_datetime"
        )
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._8sdp))
    ) {
        components.chunked(2).forEach { rowComponents ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(SdpR.dimen._8sdp)
                )
            ) {
                rowComponents.forEach { destination ->
                    StatusComponentTile(
                        label = stringResource(destination.label),
                        iconName = destination.iconName,
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenPage(destination.page) }
                    )
                }
                if (rowComponents.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
@SuppressLint("DiscouragedApi")
private fun StatusComponentTile(
    label: String,
    iconName: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val iconResource = remember(iconName, resources) {
        resources.getIdentifier(iconName, "drawable", context.packageName)
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._14sdp)))
            .background(colorResource(R.color.colors_FFFFFF))
            .border(
                BorderStroke(
                    dimensionResource(SdpR.dimen._1sdp),
                    colorResource(R.color.colors_E9DFEF)
                ),
                RoundedCornerShape(dimensionResource(SdpR.dimen._14sdp))
            )
            .clickable(onClick = onClick)
            .padding(dimensionResource(SdpR.dimen._10sdp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._28sdp))
                .clip(CircleShape)
                .background(colorResource(R.color.colors_E0F7F1)),
            contentAlignment = Alignment.Center
        ) {
            if (iconResource != 0) {
                Icon(
                    painter = painterResource(iconResource),
                    contentDescription = null,
                    tint = colorResource(R.color.colors_12B890),
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._20sdp))
                )
            } else {
                Text(
                    text = label.take(1),
                    color = colorResource(R.color.colors_12B890),
                    fontFamily = FontFamily(Font(R.font.inter_bold))
                )
            }
        }
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._5sdp)))
        Text(
            text = label,
            color = colorResource(R.color.colors_2F2440),
            fontFamily = FontFamily(Font(R.font.inter_semibold)),
            fontSize = dimensionResource(SspR.dimen._8ssp).value.sp
        )
        Text(
            text = stringResource(R.string.battery_component_customize),
            color = colorResource(R.color.colors_12B890),
            fontSize = dimensionResource(SspR.dimen._7ssp).value.sp
        )
    }
}

private data class StatusComponentDestination(
    val label: Int,
    val page: BatteryEditorPage,
    val iconName: String
)

@Composable
private fun AccessibilityState(accessibilityEnabled: Boolean) {
    Text(
        text = if (accessibilityEnabled) {
            stringResource(R.string.battery_accessibility_ready)
        } else {
            stringResource(R.string.battery_accessibility_required)
        },
        color = colorResource(
            if (accessibilityEnabled) R.color.colors_12B890 else R.color.colors_776D84
        ),
        fontFamily = FontFamily(Font(R.font.inter_regular)),
        fontSize = dimensionResource(SspR.dimen._8ssp).value.sp
    )
}

@Composable
private fun ApplyFooter(
    enabled: Boolean,
    selectionInProgress: Boolean,
    isApplied: Boolean,
    onApply: () -> Unit,
    onDisable: () -> Unit
) {
    val applyState = BatteryEditorLoadingPolicy.applyState(
        themeAvailable = enabled,
        selectionInProgress = selectionInProgress
    )
    val activeContainerColor = colorResource(R.color.colors_12B890)
    val activeContentColor = colorResource(R.color.colors_FFFFFF)
    val unavailableContainerColor = colorResource(R.color.colors_C8C8C9)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.colors_FFFFFF))
            .padding(
                horizontal = dimensionResource(SdpR.dimen._16sdp),
                vertical = dimensionResource(SdpR.dimen._10sdp)
            )
    ) {
        Button(
            onClick = onApply,
            enabled = applyState.enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = activeContainerColor,
                contentColor = activeContentColor,
                disabledContainerColor = if (applyState.keepActiveAppearance) {
                    activeContainerColor
                } else {
                    unavailableContainerColor
                },
                disabledContentColor = activeContentColor
            ),
            shape = RoundedCornerShape(dimensionResource(SdpR.dimen._20sdp)),
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(SdpR.dimen._48sdp))
        ) {
            Text(
                text = stringResource(R.string.battery_apply),
                fontFamily = FontFamily(Font(R.font.inter_semibold))
            )
        }
        if (isApplied) {
            TextButton(
                onClick = onDisable,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.battery_disable),
                    color = colorResource(R.color.colors_E45D6A)
                )
            }
        }
    }
}

@Composable
private fun BatteryPreview(
    state: BatteryEditorUiState,
    page: BatteryEditorPage
) {
    val config = state.config
    val previewDescription = stringResource(R.string.battery_overlay_description, 82)
    val focusedComponent = page.previewComponent()
    val backgroundPath = state.backgrounds
        .firstOrNull { it.id == config.backgroundDecorationId }
        ?.assetPath
    val emotionPath = state.emotions
        .firstOrNull { it.id == config.emotionDecorationId }
        ?.assetPath
    val animationPath = state.animations
        .firstOrNull { it.name == config.animationAssetName }
        ?.assetPath
    val previewDate = remember(config.dateFormat) {
        SimpleDateFormat(config.dateFormat.pattern, Locale.getDefault()).format(Date())
    }
    val previewDateFont = previewDateFontFamily(config.dateTimeFont)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(config.barHeightDp.dp)
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._20sdp)))
            .background(Color(config.backgroundColorArgb))
            .semantics { contentDescription = previewDescription }
    ) {
        val layout = remember(
            config,
            state.theme.emojiPath,
            emotionPath,
            animationPath,
            maxWidth,
            focusedComponent
        ) {
            batteryPreviewLayout(
                config = config,
                availableWidthDp = maxWidth.value -
                    config.leftPaddingDp -
                    config.rightPaddingDp,
                hasEmoji = state.theme.emojiPath != null,
                hasEmotion = emotionPath != null,
                hasAnimation = animationPath != null,
                focusedComponent = focusedComponent
            )
        }
        backgroundPath?.let { path ->
            AsyncImage(
                model = path,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = config.leftPaddingDp.dp,
                    end = config.rightPaddingDp.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (layout.shows(BatteryStatusComponent.TIME)) {
                Text(
                    text = stringResource(R.string.battery_preview_time),
                    color = Color(config.dateTimeColorArgb),
                    fontFamily = previewDateFont,
                    fontSize = config.dateTimeSizeDp.sp,
                    maxLines = 1
                )
            }
            if (layout.shows(BatteryStatusComponent.DATE)) {
                Text(
                    text = previewDate,
                    color = Color(config.dateTimeColorArgb),
                    fontFamily = previewDateFont,
                    fontSize = config.dateTimeSizeDp.sp,
                    maxLines = 1
                )
            }
            if (layout.shows(BatteryStatusComponent.AIRPLANE)) {
                PreviewStatusIcon(
                    iconName = BatterySystemStatusPolicy.airplaneIcon(
                        config.airplaneIconStyleIndex
                    ),
                    sizeDp = config.airplaneSizeDp,
                    colorArgb = config.airplaneColorArgb
                )
            }
            if (layout.shows(BatteryStatusComponent.RINGER)) {
                PreviewStatusIcon(
                    iconName = requireNotNull(
                        BatterySystemStatusPolicy.ringerIcon(
                            BatteryRingerState.SILENT,
                            config.ringerIconStyleIndex
                        )
                    ),
                    sizeDp = config.ringerSizeDp,
                    colorArgb = config.ringerColorArgb
                )
            }
            if (layout.shows(BatteryStatusComponent.ANIMATION)) {
                animationPath?.let { path ->
                    AsyncImage(
                        model = path,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(config.animationSizeDp.dp)
                    )
                }
            }
            if (layout.shows(BatteryStatusComponent.THEME_EMOJI)) {
                state.theme.emojiPath?.let { path ->
                    AsyncImage(
                        model = path,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(config.emojiSizeDp.dp)
                    )
                }
            }
            if (layout.shows(BatteryStatusComponent.EMOTION)) {
                emotionPath?.let { path ->
                    AsyncImage(
                        model = path,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(config.emojiSizeDp.dp)
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (layout.shows(BatteryStatusComponent.CHARGE)) {
                PreviewStatusIcon(
                    iconName = "charge_%02d".format(config.chargeIconIndex),
                    sizeDp = config.chargeSizeDp,
                    colorArgb = config.chargeColorArgb
                )
            }
            if (layout.shows(BatteryStatusComponent.BATTERY)) {
                state.theme.batteryPath?.let { path ->
                    AsyncImage(
                        model = path,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(config.batterySizeDp.dp)
                    )
                } ?: Box(
                    modifier = Modifier
                        .size(
                            width = config.batterySizeDp.dp,
                            height = (config.batterySizeDp * 0.48f).dp
                        )
                        .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._3sdp)))
                        .background(Color(config.foregroundColorArgb))
                    )
            }
            if (layout.shows(BatteryStatusComponent.PERCENTAGE)) {
                Text(
                    text = stringResource(R.string.battery_preview_percentage),
                    color = Color(config.percentColorArgb),
                    fontFamily = FontFamily(Font(R.font.inter_semibold)),
                    fontSize = config.percentSizeDp.sp
                )
            }
            if (layout.shows(BatteryStatusComponent.WIFI)) {
                PreviewStatusIcon(
                    iconName = BatterySystemStatusPolicy.wifiIcon(
                        BatteryConnectivityState.CONNECTED,
                        config.wifiIconStyleIndex
                    ),
                    sizeDp = config.wifiSizeDp,
                    colorArgb = config.wifiColorArgb
                )
            }
            if (layout.shows(BatteryStatusComponent.CELLULAR)) {
                Text(
                    text = config.dataType.label,
                    color = Color(config.dataColorArgb),
                    fontSize = config.dataSizeDp.sp,
                    fontWeight = FontWeight.Bold
                )
                PreviewStatusIcon(
                    iconName = BatterySystemStatusPolicy.cellularIcon(
                        BatteryConnectivityState.CONNECTED,
                        config.signalIconStyleIndex
                    ),
                    sizeDp = config.signalSizeDp,
                    colorArgb = config.signalColorArgb
                )
            }
            if (layout.shows(BatteryStatusComponent.HOTSPOT)) {
                PreviewStatusIcon(
                    iconName = requireNotNull(
                        BatterySystemStatusPolicy.hotspotIcon(
                            BatteryHotspotState.ENABLED,
                            config.hotspotIconStyleIndex
                        )
                    ),
                    sizeDp = config.hotspotSizeDp,
                    colorArgb = config.hotspotColorArgb
                )
            }
        }
    }
}

internal fun batteryPreviewLayout(
    config: BatteryStatusConfig,
    availableWidthDp: Float,
    hasEmoji: Boolean,
    hasEmotion: Boolean,
    hasAnimation: Boolean,
    focusedComponent: BatteryStatusComponent? = null
) = BatteryStatusLayoutPolicy().resolve(
    availableWidth = availableWidthDp,
    items = buildList {
        val gap = 4f
        if (config.showTime) {
            add(
                BatteryStatusLayoutItem(
                    BatteryStatusComponent.TIME,
                    width = config.dateTimeSizeDp * 3.2f + gap,
                    priority = 100,
                    required = focusedComponent == BatteryStatusComponent.DATE
                )
            )
        }
        if (config.showDateTime) {
            add(
                BatteryStatusLayoutItem(
                    BatteryStatusComponent.DATE,
                    width = config.dateTimeSizeDp * 4.2f + gap,
                    priority = 20,
                    required = focusedComponent == BatteryStatusComponent.DATE
                )
            )
        }
        if (focusedComponent == BatteryStatusComponent.AIRPLANE) {
            add(
                BatteryStatusLayoutItem(
                    BatteryStatusComponent.AIRPLANE,
                    width = config.airplaneSizeDp + gap,
                    priority = 65,
                    required = true
                )
            )
        }
        if (focusedComponent == BatteryStatusComponent.RINGER) {
            add(
                BatteryStatusLayoutItem(
                    BatteryStatusComponent.RINGER,
                    width = config.ringerSizeDp + gap,
                    priority = 60,
                    required = true
                )
            )
        }
        if (config.showAnimation && hasAnimation) {
            add(
                BatteryStatusLayoutItem(
                    BatteryStatusComponent.ANIMATION,
                    width = config.animationSizeDp + gap,
                    priority = 40,
                    required = focusedComponent == BatteryStatusComponent.ANIMATION
                )
            )
        }
        if (config.showEmotion && hasEmotion) {
            add(
                BatteryStatusLayoutItem(
                    BatteryStatusComponent.EMOTION,
                    width = config.emojiSizeDp + gap,
                    priority = 30
                )
            )
        }
        if (focusedComponent == BatteryStatusComponent.CHARGE) {
            add(
                BatteryStatusLayoutItem(
                    BatteryStatusComponent.CHARGE,
                    width = config.chargeSizeDp + gap,
                    priority = 85,
                    required = true
                )
            )
        }
        add(
            BatteryStatusLayoutItem(
                BatteryStatusComponent.BATTERY,
                width = maxOf(
                    config.batterySizeDp,
                    if (hasEmoji) config.emojiSizeDp else 0f
                ) + gap,
                priority = 110,
                required = true
            )
        )
        if (config.showPercentage) {
            add(
                BatteryStatusLayoutItem(
                    BatteryStatusComponent.PERCENTAGE,
                    width = config.percentSizeDp * 2.5f + gap,
                    priority = 95
                )
            )
        }
        add(
            BatteryStatusLayoutItem(
                BatteryStatusComponent.WIFI,
                width = config.wifiSizeDp * 1.4f + gap,
                priority = 90,
                required = focusedComponent == BatteryStatusComponent.WIFI
            )
        )
        if (focusedComponent != BatteryStatusComponent.AIRPLANE) {
            add(
                BatteryStatusLayoutItem(
                    BatteryStatusComponent.CELLULAR,
                    width = config.signalSizeDp * 1.4f +
                        config.dataSizeDp * 1.8f +
                        gap * 2,
                    priority = 70,
                    required = focusedComponent == BatteryStatusComponent.CELLULAR
                )
            )
        }
        if (focusedComponent == BatteryStatusComponent.HOTSPOT) {
            add(
                BatteryStatusLayoutItem(
                    BatteryStatusComponent.HOTSPOT,
                    width = config.hotspotSizeDp + gap,
                    priority = 55,
                    required = true
                )
            )
        }
    }
)

private fun BatteryEditorPage.previewComponent(): BatteryStatusComponent? = when (this) {
    BatteryEditorPage.ANIMATION -> BatteryStatusComponent.ANIMATION
    BatteryEditorPage.WIFI -> BatteryStatusComponent.WIFI
    BatteryEditorPage.DATA,
    BatteryEditorPage.SIGNAL -> BatteryStatusComponent.CELLULAR
    BatteryEditorPage.AIRPLANE -> BatteryStatusComponent.AIRPLANE
    BatteryEditorPage.HOTSPOT -> BatteryStatusComponent.HOTSPOT
    BatteryEditorPage.RINGER -> BatteryStatusComponent.RINGER
    BatteryEditorPage.CHARGE -> BatteryStatusComponent.CHARGE
    BatteryEditorPage.DATE_TIME -> BatteryStatusComponent.DATE
    else -> null
}

private fun BatteryEditorPage.analyticsScreen(): ScreenName = when (this) {
    BatteryEditorPage.OVERVIEW -> ScreenName.BATTERY_EDITOR
    BatteryEditorPage.SIZE -> ScreenName.BATTERY_SIZE_EDITOR
    BatteryEditorPage.APPEARANCE -> ScreenName.BATTERY_APPEARANCE_EDITOR
    BatteryEditorPage.EMOJI -> ScreenName.BATTERY_EMOJI_EDITOR
    BatteryEditorPage.BATTERY -> ScreenName.BATTERY_ICON_EDITOR
    BatteryEditorPage.ANIMATION -> ScreenName.BATTERY_ANIMATION_EDITOR
    BatteryEditorPage.WIFI -> ScreenName.BATTERY_WIFI_EDITOR
    BatteryEditorPage.DATA -> ScreenName.BATTERY_DATA_EDITOR
    BatteryEditorPage.SIGNAL -> ScreenName.BATTERY_SIGNAL_EDITOR
    BatteryEditorPage.AIRPLANE -> ScreenName.BATTERY_AIRPLANE_EDITOR
    BatteryEditorPage.HOTSPOT -> ScreenName.BATTERY_HOTSPOT_EDITOR
    BatteryEditorPage.RINGER -> ScreenName.BATTERY_RINGER_EDITOR
    BatteryEditorPage.CHARGE -> ScreenName.BATTERY_CHARGE_EDITOR
    BatteryEditorPage.DATE_TIME -> ScreenName.BATTERY_DATE_TIME_EDITOR
}

@Composable
@SuppressLint("DiscouragedApi")
private fun PreviewStatusIcon(
    iconName: String,
    sizeDp: Float,
    colorArgb: Int
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val iconResource = remember(iconName, resources) {
        resources.getIdentifier(iconName, "drawable", context.packageName)
    }
    if (iconResource != 0) {
        Icon(
            painter = painterResource(iconResource),
            contentDescription = null,
            tint = Color(colorArgb),
            modifier = Modifier.size(sizeDp.dp)
        )
    } else {
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
                .background(Color(colorArgb))
        )
    }
}

@Composable
@SuppressLint("DiscouragedApi")
private fun previewDateFontFamily(font: BatteryDateFont): FontFamily {
    val context = LocalContext.current
    val resources = LocalResources.current
    val fontResource = remember(font, resources) {
        resources.getIdentifier(font.resourceName, "font", context.packageName)
    }
    return remember(fontResource) {
        if (fontResource == 0) FontFamily.Default else FontFamily(Font(fontResource))
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(SdpR.dimen._4sdp)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = colorResource(R.color.colors_2F2440),
            fontFamily = FontFamily(Font(R.font.inter_medium)),
            fontSize = dimensionResource(SspR.dimen._10ssp).value.sp
        )
        AppSwitch(checked = checked, onCheckedChange = { onChecked(!checked) })
    }
}

@Composable
private fun EditorSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValue: (Float) -> Unit
) {
    Text(
        text = stringResource(R.string.battery_slider_value, label, value.toInt()),
        color = colorResource(R.color.colors_776D84),
        fontFamily = FontFamily(Font(R.font.inter_medium)),
        fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
        modifier = Modifier.padding(top = dimensionResource(SdpR.dimen._8sdp))
    )
    Slider(
        value = value,
        onValueChange = onValue,
        valueRange = range,
        colors = SliderDefaults.colors(
            thumbColor = colorResource(R.color.colors_12B890),
            activeTrackColor = colorResource(R.color.colors_12B890),
            inactiveTrackColor = colorResource(R.color.colors_E0F7F1),
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent
        )
    )
}

@Composable
private fun ColorPalette(label: String, selected: Int, onColor: (Int) -> Unit) {
    val colors = listOf(
        R.color.colors_E0F7F1,
        R.color.colors_FFFFFF,
        R.color.colors_12B890,
        R.color.colors_FFE8EF,
        R.color.colors_EDE4FF,
        R.color.colors_FFC466,
        R.color.colors_1D86F6,
        R.color.colors_111827
    )
    Text(
        text = label,
        color = colorResource(R.color.colors_776D84),
        fontFamily = FontFamily(Font(R.font.inter_medium)),
        fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
        modifier = Modifier.padding(top = dimensionResource(SdpR.dimen._8sdp))
    )
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(SdpR.dimen._7sdp)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._8sdp))
    ) {
        items(colors) { colorRes ->
            val color = colorResource(colorRes)
            val isSelected = selected == color.toArgb()
            Box(
                modifier = Modifier
                    .size(dimensionResource(SdpR.dimen._30sdp))
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = dimensionResource(
                            if (isSelected) SdpR.dimen._3sdp else SdpR.dimen._1sdp
                        ),
                        color = colorResource(
                            if (isSelected) R.color.colors_12B890 else R.color.colors_C8C8C9
                        ),
                        shape = CircleShape
                    )
                    .semantics {
                        contentDescription = "$label #${color.toArgb().toUInt().toString(16)}"
                        this.selected = isSelected
                    }
                    .clickable { onColor(color.toArgb()) }
            )
        }
    }
}

@Composable
private fun DecorationPicker(
    label: String,
    decorations: List<BatteryDecorationEntry>,
    selectedId: Int,
    includeNone: Boolean,
    onSelect: (Int) -> Unit
) {
    if (decorations.isEmpty()) return
    Text(
        text = label,
        color = colorResource(R.color.colors_776D84),
        fontFamily = FontFamily(Font(R.font.inter_medium)),
        fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
        modifier = Modifier.padding(top = dimensionResource(SdpR.dimen._8sdp))
    )
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(SdpR.dimen._7sdp)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._8sdp))
    ) {
        if (includeNone) {
            item {
                DecorationOption(
                    selected = selectedId == 0,
                    contentDescription = stringResource(R.string.battery_background_solid),
                    onClick = { onSelect(0) }
                ) {
                    Text(
                        text = stringResource(R.string.battery_background_solid),
                        color = colorResource(R.color.colors_776D84),
                        fontSize = dimensionResource(SspR.dimen._7ssp).value.sp
                    )
                }
            }
        }
        items(decorations, key = BatteryDecorationEntry::id) { decoration ->
            DecorationOption(
                selected = selectedId == decoration.id,
                contentDescription = decoration.name,
                onClick = { onSelect(decoration.id) }
            ) {
                AsyncImage(
                    model = decoration.assetPath,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun DecorationOption(
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .width(dimensionResource(SdpR.dimen._76sdp))
            .height(dimensionResource(SdpR.dimen._44sdp))
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._10sdp)))
            .background(colorResource(R.color.colors_FFFFFF))
            .border(
                width = dimensionResource(
                    if (selected) SdpR.dimen._3sdp else SdpR.dimen._1sdp
                ),
                color = colorResource(
                    if (selected) R.color.colors_12B890 else R.color.colors_C8C8C9
                ),
                shape = RoundedCornerShape(dimensionResource(SdpR.dimen._10sdp))
            )
            .semantics {
                this.contentDescription = contentDescription
                this.selected = selected
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun BatteryEditorOverviewPreview() {
    BatteryEditorContent(
        state = BatteryEditorUiState(),
        page = BatteryEditorPage.OVERVIEW,
        accessibilityEnabled = true,
        onBack = {},
        onDone = {},
        onOpenPage = {},
        onShowTime = {},
        onShowPercentage = {},
        onBarHeight = {},
        onEmojiSize = {},
        onBatterySize = {},
        onBackgroundColor = {},
        onForegroundColor = {},
        onBackgroundDecoration = {},
        onShowEmotion = {},
        onEmotionDecoration = {},
        onSelectTheme = { _, _ -> },
        onConfig = {},
        onApply = {},
        onDisable = {}
    )
}
