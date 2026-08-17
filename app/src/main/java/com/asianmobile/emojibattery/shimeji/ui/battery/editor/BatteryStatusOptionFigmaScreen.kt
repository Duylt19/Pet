package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryConnectivityState
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryHotspotState
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryRingerState
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryStatusDrawableCatalog
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatterySystemStatusPolicy
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDateFont
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDateFormat
import com.asianmobile.emojibattery.shimeji.data.model.BatteryAnimationEntry
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.ui.shared.component.AppSwitch
import com.asianmobile.emojibattery.shimeji.ui.shared.theme.RobotoFontFamily
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

private val OptionColorWheel = Brush.sweepGradient(
    (0 until 360 step 15).map { hue -> Color.hsv(hue.toFloat(), 0.72f, 0.94f) }
)

internal fun BatteryEditorPage.isStatusOptionPage(): Boolean = this in setOf(
    BatteryEditorPage.AIRPLANE,
    BatteryEditorPage.RINGER,
    BatteryEditorPage.DATE_TIME,
    BatteryEditorPage.HOTSPOT,
    BatteryEditorPage.CHARGE,
    BatteryEditorPage.CLOCK,
    BatteryEditorPage.ANIMATION,
    BatteryEditorPage.WIFI,
    BatteryEditorPage.SIGNAL,
    BatteryEditorPage.DATA
)

@Composable
internal fun BatteryStatusOptionFigmaScreen(
    state: BatteryEditorUiState,
    page: BatteryEditorPage,
    onBack: () -> Unit,
    onConfig: (BatteryStatusConfig) -> Unit,
    showEmbeddedPreview: Boolean = true
) {
    val config = state.config
    var showColorPicker by remember { mutableStateOf(false) }
    val spec = optionSpec(page, config)

    Box(Modifier.fillMaxSize()) {
        StatusBarEditorWallpaper()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            StatusOptionTopBar(
                title = spec.title,
                checked = spec.enabled,
                onBack = onBack,
                onCheckedChange = { enabled -> onConfig(spec.withEnabled(enabled)) }
            )
            if (showEmbeddedPreview) {
                BatteryPreview(
                    state = state,
                    page = page,
                    modifier = Modifier.padding(
                        start = dimensionResource(SdpR.dimen._12sdp),
                        end = dimensionResource(SdpR.dimen._12sdp),
                        top = dimensionResource(SdpR.dimen._9sdp)
                    )
                )
            }
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = dimensionResource(SdpR.dimen._12sdp),
                    end = dimensionResource(SdpR.dimen._12sdp),
                    bottom = dimensionResource(SdpR.dimen._12sdp)
                ),
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(SdpR.dimen._12sdp)
                )
            ) {
                item {
                    DesignSlider(
                        label = stringResource(R.string.battery_editor_size_label),
                        value = spec.size,
                        range = if (page == BatteryEditorPage.ANIMATION) 12f..36f else 8f..32f,
                        onValueChange = { onConfig(spec.withSize(it)) }
                    )
                }
                if (page != BatteryEditorPage.ANIMATION) {
                    item {
                        StatusOptionColorSection(
                            selected = spec.color,
                            onSelected = { onConfig(spec.withColor(it)) },
                            onCustomClick = { showColorPicker = true }
                        )
                    }
                }
                when (page) {
                    BatteryEditorPage.ANIMATION -> animationStyleItems(
                        animations = state.animations,
                        selectedName = config.animationAssetName,
                        onSelected = { animation ->
                            onConfig(
                                config.copy(
                                    showAnimation = true,
                                    animationAssetName = animation.name
                                )
                            )
                        }
                    )
                    BatteryEditorPage.WIFI -> item {
                        StatusOptionStyleSection(
                            title = stringResource(R.string.battery_status_icon_style),
                            selected = config.wifiIconStyleIndex,
                            color = config.wifiColorArgb,
                            iconStyles = wifiStyles(),
                            onSelected = { onConfig(config.copy(wifiIconStyleIndex = it)) }
                        )
                    }
                    BatteryEditorPage.SIGNAL -> item {
                        StatusOptionStyleSection(
                            title = stringResource(R.string.battery_status_icon_style),
                            selected = config.signalIconStyleIndex,
                            color = config.signalColorArgb,
                            iconStyles = signalStyles(),
                            onSelected = { onConfig(config.copy(signalIconStyleIndex = it)) }
                        )
                    }
                    BatteryEditorPage.DATE_TIME -> {
                        item { DateFormatSection(config, onConfig) }
                        item { DateStyleSection(config, onConfig) }
                    }
                    BatteryEditorPage.AIRPLANE -> item {
                        StatusOptionStyleSection(
                            title = stringResource(R.string.battery_status_icon_style),
                            selected = config.airplaneIconStyleIndex,
                            color = config.airplaneColorArgb,
                            iconStyles = airplaneStyles(),
                            onSelected = {
                                onConfig(config.copy(airplaneIconStyleIndex = it))
                            }
                        )
                    }
                    BatteryEditorPage.RINGER -> item {
                        StatusOptionStyleSection(
                            title = stringResource(R.string.battery_status_icon_style),
                            selected = config.ringerIconStyleIndex,
                            color = config.ringerColorArgb,
                            iconStyles = ringerStyles(),
                            onSelected = { onConfig(config.copy(ringerIconStyleIndex = it)) }
                        )
                    }
                    BatteryEditorPage.HOTSPOT -> item {
                        StatusOptionStyleSection(
                            title = stringResource(R.string.battery_status_icon_style),
                            selected = config.hotspotIconStyleIndex,
                            color = config.hotspotColorArgb,
                            iconStyles = hotspotStyles(),
                            onSelected = { onConfig(config.copy(hotspotIconStyleIndex = it)) }
                        )
                    }
                    BatteryEditorPage.CHARGE -> item {
                        StatusOptionStyleSection(
                            title = stringResource(R.string.battery_charge_style_figma),
                            selected = config.chargeIconIndex,
                            color = config.chargeColorArgb,
                            iconStyles = CHARGE_STYLE_ORDER.map { index ->
                                listOf("charge_%02d".format(index))
                            },
                            styleValues = CHARGE_STYLE_ORDER,
                            onSelected = { onConfig(config.copy(chargeIconIndex = it)) }
                        )
                    }
                    else -> Unit
                }
            }
        }
    }

    if (showColorPicker) {
        StatusBarColorPickerSheet(
            selectedColor = spec.color,
            onColorChange = { onConfig(spec.withColor(it)) },
            onDismiss = { showColorPicker = false }
        )
    }
}

@Composable
private fun StatusOptionTopBar(
    title: String,
    checked: Boolean,
    onBack: () -> Unit,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._43sdp))
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._22sdp))
                .clip(CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_favorite_recent_back),
                contentDescription = stringResource(R.string.back),
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.width(dimensionResource(SdpR.dimen._9sdp)))
        androidx.compose.material3.Text(
            text = title,
            color = colorResource(R.color.colors_212327),
            fontFamily = RobotoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = dimensionResource(SspR.dimen._15ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._22ssp).value.sp
        )
        Spacer(Modifier.weight(1f))
        AppSwitch(
            checked = checked,
            onCheckedChange = { onCheckedChange(!checked) }
        )
    }
}

@Composable
private fun StatusOptionColorSection(
    selected: Int,
    onSelected: (Int) -> Unit,
    onCustomClick: () -> Unit
) {
    val colors = listOf(
        0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFF545454.toInt(),
        0xFFFF3939.toInt(), 0xFFFF7E39.toInt(), 0xFFFBC41F.toInt(),
        0xFF04F000.toInt(), 0xFF39C0FF.toInt(), 0xFF394AFF.toInt(),
        0xFFFF39EF.toInt(), 0xFFFF397E.toInt()
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
    ) {
        OptionSectionTitle(stringResource(R.string.battery_editor_color))
        val options = listOf<Int?>(null) + colors
        options.chunked(6).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                row.forEach { color ->
                    val active = color?.let { selected == it } ?: (selected !in colors)
                    Box(
                        modifier = Modifier
                            .size(dimensionResource(SdpR.dimen._31sdp))
                            .clip(CircleShape)
                            .background(color?.let(::Color) ?: Color.Transparent)
                            .then(
                                if (color == null) Modifier.background(OptionColorWheel)
                                else Modifier
                            )
                            .border(
                                dimensionResource(if (active) SdpR.dimen._2sdp else SdpR.dimen._1sdp),
                                colorResource(if (active) R.color.colors_FB3675 else R.color.colors_FFEBF1),
                                CircleShape
                            )
                            .semantics {
                                this.selected = active
                                contentDescription = color?.let {
                                    "#${it.toUInt().toString(16)}"
                                } ?: "Custom color"
                            }
                            .clickable {
                                if (color == null) onCustomClick() else onSelected(color)
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionSectionTitle(value: String) {
    androidx.compose.material3.Text(
        text = value,
        color = colorResource(R.color.colors_212327),
        fontFamily = RobotoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
        lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp
    )
}

private fun LazyListScope.animationStyleItems(
    animations: List<BatteryAnimationEntry>,
    selectedName: String,
    onSelected: (BatteryAnimationEntry) -> Unit
) {
    item(key = "animation_style_title") {
        OptionSectionTitle(stringResource(R.string.battery_animation_style))
    }
    val rows = animations.chunked(4)
    items(
        count = rows.size,
        key = { rowIndex -> "animation_row_${rows[rowIndex].first().id}" }
    ) { rowIndex ->
        val rowAnimations = rows[rowIndex]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
        ) {
            rowAnimations.forEach { animation ->
                val active = animation.name == selectedName
                val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(shape)
                        .background(
                            colorResource(
                                if (active) R.color.colors_FFEBF1 else R.color.colors_FFFFFF
                            )
                        )
                        .border(
                            dimensionResource(SdpR.dimen._1sdp),
                            colorResource(
                                if (active) R.color.colors_FB3675 else R.color.colors_DEDEDF
                            ),
                            shape
                        )
                        .semantics { selected = active }
                        .clickable { onSelected(animation) }
                        .padding(dimensionResource(SdpR.dimen._6sdp)),
                    contentAlignment = Alignment.Center
                ) {
                    BatteryAnimationAsset(
                        animation = animation,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            repeat(4 - rowAnimations.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun StatusOptionStyleSection(
    title: String,
    selected: Int,
    color: Int,
    iconStyles: List<List<String>>,
    styleValues: List<Int> = iconStyles.indices.map { it + 1 },
    onSelected: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))) {
        OptionSectionTitle(title)
        iconStyles.chunked(4).forEachIndexed { rowIndex, rowStyles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(SdpR.dimen._9sdp)
                )
            ) {
                rowStyles.forEachIndexed { columnIndex, icons ->
                    val position = rowIndex * 4 + columnIndex
                    val styleValue = styleValues[position]
                    StatusOptionStyleCard(
                        iconNames = icons,
                        selected = selected == styleValue,
                        color = color,
                        onClick = { onSelected(styleValue) },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    )
                }
                repeat(4 - rowStyles.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun StatusOptionStyleCard(
    iconNames: List<String>,
    selected: Boolean,
    color: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val resourceIds = remember(iconNames) {
        iconNames.map(BatteryStatusDrawableCatalog::resolve)
    }
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
    Row(
        modifier = modifier
            .clip(shape)
            .background(colorResource(if (selected) R.color.colors_FFEBF1 else R.color.colors_FFFFFF))
            .border(
                dimensionResource(SdpR.dimen._1sdp),
                colorResource(if (selected) R.color.colors_FB3675 else R.color.colors_DEDEDF),
                shape
            )
            .semantics { this.selected = selected }
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val validIds = resourceIds.filter { it != 0 }
        if (validIds.isEmpty()) {
            Image(
                painter = painterResource(R.drawable.ic_statusbar_custom_charge),
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(SdpR.dimen._32sdp))
            )
        } else {
            validIds.forEach { id ->
                androidx.compose.material3.Icon(
                    painter = painterResource(id),
                    contentDescription = null,
                    tint = Color(color),
                    modifier = Modifier.size(
                        dimensionResource(
                            if (validIds.size > 1) SdpR.dimen._20sdp else SdpR.dimen._32sdp
                        )
                    )
                )
            }
        }
    }
}

@Composable
private fun DateFormatSection(config: BatteryStatusConfig, onConfig: (BatteryStatusConfig) -> Unit) {
    val options = listOf(
        BatteryDateFormat.WEEKDAY_MONTH_DAY to "Mon, Aug 10",
        BatteryDateFormat.WEEKDAY_DAY to "Mon, 10",
        BatteryDateFormat.MONTH_DAY to "Aug 10",
        BatteryDateFormat.WEEKDAY_FULL to "Monday"
    )
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))) {
        OptionSectionTitle(stringResource(R.string.battery_date_format))
        options.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
            ) {
                row.forEach { (format, label) ->
                    DateOptionCard(
                        selected = config.dateFormat == format,
                        onClick = { onConfig(config.copy(dateFormat = format)) },
                        modifier = Modifier
                            .weight(1f)
                            .height(dimensionResource(SdpR.dimen._38sdp))
                    ) {
                        androidx.compose.material3.Text(
                            text = label,
                            color = colorResource(
                                if (config.dateFormat == format) R.color.colors_FB3675 else R.color.colors_000000
                            ),
                            fontFamily = RobotoFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
                            lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateStyleSection(config: BatteryStatusConfig, onConfig: (BatteryStatusConfig) -> Unit) {
    val preview = "Mon, Aug 10"
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))) {
        OptionSectionTitle(stringResource(R.string.battery_date_style))
        BatteryDateFont.entries.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
            ) {
                row.forEach { font ->
                    val active = config.dateTimeFont == font
                    DateOptionCard(
                        selected = active,
                        onClick = { onConfig(config.copy(dateTimeFont = font)) },
                        modifier = Modifier
                            .weight(1f)
                            .height(dimensionResource(SdpR.dimen._62sdp))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            androidx.compose.material3.Text(
                                text = preview,
                                color = colorResource(if (active) R.color.colors_FB3675 else R.color.colors_000000),
                                fontFamily = optionFontFamily(font),
                                fontWeight = optionFontWeight(font),
                                fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
                                lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp,
                                maxLines = 1
                            )
                            androidx.compose.material3.Text(
                                text = font.displayName,
                                color = colorResource(if (active) R.color.colors_FC5E91 else R.color.colors_6F7073),
                                fontFamily = RobotoFontFamily,
                                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                                lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateOptionCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
    Box(
        modifier = modifier
            .clip(shape)
            .background(colorResource(if (selected) R.color.colors_FFEBF1 else R.color.colors_F6F6F6))
            .border(
                dimensionResource(SdpR.dimen._1sdp),
                colorResource(if (selected) R.color.colors_FB3675 else R.color.colors_DEDEDF),
                shape
            )
            .semantics { this.selected = selected }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
@SuppressLint("DiscouragedApi")
private fun optionFontFamily(font: BatteryDateFont): FontFamily {
    val resources = LocalResources.current
    val packageName = LocalContext.current.packageName
    val resourceId = remember(font, resources, packageName) {
        resources.getIdentifier(font.resourceName, "font", packageName)
    }
    return remember(resourceId) {
        if (resourceId == 0) RobotoFontFamily else FontFamily(
            Font(resourceId, weight = optionFontWeight(font))
        )
    }
}

private fun optionFontWeight(font: BatteryDateFont): FontWeight = when (font) {
    BatteryDateFont.MEDIUM -> FontWeight.Medium
    BatteryDateFont.BOLD,
    BatteryDateFont.NUNITO,
    BatteryDateFont.DANCING_SCRIPT -> FontWeight.Bold
    BatteryDateFont.RUSSO_ONE,
    BatteryDateFont.COINY -> FontWeight.Normal
}

private data class StatusOptionSpec(
    val title: String,
    val enabled: Boolean,
    val size: Float,
    val color: Int,
    val withEnabled: (Boolean) -> BatteryStatusConfig,
    val withSize: (Float) -> BatteryStatusConfig,
    val withColor: (Int) -> BatteryStatusConfig
)

@Composable
private fun optionSpec(page: BatteryEditorPage, config: BatteryStatusConfig): StatusOptionSpec =
    when (page) {
        BatteryEditorPage.AIRPLANE -> StatusOptionSpec(
            stringResource(R.string.battery_component_airplane), config.showAirplane,
            config.airplaneSizeDp, config.airplaneColorArgb,
            { config.copy(showAirplane = it) }, { config.copy(airplaneSizeDp = it) },
            { config.copy(airplaneColorArgb = it) }
        )
        BatteryEditorPage.RINGER -> StatusOptionSpec(
            stringResource(R.string.battery_component_ringer), config.showRinger,
            config.ringerSizeDp, config.ringerColorArgb,
            { config.copy(showRinger = it) }, { config.copy(ringerSizeDp = it) },
            { config.copy(ringerColorArgb = it) }
        )
        BatteryEditorPage.DATE_TIME -> StatusOptionSpec(
            stringResource(R.string.battery_component_date_short), config.showDateTime,
            config.dateTimeSizeDp, config.dateTimeColorArgb,
            { config.copy(showDateTime = it) }, { config.copy(dateTimeSizeDp = it) },
            { config.copy(dateTimeColorArgb = it) }
        )
        BatteryEditorPage.HOTSPOT -> StatusOptionSpec(
            stringResource(R.string.battery_component_hotspot), config.showHotspot,
            config.hotspotSizeDp, config.hotspotColorArgb,
            { config.copy(showHotspot = it) }, { config.copy(hotspotSizeDp = it) },
            { config.copy(hotspotColorArgb = it) }
        )
        BatteryEditorPage.CHARGE -> StatusOptionSpec(
            stringResource(R.string.battery_component_charge_short), config.showCharge,
            config.chargeSizeDp, config.chargeColorArgb,
            { config.copy(showCharge = it) }, { config.copy(chargeSizeDp = it) },
            { config.copy(chargeColorArgb = it) }
        )
        BatteryEditorPage.CLOCK -> StatusOptionSpec(
            stringResource(R.string.battery_component_clock), config.showTime,
            config.clockSizeDp, config.clockColorArgb,
            { config.copy(showTime = it) }, { config.copy(clockSizeDp = it) },
            { config.copy(clockColorArgb = it) }
        )
        BatteryEditorPage.ANIMATION -> StatusOptionSpec(
            stringResource(R.string.battery_component_animation), config.showAnimation,
            config.animationSizeDp, config.foregroundColorArgb,
            { config.copy(showAnimation = it) }, { config.copy(animationSizeDp = it) },
            { config }
        )
        BatteryEditorPage.WIFI -> StatusOptionSpec(
            stringResource(R.string.battery_component_wifi), config.showWifi,
            config.wifiSizeDp, config.wifiColorArgb,
            { config.copy(showWifi = it) }, { config.copy(wifiSizeDp = it) },
            { config.copy(wifiColorArgb = it) }
        )
        BatteryEditorPage.SIGNAL -> StatusOptionSpec(
            stringResource(R.string.battery_component_signal), config.showSignal,
            config.signalSizeDp, config.signalColorArgb,
            { config.copy(showSignal = it) }, { config.copy(signalSizeDp = it) },
            { config.copy(signalColorArgb = it) }
        )
        BatteryEditorPage.DATA -> StatusOptionSpec(
            stringResource(R.string.battery_component_data), config.showData,
            config.dataSizeDp, config.dataColorArgb,
            { config.copy(showData = it) }, { config.copy(dataSizeDp = it) },
            { config.copy(dataColorArgb = it) }
        )
        else -> error("Unsupported status option page: $page")
    }

private fun airplaneStyles() = (1..4).map { index ->
    listOf(BatterySystemStatusPolicy.airplaneIcon(index))
}

private fun hotspotStyles() = (1..4).map { index ->
    listOfNotNull(BatterySystemStatusPolicy.hotspotIcon(BatteryHotspotState.ENABLED, index))
}

private fun ringerStyles() = (1..4).map { index ->
    listOfNotNull(
        BatterySystemStatusPolicy.ringerIcon(BatteryRingerState.VIBRATE, index),
        BatterySystemStatusPolicy.ringerIcon(BatteryRingerState.SILENT, index)
    )
}

private fun wifiStyles() = (1..4).map { index ->
    listOf(BatterySystemStatusPolicy.wifiIcon(BatteryConnectivityState.CONNECTED, index))
}

private fun signalStyles() = (1..4).map { index ->
    listOf(BatterySystemStatusPolicy.cellularIcon(BatteryConnectivityState.CONNECTED, index))
}

private val CHARGE_STYLE_ORDER = listOf(10, 8, 1, 3, 9, 5, 7, 6, 4, 11, 2, 12)
