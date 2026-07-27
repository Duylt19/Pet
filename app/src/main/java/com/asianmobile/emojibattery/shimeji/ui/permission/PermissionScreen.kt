package com.asianmobile.emojibattery.shimeji.ui.permission

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
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
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_PERMISSION
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.NativeAdInternal
import com.asianmobile.emojibattery.shimeji.ads.ui.interstitial.InterstitialUtil
import com.asianmobile.emojibattery.shimeji.pet.overlay.PetOverlay
import com.asianmobile.emojibattery.shimeji.ui.component.TransparentStatusBarEffect
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

@Composable
fun PermissionScreen(
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    viewModel: PermissionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val overlaySettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshPermissions()
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshPermissions()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    PermissionScreenContent(
        uiState = uiState,
        onRequestOverlay = {
            InterstitialUtil.getInstance().openAd?.needShowOpenAds = false
            runCatching {
                overlaySettingsLauncher.launch(PetOverlay.permissionIntent(context))
            }.onFailure {
                overlaySettingsLauncher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            }
        },
        onRequestNotifications = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onContinue = onContinue,
        onSkip = onSkip
    )
}

@Composable
private fun PermissionScreenContent(
    uiState: PermissionUiState,
    onRequestOverlay: () -> Unit,
    onRequestNotifications: () -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    TransparentStatusBarEffect(useDarkIcons = false)
    TrackScreenView(ScreenName.PERMISSION)
    BackHandler { }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_161718))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimensionResource(SdpR.dimen._18sdp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._18sdp)))
            Icon(
                painter = painterResource(R.drawable.ic_notification_pet),
                contentDescription = null,
                tint = colorResource(R.color.pet_demo_fur),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._72sdp))
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
            Text(
                text = stringResource(R.string.permission_title),
                color = colorResource(R.color.white),
                fontFamily = FontFamily(Font(R.font.inter_semibold)),
                fontSize = dimensionResource(SspR.dimen._18ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._24ssp).value.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._6sdp)))
            Text(
                text = stringResource(R.string.permission_subtitle),
                color = colorResource(R.color.colors_9B9C9E),
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._18sdp)))
            PermissionItem(
                title = stringResource(R.string.permission_overlay_title),
                subtitle = stringResource(R.string.permission_overlay_subtitle),
                granted = uiState.overlayGranted,
                onRequest = onRequestOverlay
            )
            if (uiState.notificationPermissionRequired) {
                Spacer(Modifier.height(dimensionResource(SdpR.dimen._9sdp)))
                PermissionItem(
                    title = stringResource(R.string.permission_notification_title),
                    subtitle = stringResource(R.string.permission_notification_subtitle),
                    granted = uiState.notificationGranted,
                    onRequest = onRequestNotifications
                )
            }
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._18sdp)))
            PermissionContinueActions(
                enabled = uiState.actionsEnabled,
                onContinue = onContinue,
                onSkip = onSkip
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
        }

        NativeAdInternal(
            screenCode = SCREEN_PERMISSION,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PermissionItem(
    title: String,
    subtitle: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = colorResource(R.color.colors_212327),
                shape = RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp))
            )
            .padding(dimensionResource(SdpR.dimen._12sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colorResource(R.color.white),
                fontFamily = FontFamily(Font(R.font.inter_medium)),
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._3sdp)))
            Text(
                text = subtitle,
                color = colorResource(R.color.colors_9B9C9E),
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._11ssp).value.sp
            )
        }
        Button(
            onClick = onRequest,
            enabled = !granted,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.colors_3369FD),
                contentColor = colorResource(R.color.white),
                disabledContainerColor = colorResource(R.color.colors_00C950),
                disabledContentColor = colorResource(R.color.white)
            )
        ) {
            Text(
                text = stringResource(
                    if (granted) R.string.permission_allowed else R.string.permission_allow
                )
            )
        }
    }
}

@Composable
private fun PermissionContinueActions(
    enabled: Boolean,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    Button(
        onClick = onContinue,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(R.color.colors_3369FD),
            contentColor = colorResource(R.color.white),
            disabledContentColor = colorResource(R.color.white)
        )
    ) {
        Text(text = stringResource(R.string.permission_continue))
    }
    Spacer(Modifier.height(dimensionResource(SdpR.dimen._6sdp)))
    Button(
        onClick = onSkip,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.textButtonColors(
            contentColor = colorResource(R.color.colors_9B9C9E)
        )
    ) {
        Text(text = stringResource(R.string.grant_permission_later))
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF161718,
    widthDp = 360,
    heightDp = 800
)
@Composable
private fun PermissionScreenPreview() {
    PermissionScreenContent(
        uiState = PermissionUiState(notificationPermissionRequired = true),
        onRequestOverlay = {},
        onRequestNotifications = {},
        onContinue = {},
        onSkip = {}
    )
}
