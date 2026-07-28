package com.asianmobile.emojibattery.shimeji.ui.home

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ads.ui.interstitial.InterstitialUtil
import com.asianmobile.emojibattery.shimeji.pet.overlay.PetOverlay
import com.asianmobile.emojibattery.shimeji.ui.component.CutePetCard
import com.asianmobile.emojibattery.shimeji.ui.component.CutePetIconAction
import com.asianmobile.emojibattery.shimeji.ui.component.CutePetPrimaryButton
import com.asianmobile.emojibattery.shimeji.ui.component.CutePetSectionHeader
import com.asianmobile.emojibattery.shimeji.ui.component.CutePetStatusPill
import com.asianmobile.emojibattery.shimeji.ui.component.CutePetTitleFont
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

@Composable
fun HomeScreen(
    onNavigateToCatalog: (Int) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPremium: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    TrackScreenView(ScreenName.HOME)

    val overlaySettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshPermissions()
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.onNotificationPermissionResult()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                HomeEffect.OpenOverlaySettings -> {
                    InterstitialUtil.getInstance().openAd?.needShowOpenAds = false
                    runCatching {
                        overlaySettingsLauncher.launch(PetOverlay.permissionIntent(context))
                    }.onFailure {
                        overlaySettingsLauncher.launch(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                        )
                    }
                }
                HomeEffect.RequestNotificationPermission -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    } else {
                        viewModel.onNotificationPermissionResult()
                    }
                }
            }
        }
    }

    HomeScreenContent(
        uiState = uiState,
        onPetButtonClicked = viewModel::onPetButtonClicked,
        onDismissMessage = viewModel::clearMessage,
        onNavigateToCatalog = { onNavigateToCatalog(0) },
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToPremium = onNavigateToPremium
    )
}

@Composable
private fun HomeScreenContent(
    uiState: HomeUiState,
    onPetButtonClicked: () -> Unit,
    onDismissMessage: () -> Unit,
    onNavigateToCatalog: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPremium: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_FFF9F4))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimensionResource(SdpR.dimen._16sdp))
    ) {
        HomeHeader(
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToPremium = onNavigateToPremium
        )
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
        Text(
            text = stringResource(R.string.home_greeting),
            color = colorResource(R.color.colors_2F2440),
            fontFamily = CutePetTitleFont,
            fontWeight = FontWeight.Bold,
            fontSize = dimensionResource(SspR.dimen._22ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._27ssp).value.sp
        )
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._5sdp)))
        Text(
            text = stringResource(R.string.home_pet_subtitle),
            color = colorResource(R.color.colors_776D84),
            fontFamily = FontFamily(Font(R.font.inter_regular)),
            fontSize = dimensionResource(SspR.dimen._10ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp
        )
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._16sdp)))

        PetRoomCard(uiState = uiState, onPetButtonClicked = onPetButtonClicked)

        if (uiState.message == HomeMessage.PET_START_FAILED) {
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._8sdp)))
            Text(
                text = stringResource(R.string.home_pet_start_failed),
                color = colorResource(R.color.colors_E45D6A),
                fontFamily = FontFamily(Font(R.font.inter_medium)),
                fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._10sdp)))
                    .background(colorResource(R.color.colors_FFE8EF))
                    .padding(dimensionResource(SdpR.dimen._10sdp))
            )
            LaunchedEffect(uiState.message) {
                kotlinx.coroutines.delay(MESSAGE_DURATION_MILLIS)
                onDismissMessage()
            }
        }

        Spacer(Modifier.height(dimensionResource(SdpR.dimen._18sdp)))
        CutePetSectionHeader(
            title = stringResource(R.string.home_family_title),
            action = stringResource(R.string.home_family_manage),
            onAction = onNavigateToSettings
        )
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._10sdp)))
        PetFamilyRow(uiState)

        Spacer(Modifier.height(dimensionResource(SdpR.dimen._18sdp)))
        ExplorePetCard(onClick = onNavigateToCatalog)
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._20sdp)))
    }
}

@Composable
private fun HomeHeader(
    onNavigateToSettings: () -> Unit,
    onNavigateToPremium: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._48sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._34sdp))
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._11sdp)))
                .background(colorResource(R.color.colors_EDE4FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_notification_pet),
                contentDescription = null,
                tint = colorResource(R.color.colors_7B61FF),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._23sdp))
            )
        }
        Spacer(Modifier.size(dimensionResource(SdpR.dimen._8sdp)))
        Text(
            text = stringResource(R.string.home_brand_name),
            color = colorResource(R.color.colors_2F2440),
            fontFamily = CutePetTitleFont,
            fontSize = dimensionResource(SspR.dimen._16ssp).value.sp,
            modifier = Modifier.weight(1f)
        )
        CutePetIconAction(
            iconRes = R.drawable.ic_crown_premium,
            contentDescription = stringResource(R.string.home_open_premium),
            onClick = onNavigateToPremium
        )
        Spacer(Modifier.size(dimensionResource(SdpR.dimen._8sdp)))
        CutePetIconAction(
            iconRes = R.drawable.ic_settings_outline,
            contentDescription = stringResource(R.string.home_open_settings),
            onClick = onNavigateToSettings
        )
    }
}

@Composable
private fun PetRoomCard(
    uiState: HomeUiState,
    onPetButtonClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._24sdp)))
            .background(colorResource(R.color.colors_EDE4FF))
            .padding(dimensionResource(SdpR.dimen._16sdp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CutePetStatusPill(
            text = stringResource(
                if (uiState.isPetRunning) {
                    R.string.home_status_live
                } else {
                    R.string.home_status_resting
                }
            ),
            active = uiState.isPetRunning
        )
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._8sdp)))
        PetPreviewStack(uiState = uiState)
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._8sdp)))
        Text(
            text = if (uiState.selectedPetNames.isEmpty()) {
                stringResource(R.string.home_pet_default_name)
            } else {
                uiState.selectedPetNames.joinToString(separator = "  •  ")
            },
            color = colorResource(R.color.colors_2F2440),
            fontFamily = FontFamily(Font(R.font.inter_semibold)),
            fontSize = dimensionResource(SspR.dimen._13ssp).value.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._3sdp)))
        Text(
            text = pluralStringResource(
                R.plurals.home_pet_configured_count,
                uiState.petCount,
                uiState.petCount
            ),
            color = colorResource(R.color.colors_776D84),
            fontFamily = FontFamily(Font(R.font.inter_regular)),
            fontSize = dimensionResource(SspR.dimen._9ssp).value.sp
        )
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
        CutePetPrimaryButton(
            text = stringResource(
                when {
                    uiState.isStartingPet -> R.string.home_pet_action_starting
                    uiState.isPetRunning -> R.string.home_pet_action_stop
                    !uiState.overlayGranted -> R.string.home_pet_action_allow_overlay
                    else -> R.string.home_pet_action_start
                }
            ),
            onClick = onPetButtonClicked,
            enabled = uiState.actionsEnabled,
            isLoading = uiState.isStartingPet,
            isDanger = uiState.isPetRunning,
            modifier = Modifier.fillMaxWidth()
        )
        if (!uiState.overlayGranted ||
            (uiState.notificationPermissionRequired && !uiState.notificationGranted)
        ) {
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._8sdp)))
            Text(
                text = stringResource(
                    if (!uiState.overlayGranted) {
                        R.string.home_pet_overlay_required
                    } else {
                        R.string.home_pet_notification_will_request
                    }
                ),
                color = colorResource(R.color.colors_776D84),
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PetPreviewStack(uiState: HomeUiState) {
    Row(
        modifier = Modifier.height(dimensionResource(SdpR.dimen._90sdp)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(uiState.petCount.coerceAtLeast(1)) { index ->
            val preview = uiState.selectedPetPreviewPaths.getOrNull(index)
            Box(
                modifier = Modifier
                    .offset(
                        x = if (index == 0) {
                            dimensionResource(SdpR.dimen._1sdp) * 0f
                        } else {
                            -dimensionResource(SdpR.dimen._10sdp)
                        }
                    )
                    .size(
                        if (index == 0) {
                            dimensionResource(SdpR.dimen._84sdp)
                        } else {
                            dimensionResource(SdpR.dimen._72sdp)
                        }
                    )
                    .clip(CircleShape)
                    .background(
                        colorResource(
                            when (index % 3) {
                                0 -> R.color.colors_FFF0D6
                                1 -> R.color.colors_E7F7F1
                                else -> R.color.colors_FFE8EF
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (preview != null) {
                    AsyncImage(
                        model = preview,
                        contentDescription = uiState.selectedPetNames.getOrNull(index),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(dimensionResource(SdpR.dimen._6sdp))
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_notification_pet),
                        contentDescription = null,
                        tint = colorResource(R.color.pet_demo_fur),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(dimensionResource(SdpR.dimen._12sdp))
                    )
                }
            }
        }
    }
}

@Composable
private fun PetFamilyRow(uiState: HomeUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._8sdp))
    ) {
        repeat(uiState.petCount.coerceAtLeast(1)) { index ->
            CutePetCard(
                modifier = Modifier.size(
                    width = dimensionResource(SdpR.dimen._104sdp),
                    height = dimensionResource(SdpR.dimen._112sdp)
                )
            ) {
                Box(
                    modifier = Modifier
                        .size(dimensionResource(SdpR.dimen._52sdp))
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._15sdp)))
                        .background(colorResource(R.color.colors_F7F0FF)),
                    contentAlignment = Alignment.Center
                ) {
                    val preview = uiState.selectedPetPreviewPaths.getOrNull(index)
                    if (preview != null) {
                        AsyncImage(
                            model = preview,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(dimensionResource(SdpR.dimen._4sdp))
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_notification_pet),
                            contentDescription = null,
                            tint = colorResource(R.color.pet_demo_fur),
                            modifier = Modifier.padding(dimensionResource(SdpR.dimen._7sdp))
                        )
                    }
                }
                Spacer(Modifier.height(dimensionResource(SdpR.dimen._7sdp)))
                Text(
                    text = uiState.selectedPetNames.getOrNull(index)
                        ?: stringResource(R.string.home_pet_default_name),
                    color = colorResource(R.color.colors_2F2440),
                    fontFamily = FontFamily(Font(R.font.inter_semibold)),
                    fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ExplorePetCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._20sdp)))
            .background(colorResource(R.color.colors_FFF0D6))
            .clickable(onClick = onClick)
            .padding(dimensionResource(SdpR.dimen._14sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._44sdp))
                .clip(CircleShape)
                .background(colorResource(R.color.colors_FFB84D)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_plus),
                contentDescription = null,
                tint = colorResource(R.color.colors_FFFFFF),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._20sdp))
            )
        }
        Spacer(Modifier.size(dimensionResource(SdpR.dimen._10sdp)))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_explore_title),
                color = colorResource(R.color.colors_2F2440),
                fontFamily = FontFamily(Font(R.font.inter_semibold)),
                fontSize = dimensionResource(SspR.dimen._12ssp).value.sp
            )
            Text(
                text = stringResource(R.string.home_explore_subtitle),
                color = colorResource(R.color.colors_776D84),
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._11ssp).value.sp
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = stringResource(R.string.pet_catalog_open),
            tint = colorResource(R.color.colors_2F2440),
            modifier = Modifier.size(dimensionResource(SdpR.dimen._18sdp))
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    HomeScreenContent(
        uiState = HomeUiState(
            overlayGranted = true,
            selectedPetNames = listOf("Orange Cat", "Mochi"),
            selectedPetPreviewPaths = listOf(null, null),
            petCount = 2
        ),
        onPetButtonClicked = {},
        onDismissMessage = {},
        onNavigateToCatalog = {},
        onNavigateToSettings = {},
        onNavigateToPremium = {}
    )
}

private const val MESSAGE_DURATION_MILLIS = 3_000L
