package com.asianmobile.emojibattery.shimeji.ui.petroom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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

/** Material 3 slider, snapped to the steps the pet settings accept and themed pink. */
@Composable
private fun StepSlider(
    value: Int,
    steps: List<Int>,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pink = colorResource(R.color.colors_FB3675)
    val index = steps.indexOf(PetRoomSettingsPolicy.nearest(value, steps)).coerceAtLeast(0)
    Slider(
        value = index.toFloat(),
        onValueChange = { position ->
            onValueChange(steps[position.roundToInt().coerceIn(0, steps.lastIndex)])
        },
        valueRange = 0f..(steps.size - 1).toFloat(),
        steps = (steps.size - 2).coerceAtLeast(0),
        colors = SliderDefaults.colors(
            thumbColor = pink,
            activeTrackColor = pink,
            inactiveTrackColor = colorResource(R.color.colors_FFEBF1),
            // Figma draws a plain track, so the step ticks stay invisible.
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent
        ),
        modifier = modifier.fillMaxWidth()
    )
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
