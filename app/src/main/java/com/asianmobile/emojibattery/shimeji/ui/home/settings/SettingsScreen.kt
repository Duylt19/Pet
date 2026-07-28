package com.asianmobile.emojibattery.shimeji.ui.home.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_SETTING
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.NativeAdInternal
import com.asianmobile.emojibattery.shimeji.ui.component.AppHeaderBar
import com.asianmobile.emojibattery.shimeji.ui.component.AppHeaderLeading
import com.asianmobile.emojibattery.shimeji.ui.component.SettingsRow
import com.asianmobile.emojibattery.shimeji.ui.component.SettingsSection
import com.asianmobile.emojibattery.shimeji.ui.component.SettingsTrailing
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onNavigateToLanguage: () -> Unit = {},
    onNavigateToPetCustomization: (Int) -> Unit = {},
    onAddPet: (Int) -> Unit = {}
) {
    TrackScreenView(ScreenName.SETTINGS)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var rateAppState by remember { mutableStateOf(RateAppUiState()) }
    RateAppFlow(
        context = context,
        state = rateAppState,
        onStateChange = { rateAppState = it },
        onSendFeedback = viewModel::sendRateFeedback
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_FFF9F4))
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
            Text(
                text = stringResource(R.string.settings_heading),
                color = colorResource(R.color.colors_2F2440),
                fontFamily = FontFamily(Font(R.font.inter_bold)),
                fontSize = dimensionResource(SspR.dimen._20ssp).value.sp,
                modifier = Modifier.padding(
                    start = dimensionResource(SdpR.dimen._4sdp),
                    end = dimensionResource(SdpR.dimen._4sdp)
                )
            )
            Text(
                text = stringResource(R.string.settings_subtitle),
                color = colorResource(R.color.colors_776D84),
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = dimensionResource(SspR.dimen._10ssp).value.sp,
                modifier = Modifier.padding(
                    start = dimensionResource(SdpR.dimen._4sdp),
                    end = dimensionResource(SdpR.dimen._4sdp),
                    top = dimensionResource(SdpR.dimen._3sdp),
                    bottom = dimensionResource(SdpR.dimen._14sdp)
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_section_my_pets),
                    color = colorResource(R.color.colors_2F2440),
                    fontFamily = FontFamily(Font(R.font.inter_semibold)),
                    fontSize = dimensionResource(SspR.dimen._11ssp).value.sp
                )
                Text(
                    text = stringResource(
                        R.string.settings_pet_count_compact,
                        state.petCount,
                        state.maxPets
                    ),
                    color = colorResource(R.color.colors_776D84),
                    fontFamily = FontFamily(Font(R.font.inter_regular)),
                    fontSize = dimensionResource(SspR.dimen._9ssp).value.sp
                )
            }
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._7sdp)))
            state.petSlots.forEach { slot ->
                PetProfileCard(
                    slot = slot,
                    onClick = { onNavigateToPetCustomization(slot.slotIndex) }
                )
                Spacer(Modifier.height(dimensionResource(SdpR.dimen._8sdp)))
            }
            if (state.canAddPet) {
                OutlinedButton(
                    onClick = { viewModel.nextPetSlotForAdd()?.let(onAddPet) },
                    border = BorderStroke(
                        width = dimensionResource(SdpR.dimen._1sdp),
                        color = colorResource(R.color.colors_7B61FF)
                    ),
                    shape = RoundedCornerShape(dimensionResource(SdpR.dimen._16sdp)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_plus),
                        contentDescription = null,
                        tint = colorResource(R.color.colors_7B61FF),
                        modifier = Modifier.size(dimensionResource(SdpR.dimen._16sdp))
                    )
                    Spacer(Modifier.width(dimensionResource(SdpR.dimen._6sdp)))
                    Text(
                        text = stringResource(R.string.settings_add_pet),
                        color = colorResource(R.color.colors_7B61FF),
                        fontFamily = FontFamily(Font(R.font.inter_semibold)),
                        fontSize = dimensionResource(SspR.dimen._10ssp).value.sp
                    )
                }
            }
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
            SettingsSection(title = stringResource(R.string.settings_app_title)) {
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
                color = colorResource(R.color.colors_776D84),
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
private fun PetProfileCard(
    slot: SettingsPetSlotUiState,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._18sdp)))
            .background(colorResource(R.color.colors_FFFFFB))
            .border(
                width = dimensionResource(SdpR.dimen._1sdp),
                color = colorResource(R.color.colors_E9DFEF),
                shape = RoundedCornerShape(dimensionResource(SdpR.dimen._18sdp))
            )
            .clickable(onClick = onClick)
            .padding(dimensionResource(SdpR.dimen._11sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._42sdp))
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._10sdp)))
                .background(
                    colorResource(
                        when (slot.slotIndex % 3) {
                            0 -> R.color.pet_demo_fur
                            1 -> R.color.colors_BFEBDD
                            else -> R.color.colors_FF7A9E
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (slot.previewImagePath != null) {
                AsyncImage(
                    model = slot.previewImagePath,
                    contentDescription = slot.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(dimensionResource(SdpR.dimen._3sdp))
                )
            } else {
                Text(
                    text = (slot.slotIndex + 1).toString(),
                    color = colorResource(R.color.white),
                    fontFamily = FontFamily(Font(R.font.inter_semibold)),
                    fontSize = dimensionResource(SspR.dimen._13ssp).value.sp
                )
            }
        }
        Spacer(Modifier.width(dimensionResource(SdpR.dimen._10sdp)))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = slot.name,
                color = colorResource(R.color.colors_2F2440),
                fontFamily = FontFamily(Font(R.font.inter_semibold)),
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp
            )
            Text(
                text = stringResource(
                    R.string.settings_pet_profile_summary,
                    slot.sizePercent,
                    slot.speedPercent
                ),
                color = colorResource(R.color.colors_776D84),
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = dimensionResource(SspR.dimen._8ssp).value.sp
            )
            Text(
                text = stringResource(
                    if (slot.messagesEnabled && slot.interactionEnabled) {
                        R.string.settings_pet_profile_fully_interactive
                    } else {
                        R.string.settings_pet_profile_limited
                    }
                ),
                color = colorResource(R.color.colors_7B61FF),
                fontFamily = FontFamily(Font(R.font.inter_medium)),
                fontSize = dimensionResource(SspR.dimen._8ssp).value.sp
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_setting_chevron_right_v2),
            contentDescription = stringResource(R.string.settings_customize_pet, slot.name),
            tint = colorResource(R.color.colors_776D84),
            modifier = Modifier.size(dimensionResource(SdpR.dimen._15sdp))
        )
    }
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
        thickness = dimensionResource(SdpR.dimen._1sdp),
        color = colorResource(R.color.colors_E9DFEF)
    )
}

@Preview(showBackground = true)
@Composable
private fun PetProfileCardPreview() {
    PetProfileCard(
        slot = SettingsPetSlotUiState(
            slotIndex = 0,
            name = stringResource(R.string.home_pet_default_name),
            previewImagePath = null,
            sizePercent = 100,
            speedPercent = 100,
            messagesEnabled = true,
            interactionEnabled = true
        ),
        onClick = {}
    )
}
