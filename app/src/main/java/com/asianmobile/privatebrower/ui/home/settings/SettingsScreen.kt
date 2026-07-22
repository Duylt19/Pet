package com.asianmobile.privatebrower.ui.home.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import com.asianmobile.privatebrower.ui.searchengine.SearchEnginePickerSheet
import com.asianmobile.privatebrower.utils.TrackScreenView
import com.asianmobile.privatebrower.utils.ScreenName
import com.intuit.sdp.R as SdpR

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onNavigateToLanguage: () -> Unit = {},
    onNavigateToPrivacyPolicy: () -> Unit = {}
) {
    TrackScreenView(ScreenName.SETTINGS)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val defaultBrowserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshDefaultBrowserStatus()
    }

    // Search Engine Picker Bottom Sheet
    if (state.showSearchEngineSheet) {
        SearchEnginePickerSheet(
            selected = state.currentSearchEngine,
            onSelect = viewModel::onSearchEngineSelected,
            onDismiss = viewModel::onDismissSearchEngineSheet
        )
    }

    if (state.showClearHistorySheet) {
        ClearHistorySheet(
            options = state.clearHistoryOptions,
            isClearing = state.isClearingBrowsingData,
            profileIsolationSupported = state.profileIsolationSupported,
            onOptionsChanged = viewModel::onClearHistoryOptionsChanged,
            onConfirm = { viewModel.onConfirmClearHistory(context) },
            onDismiss = viewModel::onDismissClearHistorySheet
        )
    }

    // Rate App Dialog — local state management
    var rateAppState by remember { mutableStateOf(RateAppUiState(isDialogVisible = false)) }
    if (rateAppState.isDialogVisible) {
        RateAppDialog(
            state = rateAppState,
            onSelectStars = { stars ->
                rateAppState = rateAppState.copy(
                    selectedStars = stars,
                    step = if (stars >= 4) RateAppStep.HighRating else RateAppStep.LowRating
                )
            },
            onDismiss = { rateAppState = rateAppState.copy(isDialogVisible = false) },
            onRateOnPlayStore = {
                // Open Play Store
                val appPackage = context.packageName
                try {
                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("market://details?id=$appPackage")))
                } catch (_: Exception) {
                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://play.google.com/store/apps/details?id=$appPackage")))
                }
                rateAppState = rateAppState.copy(isDialogVisible = false)
            },
            onGoToFeedbackForm = {
                rateAppState = rateAppState.copy(step = RateAppStep.FeedbackForm)
            },
            onToggleFeedbackOption = { index ->
                val updated = rateAppState.feedbackOptions.mapIndexed { i, opt ->
                    if (i == index) opt.copy(isSelected = !opt.isSelected) else opt
                }
                rateAppState = rateAppState.copy(feedbackOptions = updated)
            },
            onUpdateOtherText = { text ->
                rateAppState = rateAppState.copy(otherFeedbackText = text)
            },
            onSendFeedback = {
                val feedbackSnapshot = rateAppState
                if (feedbackSnapshot.canSendFeedback()) {
                    rateAppState = feedbackSnapshot.copy(
                        isSendingFeedback = true,
                        step = RateAppStep.ThankYou
                    )
                    viewModel.sendRateFeedback(context, feedbackSnapshot)
                }
            },
            onShowThankYou = {
                rateAppState = rateAppState.copy(step = RateAppStep.ThankYou)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_161718))
            .statusBarsPadding()
    ) {
        // Top Bar
        AppHeaderBar(
            title = stringResource(R.string.settings_title),
            leadingIcon = AppHeaderLeading.Back,
            onLeadingClick = onBack
        )

        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimensionResource(SdpR.dimen._12sdp))
        ) {
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))

            // General Section
            SettingsSection(title = stringResource(R.string.settings_section_general)) {
                // Set as Default Browser — with Switch
                SettingsRow(
                    iconRes = R.drawable.ic_splash,
                    title = stringResource(R.string.settings_set_default_title),
                    subtitle = stringResource(R.string.settings_set_default_subtitle),
                    renderIconAsImage = true,
                    trailing = SettingsTrailing.SwitchTrailing(
                        checked = state.isDefaultBrowser,
                        onCheckedChange = { isChecked ->
                            if (!state.isDefaultBrowser) {
                                val intent = viewModel.onSetDefaultClicked()
                                defaultBrowserLauncher.launch(intent)
                            }
                        }
                    ),
                    onClick = {
                        if (!state.isDefaultBrowser) {
                            val intent = viewModel.onSetDefaultClicked()
                            defaultBrowserLauncher.launch(intent)
                        }
                    }
                )

                HorizontalDivider(
                    thickness = 1.dp,
                    color = colorResource(R.color.colors_333538)
                )

                // Default Browser (Search Engine)
                SettingsRow(
                    iconRes = R.drawable.ic_setting_default_browser,
                    title = stringResource(R.string.settings_default_browser_title),
                    subtitle = state.currentSearchEngine.displayName,
                    trailing = SettingsTrailing.Custom {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(dimensionResource(SdpR.dimen._18sdp))
                                    .background(
                                        color = colorResource(R.color.colors_FFFFFF),
                                        shape = RoundedCornerShape(
                                            dimensionResource(SdpR.dimen._3sdp)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = androidx.compose.ui.res.painterResource(
                                        state.currentSearchEngine.iconRes
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(dimensionResource(SdpR.dimen._14sdp))
                                )
                            }
                            Spacer(Modifier.width(dimensionResource(SdpR.dimen._6sdp)))
                            androidx.compose.material3.Icon(
                                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_setting_chevron_right_v2),
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color.Unspecified,
                                modifier = Modifier.size(dimensionResource(SdpR.dimen._15sdp))
                            )
                        }
                    },
                    onClick = viewModel::onSearchEngineClicked
                )

                HorizontalDivider(
                    thickness = 1.dp,
                    color = colorResource(R.color.colors_333538)
                )

                // Clear History
                SettingsRow(
                    iconRes = R.drawable.ic_setting_trash_v2,
                    title = stringResource(R.string.settings_clear_history_title),
                    onClick = viewModel::onClearHistoryClicked
                )
            }

            Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))

            // Other Section
            SettingsSection(title = stringResource(R.string.settings_section_other_short)) {
                // Language — read from app's saved language, not system locale
                val langPrefs = context.getSharedPreferences("language_cache", android.content.Context.MODE_PRIVATE)
                val currentLangKey = langPrefs.getString("key_language", "en") ?: "en"
                val currentLangCountry = langPrefs.getString("country_language", "US") ?: "US"
                val langFlagRes = when (currentLangKey) {
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
                val langDisplayName = java.util.Locale(currentLangKey, currentLangCountry)
                    .getDisplayLanguage(java.util.Locale(currentLangKey, currentLangCountry))
                    .replaceFirstChar { it.uppercase() }
                SettingsRow(
                    iconRes = R.drawable.ic_setting_language_v2,
                    title = stringResource(R.string.settings_language_title),
                    subtitle = langDisplayName,
                    trailing = SettingsTrailing.Custom {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = androidx.compose.ui.res.painterResource(langFlagRes),
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier
                                    .size(dimensionResource(SdpR.dimen._18sdp))
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                            )
                            Spacer(Modifier.width(dimensionResource(SdpR.dimen._6sdp)))
                            androidx.compose.material3.Icon(
                                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_setting_chevron_right_v2),
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color.Unspecified,
                                modifier = Modifier.size(dimensionResource(SdpR.dimen._15sdp))
                            )
                        }
                    },
                    onClick = onNavigateToLanguage
                )

                HorizontalDivider(
                    thickness = 1.dp,
                    color = colorResource(R.color.colors_333538)
                )

                // Share with friends
                SettingsRow(
                    iconRes = R.drawable.ic_setting_share_v2,
                    title = stringResource(R.string.settings_share_title),
                    onClick = { viewModel.onShareClicked(context) }
                )

                HorizontalDivider(
                    thickness = 1.dp,
                    color = colorResource(R.color.colors_333538)
                )

                // Privacy Policy
                SettingsRow(
                    iconRes = R.drawable.ic_setting_privacy_v2,
                    title = stringResource(R.string.settings_privacy_policy_title),
                    onClick = onNavigateToPrivacyPolicy
                )

                HorizontalDivider(
                    thickness = 1.dp,
                    color = colorResource(R.color.colors_333538)
                )

                // Rate us
                SettingsRow(
                    iconRes = R.drawable.ic_setting_rate_us_v2,
                    title = stringResource(R.string.settings_rate_us_title),
                    onClick = { rateAppState = RateAppUiState(isDialogVisible = true) }
                )

                HorizontalDivider(
                    thickness = 1.dp,
                    color = colorResource(R.color.colors_333538)
                )

                // Feedback
                SettingsRow(
                    iconRes = R.drawable.ic_setting_feedback_v2,
                    title = stringResource(R.string.settings_feedback_title),
                    onClick = { viewModel.onFeedbackClicked(context) }
                )
            }

            Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
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
