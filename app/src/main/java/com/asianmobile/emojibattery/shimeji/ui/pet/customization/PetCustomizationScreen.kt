package com.asianmobile.emojibattery.shimeji.ui.pet.customization

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.pet.settings.PetSettingsPolicy
import com.asianmobile.emojibattery.shimeji.ui.shared.component.AppHeaderBar
import com.asianmobile.emojibattery.shimeji.ui.shared.component.AppHeaderLeading
import com.asianmobile.emojibattery.shimeji.ui.shared.component.SettingsRow
import com.asianmobile.emojibattery.shimeji.ui.shared.component.SettingsSection
import com.asianmobile.emojibattery.shimeji.ui.shared.component.SettingsTrailing
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR
import kotlin.math.roundToInt

@Composable
fun PetCustomizationScreen(
    viewModel: PetCustomizationViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onChangeCharacter: (Int) -> Unit,
    onPetRemoved: () -> Unit
) {
    TrackScreenView(ScreenName.PET_CUSTOMIZATION)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var isMessageEditorVisible by remember { mutableStateOf(false) }
    var isRemoveConfirmationVisible by remember { mutableStateOf(false) }

    if (isMessageEditorVisible) {
        PetMessageEditorDialog(
            initialMessages = state.customMessages,
            onSave = { messages ->
                viewModel.setCustomMessages(messages)
                isMessageEditorVisible = false
            },
            onDismiss = { isMessageEditorVisible = false }
        )
    }
    if (isRemoveConfirmationVisible) {
        RemovePetDialog(
            petName = state.name,
            onConfirm = {
                isRemoveConfirmationVisible = false
                if (viewModel.removePet()) onPetRemoved()
            },
            onDismiss = { isRemoveConfirmationVisible = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_FFF9F4))
            .navigationBarsPadding()
    ) {
        AppHeaderBar(
            title = stringResource(
                R.string.pet_customization_title,
                state.slotIndex + 1
            ),
            leadingIcon = AppHeaderLeading.Back,
            onLeadingClick = onBack
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimensionResource(SdpR.dimen._12sdp))
        ) {
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._10sdp)))
            Text(
                text = stringResource(
                    R.string.pet_customization_heading,
                    state.name.ifBlank { stringResource(R.string.home_pet_default_name) }
                ),
                color = colorResource(R.color.colors_2F2440),
                fontFamily = FontFamily(Font(R.font.roboto_bold)),
                fontSize = dimensionResource(SspR.dimen._20ssp).value.sp
            )
            Text(
                text = stringResource(R.string.pet_customization_subtitle),
                color = colorResource(R.color.colors_776D84),
                fontFamily = FontFamily(Font(R.font.roboto_regular)),
                fontSize = dimensionResource(SspR.dimen._10ssp).value.sp
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
            PetIdentityCard(
                state = state,
                onChangeCharacter = { onChangeCharacter(state.slotIndex) }
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
            SettingsSection(
                title = stringResource(R.string.settings_section_appearance_motion)
            ) {
                PetSizeSettingsRow(
                    iconRes = R.drawable.ic_pet_size,
                    title = stringResource(R.string.settings_pet_size_title),
                    subtitle = stringResource(R.string.pet_customization_size_hint),
                    sizePercent = state.sizePercent,
                    onSizeChange = viewModel::updateSize
                )
                SettingsDivider()
                PetSpeedSettingsRow(
                    iconRes = R.drawable.ic_pet_speed,
                    title = stringResource(R.string.settings_pet_speed_title),
                    subtitle = stringResource(R.string.pet_customization_speed_hint),
                    speedPercent = state.speedPercent,
                    onSpeedChange = viewModel::updateSpeed
                )
                SettingsDivider()
                SettingsRow(
                    iconRes = R.drawable.ic_refresh,
                    title = stringResource(R.string.pet_customization_reset_position),
                    subtitle = stringResource(R.string.pet_customization_reset_position_hint),
                    onClick = viewModel::resetPosition
                )
            }
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
            SettingsSection(
                title = stringResource(R.string.settings_section_interaction_speech)
            ) {
                SettingsRow(
                    iconRes = R.drawable.ic_settings_outline,
                    title = stringResource(R.string.settings_pet_interaction_title),
                    subtitle = stringResource(R.string.settings_pet_interaction_subtitle),
                    trailing = SettingsTrailing.SwitchTrailing(
                        checked = state.interactionEnabled,
                        onCheckedChange = viewModel::setInteractionEnabled
                    ),
                    onClick = {
                        viewModel.setInteractionEnabled(!state.interactionEnabled)
                    }
                )
                SettingsDivider()
                SettingsRow(
                    iconRes = R.drawable.ic_chat_bubble,
                    title = stringResource(R.string.settings_pet_messages_title),
                    subtitle = stringResource(R.string.pet_customization_messages_hint),
                    trailing = SettingsTrailing.SwitchTrailing(
                        checked = state.messagesEnabled,
                        onCheckedChange = viewModel::setMessagesEnabled
                    ),
                    onClick = { viewModel.setMessagesEnabled(!state.messagesEnabled) }
                )
                SettingsDivider()
                SettingsRow(
                    iconRes = R.drawable.ic_document_text,
                    title = stringResource(R.string.settings_pet_custom_messages_title),
                    subtitle = if (state.customMessages.isEmpty()) {
                        stringResource(R.string.settings_pet_custom_messages_builtin)
                    } else {
                        pluralStringResource(
                            R.plurals.settings_pet_custom_messages_count,
                            state.customMessages.size,
                            state.customMessages.size
                        )
                    },
                    trailing = SettingsTrailing.TextTrailing(
                        state.customMessages.size.toString()
                    ),
                    onClick = { isMessageEditorVisible = true }
                )
            }
            Text(
                text = stringResource(R.string.pet_customization_apply_note),
                color = colorResource(R.color.colors_776D84),
                fontFamily = FontFamily(Font(R.font.roboto_regular)),
                fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
                modifier = Modifier.padding(
                    horizontal = dimensionResource(SdpR.dimen._4sdp),
                    vertical = dimensionResource(SdpR.dimen._10sdp)
                )
            )
            if (state.canRemove) {
                Text(
                    text = stringResource(R.string.pet_customization_remove),
                    color = colorResource(R.color.colors_E45D6A),
                    fontFamily = FontFamily(Font(R.font.roboto_semibold)),
                    fontSize = dimensionResource(SspR.dimen._10ssp).value.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._10sdp)))
                        .background(colorResource(R.color.colors_FFE8EF))
                        .clickable { isRemoveConfirmationVisible = true }
                        .padding(vertical = dimensionResource(SdpR.dimen._12sdp))
                )
            }
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._16sdp)))
        }
    }
}

@Composable
private fun PetIdentityCard(
    state: PetCustomizationUiState,
    onChangeCharacter: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._20sdp)))
            .background(colorResource(R.color.colors_EDE4FF))
            .padding(dimensionResource(SdpR.dimen._14sdp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._64sdp))
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._16sdp)))
                .background(colorResource(R.color.colors_FFF9F4)),
            contentAlignment = Alignment.Center
        ) {
            if (state.previewImagePath != null) {
                AsyncImage(
                    model = state.previewImagePath,
                    contentDescription = state.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(dimensionResource(SdpR.dimen._4sdp))
                )
            } else {
                Text(
                    text = (state.slotIndex + 1).toString(),
                    color = colorResource(R.color.colors_7B61FF),
                    fontFamily = FontFamily(Font(R.font.roboto_semibold)),
                    fontSize = dimensionResource(SspR.dimen._17ssp).value.sp
                )
            }
        }
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._8sdp)))
        Text(
            text = state.name.ifBlank { stringResource(R.string.home_pet_default_name) },
            color = colorResource(R.color.colors_2F2440),
            fontFamily = FontFamily(Font(R.font.roboto_semibold)),
            fontSize = dimensionResource(SspR.dimen._14ssp).value.sp
        )
        if (state.author.isNotBlank()) {
            Text(
                text = stringResource(R.string.pet_customization_by_author, state.author),
                color = colorResource(R.color.colors_776D84),
                fontFamily = FontFamily(Font(R.font.roboto_regular)),
                fontSize = dimensionResource(SspR.dimen._9ssp).value.sp
            )
        }
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._8sdp)))
        Text(
            text = stringResource(R.string.pet_customization_change_character),
            color = colorResource(R.color.colors_5D46D7),
            fontFamily = FontFamily(Font(R.font.roboto_semibold)),
            fontSize = dimensionResource(SspR.dimen._10ssp).value.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp)))
                .background(colorResource(R.color.colors_FFFFFB))
                .clickable(onClick = onChangeCharacter)
                .padding(
                    horizontal = dimensionResource(SdpR.dimen._14sdp),
                    vertical = dimensionResource(SdpR.dimen._8sdp)
                )
        )
    }
}

@Composable
private fun PetSizeSettingsRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    sizePercent: Int,
    onSizeChange: (Int) -> Unit
) {
    val policy = remember { PetSettingsPolicy() }
    PetPercentageSettingsRow(
        iconRes = iconRes,
        title = title,
        subtitle = subtitle,
        percent = sizePercent,
        minimumPercent = PetSettingsPolicy.MIN_SIZE_PERCENT,
        maximumPercent = PetSettingsPolicy.MAX_SIZE_PERCENT,
        stepPercent = PetSettingsPolicy.SIZE_STEP_PERCENT,
        sanitizePercent = policy::sanitizeSizePercent,
        onPercentChange = onSizeChange
    )
}

@Composable
private fun PetSpeedSettingsRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    speedPercent: Int,
    onSpeedChange: (Int) -> Unit
) {
    val policy = remember { PetSettingsPolicy() }
    PetPercentageSettingsRow(
        iconRes = iconRes,
        title = title,
        subtitle = subtitle,
        percent = speedPercent,
        minimumPercent = PetSettingsPolicy.MIN_SPEED_PERCENT,
        maximumPercent = PetSettingsPolicy.MAX_SPEED_PERCENT,
        stepPercent = PetSettingsPolicy.SPEED_STEP_PERCENT,
        sanitizePercent = policy::sanitizeSpeedPercent,
        onPercentChange = onSpeedChange
    )
}

@Composable
private fun PetPercentageSettingsRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    percent: Int,
    minimumPercent: Int,
    maximumPercent: Int,
    stepPercent: Int,
    sanitizePercent: (Int) -> Int,
    onPercentChange: (Int) -> Unit
) {
    var sliderValue by remember {
        mutableFloatStateOf(percent.toFloat())
    }
    var pendingPercent by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(percent) {
        if (pendingPercent == null || pendingPercent == percent) {
            sliderValue = percent.toFloat()
            pendingPercent = null
        }
    }
    val updatePercent = { requestedPercent: Int ->
        val sanitized = sanitizePercent(requestedPercent)
        sliderValue = sanitized.toFloat()
        if (sanitized != (pendingPercent ?: percent)) {
            pendingPercent = sanitized
            onPercentChange(sanitized)
        }
    }

    Column {
        PetValueSettingsRow(
            iconRes = iconRes,
            title = title,
            subtitle = subtitle,
            value = "${sliderValue.roundToInt()}%",
            canDecrease = sliderValue > minimumPercent,
            canIncrease = sliderValue < maximumPercent,
            onDecrease = {
                updatePercent(sliderValue.roundToInt() - stepPercent)
            },
            onIncrease = {
                updatePercent(sliderValue.roundToInt() + stepPercent)
            }
        )
        Slider(
            value = sliderValue,
            onValueChange = { value ->
                updatePercent(value.roundToInt())
            },
            valueRange = minimumPercent.toFloat()..maximumPercent.toFloat(),
            steps = ((maximumPercent - minimumPercent) / stepPercent) - 1,
            colors = SliderDefaults.colors(
                thumbColor = colorResource(R.color.colors_7B61FF),
                activeTrackColor = colorResource(R.color.colors_7B61FF),
                activeTickColor = colorResource(R.color.colors_FFFFFF),
                inactiveTrackColor = colorResource(R.color.colors_E9DFEF),
                inactiveTickColor = colorResource(R.color.colors_776D84)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = title }
        )
    }
}

@Composable
private fun PetValueSettingsRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    value: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    val decreaseDescription = stringResource(R.string.settings_decrease_value, title)
    val increaseDescription = stringResource(R.string.settings_increase_value, title)
    SettingsRow(
        iconRes = iconRes,
        title = title,
        subtitle = subtitle,
        trailing = SettingsTrailing.Custom {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.background(
                    colorResource(R.color.colors_F7F0FF),
                    RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
                )
            ) {
                StepperButton(
                    symbol = stringResource(R.string.common_decrease_symbol),
                    enabled = canDecrease,
                    contentDescription = decreaseDescription,
                    onClick = onDecrease
                )
                Text(
                    text = value,
                    color = colorResource(R.color.colors_2F2440),
                    fontFamily = FontFamily(Font(R.font.roboto_medium)),
                    fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(dimensionResource(SdpR.dimen._34sdp))
                )
                StepperButton(
                    symbol = stringResource(R.string.common_increase_symbol),
                    enabled = canIncrease,
                    contentDescription = increaseDescription,
                    onClick = onIncrease
                )
            }
        },
        onClick = {}
    )
}

@Composable
private fun StepperButton(
    symbol: String,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    Text(
        text = symbol,
        color = colorResource(
            if (enabled) R.color.colors_7B61FF else R.color.colors_776D84
        ),
        fontFamily = FontFamily(Font(R.font.roboto_semibold)),
        fontSize = dimensionResource(SspR.dimen._13ssp).value.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .size(dimensionResource(SdpR.dimen._24sdp))
            .semantics { this.contentDescription = contentDescription }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(top = dimensionResource(SdpR.dimen._2sdp))
    )
}

@Composable
private fun RemovePetDialog(
    petName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pet_customization_remove_title)) },
        text = {
            Text(
                stringResource(
                    R.string.pet_customization_remove_message,
                    petName
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(R.string.pet_customization_remove_confirm),
                    color = colorResource(R.color.colors_E45D6A)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel_label))
            }
        },
        containerColor = colorResource(R.color.colors_FFFFFB),
        titleContentColor = colorResource(R.color.colors_2F2440),
        textContentColor = colorResource(R.color.colors_776D84)
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        thickness = dimensionResource(SdpR.dimen._1sdp),
        color = colorResource(R.color.colors_E9DFEF)
    )
}

@Preview(showBackground = true)
@Composable
private fun PetIdentityCardPreview() {
    PetIdentityCard(
        state = PetCustomizationUiState(
            name = stringResource(R.string.home_pet_default_name),
            author = stringResource(R.string.pet_catalog_unknown_author)
        ),
        onChangeCharacter = {}
    )
}
