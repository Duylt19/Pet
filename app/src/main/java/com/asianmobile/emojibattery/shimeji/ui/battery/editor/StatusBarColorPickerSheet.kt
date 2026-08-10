package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.asianmobile.emojibattery.shimeji.R
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val ColorPickerRobotoMedium = FontFamily(Font(R.font.roboto_medium))
private val ColorPickerRobotoSemiBold = FontFamily(Font(R.font.roboto_semibold))

@Composable
internal fun StatusBarColorPickerSheet(
    selectedColor: Int,
    onColorChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        HideColorPickerNavigationBar()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(R.color.colors_000000).copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            StatusBarColorPickerSurface(
                selectedColor = selectedColor,
                onColorChange = onColorChange,
                onDismiss = onDismiss,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            )
        }
    }
}

@Composable
internal fun StatusBarColorPickerSurface(
    selectedColor: Int,
    onColorChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initial = remember { argbToHsvAlpha(selectedColor) }
    var hue by remember { mutableFloatStateOf(initial.hue) }
    var saturation by remember { mutableFloatStateOf(initial.saturation) }
    var brightness by remember { mutableFloatStateOf(initial.brightness) }
    var alpha by remember { mutableFloatStateOf(initial.alpha) }
    val selected = hsvAlphaToArgb(hue, saturation, brightness, alpha)
    val selectedOpaque = Color(hsvAlphaToArgb(hue, saturation, brightness, 1f))

    fun updateColor(
        newHue: Float = hue,
        newSaturation: Float = saturation,
        newBrightness: Float = brightness,
        newAlpha: Float = alpha
    ) {
        hue = newHue.coerceIn(0f, 360f)
        saturation = newSaturation.coerceIn(0f, 1f)
        brightness = newBrightness.coerceIn(0f, 1f)
        alpha = newAlpha.coerceIn(0f, 1f)
        onColorChange(hsvAlphaToArgb(hue, saturation, brightness, alpha))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    topStart = dimensionResource(SdpR.dimen._18sdp),
                    topEnd = dimensionResource(SdpR.dimen._18sdp)
                )
            )
            .background(colorResource(R.color.colors_FFFFFF))
            .padding(
                horizontal = dimensionResource(SdpR.dimen._9sdp),
                vertical = dimensionResource(SdpR.dimen._12sdp)
            ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
    ) {
        ColorPickerHeader(onDismiss)
        ColorPickerTabs()
        SaturationBrightnessPicker(
            hue = hue,
            saturation = saturation,
            brightness = brightness,
            selectedColor = Color(selected),
            onChange = { newSaturation, newBrightness ->
                updateColor(
                    newSaturation = newSaturation,
                    newBrightness = newBrightness
                )
            }
        )
        HuePicker(hue = hue, onHueChange = { updateColor(newHue = it) })
        OpacityPicker(
            color = selectedOpaque,
            alpha = alpha,
            onAlphaChange = { updateColor(newAlpha = it) }
        )
        ColorValueFields(selected)
    }
}

@Composable
private fun ColorPickerHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._23sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.battery_color_picker_title),
            color = colorResource(R.color.colors_0D0D0D),
            fontFamily = ColorPickerRobotoSemiBold,
            fontSize = dimensionResource(SspR.dimen._15ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._22ssp).value.sp,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._23sdp))
                .clip(CircleShape)
                .background(colorResource(R.color.colors_000000).copy(alpha = 0.05f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close_x),
                contentDescription = null,
                tint = colorResource(R.color.colors_9B9C9E),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._12sdp))
            )
        }
    }
}

@Composable
private fun ColorPickerTabs() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._25sdp))
            .clip(CircleShape)
            .background(colorResource(R.color.colors_F6F6F6))
            .padding(dimensionResource(SdpR.dimen._2sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .shadow(
                    elevation = dimensionResource(SdpR.dimen._3sdp),
                    shape = CircleShape,
                    ambientColor = colorResource(R.color.colors_1A000000),
                    spotColor = colorResource(R.color.colors_1A000000)
                )
                .clip(CircleShape)
                .background(colorResource(R.color.colors_FFEBF1))
                .border(
                    dimensionResource(SdpR.dimen._1sdp),
                    colorResource(R.color.colors_FB3675),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            PickerTabText(
                text = stringResource(R.string.battery_color_picker_grid),
                color = colorResource(R.color.colors_FB3675)
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            PickerTabText(
                text = stringResource(R.string.battery_color_picker_sliders),
                color = colorResource(R.color.colors_6F7073)
            )
        }
    }
}

@Composable
private fun PickerTabText(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontFamily = ColorPickerRobotoMedium,
        fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
        lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp
    )
}

@Composable
private fun SaturationBrightnessPicker(
    hue: Float,
    saturation: Float,
    brightness: Float,
    selectedColor: Color,
    onChange: (Float, Float) -> Unit
) {
    val white = colorResource(R.color.colors_FFFFFF)
    val black = colorResource(R.color.colors_000000)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._148sdp))
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._6sdp)))
            .pointerInput(hue) {
                fun update(position: Offset) {
                    onChange(
                        (position.x / size.width).coerceIn(0f, 1f),
                        (1f - position.y / size.height).coerceIn(0f, 1f)
                    )
                }
                detectDragGestures(
                    onDragStart = ::update,
                    onDrag = { change, _ -> update(change.position) }
                )
            }
    ) {
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(white, Color.hsv(hue, 1f, 1f))
            )
        )
        drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, black)))
        val center = Offset(saturation * size.width, (1f - brightness) * size.height)
        drawCircle(white, radius = dimensionResourcePx(10.5f), center = center)
        drawCircle(selectedColor, radius = dimensionResourcePx(5.25f), center = center)
        drawRoundRect(
            color = black.copy(alpha = 0.1f),
            style = Stroke(width = dimensionResourcePx(1f)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(dimensionResourcePx(8f))
        )
    }
}

@Composable
private fun HuePicker(hue: Float, onHueChange: (Float) -> Unit) {
    val markerBorder = colorResource(R.color.colors_E6E6E6)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._9sdp))
            .pointerInput(Unit) {
                fun update(position: Offset) {
                    onHueChange((position.x / size.width).coerceIn(0f, 1f) * 360f)
                }
                detectDragGestures(
                    onDragStart = ::update,
                    onDrag = { change, _ -> update(change.position) }
                )
            }
    ) {
        val trackHeight = dimensionResourcePx(6f)
        drawRoundRect(
            brush = Brush.horizontalGradient(
                (0..6).map { Color.hsv(it * 60f, 1f, 1f) }
            ),
            topLeft = Offset(0f, (size.height - trackHeight) / 2f),
            size = Size(size.width, trackHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2f)
        )
        val markerRadius = dimensionResourcePx(8f)
        val center = Offset(
            markerRadius + (hue / 360f) * (size.width - markerRadius * 2f),
            size.height / 2f
        )
        drawCircle(Color.White, radius = markerRadius, center = center)
        drawCircle(
            markerBorder,
            radius = markerRadius,
            center = center,
            style = Stroke(width = dimensionResourcePx(1f))
        )
        drawCircle(Color.hsv(hue, 1f, 1f), radius = dimensionResourcePx(5f), center = center)
    }
}

@Composable
private fun OpacityPicker(color: Color, alpha: Float, onAlphaChange: (Float) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._78sdp))
            .padding(top = dimensionResource(SdpR.dimen._14sdp)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._5sdp))
    ) {
        Text(
            text = stringResource(R.string.battery_color_picker_opacity),
            color = colorResource(R.color.colors_6F7073),
            fontFamily = ColorPickerRobotoMedium,
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OpacityTrack(
                color = color,
                alpha = alpha,
                onAlphaChange = onAlphaChange,
                modifier = Modifier
                    .weight(1f)
                    .height(dimensionResource(SdpR.dimen._28sdp))
            )
            Spacer(Modifier.width(dimensionResource(SdpR.dimen._12sdp)))
            Box(
                modifier = Modifier
                    .width(dimensionResource(SdpR.dimen._58sdp))
                    .height(dimensionResource(SdpR.dimen._28sdp))
                    .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._6sdp)))
                    .background(colorResource(R.color.colors_FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(
                        R.string.battery_color_picker_percentage,
                        (alpha * 100f).roundToInt()
                    ),
                    color = colorResource(R.color.colors_000000),
                    fontFamily = ColorPickerRobotoMedium,
                    fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
                    lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .fillMaxWidth()
                .height(dimensionResource(SdpR.dimen._1sdp))
                .background(colorResource(R.color.colors_1A000000))
        )
    }
}

@Composable
private fun OpacityTrack(
    color: Color,
    alpha: Float,
    onAlphaChange: (Float) -> Unit,
    modifier: Modifier
) {
    val light = colorResource(R.color.colors_F6F6F6)
    val dark = colorResource(R.color.colors_E6E6E6)
    Canvas(
        modifier = modifier
            .clip(CircleShape)
            .pointerInput(Unit) {
                fun update(position: Offset) {
                    onAlphaChange((position.x / size.width).coerceIn(0f, 1f))
                }
                detectDragGestures(
                    onDragStart = ::update,
                    onDrag = { change, _ -> update(change.position) }
                )
            }
    ) {
        val cell = size.height / 4f
        for (row in 0..3) {
            var column = 0
            while (column * cell < size.width) {
                drawRect(
                    color = if ((row + column) % 2 == 0) light else dark,
                    topLeft = Offset(column * cell, row * cell),
                    size = Size(cell, cell)
                )
                column++
            }
        }
        drawRect(Brush.horizontalGradient(listOf(color.copy(alpha = 0f), color)))
        val markerRadius = size.height * 0.45f
        val center = Offset(
            markerRadius + alpha * (size.width - markerRadius * 2f),
            size.height / 2f
        )
        drawCircle(Color.White, radius = markerRadius, center = center)
        drawCircle(
            Color.Black,
            radius = size.height * 0.43f,
            center = center,
            style = Stroke(width = dimensionResourcePx(2f))
        )
    }
}

@Composable
private fun ColorValueFields(argb: Int) {
    val values = listOf(
        AndroidColor.red(argb).toString(),
        AndroidColor.green(argb).toString(),
        AndroidColor.blue(argb).toString(),
        stringResource(
            R.string.battery_color_picker_percentage,
            (AndroidColor.alpha(argb) / 255f * 100f).roundToInt()
        )
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._23sdp)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
    ) {
        Box(
            modifier = Modifier
                .width(dimensionResource(SdpR.dimen._34sdp))
                .fillMaxSize()
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._6sdp)))
                .border(
                    dimensionResource(SdpR.dimen._1sdp),
                    colorResource(R.color.colors_808080),
                    RoundedCornerShape(dimensionResource(SdpR.dimen._6sdp))
                ),
            contentAlignment = Alignment.Center
        ) {
            ColorValueText(
                text = stringResource(R.string.battery_color_picker_rgb),
                color = colorResource(R.color.colors_808080)
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._6sdp))),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._1sdp))
        ) {
            values.forEach { value ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(colorResource(R.color.colors_E6E6E6)),
                    contentAlignment = Alignment.Center
                ) {
                    ColorValueText(
                        text = value,
                        color = colorResource(R.color.colors_666666)
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorValueText(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontFamily = ColorPickerRobotoMedium,
        fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
        lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
        textAlign = TextAlign.Center
    )
}

internal data class HsvAlphaColor(
    val hue: Float,
    val saturation: Float,
    val brightness: Float,
    val alpha: Float
)

internal fun argbToHsvAlpha(argb: Int): HsvAlphaColor {
    val red = ((argb ushr 16) and 0xFF) / 255f
    val green = ((argb ushr 8) and 0xFF) / 255f
    val blue = (argb and 0xFF) / 255f
    val maximum = max(red, max(green, blue))
    val minimum = min(red, min(green, blue))
    val delta = maximum - minimum
    val hue = when {
        delta == 0f -> 0f
        maximum == red -> 60f * (((green - blue) / delta) % 6f)
        maximum == green -> 60f * (((blue - red) / delta) + 2f)
        else -> 60f * (((red - green) / delta) + 4f)
    }.let { if (it < 0f) it + 360f else it }
    return HsvAlphaColor(
        hue = hue,
        saturation = if (maximum == 0f) 0f else delta / maximum,
        brightness = maximum,
        alpha = ((argb ushr 24) and 0xFF) / 255f
    )
}

internal fun hsvAlphaToArgb(
    hue: Float,
    saturation: Float,
    brightness: Float,
    alpha: Float
): Int {
    val normalizedHue = hue.coerceIn(0f, 360f).let { if (it == 360f) 0f else it }
    val safeSaturation = saturation.coerceIn(0f, 1f)
    val safeBrightness = brightness.coerceIn(0f, 1f)
    val chroma = safeBrightness * safeSaturation
    val hueSector = normalizedHue / 60f
    val secondary = chroma * (1f - kotlin.math.abs(hueSector % 2f - 1f))
    val (redPrime, greenPrime, bluePrime) = when (floor(hueSector).toInt()) {
        0 -> Triple(chroma, secondary, 0f)
        1 -> Triple(secondary, chroma, 0f)
        2 -> Triple(0f, chroma, secondary)
        3 -> Triple(0f, secondary, chroma)
        4 -> Triple(secondary, 0f, chroma)
        else -> Triple(chroma, 0f, secondary)
    }
    val match = safeBrightness - chroma
    val alphaChannel = (alpha.coerceIn(0f, 1f) * 255f).roundToInt()
    val red = ((redPrime + match) * 255f).roundToInt().coerceIn(0, 255)
    val green = ((greenPrime + match) * 255f).roundToInt().coerceIn(0, 255)
    val blue = ((bluePrime + match) * 255f).roundToInt().coerceIn(0, 255)
    return (alphaChannel shl 24) or (red shl 16) or (green shl 8) or blue
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.dimensionResourcePx(
    designPixels: Float
): Float = designPixels / 1.3f * density

@Composable
@Suppress("DEPRECATION")
private fun HideColorPickerNavigationBar() {
    val view = LocalView.current
    DisposableEffect(view) {
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window
        if (dialogWindow == null) {
            onDispose { }
        } else {
            val decorView = dialogWindow.decorView
            WindowCompat.setDecorFitsSystemWindows(dialogWindow, false)
            dialogWindow.navigationBarColor = AndroidColor.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                dialogWindow.isNavigationBarContrastEnforced = false
            }
            val controller = WindowInsetsControllerCompat(dialogWindow, decorView).apply {
                isAppearanceLightNavigationBars = false
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            fun hideNavigationBar() {
                controller.hide(WindowInsetsCompat.Type.navigationBars())
            }
            val focusListener =
                android.view.ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
                if (hasFocus) hideNavigationBar()
            }
            hideNavigationBar()
            decorView.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
            onDispose {
                if (decorView.viewTreeObserver.isAlive) {
                    decorView.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
                }
            }
        }
    }
}
