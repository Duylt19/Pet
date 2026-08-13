package com.asianmobile.emojibattery.shimeji.ui.battery.troll

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import com.asianmobile.emojibattery.shimeji.battery.troll.batteryTrollEmojiSizeDp
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollMode
import com.asianmobile.emojibattery.shimeji.data.remote.BatteryTrollServerConfig
import com.asianmobile.emojibattery.shimeji.ui.battery.editor.BatteryDiscardChangesSheet
import com.asianmobile.emojibattery.shimeji.ui.battery.editor.StatusBarEditorWallpaper
import com.asianmobile.emojibattery.shimeji.ui.shared.component.AppSwitch
import com.asianmobile.emojibattery.shimeji.ui.shared.component.BatteryStatusPreviewCard
import com.asianmobile.emojibattery.shimeji.ui.shared.component.GrantPermissionDialog
import com.asianmobile.emojibattery.shimeji.ui.home.shell.HomeEnableCard
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

/**
 * Battery Troll — Customize (Figma `8315:8232` default, `8359:6992` Real + Random,
 * `8359:7165` discard).
 *
 * The screen reports itself as [ScreenName.BATTERY_TROLL_CUSTOMIZE]. The bottom banner is not a
 * parameter: `NavGraph.showBatteryEditorBottomBanner` already covers this route from the shell, so
 * a slot here would either render nothing or duplicate that banner.
 */
@Composable
fun BatteryTrollCustomizeScreen(
    onNavigateBack: () -> Unit,
    viewModel: BatteryTrollCustomizeViewModel = hiltViewModel(),
    accessibilityHowToUseResult: Boolean? = null,
    onAccessibilityHowToUseResultConsumed: () -> Unit = {},
    onNavigateToAccessibilityHowToUse: () -> Unit = {}
) {
    TrackScreenView(ScreenName.BATTERY_TROLL_CUSTOMIZE)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    var showAccessibilityDisclosure by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                BatteryTrollCustomizeEffect.Close -> onNavigateBack()
                BatteryTrollCustomizeEffect.RequestBatteryAccessibility -> {
                    showAccessibilityDisclosure = true
                }
            }
        }
    }
    LaunchedEffect(accessibilityHowToUseResult) {
        accessibilityHowToUseResult?.let { permissionGranted ->
            if (permissionGranted) {
                viewModel.refreshAccessibility()
            } else {
                viewModel.cancelPendingBatteryEnable()
            }
            onAccessibilityHowToUseResultConsumed()
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshAccessibility()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    BackHandler(enabled = true) { viewModel.onBackRequest() }

    BatteryTrollCustomizeContent(
        uiState = uiState,
        onBack = viewModel::onBackRequest,
        onBatteryToggle = viewModel::onBatteryToggle,
        onModeChange = viewModel::onModeChange,
        onEditPercentRequest = viewModel::onEditPercentRequest,
        onEditPercentConfirm = viewModel::onEditPercentConfirm,
        onEditPercentDismiss = viewModel::onEditPercentDismiss,
        onShowPercentageToggle = viewModel::onShowPercentageToggle,
        onPercentSizeChange = viewModel::onPercentSizeChange,
        onShowEmojiToggle = viewModel::onShowEmojiToggle,
        onRandomArtworkChange = viewModel::onRandomArtworkChange,
        onEmojiLevelChange = viewModel::onEmojiLevelChange,
        onBatteryLevelChange = viewModel::onBatteryLevelChange,
        onDiscardDismiss = viewModel::onDiscardDismiss,
        onDiscardConfirm = viewModel::onDiscardConfirm,
        onRetry = viewModel::retry,
        onApply = viewModel::apply
    )

    if (showAccessibilityDisclosure) {
        GrantPermissionDialog(
            onGrantPermission = {
                showAccessibilityDisclosure = false
                onNavigateToAccessibilityHowToUse()
            },
            onMaybeLater = {
                showAccessibilityDisclosure = false
                viewModel.cancelPendingBatteryEnable()
            }
        )
    }
}

@Composable
internal fun BatteryTrollCustomizeContent(
    uiState: BatteryTrollCustomizeUiState,
    onBack: () -> Unit,
    onBatteryToggle: () -> Unit,
    onModeChange: (BatteryTrollMode) -> Unit,
    onEditPercentRequest: () -> Unit,
    onEditPercentConfirm: (Int) -> Unit,
    onEditPercentDismiss: () -> Unit,
    onShowPercentageToggle: () -> Unit,
    onPercentSizeChange: (Float) -> Unit,
    onShowEmojiToggle: () -> Unit,
    onRandomArtworkChange: (Boolean) -> Unit,
    onEmojiLevelChange: (Int) -> Unit,
    onBatteryLevelChange: (Int) -> Unit,
    onDiscardDismiss: () -> Unit,
    onDiscardConfirm: () -> Unit,
    onRetry: () -> Unit,
    onApply: () -> Unit
) {
    // Random mode rotates the artwork on the status bar, so the previews rotate too instead of
    // freezing on a pick the bar will ignore.
    val rotationStep = rememberTrollRotationStep(uiState.draft.randomArtwork)
    val previewEmojiIndex = if (uiState.draft.randomArtwork) {
        rotationStep
    } else {
        uiState.draft.emojiLevelIndex
    }
    val previewBatteryIndex = if (uiState.draft.randomArtwork) {
        rotationStep
    } else {
        uiState.draft.batteryLevelIndex
    }
    Box(modifier = Modifier.fillMaxSize()) {
        StatusBarEditorWallpaper()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            TrollCustomizeTopBar(
                title = stringResource(R.string.battery_troll_customize_title),
                onBack = onBack
            )
            HomeEnableCard(
                text = stringResource(R.string.discover_battery_enable_prompt),
                checked = uiState.isBatteryEnabled,
                onCheckedChange = onBatteryToggle
            )
            // The same card the status-bar editor previews with, in the same place relative to
            // the switch above it: one renderer means a troll can never be previewed by rules the
            // real bar does not follow. `HomeEnableCard` already contributes the 9sdp above it,
            // which is the gap `StatusBarOverview` leaves over its own preview.
            BatteryStatusPreviewCard(
                config = uiState.previewConfig,
                deviceState = uiState.systemState.deviceState,
                powerState = uiState.systemState.powerState,
                // Off means "draw no character": the card has no fallback, which is exactly what
                // `trollShowEmoji` promises on the real bar.
                emojiPath = if (uiState.draft.showEmoji) {
                    uiState.troll?.emojiPaths
                        ?.getOrNull(previewEmojiIndex)
                        ?.let(BatteryTrollServerConfig::resolve)
                } else {
                    null
                },
                batteryPath = uiState.troll?.batteryPaths
                    ?.getOrNull(previewBatteryIndex)
                    ?.let(BatteryTrollServerConfig::resolve),
                backgroundPath = uiState.backgroundPath,
                emotionPath = uiState.emotionPath,
                animation = uiState.animation,
                // Nothing on this screen edits a single status-bar component, so no component is
                // forced on for the sake of showing it off.
                focusedComponent = null,
                mobileDataLabel = uiState.systemState.deviceState.mobileDataBadge?.label,
                // The prank's number, not the device's — the true level still reaches the card
                // through `powerState` and keeps the layout honest.
                displayPercent = uiState.previewPercent,
                // The pair is one drawing, so the character is sized off the artwork rather than
                // off the emoji slider, exactly as the real bar does it.
                trollEmojiSizeDp = uiState.troll?.let { troll ->
                    batteryTrollEmojiSizeDp(
                        batterySizeDp = uiState.previewConfig.batterySizeDp,
                        emojiCanvasPx = troll.emojiCanvasPx,
                        batteryCanvasPx = troll.batteryCanvasPx,
                        fallbackEmojiSizeDp = uiState.previewConfig.emojiSizeDp
                    )
                },
                modifier = Modifier.padding(
                    horizontal = dimensionResource(SdpR.dimen._12sdp)
                )
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
            ) {
                when {
                    uiState.isLoading -> TrollCustomizeLoading()
                    uiState.isUnavailable -> TrollCustomizeUnavailable(
                        error = uiState.catalogError,
                        onRetry = onRetry
                    )

                    else -> {
                        TrollModeGroup(
                            uiState = uiState,
                            emojiLevelIndex = previewEmojiIndex,
                            batteryLevelIndex = previewBatteryIndex,
                            onModeChange = onModeChange,
                            onEditPercentRequest = onEditPercentRequest
                        )
                        TrollPercentageGroup(
                            uiState = uiState,
                            onShowPercentageToggle = onShowPercentageToggle,
                            onPercentSizeChange = onPercentSizeChange
                        )
                        TrollEmojiGroup(
                            uiState = uiState,
                            onShowEmojiToggle = onShowEmojiToggle,
                            onRandomArtworkChange = onRandomArtworkChange,
                            onEmojiLevelChange = onEmojiLevelChange,
                            onBatteryLevelChange = onBatteryLevelChange
                        )
                    }
                }
                Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
            }
            TrollApplyPanel(onApply = onApply, enabled = uiState.isApplyEnabled)
        }
    }

    if (uiState.isEditingFakePercent) {
        BatteryTrollEditPercentDialog(
            initialPercent = uiState.draft.fakePercent,
            onConfirm = onEditPercentConfirm,
            onDismiss = onEditPercentDismiss
        )
    }
    if (uiState.isDiscardVisible) {
        BatteryDiscardChangesSheet(
            onDismiss = onDiscardDismiss,
            onExit = onDiscardConfirm
        )
    }
}

@Composable
private fun TrollModeGroup(
    uiState: BatteryTrollCustomizeUiState,
    emojiLevelIndex: Int,
    batteryLevelIndex: Int,
    onModeChange: (BatteryTrollMode) -> Unit,
    onEditPercentRequest: () -> Unit
) {
    TrollGroup(title = stringResource(R.string.battery_troll_mode)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
        ) {
            TrollModeSegmentedControl(
                mode = uiState.draft.mode,
                onModeChange = onModeChange
            )
            TrollInfoChip(text = stringResource(R.string.battery_troll_mode_hint))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
        ) {
            Column(
                modifier = Modifier.weight(1f),
                // Figma `8315:8232` centres the number over the Edit chip in this cell.
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
            ) {
                Text(
                    text = stringResource(
                        R.string.battery_troll_percent_value,
                        uiState.previewPercent
                    ),
                    color = colorResource(R.color.colors_212327),
                    fontFamily = CustomizeRobotoSemiBold,
                    fontSize = dimensionResource(SspR.dimen._25ssp).value.sp,
                    lineHeight = dimensionResource(SspR.dimen._34ssp).value.sp,
                    maxLines = 1
                )
                TrollEditChip(
                    enabled = uiState.isEditEnabled,
                    onClick = onEditPercentRequest
                )
            }
            TrollThemePreview(
                troll = uiState.troll,
                showEmoji = uiState.draft.showEmoji,
                emojiLevelIndex = emojiLevelIndex,
                batteryLevelIndex = batteryLevelIndex
            )
        }
    }
}

@Composable
private fun TrollPercentageGroup(
    uiState: BatteryTrollCustomizeUiState,
    onShowPercentageToggle: () -> Unit,
    onPercentSizeChange: (Float) -> Unit
) {
    TrollGroup(title = stringResource(R.string.battery_troll_percentage)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.battery_troll_size),
                    color = colorResource(R.color.colors_212327),
                    fontFamily = CustomizeRobotoMedium,
                    fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
                    lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp,
                    modifier = Modifier.weight(1f)
                )
                AppSwitch(
                    checked = uiState.draft.showPercentage,
                    onCheckedChange = onShowPercentageToggle
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(
                        if (uiState.draft.showPercentage) 1f else TROLL_DISABLED_ALPHA
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
            ) {
                TrollSizeSlider(
                    value = uiState.draft.percentSizeDp,
                    range = PERCENT_SIZE_RANGE,
                    enabled = uiState.draft.showPercentage,
                    onValueChange = onPercentSizeChange,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(
                        R.string.battery_editor_dp_value,
                        uiState.draft.percentSizeDp.toInt()
                    ),
                    color = colorResource(R.color.colors_212327),
                    fontFamily = CustomizeRobotoMedium,
                    fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                    lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun TrollEmojiGroup(
    uiState: BatteryTrollCustomizeUiState,
    onShowEmojiToggle: () -> Unit,
    onRandomArtworkChange: (Boolean) -> Unit,
    onEmojiLevelChange: (Int) -> Unit,
    onBatteryLevelChange: (Int) -> Unit
) {
    TrollGroup(title = stringResource(R.string.battery_troll_emoji)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._1sdp))
            ) {
                Text(
                    text = stringResource(R.string.battery_troll_custom),
                    color = colorResource(R.color.colors_212327),
                    fontFamily = CustomizeRobotoMedium,
                    fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
                    lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp
                )
                Text(
                    text = stringResource(R.string.battery_troll_custom_hint),
                    color = colorResource(R.color.colors_9B9C9E),
                    fontFamily = CustomizeRobotoRegular,
                    fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
                    lineHeight = dimensionResource(SspR.dimen._12ssp).value.sp
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._31sdp))
            ) {
                TrollRadioOption(
                    label = stringResource(R.string.battery_troll_custom),
                    selected = !uiState.draft.randomArtwork,
                    onClick = { onRandomArtworkChange(false) }
                )
                TrollRadioOption(
                    label = stringResource(R.string.battery_troll_random),
                    selected = uiState.draft.randomArtwork,
                    onClick = { onRandomArtworkChange(true) }
                )
            }
        }
        HorizontalDivider(
            thickness = dimensionResource(SdpR.dimen._1sdp),
            color = colorResource(R.color.colors_DEDEDF)
        )
        TrollArtworkBlock(
            iconRes = R.drawable.img_emoji_love,
            label = stringResource(R.string.battery_troll_emoji),
            paths = uiState.troll?.emojiPaths.orEmpty(),
            selectedIndex = uiState.draft.emojiLevelIndex,
            enabled = uiState.isArtworkPickerEnabled,
            onSelect = onEmojiLevelChange,
            // Off keeps the troll running with its battery shell and faked percentage but drops
            // the character, which is exactly what `BatteryTrollAssetPolicy` does with
            // `BatteryStatusConfig.trollShowEmoji`. The level tiles dim with it because a level
            // for artwork nobody draws is not a choice.
            tilesEnabled = uiState.draft.showEmoji,
            trailingSwitch = {
                AppSwitch(
                    checked = uiState.draft.showEmoji,
                    onCheckedChange = onShowEmojiToggle
                )
            }
        )
        TrollArtworkBlock(
            iconRes = R.drawable.ic_logo_battery_emoji,
            label = stringResource(R.string.battery_troll_battery),
            paths = uiState.troll?.batteryPaths.orEmpty(),
            selectedIndex = uiState.draft.batteryLevelIndex,
            enabled = uiState.isArtworkPickerEnabled,
            onSelect = onBatteryLevelChange
        )
    }
}

@Composable
private fun TrollGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
    ) {
        TrollSectionHeader(title)
        TrollCard(content = content)
    }
}

/** Same range the status-bar editor gives the percentage, because it is the same stored field. */
private val PERCENT_SIZE_RANGE = 10f..32f
