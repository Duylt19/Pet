package com.asianmobile.emojibattery.shimeji.ui.shared.component

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.battery.overlay.BATTERY_STATUS_COMPONENT_GAP_DP
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryConnectivityState
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryDeviceState
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryPowerState
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryPreviewSystemStatePolicy
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryStatusComponent
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatterySystemStatusPolicy
import com.asianmobile.emojibattery.shimeji.battery.settings.systemStatusBarHeightDp
import com.asianmobile.emojibattery.shimeji.data.model.BatteryAnimationEntry
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDateFont
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_BAR_HEIGHT_DP
import com.asianmobile.emojibattery.shimeji.ui.battery.editor.BatteryAnimationAsset
import com.asianmobile.emojibattery.shimeji.ui.battery.editor.batteryPreviewLayout
import com.asianmobile.emojibattery.shimeji.ui.battery.editor.batteryPreviewTrailingOrder
import com.asianmobile.emojibattery.shimeji.ui.shared.theme.RobotoFontFamily
import com.intuit.sdp.R as SdpR
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * The percentage every "this is what your status bar will look like" card writes when the caller
 * has no better number. It is a design sample, not a reading of the device.
 */
internal const val BATTERY_PREVIEW_DEFAULT_PERCENT = 82

internal fun resolveBatteryStatusPreviewHeightDp(
    barHeightDp: Float,
    systemStatusBarHeightDp: Float
): Float = barHeightDp.takeIf { it.isFinite() && it > 0f }
    ?: systemStatusBarHeightDp.takeIf { it.isFinite() && it > 0f }
    ?: DEFAULT_BATTERY_BAR_HEIGHT_DP

/**
 * The embedded status-bar card. It is the single renderer for "what your status bar will look
 * like": the Status Bar editor and Battery Troll Customize both draw it, so a layout rule can
 * never drift between the two screens.
 *
 * Everything the card needs arrives as a parameter — no feature `UiState` — because the two
 * callers resolve the same things from different models. In particular:
 *
 * - [displayPercent] is the number to write, which is deliberately *not* [powerState]`.level`.
 *   `BatteryStatusBarView` lets Battery Troll's fake percentage reach the text while the clamped
 *   real level still drives the battery artwork; the preview has to be able to lie in exactly the
 *   same place, or it would not be previewing the prank.
 * - [emojiPath] `== null` means "draw no character", never "fall back to something". Battery Troll
 *   lets the user switch the character off, and a fallback would show a bar the user disabled.
 *
 * [deviceState] and [powerState] are the *actual* system state; the card applies
 * [BatteryPreviewSystemStatePolicy] itself so that a focused component (say, Airplane) is shown
 * switched on even when the device has it off.
 */
@Composable
internal fun BatteryStatusPreviewCard(
    config: BatteryStatusConfig,
    deviceState: BatteryDeviceState,
    powerState: BatteryPowerState,
    emojiPath: String?,
    batteryPath: String?,
    backgroundPath: String?,
    emotionPath: String?,
    animation: BatteryAnimationEntry?,
    focusedComponent: BatteryStatusComponent?,
    mobileDataLabel: String?,
    displayPercent: Int = BATTERY_PREVIEW_DEFAULT_PERCENT,
    /**
     * Set while a troll owns the artwork, so the preview sizes the character off the drawing the
     * way the real bar does instead of off the user's slider. Null for a normal theme.
     */
    trollEmojiSizeDp: Float? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val emojiSizeDp = trollEmojiSizeDp ?: config.emojiSizeDp
    val previewDescription = stringResource(
        R.string.battery_overlay_description,
        displayPercent
    )
    val previewDeviceState = BatteryPreviewSystemStatePolicy.deviceState(
        deviceState,
        focusedComponent
    )
    val previewPowerState = BatteryPreviewSystemStatePolicy.powerState(
        powerState,
        focusedComponent
    )
    val systemStatusBarHeightDp = remember(context) { context.systemStatusBarHeightDp() }
    val previewHeightDp = resolveBatteryStatusPreviewHeightDp(
        barHeightDp = config.barHeightDp,
        systemStatusBarHeightDp = systemStatusBarHeightDp
    )
    // A preview that formats `Date()` makes every screenshot golden expire at midnight, which
    // is how seven battery-editor goldens ended up failing daily. Renders pin to a fixed day.
    val isInspecting = LocalInspectionMode.current
    val previewDate = remember(config.dateFormat, isInspecting) {
        val locale = if (isInspecting) Locale.US else Locale.getDefault()
        val formatter = SimpleDateFormat(config.dateFormat.pattern, locale)
        if (isInspecting) {
            // Pinning the instant is not enough: a negative UTC offset would roll the golden
            // back a day and make it machine-dependent instead of date-dependent.
            formatter.timeZone = TimeZone.getTimeZone("UTC")
        }
        formatter.format(if (isInspecting) Date(GOLDEN_PREVIEW_DATE_MILLIS) else Date())
    }
    val previewDateFont = previewDateFontFamily(config.dateTimeFont)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(previewHeightDp.dp)
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._11sdp)))
            .background(
                if (backgroundPath == null) {
                    Color(config.backgroundColorArgb)
                } else {
                    Color.Transparent
                }
            )
            .semantics { contentDescription = previewDescription }
    ) {
        val layout = remember(
            config,
            emojiPath,
            batteryPath,
            emotionPath,
            animation?.assetPath,
            mobileDataLabel,
            maxWidth,
            focusedComponent,
            previewDeviceState,
            previewPowerState
        ) {
            batteryPreviewLayout(
                config = config,
                availableWidthDp = maxWidth.value -
                    config.leftPaddingDp -
                    config.rightPaddingDp,
                hasEmoji = emojiPath != null,
                trollEmojiSizeDp = trollEmojiSizeDp,
                hasEmotion = emotionPath != null,
                hasAnimation = animation != null,
                mobileDataLabel = mobileDataLabel,
                focusedComponent = focusedComponent,
                deviceState = previewDeviceState,
                powerState = previewPowerState
            )
        }
        backgroundPath?.let { path ->
            PreviewAsyncImage(
                model = path,
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BATTERY_STATUS_COMPONENT_GAP_DP.dp)
        ) {
            if (layout.shows(BatteryStatusComponent.TIME)) {
                Text(
                    text = stringResource(R.string.battery_preview_time),
                    color = Color(config.clockColorArgb),
                    fontFamily = RobotoFontFamily,
                    fontSize = config.clockSizeDp.sp,
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
                            previewDeviceState.ringer,
                            config.ringerIconStyleIndex
                        )
                    ),
                    sizeDp = config.ringerSizeDp,
                    colorArgb = config.ringerColorArgb
                )
            }
            if (layout.shows(BatteryStatusComponent.ANIMATION)) {
                animation?.let { entry ->
                    BatteryAnimationAsset(
                        animation = entry,
                        modifier = Modifier.size(config.animationSizeDp.dp)
                    )
                }
            }
            if (layout.shows(BatteryStatusComponent.EMOTION)) {
                emotionPath?.let { path ->
                    PreviewAsyncImage(
                        model = path,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(config.emojiSizeDp.dp)
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            batteryPreviewTrailingOrder(layout).forEach { component ->
                when (component) {
                    BatteryStatusComponent.HOTSPOT -> PreviewStatusIcon(
                        iconName = requireNotNull(
                            BatterySystemStatusPolicy.hotspotIcon(
                                previewDeviceState.hotspot,
                                config.hotspotIconStyleIndex
                            )
                        ),
                        sizeDp = config.hotspotSizeDp,
                        colorArgb = config.hotspotColorArgb
                    )
                    BatteryStatusComponent.CELLULAR -> {
                        if (config.showSignal) {
                            PreviewStatusIcon(
                                iconName = BatterySystemStatusPolicy.cellularIcon(
                                    BatteryConnectivityState.CONNECTED,
                                    config.signalIconStyleIndex
                                ),
                                sizeDp = config.signalSizeDp,
                                colorArgb = config.signalColorArgb
                            )
                        }
                        if (config.showData) {
                            mobileDataLabel?.let { label ->
                                Text(
                                    text = label,
                                    color = Color(config.dataColorArgb),
                                    fontSize = config.dataSizeDp.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    BatteryStatusComponent.WIFI -> PreviewStatusIcon(
                        iconName = BatterySystemStatusPolicy.wifiIcon(
                            BatteryConnectivityState.CONNECTED,
                            config.wifiIconStyleIndex
                        ),
                        sizeDp = config.wifiSizeDp,
                        colorArgb = config.wifiColorArgb
                    )
                    BatteryStatusComponent.PERCENTAGE -> Text(
                        text = stringResource(
                            R.string.battery_overlay_percentage,
                            displayPercent
                        ),
                        color = Color(config.percentColorArgb),
                        fontFamily = FontFamily(Font(R.font.roboto_medium)),
                        fontSize = config.percentSizeDp.sp
                    )
                    BatteryStatusComponent.BATTERY -> {
                        val pairSize = maxOf(
                            config.batterySizeDp,
                            if (emojiPath != null) emojiSizeDp else 0f
                        )
                        Box(
                            modifier = Modifier.size(pairSize.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            batteryPath?.let { path ->
                                PreviewAsyncImage(
                                    model = path,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(config.batterySizeDp.dp)
                                )
                            } ?: Box(
                                modifier = Modifier
                                    .size(
                                        width = config.batterySizeDp.dp,
                                        height = (config.batterySizeDp * 0.48f).dp
                                    )
                                    .clip(
                                        RoundedCornerShape(
                                            dimensionResource(SdpR.dimen._3sdp)
                                        )
                                    )
                                    .background(Color(config.foregroundColorArgb))
                            )
                            emojiPath?.let { path ->
                                PreviewAsyncImage(
                                    model = path,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(emojiSizeDp.dp)
                                )
                            }
                        }
                    }
                    BatteryStatusComponent.CHARGE -> PreviewStatusIcon(
                        iconName = "charge_%02d".format(config.chargeIconIndex),
                        sizeDp = config.chargeSizeDp,
                        colorArgb = config.chargeColorArgb
                    )
                    else -> Unit
                }
            }
        }
    }
}

/**
 * Card artwork is remote, so it all goes through one loader.
 *
 * Layoutlib drains a preview's pending work before disposing the render, and a Coil request that
 * can never resolve offline keeps that render open — the *next* screenshot test then dies on
 * "initSystem called twice". Inspection reserves the same space instead, which leaves the golden
 * unchanged (there is nothing to download in a test) and keeps the card usable from `@Preview`.
 */
@Composable
private fun PreviewAsyncImage(
    model: String,
    contentScale: ContentScale,
    modifier: Modifier = Modifier
) {
    if (LocalInspectionMode.current) {
        Spacer(modifier)
        return
    }
    AsyncImage(
        model = model,
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier
    )
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
        if (fontResource == 0) RobotoFontFamily else FontFamily(Font(fontResource))
    }
}

/** 2026-01-15, a Thursday: fixed so date-bearing goldens do not expire overnight. */
private const val GOLDEN_PREVIEW_DATE_MILLIS = 1_768_435_200_000L
