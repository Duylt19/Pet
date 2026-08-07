package com.asianmobile.emojibattery.shimeji.ui.petroom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ui.component.DismissibleDialogBackdrop
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR
import kotlin.math.roundToInt

/**
 * One speed and one size for every pet. The design has no room for a per-pet profile yet, so the
 * dialog edits the shared values and Save applies them to the whole roster at once.
 */
@Composable
fun PetRoomSettingsDialog(
    settings: PetRoomSettingsUiState,
    onSpeedChange: (Int) -> Unit,
    onSizeChange: (Int) -> Unit,
    onSave: () -> Unit,
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
                PetRoomSettingsCard(
                    settings = settings,
                    onSpeedChange = onSpeedChange,
                    onSizeChange = onSizeChange,
                    onSave = onSave,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
internal fun PetRoomSettingsCard(
    settings: PetRoomSettingsUiState,
    onSpeedChange: (Int) -> Unit,
    onSizeChange: (Int) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth(SETTINGS_DIALOG_WIDTH_FRACTION)
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._18sdp)))
            .background(colorResource(R.color.colors_FFFFFF))
            .padding(dimensionResource(SdpR.dimen._12sdp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.pet_room_settings_title),
            color = colorResource(R.color.colors_212327),
            fontWeight = FontWeight.Medium,
            fontSize = dimensionResource(SspR.dimen._15ssp).value.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        HorizontalDivider(
            color = colorResource(R.color.colors_F2F2F2),
            modifier = Modifier.padding(vertical = dimensionResource(SdpR.dimen._12sdp))
        )
        SettingSlider(
            labelRes = R.string.pet_room_settings_speed,
            value = settings.speedPercent,
            steps = PetRoomSettingsPolicy.SPEED_STEPS,
            onValueChange = onSpeedChange,
            onReset = { onSpeedChange(PetRoomSettingsPolicy.DEFAULT_PERCENT) }
        )
        SettingSlider(
            labelRes = R.string.pet_room_settings_size,
            value = settings.sizePercent,
            steps = PetRoomSettingsPolicy.SIZE_STEPS,
            onValueChange = onSizeChange,
            onReset = { onSizeChange(PetRoomSettingsPolicy.DEFAULT_PERCENT) }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimensionResource(SdpR.dimen._9sdp)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
        ) {
            DialogButton(
                labelRes = R.string.pet_room_settings_cancel,
                backgroundRes = R.color.colors_F2F2F2,
                labelColorRes = R.color.colors_6F7073,
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            DialogButton(
                labelRes = R.string.pet_room_settings_save,
                backgroundRes = R.color.colors_FB3675,
                labelColorRes = R.color.colors_FFFFFF,
                onClick = onSave,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SettingSlider(
    labelRes: Int,
    value: Int,
    steps: List<Int>,
    onValueChange: (Int) -> Unit,
    onReset: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(labelRes),
                color = colorResource(R.color.colors_212327),
                fontWeight = FontWeight.Medium,
                fontSize = dimensionResource(SspR.dimen._12ssp).value.sp
            )
            Text(
                text = stringResource(
                    R.string.pet_room_settings_value,
                    PetRoomSettingsPolicy.label(value)
                ),
                color = colorResource(R.color.colors_FB3675),
                fontWeight = FontWeight.Medium,
                fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                painter = painterResource(R.drawable.ic_pet_room_reset),
                contentDescription = stringResource(R.string.pet_room_settings_reset),
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(dimensionResource(SdpR.dimen._18sdp))
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onReset)
            )
        }
        StepSlider(
            value = value,
            steps = steps,
            onValueChange = onValueChange,
            modifier = Modifier.padding(
                top = dimensionResource(SdpR.dimen._6sdp),
                bottom = dimensionResource(SdpR.dimen._12sdp)
            )
        )
    }
}

/**
 * A slider that only stops on the steps the pet settings accept, drawn the way Figma draws it:
 * a soft pink track with a solid bar as the thumb.
 */
@Composable
private fun StepSlider(
    value: Int,
    steps: List<Int>,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSteps = rememberUpdatedState(steps)
    val onChange = rememberUpdatedState(onValueChange)
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._29sdp))
    ) {
        val trackWidthPx = with(density) { maxWidth.toPx() }
        val fraction = PetRoomSettingsPolicy.fraction(value, steps)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(dimensionResource(SdpR.dimen._9sdp))
                .clip(CircleShape)
                .background(colorResource(R.color.colors_FFEBF1))
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(fraction.coerceAtLeast(MIN_PROGRESS_FRACTION))
                .height(dimensionResource(SdpR.dimen._9sdp))
                .clip(CircleShape)
                .background(colorResource(R.color.colors_FB3675))
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offsetFraction(fraction)
                .width(dimensionResource(SdpR.dimen._3sdp))
                .height(dimensionResource(SdpR.dimen._29sdp))
                .clip(CircleShape)
                .background(colorResource(R.color.colors_FB3675))
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(steps) {
                    detectHorizontalDragGestures { change, _ ->
                        val next = PetRoomSettingsPolicy.valueAt(
                            fraction = change.position.x / trackWidthPx,
                            steps = currentSteps.value
                        )
                        onChange.value(next)
                    }
                }
        )
    }
}

/** Keeps the thumb inside the track instead of hanging off both ends. */
private fun Modifier.offsetFraction(fraction: Float): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val travel = (constraints.maxWidth - placeable.width).coerceAtLeast(0)
    layout(placeable.width, placeable.height) {
        placeable.placeRelative((travel * fraction).roundToInt(), 0)
    }
}

@Composable
private fun DialogButton(
    labelRes: Int,
    backgroundRes: Int,
    labelColorRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(dimensionResource(SdpR.dimen._37sdp))
            .clip(CircleShape)
            .background(colorResource(backgroundRes))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(labelRes),
            color = colorResource(labelColorRes),
            fontWeight = FontWeight.Medium,
            fontSize = dimensionResource(SspR.dimen._12ssp).value.sp
        )
    }
}

private const val SETTINGS_DIALOG_WIDTH_FRACTION = 320f / 360f
private const val MIN_PROGRESS_FRACTION = 0.04f

@Preview(showBackground = true, backgroundColor = 0xFF9B9C9E, widthDp = 360, heightDp = 420)
@Composable
private fun PetRoomSettingsPreview() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
        contentAlignment = Alignment.Center
    ) {
        PetRoomSettingsCard(
            settings = PetRoomSettingsUiState(speedPercent = 100, sizePercent = 100),
            onSpeedChange = {},
            onSizeChange = {},
            onSave = {},
            onDismiss = {}
        )
    }
}
