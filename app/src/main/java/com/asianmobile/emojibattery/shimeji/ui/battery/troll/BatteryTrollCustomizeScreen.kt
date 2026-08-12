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
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollMode
import com.asianmobile.emojibattery.shimeji.ui.battery.editor.BatteryDiscardChangesSheet
import com.asianmobile.emojibattery.shimeji.ui.shared.component.AppSwitch
import com.asianmobile.emojibattery.shimeji.ui.shared.component.GrantPermissionDialog
import com.asianmobile.emojibattery.shimeji.ui.shared.component.HomeEnableCard
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

/**
 * Battery Troll — Customize (Figma `8315:8232` default, `8359:6992` Real + Random,
 * `8359:7165` discard).
 *
 * Analytics is deliberately not tracked here: `ScreenName` belongs to the agent that owns
 * `utils/AnalyticsHelper.kt`, and this screen must be wired into it when its route lands.
 */
@Composable
fun BatteryTrollCustomizeScreen(
    onNavigateBack: () -> Unit,
    viewModel: BatteryTrollCustomizeViewModel = hiltViewModel(),
    accessibilityHowToUseResult: Boolean? = null,
    onAccessibilityHowToUseResultConsumed: () -> Unit = {},
    onNavigateToAccessibilityHowToUse: () -> Unit = {},
    bannerAdContent: @Composable () -> Unit = {}
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
        onRandomArtworkChange = viewModel::onRandomArtworkChange,
        onEmojiLevelChange = viewModel::onEmojiLevelChange,
        onBatteryLevelChange = viewModel::onBatteryLevelChange,
        onDiscardDismiss = viewModel::onDiscardDismiss,
        onDiscardConfirm = viewModel::onDiscardConfirm,
        onApply = viewModel::apply,
        bannerAdContent = bannerAdContent
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
    onRandomArtworkChange: (Boolean) -> Unit,
    onEmojiLevelChange: (Int) -> Unit,
    onBatteryLevelChange: (Int) -> Unit,
    onDiscardDismiss: () -> Unit,
    onDiscardConfirm: () -> Unit,
    onApply: () -> Unit,
    bannerAdContent: @Composable () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        TrollWallpaper()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            TrollStatusBarPreview(
                troll = uiState.troll,
                percent = uiState.previewPercent,
                emojiLevelIndex = uiState.draft.emojiLevelIndex,
                batteryLevelIndex = uiState.draft.batteryLevelIndex
            )
            TrollCustomizeTopBar(
                title = stringResource(R.string.battery_troll_customize_title),
                onBack = onBack
            )
            HomeEnableCard(
                text = stringResource(R.string.discover_battery_enable_prompt),
                checked = uiState.isBatteryEnabled,
                onCheckedChange = onBatteryToggle
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
            ) {
                TrollModeGroup(
                    uiState = uiState,
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
                    onRandomArtworkChange = onRandomArtworkChange,
                    onEmojiLevelChange = onEmojiLevelChange,
                    onBatteryLevelChange = onBatteryLevelChange
                )
                Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
            }
            TrollApplyPanel(onApply = onApply)
            bannerAdContent()
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
                emojiLevelIndex = uiState.draft.emojiLevelIndex,
                batteryLevelIndex = uiState.draft.batteryLevelIndex
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
            trailingSwitch = {
                // Figma shows this switch on in every frame and never shows an off state, and the
                // draft has no field for it — so it stays a read-only marker until the owner says
                // what turning it off should do.
                AppSwitch(checked = true, onCheckedChange = {}, interactive = false)
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
