package com.asianmobile.privatebrower.ui.home.settings

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.pet.settings.PetSettingsPolicy
import com.asianmobile.privatebrower.ui.component.AppHeaderBar
import com.asianmobile.privatebrower.ui.component.AppHeaderLeading
import com.asianmobile.privatebrower.ui.component.SettingsRow
import com.asianmobile.privatebrower.ui.component.SettingsSection
import com.asianmobile.privatebrower.ui.component.SettingsTrailing
import com.asianmobile.privatebrower.utils.ScreenName
import com.asianmobile.privatebrower.utils.TrackScreenView
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

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
            .background(colorResource(R.color.colors_161718))
            .statusBarsPadding()
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
            PetIdentityCard(
                state = state,
                onChangeCharacter = { onChangeCharacter(state.slotIndex) }
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
            SettingsSection(
                title = stringResource(R.string.settings_section_appearance_motion)
            ) {
                PetValueSettingsRow(
                    iconRes = R.drawable.ic_pet_size,
                    title = stringResource(R.string.settings_pet_size_title),
                    subtitle = stringResource(R.string.pet_customization_size_hint),
                    value = "${state.sizePercent}%",
                    canDecrease = state.sizePercent > PetSettingsPolicy.MIN_SIZE_PERCENT,
                    canIncrease = state.sizePercent < PetSettingsPolicy.MAX_SIZE_PERCENT,
                    onDecrease = {
                        viewModel.updateSize(
                            state.sizePercent - PetSettingsPolicy.SIZE_STEP_PERCENT
                        )
                    },
                    onIncrease = {
                        viewModel.updateSize(
                            state.sizePercent + PetSettingsPolicy.SIZE_STEP_PERCENT
                        )
                    }
                )
                SettingsDivider()
                PetValueSettingsRow(
                    iconRes = R.drawable.ic_pet_speed,
                    title = stringResource(R.string.settings_pet_speed_title),
                    subtitle = stringResource(R.string.pet_customization_speed_hint),
                    value = "${state.speedPercent}%",
                    canDecrease = state.speedPercent > PetSettingsPolicy.MIN_SPEED_PERCENT,
                    canIncrease = state.speedPercent < PetSettingsPolicy.MAX_SPEED_PERCENT,
                    onDecrease = {
                        viewModel.updateSpeed(
                            state.speedPercent - PetSettingsPolicy.SPEED_STEP_PERCENT
                        )
                    },
                    onIncrease = {
                        viewModel.updateSpeed(
                            state.speedPercent + PetSettingsPolicy.SPEED_STEP_PERCENT
                        )
                    }
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
                color = colorResource(R.color.colors_9B9C9E),
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
                modifier = Modifier.padding(
                    horizontal = dimensionResource(SdpR.dimen._4sdp),
                    vertical = dimensionResource(SdpR.dimen._10sdp)
                )
            )
            if (state.canRemove) {
                Text(
                    text = stringResource(R.string.pet_customization_remove),
                    color = colorResource(R.color.colors_FF5959),
                    fontFamily = FontFamily(Font(R.font.inter_semibold)),
                    fontSize = dimensionResource(SspR.dimen._10ssp).value.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._10sdp)))
                        .background(colorResource(R.color.colors_212327))
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
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._14sdp)))
            .background(colorResource(R.color.colors_212327))
            .padding(dimensionResource(SdpR.dimen._14sdp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._64sdp))
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._16sdp)))
                .background(colorResource(R.color.pet_demo_fur)),
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
                    color = colorResource(R.color.white),
                    fontFamily = FontFamily(Font(R.font.inter_semibold)),
                    fontSize = dimensionResource(SspR.dimen._17ssp).value.sp
                )
            }
        }
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._8sdp)))
        Text(
            text = state.name.ifBlank { stringResource(R.string.home_pet_default_name) },
            color = colorResource(R.color.white),
            fontFamily = FontFamily(Font(R.font.inter_semibold)),
            fontSize = dimensionResource(SspR.dimen._14ssp).value.sp
        )
        if (state.author.isNotBlank()) {
            Text(
                text = stringResource(R.string.pet_customization_by_author, state.author),
                color = colorResource(R.color.colors_9B9C9E),
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = dimensionResource(SspR.dimen._9ssp).value.sp
            )
        }
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._8sdp)))
        Text(
            text = stringResource(R.string.pet_customization_change_character),
            color = colorResource(R.color.colors_C0D1FE),
            fontFamily = FontFamily(Font(R.font.inter_semibold)),
            fontSize = dimensionResource(SspR.dimen._10ssp).value.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp)))
                .background(colorResource(R.color.colors_333538))
                .clickable(onClick = onChangeCharacter)
                .padding(
                    horizontal = dimensionResource(SdpR.dimen._14sdp),
                    vertical = dimensionResource(SdpR.dimen._8sdp)
                )
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
                    colorResource(R.color.colors_333538),
                    RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
                )
            ) {
                StepperButton(
                    symbol = "−",
                    enabled = canDecrease,
                    contentDescription = decreaseDescription,
                    onClick = onDecrease
                )
                Text(
                    text = value,
                    color = colorResource(R.color.white),
                    fontFamily = FontFamily(Font(R.font.inter_medium)),
                    fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(dimensionResource(SdpR.dimen._34sdp))
                )
                StepperButton(
                    symbol = "+",
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
            if (enabled) R.color.colors_C0D1FE else R.color.colors_9B9C9E
        ),
        fontFamily = FontFamily(Font(R.font.inter_semibold)),
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
                    color = colorResource(R.color.colors_FF5959)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel_label))
            }
        },
        containerColor = colorResource(R.color.colors_212327),
        titleContentColor = colorResource(R.color.white),
        textContentColor = colorResource(R.color.colors_9B9C9E)
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = colorResource(R.color.colors_333538)
    )
}
