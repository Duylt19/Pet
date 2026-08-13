package com.asianmobile.emojibattery.shimeji.ui.settings.mine

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ui.shared.theme.RobotoFontFamily
import com.asianmobile.emojibattery.shimeji.ui.shared.component.GrantPermissionDialog
import com.asianmobile.emojibattery.shimeji.ui.home.shell.HomeEnableCard
import com.asianmobile.emojibattery.shimeji.ui.home.shell.HomeHeader
import com.asianmobile.emojibattery.shimeji.ui.pet.room.PetRoomSettingsDialog
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

private val MineRoboto = RobotoFontFamily
private val MineRobotoMedium = FontFamily(Font(R.font.roboto_medium))
internal const val IS_MINE_GRANT_PERMISSION_VISIBLE = false

@Composable
fun SettingsScreen(
    onSearch: () -> Unit,
    onPremium: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToMyPet: () -> Unit,
    onNavigateToFavouriteRecent: () -> Unit,
    onNavigateToGrantPermissions: () -> Unit = {},
    accessibilityHowToUseResult: Boolean? = null,
    onAccessibilityHowToUseResultConsumed: () -> Unit = {},
    onNavigateToAccessibilityHowToUse: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    TrackScreenView(ScreenName.SETTINGS)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val language = remember(context) { currentLanguage(context) }
    var rateAppState by remember { mutableStateOf(RateAppUiState()) }
    var showPermissionDisclosure by remember { mutableStateOf(false) }

    LaunchedEffect(accessibilityHowToUseResult) {
        accessibilityHowToUseResult?.let { permissionGranted ->
            if (permissionGranted) {
                viewModel.refreshAccessibility()
            } else {
                viewModel.cancelPendingBatteryEnable()
            }
            onAccessibilityHowToUseResultConsumed()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SettingsEffect.RequestBatteryAccessibility -> {
                    showPermissionDisclosure = true
                }
            }
        }
    }
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshAccessibility()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    RateAppFlow(
        context = context,
        state = rateAppState,
        onStateChange = { rateAppState = it },
        onSendFeedback = viewModel::sendRateFeedback
    )

    MineContent(
        state = state,
        languageName = language.displayName,
        languageFlagRes = language.flagRes,
        onSearch = onSearch,
        onPremium = onPremium,
        onBatteryToggle = viewModel::onBatteryToggle,
        onMyPet = onNavigateToMyPet,
        onFavouriteRecent = onNavigateToFavouriteRecent,
        onLanguage = onNavigateToLanguage,
        onAppsHidden = viewModel::openAppsHidden,
        onGrantPermission = onNavigateToGrantPermissions,
        onSettingPets = viewModel::openPetSettings,
        onRate = { rateAppState = RateAppUiState(isDialogVisible = true) },
        onShare = { viewModel.onShareClicked(context) },
        onContact = { viewModel.onContactClicked(context) },
        onPrivacy = { viewModel.onPrivacyClicked(context) }
    )

    if (showPermissionDisclosure) {
        GrantPermissionDialog(
            onGrantPermission = {
                showPermissionDisclosure = false
                onNavigateToAccessibilityHowToUse()
            },
            onMaybeLater = {
                showPermissionDisclosure = false
                viewModel.cancelPendingBatteryEnable()
            }
        )
    }

    if (state.isAppsHiddenSheetVisible) {
        AppsHiddenSheet(
            state = state,
            onToggleApp = viewModel::toggleAppHidden,
            onRetry = viewModel::retryInstalledApps,
            onDismiss = viewModel::closeAppsHidden
        )
    }

    state.petSettings?.let { settings ->
        PetRoomSettingsDialog(
            settings = settings,
            onSpeedChange = viewModel::updatePetSpeed,
            onSizeChange = viewModel::updatePetSize,
            onSave = viewModel::savePetSettings,
            onDismiss = viewModel::closePetSettings
        )
    }
}

@Composable
internal fun MineContent(
    state: SettingsUiState,
    languageName: String,
    @DrawableRes languageFlagRes: Int,
    onSearch: () -> Unit,
    onPremium: () -> Unit,
    onBatteryToggle: () -> Unit,
    onMyPet: () -> Unit,
    onFavouriteRecent: () -> Unit,
    onLanguage: () -> Unit,
    onAppsHidden: () -> Unit,
    onGrantPermission: () -> Unit,
    onSettingPets: () -> Unit,
    onRate: () -> Unit,
    onShare: () -> Unit,
    onContact: () -> Unit,
    onPrivacy: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_FFFFFF))
    ) {
        Image(
            painter = painterResource(R.drawable.img_home_wallpaper),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(SdpR.dimen._600sdp))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            HomeHeader(onSearch = onSearch, onPremium = onPremium)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                HomeEnableCard(
                    text = stringResource(
                        if (state.isBatteryEnabled) {
                            R.string.discover_battery_enabled
                        } else {
                            R.string.discover_battery_enable_prompt
                        }
                    ),
                    checked = state.isBatteryEnabled,
                    onCheckedChange = onBatteryToggle
                )
                Spacer(Modifier.height(dimensionResource(SdpR.dimen._9sdp)))
                MineQuickActions(
                    onMyPet = onMyPet,
                    onFavouriteRecent = onFavouriteRecent
                )
                Spacer(Modifier.height(dimensionResource(SdpR.dimen._9sdp)))
                Column(
                    modifier = Modifier.padding(
                        horizontal = dimensionResource(SdpR.dimen._12sdp)
                    )
                ) {
                    MineSection(title = stringResource(R.string.mine_section_general)) {
                        MineRow(
                            iconRes = R.drawable.ic_mine_language,
                            title = stringResource(R.string.settings_language_title),
                            subtitle = languageName,
                            trailingFlagRes = languageFlagRes,
                            onClick = onLanguage
                        )
                        MineDivider()
                        MineRow(
                            iconRes = R.drawable.ic_mine_apps_hidden,
                            title = stringResource(R.string.mine_apps_hidden_title),
                            subtitle = stringResource(R.string.mine_apps_hidden_subtitle),
                            onClick = onAppsHidden
                        )
                        MineDivider()
                        if (IS_MINE_GRANT_PERMISSION_VISIBLE) {
                            MineRow(
                                iconRes = R.drawable.ic_mine_permission,
                                title = stringResource(R.string.mine_grant_permission),
                                onClick = onGrantPermission
                            )
                            MineDivider()
                        }
                        MineRow(
                            iconRes = R.drawable.ic_mine_setting_pets,
                            title = stringResource(R.string.mine_setting_pets),
                            onClick = onSettingPets
                        )
                    }

                    Spacer(Modifier.height(dimensionResource(SdpR.dimen._15sdp)))

                    MineSection(title = stringResource(R.string.mine_section_other)) {
                        MineRow(
                            iconRes = R.drawable.ic_mine_rate,
                            title = stringResource(R.string.settings_rate_us_title),
                            onClick = onRate
                        )
                        MineDivider()
                        MineRow(
                            iconRes = R.drawable.ic_mine_share,
                            title = stringResource(R.string.mine_share_app),
                            onClick = onShare
                        )
                        MineDivider()
                        MineRow(
                            iconRes = R.drawable.ic_mine_contact,
                            title = stringResource(R.string.mine_contact_us),
                            onClick = onContact
                        )
                        MineDivider()
                        MineRow(
                            iconRes = R.drawable.ic_mine_privacy,
                            title = stringResource(R.string.settings_privacy_policy_title),
                            onClick = onPrivacy
                        )
                        MineDivider()
                        MineRow(
                            iconRes = R.drawable.ic_mine_version,
                            title = stringResource(
                                R.string.mine_current_version,
                                state.versionName
                            ),
                            onClick = {}
                        )
                    }
                    Spacer(Modifier.height(dimensionResource(SdpR.dimen._18sdp)))
                }
            }
        }
    }
}

@Composable
private fun MineQuickActions(
    onMyPet: () -> Unit,
    onFavouriteRecent: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
    ) {
        MineQuickCard(
            title = stringResource(R.string.mine_my_pet),
            imageRes = R.drawable.img_mine_my_pet,
            backgroundRes = R.color.colors_FBEFC7,
            borderRes = R.color.colors_FFDD69,
            imageSize = dimensionResource(SdpR.dimen._49sdp),
            onClick = onMyPet,
            modifier = Modifier.weight(1f)
        )
        MineQuickCard(
            title = stringResource(R.string.mine_favourite_recent),
            imageRes = R.drawable.img_mine_favorite_recent,
            backgroundRes = R.color.colors_FFDDEA,
            borderRes = R.color.colors_FD74A7,
            imageSize = dimensionResource(SdpR.dimen._55sdp),
            imageOffsetX = dimensionResource(SdpR.dimen._2sdp),
            imageOffsetY = -dimensionResource(SdpR.dimen._2sdp),
            onClick = onFavouriteRecent,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MineQuickCard(
    title: String,
    @DrawableRes imageRes: Int,
    backgroundRes: Int,
    borderRes: Int,
    imageSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageOffsetX: Dp = 0.dp,
    imageOffsetY: Dp = 0.dp
) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
    val shadowColor = colorResource(R.color.gray_666666).copy(alpha = 0.35f)
    Box(
        modifier = modifier
            .height(dimensionResource(SdpR.dimen._54sdp))
            .shadow(
                elevation = dimensionResource(SdpR.dimen._18sdp),
                shape = shape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(shape)
            .background(colorResource(backgroundRes))
            .border(
                dimensionResource(SdpR.dimen._1sdp),
                colorResource(borderRes),
                shape
            )
            .clickable(onClick = onClick)
    ) {
        Text(
            text = title,
            color = colorResource(R.color.colors_212327),
            fontFamily = MineRobotoMedium,
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
            maxLines = 2,
            overflow = TextOverflow.Clip,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(dimensionResource(SdpR.dimen._64sdp))
                .padding(start = dimensionResource(SdpR.dimen._9sdp))
        )
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = imageOffsetX, y = imageOffsetY)
                .size(imageSize)
        )
    }
}

@Composable
private fun MineSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Text(
        text = title,
        color = colorResource(R.color.colors_6F7073),
        fontFamily = MineRobotoMedium,
        fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
        lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(dimensionResource(SdpR.dimen._6sdp)))
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp))
    val shadowColor = colorResource(R.color.gray_666666).copy(alpha = 0.35f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = dimensionResource(SdpR.dimen._9sdp),
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
private fun MineRow(
    @DrawableRes iconRes: Int,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    @DrawableRes trailingFlagRes: Int? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimensionResource(SdpR.dimen._18sdp))
            .clickable(onClick = onClick),
        verticalAlignment = if (subtitle == null) Alignment.CenterVertically else Alignment.Top
    ) {
        MineRowIcon(iconRes = iconRes)
        Spacer(Modifier.width(dimensionResource(SdpR.dimen._9sdp)))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._2sdp))
        ) {
            Text(
                text = title,
                color = colorResource(R.color.colors_212327),
                fontFamily = MineRobotoMedium,
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
                maxLines = 1
            )
            subtitle?.let {
                Text(
                    text = it,
                    color = colorResource(R.color.colors_6F7073),
                    fontFamily = MineRoboto,
                    fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
                    lineHeight = dimensionResource(SspR.dimen._12ssp).value.sp,
                    maxLines = 2
                )
            }
        }
        trailingFlagRes?.let { flagRes ->
            Image(
                painter = painterResource(flagRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(dimensionResource(SdpR.dimen._18sdp))
                    .clip(CircleShape)
                    .border(
                        dimensionResource(SdpR.dimen._1sdp),
                        colorResource(R.color.colors_E6E6E6),
                        CircleShape
                    )
            )
            Spacer(Modifier.width(dimensionResource(SdpR.dimen._9sdp)))
        }
        Image(
            painter = painterResource(R.drawable.ic_setting_chevron_right_v2),
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(SdpR.dimen._15sdp))
        )
    }
}

@Composable
private fun MineRowIcon(@DrawableRes iconRes: Int) {
    val pink = colorResource(R.color.colors_FB3675)
    Box(modifier = Modifier.size(dimensionResource(SdpR.dimen._18sdp))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pivot = Offset(
                x = size.width * (14.8203f / 24f),
                y = size.height * (17.1453f / 24f)
            )
            rotate(degrees = -25.2222f, pivot = pivot) {
                drawOval(
                    color = pink.copy(alpha = 0.15f),
                    topLeft = Offset(
                        x = size.width * (6.3203f / 24f),
                        y = size.height * (12.1453f / 24f)
                    ),
                    size = Size(
                        width = size.width * (17f / 24f),
                        height = size.height * (10f / 24f)
                    )
                )
            }
        }
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = pink,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun MineDivider() {
    HorizontalDivider(
        thickness = dimensionResource(SdpR.dimen._1sdp),
        color = colorResource(R.color.colors_F2F2F2)
    )
}

private data class MineLanguage(
    val displayName: String,
    @param:DrawableRes val flagRes: Int
)

private fun currentLanguage(context: Context): MineLanguage {
    val preferences = context.getSharedPreferences("language_cache", Context.MODE_PRIVATE)
    val languageKey = preferences.getString("key_language", "en") ?: "en"
    val country = preferences.getString("country_language", "US") ?: "US"
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
    return MineLanguage(
        displayName = locale.getDisplayLanguage(locale).replaceFirstChar { it.uppercase() },
        flagRes = flag
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

@Preview(showBackground = true, widthDp = 360, heightDp = 950)
@Composable
private fun MineContentPreview() {
    MineContent(
        state = SettingsUiState(versionName = "100"),
        languageName = "English",
        languageFlagRes = R.drawable.ic_flag_en,
        onSearch = {},
        onPremium = {},
        onBatteryToggle = {},
        onMyPet = {},
        onFavouriteRecent = {},
        onLanguage = {},
        onAppsHidden = {},
        onGrantPermission = {},
        onSettingPets = {},
        onRate = {},
        onShare = {},
        onContact = {},
        onPrivacy = {}
    )
}
