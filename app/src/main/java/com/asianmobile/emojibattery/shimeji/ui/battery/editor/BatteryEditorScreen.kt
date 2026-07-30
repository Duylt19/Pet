package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryAccessibility
import com.asianmobile.emojibattery.shimeji.data.model.MAX_BATTERY_BAR_HEIGHT_DP
import com.asianmobile.emojibattery.shimeji.data.model.MIN_BATTERY_BAR_HEIGHT_DP
import com.asianmobile.emojibattery.shimeji.ui.component.CutePetTopBar
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR
import java.io.File

@Composable
fun BatteryEditorScreen(
    onBack: () -> Unit,
    viewModel: BatteryEditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showDisclosure by remember { mutableStateOf(false) }
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
        accessibilityEnabled = accessibilityEnabled,
        onBack = onBack,
        onShowTime = viewModel::setShowTime,
        onShowPercentage = viewModel::setShowPercentage,
        onBarHeight = viewModel::setBarHeight,
        onEmojiSize = viewModel::setEmojiSize,
        onBatterySize = viewModel::setBatterySize,
        onBackgroundColor = viewModel::setBackgroundColor,
        onForegroundColor = viewModel::setForegroundColor,
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
    accessibilityEnabled: Boolean,
    onBack: () -> Unit,
    onShowTime: (Boolean) -> Unit,
    onShowPercentage: (Boolean) -> Unit,
    onBarHeight: (Float) -> Unit,
    onEmojiSize: (Float) -> Unit,
    onBatterySize: (Float) -> Unit,
    onBackgroundColor: (Int) -> Unit,
    onForegroundColor: (Int) -> Unit,
    onApply: () -> Unit,
    onDisable: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_FFF9F4))
            .navigationBarsPadding()
    ) {
        CutePetTopBar(title = stringResource(R.string.battery_editor_title), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimensionResource(SdpR.dimen._16sdp))
        ) {
            Text(
                text = if (state.theme.isBuiltIn) {
                    stringResource(R.string.battery_builtin_theme)
                } else {
                    state.theme.name
                },
                color = colorResource(R.color.colors_2F2440),
                fontFamily = FontFamily(Font(R.font.inter_semibold)),
                fontSize = dimensionResource(SspR.dimen._15ssp).value.sp
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._10sdp)))
            BatteryPreview(state)
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(R.color.colors_FFFFFF)
                ),
                shape = RoundedCornerShape(dimensionResource(SdpR.dimen._16sdp))
            ) {
                Column(Modifier.padding(dimensionResource(SdpR.dimen._14sdp))) {
                    ToggleRow(
                        label = stringResource(R.string.battery_show_time),
                        checked = state.config.showTime,
                        onChecked = onShowTime
                    )
                    ToggleRow(
                        label = stringResource(R.string.battery_show_percentage),
                        checked = state.config.showPercentage,
                        onChecked = onShowPercentage
                    )
                    ColorPalette(
                        label = stringResource(R.string.battery_background_color),
                        selected = state.config.backgroundColorArgb,
                        onColor = onBackgroundColor
                    )
                    ColorPalette(
                        label = stringResource(R.string.battery_foreground_color),
                        selected = state.config.foregroundColorArgb,
                        onColor = onForegroundColor
                    )
                    EditorSlider(
                        label = stringResource(R.string.battery_bar_height),
                        value = state.config.barHeightDp,
                        range = MIN_BATTERY_BAR_HEIGHT_DP..MAX_BATTERY_BAR_HEIGHT_DP,
                        onValue = onBarHeight
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
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._10sdp)))
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
                fontSize = dimensionResource(SspR.dimen._9ssp).value.sp
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._14sdp)))
        }
        Button(
            onClick = onApply,
            enabled = state.isThemeAvailable,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.colors_12B890)
            ),
            shape = RoundedCornerShape(dimensionResource(SdpR.dimen._20sdp)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(SdpR.dimen._16sdp))
                .height(dimensionResource(SdpR.dimen._48sdp))
        ) {
            Text(
                text = stringResource(R.string.battery_apply),
                color = colorResource(R.color.colors_FFFFFF),
                fontFamily = FontFamily(Font(R.font.inter_semibold))
            )
        }
        if (state.config.enabled) {
            TextButton(
                onClick = onDisable,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(SdpR.dimen._16sdp))
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(config.barHeightDp.dp)
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._20sdp)))
            .background(Color(config.backgroundColorArgb))
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (config.showTime) {
            Text(
                text = stringResource(R.string.battery_preview_time),
                color = Color(config.foregroundColorArgb),
                fontFamily = FontFamily(Font(R.font.inter_semibold))
            )
        }
        state.theme.emojiPath?.let { path ->
            AsyncImage(
                model = File(path),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(config.emojiSizeDp.dp)
            )
        }
        Spacer(Modifier.weight(1f))
        if (config.showPercentage) {
            Text(
                text = stringResource(R.string.battery_preview_percentage),
                color = Color(config.foregroundColorArgb),
                fontFamily = FontFamily(Font(R.font.inter_semibold))
            )
        }
        state.theme.batteryPath?.let { path ->
            AsyncImage(
                model = File(path),
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

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = colorResource(R.color.colors_2F2440))
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedTrackColor = colorResource(R.color.colors_12B890)
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
        color = colorResource(R.color.colors_776D84)
    )
    Slider(value = value, onValueChange = onValue, valueRange = range)
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
    Text(text = label, color = colorResource(R.color.colors_776D84))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(SdpR.dimen._6sdp)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._8sdp))
    ) {
        colors.forEach { colorRes ->
            val color = colorResource(colorRes)
            val isSelected = selected == color.toArgb()
            Box(
                modifier = Modifier
                    .size(dimensionResource(SdpR.dimen._28sdp))
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
                    .clickable { onColor(color.toArgb()) }
            )
        }
    }
}
