package com.asianmobile.privatebrower.ui.home.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.ads.config.SCREEN_SETTING
import com.asianmobile.privatebrower.ads.ui.compose.NativeAdInternal
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
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onNavigateToLanguage: () -> Unit = {}
) {
    TrackScreenView(ScreenName.SETTINGS)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var rateAppState by remember { mutableStateOf(RateAppUiState()) }
    var isMessageEditorVisible by remember { mutableStateOf(false) }
    RateAppFlow(
        context = context,
        state = rateAppState,
        onStateChange = { rateAppState = it },
        onSendFeedback = viewModel::sendRateFeedback
    )
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_161718))
            .statusBarsPadding()
    ) {
        AppHeaderBar(
            title = stringResource(R.string.settings_title),
            leadingIcon = AppHeaderLeading.Back,
            onLeadingClick = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimensionResource(SdpR.dimen._12sdp))
        ) {
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
            SettingsSection(title = stringResource(R.string.settings_section_screen_pets)) {
                PetValueSettingsRow(
                    iconRes = R.drawable.ic_notification_pet,
                    title = stringResource(R.string.settings_pet_count_title),
                    subtitle = stringResource(R.string.settings_pet_count_subtitle, state.maxPets),
                    value = state.petCount.toString(),
                    canDecrease = state.petCount > 1,
                    canIncrease = state.petCount < state.maxPets,
                    onDecrease = viewModel::decreasePetCount,
                    onIncrease = viewModel::increasePetCount
                )
                SettingsDivider()
                PetValueSettingsRow(
                    iconRes = R.drawable.ic_pet_size,
                    title = stringResource(R.string.settings_pet_size_title),
                    value = "${state.sizePercent}%",
                    canDecrease = state.sizePercent > 75,
                    canIncrease = state.sizePercent < 150,
                    onDecrease = viewModel::decreaseSize,
                    onIncrease = viewModel::increaseSize
                )
                SettingsDivider()
                PetValueSettingsRow(
                    iconRes = R.drawable.ic_pet_speed,
                    title = stringResource(R.string.settings_pet_speed_title),
                    value = "${state.speedPercent}%",
                    canDecrease = state.speedPercent > 50,
                    canIncrease = state.speedPercent < 150,
                    onDecrease = viewModel::decreaseSpeed,
                    onIncrease = viewModel::increaseSpeed
                )
                SettingsDivider()
                SettingsRow(
                    iconRes = R.drawable.ic_media_volume,
                    title = stringResource(R.string.settings_pet_sound_title),
                    subtitle = stringResource(R.string.settings_pet_sound_subtitle),
                    trailing = SettingsTrailing.SwitchTrailing(
                        checked = state.soundEnabled,
                        onCheckedChange = viewModel::setSoundEnabled
                    ),
                    onClick = { viewModel.setSoundEnabled(!state.soundEnabled) }
                )
                SettingsDivider()
                SettingsRow(
                    iconRes = R.drawable.ic_chat_bubble,
                    title = stringResource(R.string.settings_pet_messages_title),
                    subtitle = stringResource(R.string.settings_pet_messages_subtitle),
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
                SettingsDivider()
                SettingsRow(
                    iconRes = R.drawable.ic_settings_outline,
                    title = stringResource(R.string.settings_pet_interaction_title),
                    subtitle = stringResource(R.string.settings_pet_interaction_subtitle),
                    trailing = SettingsTrailing.SwitchTrailing(
                        checked = state.interactionEnabled,
                        onCheckedChange = viewModel::setInteractionEnabled
                    ),
                    onClick = { viewModel.setInteractionEnabled(!state.interactionEnabled) }
                )
                Text(
                    text = stringResource(R.string.settings_pet_apply_on_restart),
                    color = colorResource(R.color.colors_9B9C9E),
                    fontFamily = FontFamily(Font(R.font.inter_regular)),
                    fontSize = dimensionResource(SspR.dimen._8ssp).value.sp
                )
            }
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
            SettingsSection(title = stringResource(R.string.settings_section_other_short)) {
                LanguageSettingsRow(
                    context = context,
                    onClick = onNavigateToLanguage
                )
                SettingsDivider()
                SettingsRow(
                    iconRes = R.drawable.ic_setting_share_v2,
                    title = stringResource(R.string.settings_share_title),
                    onClick = { viewModel.onShareClicked(context) }
                )
                SettingsDivider()
                SettingsRow(
                    iconRes = R.drawable.ic_setting_rate_us_v2,
                    title = stringResource(R.string.settings_rate_us_title),
                    onClick = {
                        rateAppState = RateAppUiState(isDialogVisible = true)
                    }
                )
                SettingsDivider()
                SettingsRow(
                    iconRes = R.drawable.ic_setting_feedback_v2,
                    title = stringResource(R.string.settings_feedback_title),
                    onClick = { viewModel.onFeedbackClicked(context) }
                )
            }
            Text(
                text = stringResource(R.string.settings_version_format, state.versionName),
                color = colorResource(R.color.colors_9B9C9E),
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = dimensionResource(SdpR.dimen._12sdp))
            )
        }

        NativeAdInternal(
            screenCode = SCREEN_SETTING,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = dimensionResource(SdpR.dimen._12sdp),
                    end = dimensionResource(SdpR.dimen._12sdp),
                    bottom = dimensionResource(SdpR.dimen._12sdp)
                )
        )
    }
}

@Composable
private fun PetValueSettingsRow(
    iconRes: Int,
    title: String,
    value: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    subtitle: String? = null
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
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.width(dimensionResource(SdpR.dimen._30sdp))
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
        color = colorResource(if (enabled) R.color.colors_C0D1FE else R.color.colors_9B9C9E),
        fontFamily = FontFamily(Font(R.font.inter_semibold)),
        fontSize = dimensionResource(SspR.dimen._13ssp).value.sp,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier
            .size(dimensionResource(SdpR.dimen._24sdp))
            .semantics { this.contentDescription = contentDescription }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(top = dimensionResource(SdpR.dimen._2sdp))
    )
}

@Composable
private fun RateAppFlow(
    context: Context,
    state: RateAppUiState,
    onStateChange: (RateAppUiState) -> Unit,
    onSendFeedback: (Context, RateAppUiState) -> Unit
) {
    if (!state.isDialogVisible) return

    RateAppDialog(
        state = state,
        onSelectStars = { stars ->
            onStateChange(
                state.copy(
                    selectedStars = stars,
                    step = if (stars >= 4) RateAppStep.HighRating else RateAppStep.LowRating
                )
            )
        },
        onDismiss = { onStateChange(state.copy(isDialogVisible = false)) },
        onRateOnPlayStore = {
            val appPackage = context.packageName
            val marketIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=$appPackage")
            )
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$appPackage")
            )
            runCatching { context.startActivity(marketIntent) }
                .onFailure { context.startActivity(webIntent) }
            onStateChange(state.copy(isDialogVisible = false))
        },
        onGoToFeedbackForm = {
            onStateChange(state.copy(step = RateAppStep.FeedbackForm))
        },
        onToggleFeedbackOption = { index ->
            val options = state.feedbackOptions.mapIndexed { optionIndex, option ->
                if (optionIndex == index) option.copy(isSelected = !option.isSelected) else option
            }
            onStateChange(state.copy(feedbackOptions = options))
        },
        onUpdateOtherText = { text ->
            onStateChange(state.copy(otherFeedbackText = text))
        },
        onSendFeedback = {
            if (state.canSendFeedback()) {
                onStateChange(
                    state.copy(
                        isSendingFeedback = true,
                        step = RateAppStep.ThankYou
                    )
                )
                onSendFeedback(context, state)
            }
        },
        onShowThankYou = {
            onStateChange(state.copy(step = RateAppStep.ThankYou))
        }
    )
}

@Composable
private fun LanguageSettingsRow(
    context: Context,
    onClick: () -> Unit
) {
    val languagePreferences = remember(context) {
        context.getSharedPreferences("language_cache", Context.MODE_PRIVATE)
    }
    val languageKey = languagePreferences.getString("key_language", "en") ?: "en"
    val country = languagePreferences.getString("country_language", "US") ?: "US"
    val flag = when (languageKey) {
        "en" -> R.drawable.ic_flag_en
        "hi" -> R.drawable.ic_flag_hi
        "es" -> R.drawable.ic_flag_es
        "pt" -> R.drawable.ic_flag_pt
        "de" -> R.drawable.ic_flag_de
        "ar" -> R.drawable.ic_flag_ar
        "vi" -> R.drawable.ic_flag_vi
        "fr" -> R.drawable.ic_flag_fr
        "ha" -> R.drawable.ic_flag_ha
        "af" -> R.drawable.ic_flag_af
        "zh" -> R.drawable.ic_flag_zh
        else -> R.drawable.ic_flag_en
    }
    val locale = java.util.Locale.Builder()
        .setLanguage(languageKey)
        .setRegion(country)
        .build()
    val displayName = locale.getDisplayLanguage(locale).replaceFirstChar { it.uppercase() }

    SettingsRow(
        iconRes = R.drawable.ic_setting_language_v2,
        title = stringResource(R.string.settings_language_title),
        subtitle = displayName,
        trailing = SettingsTrailing.Custom {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(flag),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(dimensionResource(SdpR.dimen._18sdp))
                        .clip(CircleShape)
                )
                Spacer(Modifier.width(dimensionResource(SdpR.dimen._6sdp)))
                Icon(
                    painter = painterResource(R.drawable.ic_setting_chevron_right_v2),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._15sdp))
                )
            }
        },
        onClick = onClick
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = colorResource(R.color.colors_333538)
    )
}
