package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryAccessibility
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDecorationEntry
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDataType
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDateFont
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDateFormat
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.MAX_BATTERY_BAR_HEIGHT_DP
import com.asianmobile.emojibattery.shimeji.data.model.MIN_BATTERY_BAR_HEIGHT_DP
import com.asianmobile.emojibattery.shimeji.ui.component.CutePetTopBar
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

private enum class BatteryEditorPage {
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
    DATE_TIME
}

@Composable
fun BatteryEditorScreen(
    onBack: () -> Unit,
    viewModel: BatteryEditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showDisclosure by remember { mutableStateOf(false) }
    var pageName by rememberSaveable {
        mutableStateOf(BatteryEditorPage.OVERVIEW.name)
    }
    val page = BatteryEditorPage.entries.firstOrNull { it.name == pageName }
        ?: BatteryEditorPage.OVERVIEW
    val openPage: (BatteryEditorPage) -> Unit = { pageName = it.name }
    val closePage = { pageName = BatteryEditorPage.OVERVIEW.name }
    var accessibilityEnabled by remember {
        mutableStateOf(BatteryAccessibility.isEnabled(context))
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        accessibilityEnabled = BatteryAccessibility.isEnabled(context)
        if (accessibilityEnabled) viewModel.apply()
    }

    TrackScreenView(ScreenName.BATTERY_EDITOR)
    BackHandler(enabled = page != BatteryEditorPage.OVERVIEW, onBack = closePage)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityEnabled = BatteryAccessibility.isEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BatteryEditorContent(
        state = state,
        page = page,
        accessibilityEnabled = accessibilityEnabled,
        onBack = if (page == BatteryEditorPage.OVERVIEW) onBack else closePage,
        onDone = closePage,
        onOpenPage = openPage,
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
        onConfig = viewModel::setConfig,
        onApply = {
            if (accessibilityEnabled) viewModel.apply() else showDisclosure = true
        },
        onDisable = viewModel::disable
    )

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
    onConfig: (BatteryStatusConfig) -> Unit,
    onApply: () -> Unit,
    onDisable: () -> Unit
) {
    val scrollState = remember(page) { ScrollState(initial = 0) }
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
            ThemeName(state)
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._8sdp)))
            BatteryPreview(state)
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
            when (page) {
                BatteryEditorPage.OVERVIEW -> OverviewEditor(
                    state = state,
                    onOpenPage = onOpenPage,
                    onShowTime = onShowTime,
                    onShowPercentage = onShowPercentage
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
                    { onConfig(state.config.copy(wifiSizeDp = it)) },
                    { onConfig(state.config.copy(wifiColorArgb = it)) }
                )
                BatteryEditorPage.DATA -> DataEditor(state.config, onConfig)
                BatteryEditorPage.SIGNAL -> StatusComponentEditor(
                    state.config,
                    state.config.signalSizeDp,
                    state.config.signalColorArgb,
                    { onConfig(state.config.copy(signalSizeDp = it)) },
                    { onConfig(state.config.copy(signalColorArgb = it)) }
                )
                BatteryEditorPage.AIRPLANE -> StatusComponentEditor(
                    state.config,
                    state.config.airplaneSizeDp,
                    state.config.airplaneColorArgb,
                    { onConfig(state.config.copy(airplaneSizeDp = it)) },
                    { onConfig(state.config.copy(airplaneColorArgb = it)) }
                )
                BatteryEditorPage.HOTSPOT -> StatusComponentEditor(
                    state.config,
                    state.config.hotspotSizeDp,
                    state.config.hotspotColorArgb,
                    { onConfig(state.config.copy(hotspotSizeDp = it)) },
                    { onConfig(state.config.copy(hotspotColorArgb = it)) }
                )
                BatteryEditorPage.RINGER -> StatusComponentEditor(
                    state.config,
                    state.config.ringerSizeDp,
                    state.config.ringerColorArgb,
                    { onConfig(state.config.copy(ringerSizeDp = it)) },
                    { onConfig(state.config.copy(ringerColorArgb = it)) }
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
private fun ThemeName(state: BatteryEditorUiState) {
    Text(
        text = if (state.theme.isBuiltIn) {
            stringResource(R.string.battery_builtin_theme)
        } else {
            state.theme.name
        },
        color = colorResource(R.color.colors_2F2440),
        fontFamily = FontFamily(Font(R.font.inter_semibold)),
        fontSize = dimensionResource(SspR.dimen._13ssp).value.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun OverviewEditor(
    state: BatteryEditorUiState,
    onOpenPage: (BatteryEditorPage) -> Unit,
    onShowTime: (Boolean) -> Unit,
    onShowPercentage: (Boolean) -> Unit
) {
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
            range = MIN_BATTERY_BAR_HEIGHT_DP..MAX_BATTERY_BAR_HEIGHT_DP,
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
    onSize: (Float) -> Unit,
    onColor: (Int) -> Unit
) {
    EditorPageHint(stringResource(R.string.battery_status_component_hint))
    EditorCard {
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
private fun ChargeStylePicker(
    selected: Int,
    color: Int,
    onSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    Text(
        text = stringResource(R.string.battery_charge_style),
        color = colorResource(R.color.colors_776D84),
        fontFamily = FontFamily(Font(R.font.inter_medium)),
        fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
        modifier = Modifier.padding(top = dimensionResource(SdpR.dimen._8sdp))
    )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._8sdp)),
        modifier = Modifier.padding(vertical = dimensionResource(SdpR.dimen._7sdp))
    ) {
        items((1..12).toList()) { index ->
            val resourceId = remember(index) {
                context.resources.getIdentifier(
                    "charge_%02d".format(index),
                    "drawable",
                    context.packageName
                )
            }
            Box(
                modifier = Modifier
                    .size(dimensionResource(SdpR.dimen._54sdp))
                    .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp)))
                    .background(colorResource(R.color.colors_FFFFFF))
                    .border(
                        width = dimensionResource(
                            if (selected == index) {
                                SdpR.dimen._3sdp
                            } else {
                                SdpR.dimen._1sdp
                            }
                        ),
                        color = colorResource(
                            if (selected == index) {
                                R.color.colors_12B890
                            } else {
                                R.color.colors_E9DFEF
                            }
                        ),
                        shape = RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp))
                    )
                    .clickable { onSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                if (resourceId != 0) {
                    Icon(
                        painter = painterResource(resourceId),
                        contentDescription = index.toString(),
                        tint = Color(color),
                        modifier = Modifier.size(dimensionResource(SdpR.dimen._28sdp))
                    )
                } else {
                    Text(index.toString(), color = Color(color))
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
            label = stringResource(R.string.battery_status_icon_size),
            value = config.dateTimeSizeDp,
            range = 8f..32f,
            onValue = { onConfig(config.copy(dateTimeSizeDp = it)) }
        )
        ColorPalette(
            label = stringResource(R.string.battery_status_icon_color),
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
        R.string.battery_component_animation to BatteryEditorPage.ANIMATION,
        R.string.battery_component_wifi to BatteryEditorPage.WIFI,
        R.string.battery_component_data to BatteryEditorPage.DATA,
        R.string.battery_component_signal to BatteryEditorPage.SIGNAL,
        R.string.battery_component_airplane to BatteryEditorPage.AIRPLANE,
        R.string.battery_component_hotspot to BatteryEditorPage.HOTSPOT,
        R.string.battery_component_ringer to BatteryEditorPage.RINGER,
        R.string.battery_component_charge to BatteryEditorPage.CHARGE,
        R.string.battery_component_date to BatteryEditorPage.DATE_TIME
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
                rowComponents.forEach { (label, page) ->
                    StatusComponentTile(
                        label = stringResource(label),
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenPage(page) }
                    )
                }
                if (rowComponents.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatusComponentTile(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
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
            Text(
                text = label.take(1),
                color = colorResource(R.color.colors_12B890),
                fontFamily = FontFamily(Font(R.font.inter_bold))
            )
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
    isApplied: Boolean,
    onApply: () -> Unit,
    onDisable: () -> Unit
) {
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
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.colors_12B890)
            ),
            shape = RoundedCornerShape(dimensionResource(SdpR.dimen._20sdp)),
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(SdpR.dimen._48sdp))
        ) {
            Text(
                text = stringResource(R.string.battery_apply),
                color = colorResource(R.color.colors_FFFFFF),
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
private fun BatteryPreview(state: BatteryEditorUiState) {
    val config = state.config
    val backgroundPath = state.backgrounds
        .firstOrNull { it.id == config.backgroundDecorationId }
        ?.assetPath
    val emotionPath = state.emotions
        .firstOrNull { it.id == config.emotionDecorationId }
        ?.assetPath
    val animationPath = state.animations
        .firstOrNull { it.name == config.animationAssetName }
        ?.assetPath
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(config.barHeightDp.dp)
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._20sdp)))
            .background(Color(config.backgroundColorArgb))
    ) {
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
            if (config.showTime) {
                Text(
                    text = stringResource(R.string.battery_preview_time),
                    color = Color(config.foregroundColorArgb),
                    fontFamily = FontFamily(Font(R.font.inter_semibold))
                )
            }
            if (config.showDateTime) {
                Text(
                    text = stringResource(R.string.battery_preview_date),
                    color = Color(config.dateTimeColorArgb),
                    fontSize = config.dateTimeSizeDp.sp,
                    maxLines = 1
                )
            }
            if (config.showAnimation) {
                animationPath?.let { path ->
                    AsyncImage(
                        model = path,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(config.animationSizeDp.dp)
                    )
                }
            }
            state.theme.emojiPath?.let { path ->
                AsyncImage(
                    model = path,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(config.emojiSizeDp.dp)
                )
            }
            if (config.showEmotion) {
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
            Text(
                text = stringResource(R.string.battery_preview_signal),
                color = Color(config.signalColorArgb),
                fontSize = config.signalSizeDp.sp
            )
            Text(
                text = config.dataType.label,
                color = Color(config.dataColorArgb),
                fontSize = config.dataSizeDp.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.battery_preview_wifi),
                color = Color(config.wifiColorArgb),
                fontSize = config.wifiSizeDp.sp
            )
            if (config.showPercentage) {
                Text(
                    text = stringResource(R.string.battery_preview_percentage),
                    color = Color(config.percentColorArgb),
                    fontFamily = FontFamily(Font(R.font.inter_semibold)),
                    fontSize = config.percentSizeDp.sp
                )
            }
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
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colorResource(R.color.colors_FFFFFF),
                checkedTrackColor = colorResource(R.color.colors_12B890),
                checkedBorderColor = colorResource(R.color.colors_12B890),
                uncheckedThumbColor = colorResource(R.color.colors_FFFFFF),
                uncheckedTrackColor = colorResource(R.color.colors_E0F7F1),
                uncheckedBorderColor = colorResource(R.color.colors_C8C8C9)
            )
        )
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
                        contentDescription = label
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
            .semantics { this.contentDescription = contentDescription }
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
        onConfig = {},
        onApply = {},
        onDisable = {}
    )
}
