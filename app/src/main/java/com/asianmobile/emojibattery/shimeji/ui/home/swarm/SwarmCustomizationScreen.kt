package com.asianmobile.emojibattery.shimeji.ui.home.swarm

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.data.model.PetSwarmMovementInsets
import com.asianmobile.emojibattery.shimeji.pet.settings.PetSettingsPolicy
import com.asianmobile.emojibattery.shimeji.ui.component.CutePetTopBar
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR
import kotlin.math.roundToInt

@Composable
fun SwarmCustomizationScreen(
    onBack: () -> Unit,
    onChangeCharacter: () -> Unit,
    viewModel: SwarmCustomizationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TrackScreenView(ScreenName.SWARM_CUSTOMIZATION)

    SwarmCustomizationContent(
        state = state,
        onBack = onBack,
        onChangeCharacter = onChangeCharacter,
        onCountChanged = viewModel::updateCount,
        onSizeChanged = viewModel::updateSize,
        onSpeedChanged = viewModel::updateSpeed,
        onRandomizationChanged = viewModel::setRandomizationEnabled,
        onMovementAreaChanged = viewModel::setMovementAreaEnabled,
        onMovementInsetsChanged = viewModel::updateMovementInsets
    )
}

@Composable
private fun SwarmCustomizationContent(
    state: SwarmCustomizationUiState,
    onBack: () -> Unit,
    onChangeCharacter: () -> Unit,
    onCountChanged: (Int) -> Unit,
    onSizeChanged: (Int) -> Unit,
    onSpeedChanged: (Int) -> Unit,
    onRandomizationChanged: (Boolean) -> Unit,
    onMovementAreaChanged: (Boolean) -> Unit,
    onMovementInsetsChanged: (PetSwarmMovementInsets) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_F4F8FC))
            .navigationBarsPadding()
    ) {
        CutePetTopBar(
            title = stringResource(R.string.swarm_customization_title),
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimensionResource(SdpR.dimen._16sdp))
        ) {
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._8sdp)))
            SwarmIdentityCard(
                state = state,
                onChangeCharacter = onChangeCharacter
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._16sdp)))
            SwarmSectionTitle(stringResource(R.string.swarm_customization_setup_section))
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._8sdp)))
            SwarmSettingsCard {
                SwarmCountRow(
                    count = state.count,
                    maxCount = state.maxCount,
                    onCountChanged = onCountChanged
                )
                SwarmDivider()
                SwarmToggleRow(
                    title = stringResource(R.string.swarm_customization_random_title),
                    subtitle = stringResource(R.string.swarm_customization_random_description),
                    checked = state.randomizeSizeAndSpeed,
                    onCheckedChange = onRandomizationChanged
                )
                SwarmDivider()
                SwarmValueSlider(
                    title = stringResource(R.string.swarm_customization_size),
                    value = state.sizePercent,
                    valueLabel = stringResource(
                        R.string.swarm_customization_percent_value,
                        state.sizePercent
                    ),
                    minimum = PetSettingsPolicy.MIN_SIZE_PERCENT,
                    maximum = PetSettingsPolicy.MAX_SIZE_PERCENT,
                    step = PetSettingsPolicy.SIZE_STEP_PERCENT,
                    onValueChanged = onSizeChanged
                )
                SwarmDivider()
                SwarmValueSlider(
                    title = stringResource(R.string.swarm_customization_speed),
                    value = state.speedPercent,
                    valueLabel = stringResource(
                        R.string.swarm_customization_percent_value,
                        state.speedPercent
                    ),
                    minimum = PetSettingsPolicy.MIN_SPEED_PERCENT,
                    maximum = PetSettingsPolicy.MAX_SPEED_PERCENT,
                    step = PetSettingsPolicy.SPEED_STEP_PERCENT,
                    onValueChanged = onSpeedChanged
                )
            }
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._16sdp)))
            SwarmSectionTitle(stringResource(R.string.swarm_customization_area_section))
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._8sdp)))
            SwarmSettingsCard {
                SwarmToggleRow(
                    title = stringResource(R.string.swarm_customization_area_title),
                    subtitle = stringResource(R.string.swarm_customization_area_description),
                    checked = state.constrainMovementArea,
                    onCheckedChange = onMovementAreaChanged
                )
                if (state.constrainMovementArea) {
                    SwarmDivider()
                    MovementInsetSliders(
                        insets = state.movementInsets,
                        onInsetsChanged = onMovementInsetsChanged
                    )
                }
            }
            Text(
                text = stringResource(R.string.swarm_customization_live_note),
                color = colorResource(R.color.colors_776D84),
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
                modifier = Modifier.padding(
                    horizontal = dimensionResource(SdpR.dimen._4sdp),
                    vertical = dimensionResource(SdpR.dimen._12sdp)
                )
            )
        }
        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(SdpR.dimen._16sdp),
                    vertical = dimensionResource(SdpR.dimen._10sdp)
                )
                .height(dimensionResource(SdpR.dimen._46sdp)),
            shape = RoundedCornerShape(dimensionResource(SdpR.dimen._15sdp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.colors_12B890),
                contentColor = colorResource(R.color.colors_FFFFFF)
            )
        ) {
            Text(
                text = stringResource(R.string.common_done),
                fontFamily = FontFamily(Font(R.font.inter_semibold)),
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp
            )
        }
    }
}

@Composable
private fun SwarmIdentityCard(
    state: SwarmCustomizationUiState,
    onChangeCharacter: () -> Unit
) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._20sdp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colorResource(R.color.colors_D8F4EE))
            .border(
                dimensionResource(SdpR.dimen._1sdp),
                colorResource(R.color.colors_12B890),
                shape
            )
            .clickable(onClick = onChangeCharacter)
            .padding(dimensionResource(SdpR.dimen._14sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._84sdp))
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._16sdp)))
                .background(colorResource(R.color.colors_FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            if (state.previewImagePath != null) {
                AsyncImage(
                    model = state.previewImagePath,
                    contentDescription = state.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(dimensionResource(SdpR.dimen._6sdp))
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_notification_pet),
                    contentDescription = null,
                    tint = colorResource(R.color.colors_12B890),
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._42sdp))
                )
            }
        }
        Spacer(Modifier.size(dimensionResource(SdpR.dimen._12sdp)))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.name.ifBlank {
                    stringResource(R.string.home_pet_default_name)
                },
                color = colorResource(R.color.colors_2F2440),
                fontFamily = FontFamily(Font(R.font.inter_bold)),
                fontSize = dimensionResource(SspR.dimen._15ssp).value.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (state.author.isNotBlank()) {
                Text(
                    text = stringResource(
                        R.string.pet_customization_by_author,
                        state.author
                    ),
                    color = colorResource(R.color.colors_776D84),
                    fontFamily = FontFamily(Font(R.font.inter_regular)),
                    fontSize = dimensionResource(SspR.dimen._8ssp).value.sp
                )
            }
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._7sdp)))
            Text(
                text = stringResource(R.string.pet_customization_change_character),
                color = colorResource(R.color.colors_12B890),
                fontFamily = FontFamily(Font(R.font.inter_semibold)),
                fontSize = dimensionResource(SspR.dimen._9ssp).value.sp
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = stringResource(R.string.pet_customization_change_character),
            tint = colorResource(R.color.colors_12B890),
            modifier = Modifier.size(dimensionResource(SdpR.dimen._18sdp))
        )
    }
}

@Composable
private fun SwarmSectionTitle(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(
                    width = dimensionResource(SdpR.dimen._4sdp),
                    height = dimensionResource(SdpR.dimen._22sdp)
                )
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._3sdp)))
                .background(colorResource(R.color.colors_12B890))
        )
        Spacer(Modifier.size(dimensionResource(SdpR.dimen._8sdp)))
        Text(
            text = title,
            color = colorResource(R.color.colors_2F2440),
            fontFamily = FontFamily(Font(R.font.inter_bold)),
            fontWeight = FontWeight.Bold,
            fontSize = dimensionResource(SspR.dimen._15ssp).value.sp
        )
    }
}

@Composable
private fun SwarmSettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._18sdp)))
            .background(colorResource(R.color.colors_FFFFFF))
            .padding(dimensionResource(SdpR.dimen._14sdp)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._10sdp))
    ) {
        content()
    }
}

@Composable
private fun SwarmCountRow(
    count: Int,
    maxCount: Int,
    onCountChanged: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.swarm_customization_count),
                color = colorResource(R.color.colors_2F2440),
                fontFamily = FontFamily(Font(R.font.inter_semibold)),
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp
            )
            Text(
                text = stringResource(R.string.swarm_customization_count_description, maxCount),
                color = colorResource(R.color.colors_776D84),
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = dimensionResource(SspR.dimen._8ssp).value.sp
            )
        }
        SwarmStepButton(
            iconRes = R.drawable.ic_remove,
            enabled = count > 1,
            contentDescription = stringResource(R.string.home_mode_swarm_decrease),
            onClick = { onCountChanged(count - 1) }
        )
        Text(
            text = count.toString(),
            color = colorResource(R.color.colors_12B890),
            fontFamily = FontFamily(Font(R.font.inter_bold)),
            fontSize = dimensionResource(SspR.dimen._13ssp).value.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.size(dimensionResource(SdpR.dimen._30sdp))
        )
        SwarmStepButton(
            iconRes = R.drawable.ic_plus,
            enabled = count < maxCount,
            contentDescription = stringResource(R.string.home_mode_swarm_increase),
            onClick = { onCountChanged(count + 1) }
        )
    }
}

@Composable
private fun SwarmStepButton(
    iconRes: Int,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(dimensionResource(SdpR.dimen._34sdp))
            .clip(CircleShape)
            .background(colorResource(R.color.colors_D8F4EE))
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = colorResource(
                if (enabled) R.color.colors_12B890 else R.color.colors_9297A5
            ),
            modifier = Modifier.size(dimensionResource(SdpR.dimen._17sdp))
        )
    }
}

@Composable
private fun SwarmToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colorResource(R.color.colors_2F2440),
                fontFamily = FontFamily(Font(R.font.inter_semibold)),
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp
            )
            Text(
                text = subtitle,
                color = colorResource(R.color.colors_776D84),
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = dimensionResource(SspR.dimen._8ssp).value.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = colorResource(R.color.colors_12B890),
                uncheckedTrackColor = colorResource(R.color.colors_9297A5)
            )
        )
    }
}

@Composable
private fun SwarmValueSlider(
    title: String,
    value: Int,
    valueLabel: String,
    minimum: Int,
    maximum: Int,
    step: Int,
    onValueChanged: (Int) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(value.toFloat()) }
    LaunchedEffect(value) {
        sliderValue = value.toFloat()
    }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = colorResource(R.color.colors_2F2440),
                fontFamily = FontFamily(Font(R.font.inter_semibold)),
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueLabel,
                color = colorResource(R.color.colors_12B890),
                fontFamily = FontFamily(Font(R.font.inter_medium)),
                fontSize = dimensionResource(SspR.dimen._9ssp).value.sp
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { requested ->
                val snapped = (
                    ((requested.roundToInt() - minimum + step / 2) / step) * step +
                        minimum
                    ).coerceIn(minimum, maximum)
                sliderValue = snapped.toFloat()
                if (snapped != value) onValueChanged(snapped)
            },
            valueRange = minimum.toFloat()..maximum.toFloat(),
            steps = ((maximum - minimum) / step) - 1,
            colors = SliderDefaults.colors(
                thumbColor = colorResource(R.color.colors_FFFFFF),
                activeTrackColor = colorResource(R.color.colors_12B890),
                inactiveTrackColor = colorResource(R.color.colors_D8F4EE),
                activeTickColor = colorResource(R.color.colors_12B890),
                inactiveTickColor = colorResource(R.color.colors_9297A5)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = title }
        )
    }
}

@Composable
private fun MovementInsetSliders(
    insets: PetSwarmMovementInsets,
    onInsetsChanged: (PetSwarmMovementInsets) -> Unit
) {
    MovementInsetSlider(
        title = stringResource(R.string.swarm_customization_inset_top),
        value = insets.topPercent,
        onValueChanged = { onInsetsChanged(insets.copy(topPercent = it)) }
    )
    SwarmDivider()
    MovementInsetSlider(
        title = stringResource(R.string.swarm_customization_inset_bottom),
        value = insets.bottomPercent,
        onValueChanged = { onInsetsChanged(insets.copy(bottomPercent = it)) }
    )
    SwarmDivider()
    MovementInsetSlider(
        title = stringResource(R.string.swarm_customization_inset_left),
        value = insets.leftPercent,
        onValueChanged = { onInsetsChanged(insets.copy(leftPercent = it)) }
    )
    SwarmDivider()
    MovementInsetSlider(
        title = stringResource(R.string.swarm_customization_inset_right),
        value = insets.rightPercent,
        onValueChanged = { onInsetsChanged(insets.copy(rightPercent = it)) }
    )
}

@Composable
private fun MovementInsetSlider(
    title: String,
    value: Int,
    onValueChanged: (Int) -> Unit
) {
    SwarmValueSlider(
        title = title,
        value = value,
        valueLabel = stringResource(R.string.swarm_customization_percent_value, value),
        minimum = PetSettingsPolicy.MIN_SWARM_MOVEMENT_INSET_PERCENT,
        maximum = PetSettingsPolicy.MAX_SWARM_MOVEMENT_INSET_PERCENT,
        step = PetSettingsPolicy.SWARM_MOVEMENT_INSET_STEP_PERCENT,
        onValueChanged = onValueChanged
    )
}

@Composable
private fun SwarmDivider() {
    HorizontalDivider(
        thickness = dimensionResource(SdpR.dimen._1sdp),
        color = colorResource(R.color.colors_D8F4EE)
    )
}

@Preview(showBackground = true)
@Composable
private fun SwarmCustomizationPreview() {
    SwarmCustomizationContent(
        state = SwarmCustomizationUiState(
            name = "Nanami Kento",
            count = 8,
            randomizeSizeAndSpeed = true,
            constrainMovementArea = true
        ),
        onBack = {},
        onChangeCharacter = {},
        onCountChanged = {},
        onSizeChanged = {},
        onSpeedChanged = {},
        onRandomizationChanged = {},
        onMovementAreaChanged = {},
        onMovementInsetsChanged = {}
    )
}
