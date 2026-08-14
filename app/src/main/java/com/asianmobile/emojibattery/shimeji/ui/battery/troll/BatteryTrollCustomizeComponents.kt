package com.asianmobile.emojibattery.shimeji.ui.battery.troll

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.data.model.BATTERY_TROLL_LEVEL_COUNT
import com.asianmobile.emojibattery.shimeji.data.model.BATTERY_TROLL_RANDOM_ROTATION_MS
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollCatalogError
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollEntry
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollMode
import com.asianmobile.emojibattery.shimeji.data.model.MAX_BATTERY_TROLL_FAKE_PERCENT
import com.asianmobile.emojibattery.shimeji.data.model.MIN_BATTERY_TROLL_FAKE_PERCENT
import com.asianmobile.emojibattery.shimeji.data.remote.BatteryTrollServerConfig
import com.asianmobile.emojibattery.shimeji.ui.shared.component.DismissibleDialogBackdrop
import com.asianmobile.emojibattery.shimeji.ui.shared.component.RewardOutlineButton
import com.asianmobile.emojibattery.shimeji.ui.shared.component.RewardGradientButton
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

/**
 * The controls that only Battery Troll Customize has (Figma `8315:8232` / `8359:6992`). Anything
 * the app already owns — `AppSwitch`, `HomeEnableCard`, `BatteryDiscardChangesSheet` — is reused
 * from its own file instead of being redrawn here.
 */

internal val CustomizeRobotoRegular = FontFamily(Font(R.font.roboto_regular))
internal val CustomizeRobotoMedium = FontFamily(Font(R.font.roboto_medium))
internal val CustomizeRobotoSemiBold = FontFamily(Font(R.font.roboto_semibold))

/** Random mode keeps the picked artwork visible but stops it being a control. */
internal const val TROLL_DISABLED_ALPHA = 0.30f

/**
 * The level index the previews must draw right now.
 *
 * Random mode is not a still frame on the real status bar: the artwork rotates, and a preview that
 * froze on the user's last manual pick would promise something the bar will not do. So the preview
 * steps on the bar's own period — [BATTERY_TROLL_RANDOM_ROTATION_MS], read from the model rather
 * than restated here — which is honest by construction and needs no extra copy on screen (this
 * screen may not add strings). Emoji and battery step together because `BatteryTrollPolicy` rotates
 * them in lockstep. The loop lives in a `LaunchedEffect`, so it is cancelled the moment the screen
 * leaves composition.
 */
@Composable
internal fun rememberTrollRotationStep(active: Boolean): Int {
    var step by remember { mutableIntStateOf(0) }
    // Layoutlib renders a preview by draining the composition's effects; a `while (true)` that
    // never suspends to completion leaves the render undisposed, which then fails whichever
    // screenshot test happens to run next. Previews therefore show the frozen first frame.
    val isInspecting = LocalInspectionMode.current
    LaunchedEffect(active, isInspecting) {
        if (!active || isInspecting) {
            step = 0
            return@LaunchedEffect
        }
        while (true) {
            delay(BATTERY_TROLL_RANDOM_ROTATION_MS)
            step = (step + 1) % BATTERY_TROLL_LEVEL_COUNT
        }
    }
    return step
}

/** Same top navigation as the editor's compact bar, minus the PRO pill Figma drops here. */
@Composable
internal fun TrollCustomizeTopBar(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._43sdp))
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_favorite_recent_back),
            contentDescription = stringResource(R.string.back),
            tint = colorResource(R.color.colors_212327),
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._22sdp))
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onBack)
        )
        Text(
            text = title,
            color = colorResource(R.color.colors_212327),
            fontFamily = CustomizeRobotoSemiBold,
            fontSize = dimensionResource(SspR.dimen._15ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._22ssp).value.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun TrollSectionHeader(title: String) {
    Text(
        text = title,
        color = colorResource(R.color.colors_212327),
        fontFamily = CustomizeRobotoSemiBold,
        fontSize = dimensionResource(SspR.dimen._14ssp).value.sp,
        lineHeight = dimensionResource(SspR.dimen._20ssp).value.sp
    )
}

@Composable
internal fun TrollCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp))
    val shadowColor = colorResource(R.color.colors_666666).copy(alpha = 0.12f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = dimensionResource(SdpR.dimen._6sdp),
                shape = shape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(shape)
            .background(colorResource(R.color.colors_FFFFFF))
            .padding(dimensionResource(SdpR.dimen._12sdp)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp)),
        content = content
    )
}

@Composable
internal fun TrollModeSegmentedControl(
    mode: BatteryTrollMode,
    onModeChange: (BatteryTrollMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp))
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._34sdp))
            .clip(shape)
            .background(colorResource(R.color.colors_FFFFFF))
            .border(
                width = dimensionResource(SdpR.dimen._1sdp),
                color = colorResource(R.color.colors_FEC1D4),
                shape = shape
            )
            .padding(dimensionResource(SdpR.dimen._3sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrollModeSegment(
            label = stringResource(R.string.battery_troll_mode_fake),
            selected = mode == BatteryTrollMode.FAKE,
            onClick = { onModeChange(BatteryTrollMode.FAKE) },
            modifier = Modifier.weight(1f)
        )
        TrollModeSegment(
            label = stringResource(R.string.battery_troll_mode_real),
            selected = mode == BatteryTrollMode.REAL,
            onClick = { onModeChange(BatteryTrollMode.REAL) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TrollModeSegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
    Box(
        modifier = modifier
            .height(dimensionResource(SdpR.dimen._28sdp))
            .clip(shape)
            .background(
                if (selected) colorResource(R.color.colors_FB3675) else Color.Transparent
            )
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = colorResource(
                if (selected) R.color.colors_FFFFFF else R.color.colors_6F7073
            ),
            fontFamily = CustomizeRobotoMedium,
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun TrollInfoChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._6sdp)))
            .background(colorResource(R.color.colors_FFEBF1))
            .padding(
                horizontal = dimensionResource(SdpR.dimen._6sdp),
                vertical = dimensionResource(SdpR.dimen._5sdp)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._3sdp))
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_info_rounded),
            contentDescription = null,
            tint = colorResource(R.color.colors_FB3675),
            modifier = Modifier.size(dimensionResource(SdpR.dimen._12sdp))
        )
        Text(
            text = text,
            color = colorResource(R.color.colors_FB3675),
            fontFamily = CustomizeRobotoRegular,
            fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._12ssp).value.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Real mode has no number to type, so the chip greys out instead of disappearing. */
@Composable
internal fun TrollEditChip(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = colorResource(
        if (enabled) R.color.colors_6F7073 else R.color.colors_DEDEDF
    )
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._6sdp))
    Row(
        modifier = modifier
            .width(dimensionResource(SdpR.dimen._65sdp))
            .height(dimensionResource(SdpR.dimen._25sdp))
            .clip(shape)
            .background(colorResource(R.color.colors_FFFFFF))
            .border(width = dimensionResource(SdpR.dimen._1sdp), color = contentColor, shape = shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_edit),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(dimensionResource(SdpR.dimen._12sdp))
        )
        Text(
            text = stringResource(R.string.battery_troll_edit),
            color = contentColor,
            fontFamily = CustomizeRobotoMedium,
            fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp,
            maxLines = 1
        )
    }
}

/**
 * The 96x96 preview in the Mode card: the picked battery with the picked character sitting on it,
 * which is how the pair will read once the status bar is covered.
 *
 * The two images are decorative on their own, so the box carries one merged label — the troll's
 * catalog name — instead of announcing two unnamed graphics.
 */
@Composable
internal fun TrollThemePreview(
    troll: BatteryTrollEntry?,
    showEmoji: Boolean,
    emojiLevelIndex: Int,
    batteryLevelIndex: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(dimensionResource(SdpR.dimen._74sdp))
            .semantics(mergeDescendants = true) {
                troll?.name?.let { contentDescription = it }
            },
        contentAlignment = Alignment.Center
    ) {
        TrollArtwork(
            path = troll?.batteryPaths?.getOrNull(batteryLevelIndex),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(dimensionResource(SdpR.dimen._49sdp))
        )
        if (showEmoji) {
            TrollArtwork(
                path = troll?.emojiPaths?.getOrNull(emojiLevelIndex),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(dimensionResource(SdpR.dimen._43sdp))
            )
        }
    }
}

/** Ring + dot per Figma `8359:6960` / `8359:6958`, sized by the caller. */
@Composable
private fun TrollRadioMark(
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val ring = colorResource(
        if (selected) R.color.colors_FB3675 else R.color.colors_CCCCCC
    )
    val dot = colorResource(R.color.colors_FB3675)
    Canvas(modifier) {
        // Figma: 20 outer, 12 inner, so the ring is 2 units of a 20-unit box.
        val stroke = size.minDimension * 2f / 20f
        drawCircle(
            color = ring,
            radius = (size.minDimension - stroke) / 2f,
            style = Stroke(width = stroke)
        )
        if (selected) {
            drawCircle(color = dot, radius = size.minDimension * 12f / 20f / 2f)
        }
    }
}

@Composable
internal fun TrollRadioOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.selectable(
            selected = selected,
            role = Role.RadioButton,
            onClick = onClick
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._3sdp))
    ) {
        // Figma `8359:6960` is a ring plus a dot. The shared `ic_radio_selected` drawable is a
        // ring plus a check and belongs to the Language onboarding design, so it is drawn here
        // instead of being changed underneath that screen.
        TrollRadioMark(
            selected = selected,
            modifier = Modifier.size(dimensionResource(SdpR.dimen._15sdp))
        )
        Text(
            text = label,
            color = colorResource(R.color.colors_212327),
            fontFamily = CustomizeRobotoRegular,
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
            maxLines = 1
        )
    }
}

/**
 * One artwork picker (Emoji or Battery). [enabled] is Random mode: the block keeps its highlight
 * so the user still sees what is stored, but at 30% and deaf to taps.
 *
 * [tilesEnabled] dims the tiles *only* — it is the Emoji block's own switch, and dimming the header
 * would grey out the very switch that turns the tiles back on.
 */
@Composable
internal fun TrollArtworkBlock(
    @DrawableRes iconRes: Int,
    label: String,
    paths: List<String>,
    selectedIndex: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tilesEnabled: Boolean = true,
    trailingSwitch: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else TROLL_DISABLED_ALPHA),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(SdpR.dimen._18sdp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._3sdp)),
                modifier = Modifier.weight(1f)
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._12sdp))
                )
                Text(
                    text = label,
                    color = colorResource(R.color.colors_212327),
                    fontFamily = CustomizeRobotoMedium,
                    fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
                    lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            trailingSwitch?.invoke()
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (tilesEnabled) 1f else TROLL_DISABLED_ALPHA),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._3sdp))
        ) {
            repeat(BATTERY_TROLL_LEVEL_COUNT) { index ->
                TrollArtworkTile(
                    path = paths.getOrNull(index),
                    // The artwork itself carries no text, so the block's own label plus the
                    // 1-based level is the only thing a screen reader can announce.
                    label = "$label ${index + 1}",
                    selected = index == selectedIndex,
                    enabled = enabled && tilesEnabled,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * `selectable` rather than `clickable`: it is what lets a screen reader say "Emoji 3, selected"
 * instead of announcing ten identical unlabelled radio buttons.
 */
@Composable
private fun TrollArtworkTile(
    path: String?,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._6sdp))
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(
                colorResource(
                    if (selected) R.color.colors_FFEBF1 else R.color.colors_FFFFFF
                )
            )
            .border(
                width = dimensionResource(SdpR.dimen._1sdp),
                color = colorResource(
                    if (selected) R.color.colors_FB3675 else R.color.colors_DEDEDF
                ),
                shape = shape
            )
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        TrollArtwork(
            path = path,
            modifier = Modifier.size(dimensionResource(SdpR.dimen._32sdp))
        )
    }
}

/** Catalog art is remote, so every troll image goes through the same resolver. */
@Composable
internal fun TrollArtwork(
    path: String?,
    modifier: Modifier = Modifier
) {
    // Layoutlib drains a preview's pending work before disposing the render. A Coil request that
    // can never resolve offline keeps that render open, and the *next* screenshot test then dies
    // on "initSystem called twice". Previews reserve the space instead; there is nothing to
    // download in a test anyway, so the golden is unchanged and no longer depends on the network.
    if (path.isNullOrBlank() || LocalInspectionMode.current) {
        Spacer(modifier)
        return
    }
    AsyncImage(
        model = BatteryTrollServerConfig.resolve(path),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

/**
 * [enabled] is false until the troll's artwork actually resolved — the button must not be able to
 * commit a theme id the status bar cannot draw.
 */
@Composable
internal fun TrollApplyPanel(
    onApply: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(
        topStart = dimensionResource(SdpR.dimen._18sdp),
        topEnd = dimensionResource(SdpR.dimen._18sdp)
    )
    val shadowColor = colorResource(R.color.colors_000000).copy(alpha = 0.10f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = dimensionResource(SdpR.dimen._3sdp),
                shape = shape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(shape)
            .background(colorResource(R.color.colors_FFFFFF))
            .padding(
                start = dimensionResource(SdpR.dimen._12sdp),
                top = dimensionResource(SdpR.dimen._18sdp),
                end = dimensionResource(SdpR.dimen._12sdp),
                bottom = dimensionResource(SdpR.dimen._9sdp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(SdpR.dimen._37sdp))
                .alpha(if (enabled) 1f else TROLL_DISABLED_ALPHA)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            colorResource(R.color.colors_C95DFF),
                            colorResource(R.color.colors_FB54BB)
                        )
                    )
                )
                .clickable(enabled = enabled, role = Role.Button, onClick = onApply),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.battery_apply),
                color = colorResource(R.color.colors_FFFFFF),
                fontFamily = CustomizeRobotoMedium,
                fontSize = dimensionResource(SspR.dimen._14ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._20ssp).value.sp
            )
        }
    }
}

/**
 * NOT IN FIGMA — Figma draws this screen with the catalog already resolved. Without a loading state
 * the controls render as empty grey tiles, which reads as broken artwork rather than as "not here
 * yet", so the same spinner the troll grid uses stands in until the entry arrives.
 */
@Composable
internal fun TrollCustomizeLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._115sdp)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = colorResource(R.color.colors_FB3675))
    }
}

/**
 * NOT IN FIGMA — the troll id came from the grid, so this state only happens when the catalog is
 * gone (offline, unreadable, or the id was withdrawn server-side). It mirrors the grid's own empty
 * state, including dropping the retry affordance for `DISTRIBUTION_NOT_APPROVED`, which no amount
 * of retrying can fix in a release build.
 */
@Composable
internal fun TrollCustomizeUnavailable(
    error: BatteryTrollCatalogError?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val messageRes = when (error) {
        BatteryTrollCatalogError.CATALOG_UNAVAILABLE -> R.string.battery_troll_error_offline
        BatteryTrollCatalogError.CATALOG_INVALID -> R.string.battery_troll_error_invalid
        BatteryTrollCatalogError.DISTRIBUTION_NOT_APPROVED -> R.string.battery_troll_unpublished
        null -> R.string.battery_troll_empty
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(dimensionResource(SdpR.dimen._18sdp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
    ) {
        Text(
            text = stringResource(messageRes),
            color = colorResource(R.color.colors_6F7073),
            fontFamily = CustomizeRobotoMedium,
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (error != BatteryTrollCatalogError.DISTRIBUTION_NOT_APPROVED) {
            val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._18sdp))
            Text(
                text = stringResource(R.string.battery_troll_retry),
                color = colorResource(R.color.colors_FB3675),
                fontFamily = CustomizeRobotoSemiBold,
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(shape)
                    .border(
                        width = dimensionResource(SdpR.dimen._1sdp),
                        color = colorResource(R.color.colors_FB3675),
                        shape = shape
                    )
                    .clickable(role = Role.Button, onClick = onRetry)
                    .padding(
                        horizontal = dimensionResource(SdpR.dimen._18sdp),
                        vertical = dimensionResource(SdpR.dimen._8sdp)
                    )
            )
        }
    }
}

/**
 * NOT IN FIGMA — invented so the `Edit` chip has somewhere to go. It borrows the shape of
 * `PetRoomRemoveDialog` and needs the owner's sign-off before it is treated as final.
 */
@Composable
internal fun BatteryTrollEditPercentDialog(
    initialPercent: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DismissibleDialogBackdrop(onDismissRequest = onDismiss) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
                contentAlignment = Alignment.Center
            ) {
                BatteryTrollEditPercentDialogCard(
                    initialPercent = initialPercent,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
internal fun BatteryTrollEditPercentDialogCard(
    initialPercent: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var input by rememberSaveable(initialPercent) {
        mutableStateOf(initialPercent.coerceIn(MIN_PERCENT, MAX_PERCENT).toString())
    }
    val parsed = input.toIntOrNull()
    Column(
        modifier = modifier
            .fillMaxWidth(EDIT_DIALOG_WIDTH_FRACTION)
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._18sdp)))
            .background(colorResource(R.color.colors_FFFFFF))
            .padding(
                horizontal = dimensionResource(SdpR.dimen._12sdp),
                vertical = dimensionResource(SdpR.dimen._18sdp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
        ) {
            Text(
                text = stringResource(R.string.battery_troll_edit_title),
                color = colorResource(R.color.colors_212327),
                fontFamily = CustomizeRobotoMedium,
                fontSize = dimensionResource(SspR.dimen._15ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._21ssp).value.sp,
                textAlign = TextAlign.Center
            )
            BasicTextField(
                value = input,
                onValueChange = { raw ->
                    // Digits only, three at most: the prank tops out at 999%.
                    input = raw.filter { it.isDigit() }.take(MAX_PERCENT_DIGITS)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                    color = colorResource(R.color.colors_212327),
                    fontFamily = CustomizeRobotoSemiBold,
                    fontSize = dimensionResource(SspR.dimen._21ssp).value.sp,
                    lineHeight = dimensionResource(SspR.dimen._27ssp).value.sp,
                    textAlign = TextAlign.Center
                ),
                cursorBrush = SolidColor(colorResource(R.color.colors_FB3675)),
                decorationBox = { field ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimensionResource(SdpR.dimen._46sdp))
                            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp)))
                            .background(colorResource(R.color.colors_FFFFFF))
                            .border(
                                width = 1.dp,
                                color = colorResource(R.color.colors_C8C8C9),
                                shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        field()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
        ) {
            // Same pill pair as the reward and discard sheets: gradient outline to back out,
            // gradient fill to commit.
            RewardOutlineButton(
                text = stringResource(R.string.cancel),
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            RewardGradientButton(
                text = stringResource(R.string.battery_troll_edit_confirm),
                onClick = { parsed?.let { onConfirm(it.coerceIn(MIN_PERCENT, MAX_PERCENT)) } },
                modifier = Modifier.weight(1f),
                enabled = parsed != null
            )
        }
    }
}

@Composable
private fun TrollDialogButton(
    label: String,
    backgroundRes: Int,
    labelColorRes: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(dimensionResource(SdpR.dimen._37sdp))
            // `alpha` is a draw modifier: it only fades what comes after it, so it has to precede
            // `background` or the disabled button keeps a full-opacity fill under a faded label.
            .alpha(if (enabled) 1f else TROLL_DISABLED_ALPHA)
            .clip(CircleShape)
            .background(colorResource(backgroundRes))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = colorResource(labelColorRes),
            fontFamily = CustomizeRobotoMedium,
            fontSize = dimensionResource(SspR.dimen._12ssp).value.sp
        )
    }
}

private const val MIN_PERCENT = MIN_BATTERY_TROLL_FAKE_PERCENT
private const val MAX_PERCENT = MAX_BATTERY_TROLL_FAKE_PERCENT
private const val MAX_PERCENT_DIGITS = 3
private const val EDIT_DIALOG_WIDTH_FRACTION = 320f / 360f
