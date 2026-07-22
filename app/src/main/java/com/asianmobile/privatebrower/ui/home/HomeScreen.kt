package com.asianmobile.privatebrower.ui.home

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.ads.ui.interstitial.InterstitialUtil
import com.asianmobile.privatebrower.pet.overlay.PetOverlay
import com.asianmobile.privatebrower.utils.ScreenName
import com.asianmobile.privatebrower.utils.TrackScreenView
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

@Composable
fun HomeScreen(
    onNavigateToCatalog: () -> Unit,
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
        onNavigateToCatalog = onNavigateToCatalog,
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
            .background(colorResource(R.color.colors_161718))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = dimensionResource(SdpR.dimen._24sdp))
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._24sdp)))
        Text(
            text = stringResource(R.string.home_pet_title),
            color = colorResource(R.color.white),
            fontFamily = FontFamily(Font(R.font.inter_semibold)),
            fontSize = dimensionResource(SspR.dimen._24ssp).value.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._6sdp)))
        Text(
            text = stringResource(R.string.home_pet_subtitle),
            color = colorResource(R.color.colors_9B9C9E),
            fontFamily = FontFamily(Font(R.font.inter_regular)),
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._24sdp)))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = colorResource(R.color.colors_212327),
                    shape = RoundedCornerShape(dimensionResource(SdpR.dimen._18sdp))
                )
                .padding(dimensionResource(SdpR.dimen._18sdp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_notification_pet),
                contentDescription = null,
                tint = colorResource(R.color.pet_demo_fur),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._96sdp))
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
            Text(
                text = uiState.selectedPetName,
                color = colorResource(R.color.white),
                fontFamily = FontFamily(Font(R.font.inter_semibold)),
                fontSize = dimensionResource(SspR.dimen._13ssp).value.sp
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._3sdp)))
            Text(
                text = pluralStringResource(
                    R.plurals.home_pet_configured_count,
                    uiState.petCount,
                    uiState.petCount
                ),
                color = colorResource(R.color.colors_9B9C9E),
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = dimensionResource(SspR.dimen._9ssp).value.sp
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._6sdp)))
            Text(
                text = stringResource(
                    if (uiState.isPetRunning) {
                        R.string.home_pet_status_running
                    } else {
                        R.string.home_pet_status_stopped
                    }
                ),
                color = colorResource(
                    if (uiState.isPetRunning) {
                        R.color.colors_00C950
                    } else {
                        R.color.colors_9B9C9E
                    }
                ),
                fontFamily = FontFamily(Font(R.font.inter_medium)),
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._6sdp)))
            PermissionStatus(uiState = uiState)
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._18sdp)))
            Button(
                onClick = onPetButtonClicked,
                enabled = uiState.actionsEnabled,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(
                        if (uiState.isPetRunning) {
                            R.color.colors_DC2222
                        } else {
                            R.color.colors_3369FD
                        }
                    ),
                    contentColor = colorResource(R.color.white),
                    disabledContentColor = colorResource(R.color.white)
                )
            ) {
                Text(
                    text = stringResource(
                        when {
                            uiState.isStartingPet -> R.string.home_pet_action_starting
                            uiState.isPetRunning -> R.string.home_pet_action_stop
                            !uiState.overlayGranted -> R.string.home_pet_action_allow_overlay
                            else -> R.string.home_pet_action_start
                        }
                    )
                )
            }
        }

        if (uiState.message == HomeMessage.PET_START_FAILED) {
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._9sdp)))
            Text(
                text = stringResource(R.string.home_pet_start_failed),
                color = colorResource(R.color.colors_DC2222),
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(SdpR.dimen._6sdp))
            )
            LaunchedEffect(uiState.message) {
                kotlinx.coroutines.delay(MESSAGE_DURATION_MILLIS)
                onDismissMessage()
            }
        }

        Spacer(Modifier.height(dimensionResource(SdpR.dimen._18sdp)))
        OutlinedButton(
            onClick = onNavigateToCatalog,
            enabled = uiState.actionsEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.pet_catalog_open))
        }
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._6sdp)))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onNavigateToSettings,
                enabled = uiState.actionsEnabled,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.settings_title))
            }
            Spacer(Modifier.size(dimensionResource(SdpR.dimen._9sdp)))
            OutlinedButton(
                onClick = onNavigateToPremium,
                enabled = uiState.actionsEnabled,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.premium_title))
            }
        }
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._18sdp)))
    }
}

@Composable
private fun PermissionStatus(uiState: HomeUiState) {
    Text(
        text = stringResource(
            if (uiState.overlayGranted) {
                R.string.home_pet_overlay_ready
            } else {
                R.string.home_pet_overlay_required
            }
        ),
        color = colorResource(
            if (uiState.overlayGranted) R.color.colors_C0D1FE else R.color.colors_FD9EA3
        ),
        fontFamily = FontFamily(Font(R.font.inter_regular)),
        fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
        textAlign = TextAlign.Center
    )
    if (uiState.notificationPermissionRequired && !uiState.notificationGranted) {
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._3sdp)))
        Text(
            text = stringResource(R.string.home_pet_notification_will_request),
            color = colorResource(R.color.colors_9B9C9E),
            fontFamily = FontFamily(Font(R.font.inter_regular)),
            fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF161718)
@Composable
private fun HomeScreenPreview() {
    HomeScreenContent(
        uiState = HomeUiState(overlayGranted = true),
        onPetButtonClicked = {},
        onDismissMessage = {},
        onNavigateToCatalog = {},
        onNavigateToSettings = {},
        onNavigateToPremium = {}
    )
}

private const val MESSAGE_DURATION_MILLIS = 3_000L
