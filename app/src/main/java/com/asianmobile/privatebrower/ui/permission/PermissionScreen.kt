package com.asianmobile.privatebrower.ui.permission

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.ads.config.SCREEN_PERMISSION
import com.asianmobile.privatebrower.ads.ui.compose.NativeAdInternal
import com.asianmobile.privatebrower.ads.ui.interstitial.InterstitialUtil
import com.asianmobile.privatebrower.ui.component.TransparentStatusBarEffect
import com.asianmobile.privatebrower.utils.ScreenName
import com.asianmobile.privatebrower.utils.TrackScreenView
import com.asianmobile.privatebrower.utils.dialog.PermissionRequireDialog
import com.asianmobile.privatebrower.utils.permission.AllFilesAccess
import com.asianmobile.privatebrower.utils.permission.BroadStorageAccess
import com.asianmobile.privatebrower.utils.permission.canShowPermissionRationale
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

@Composable
fun PermissionScreen(
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    viewModel: PermissionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    PermissionScreenContent(
        uiState = uiState,
        onRefresh = viewModel::refreshPermissions,
        onStorageRequestStarted = viewModel::markStoragePermissionRequested,
        onContinue = onContinue,
        onSkip = onSkip
    )
}

@Composable
private fun PermissionScreenContent(
    uiState: PermissionUiState,
    onRefresh: () -> Unit,
    onStorageRequestStarted: () -> Int,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    TransparentStatusBarEffect(useDarkIcons = false)

    TrackScreenView(ScreenName.PERMISSION)
    BackHandler { }

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val storagePermissions = remember {
        PermissionPolicy.onboardingStoragePermissions(Build.VERSION.SDK_INT)
    }
    var storageAttemptCount by rememberSaveable {
        mutableIntStateOf(uiState.storageRequestCount)
    }
    var showSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.storageRequestCount) {
        storageAttemptCount = maxOf(storageAttemptCount, uiState.storageRequestCount)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) onRefresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val runtimeStorageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onRefresh()
        if (!BroadStorageAccess.isGranted(context) &&
            PermissionPolicy.shouldOpenAppSettings(
                requestCount = storageAttemptCount,
                canShowRationale = context.canShowPermissionRationale(storagePermissions)
            )
        ) {
            showSettingsDialog = true
        }
    }
    val allFilesAccessLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        onRefresh()
    }

    fun openAllFilesAccessSettings() {
        InterstitialUtil.getInstance().openAd?.needShowOpenAds = false
        runCatching {
            allFilesAccessLauncher.launch(AllFilesAccess.settingsIntent(context))
        }.onFailure {
            openAppSettings(context)
        }
    }

    fun requestStoragePermission() {
        if (uiState.usesAllFilesAccess) {
            openAllFilesAccessSettings()
            return
        }
        if (uiState.storageGranted) {
            openAppSettings(context)
            return
        }

        when (
            PermissionPolicy.nextRequestDestination(
                isGranted = false,
                requestCount = storageAttemptCount,
                canShowRationale = context.canShowPermissionRationale(storagePermissions)
            )
        ) {
            PermissionRequestDestination.NONE -> Unit
            PermissionRequestDestination.SYSTEM_DIALOG -> {
                storageAttemptCount = onStorageRequestStarted()
                runtimeStorageLauncher.launch(storagePermissions)
            }
            PermissionRequestDestination.APP_SETTINGS -> showSettingsDialog = true
        }
    }

    if (showSettingsDialog) {
        PermissionRequireDialog(
            onDismissRequest = { showSettingsDialog = false },
            onConfirm = {
                showSettingsDialog = false
                openAppSettings(context)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_161718))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._6sdp)))
            Image(
                painter = painterResource(R.drawable.img_permission_dark),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.size(
                    width = dimensionResource(SdpR.dimen._143sdp),
                    height = dimensionResource(SdpR.dimen._98sdp)
                )
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
            Text(
                text = stringResource(R.string.permission_title),
                color = colorResource(R.color.white),
                fontFamily = FontFamily(Font(R.font.inter_semibold)),
                fontSize = dimensionResource(SspR.dimen._14ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._20ssp).value.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._3sdp)))
            Text(
                text = stringResource(R.string.permission_subtitle),
                color = colorResource(R.color.colors_9B9C9E),
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = dimensionResource(SdpR.dimen._9sdp))
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
            StoragePermissionRow(
                checked = uiState.storageGranted,
                onClick = ::requestStoragePermission
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._6sdp)))
            Text(
                text = stringResource(
                    if (uiState.storageGranted) {
                        R.string.permission_status_allowed
                    } else {
                        R.string.permission_status_not_allowed
                    }
                ),
                color = colorResource(
                    if (uiState.storageGranted) {
                        R.color.green_00C062
                    } else {
                        R.color.colors_DC2222
                    }
                ),
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._12ssp).value.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = dimensionResource(SdpR.dimen._15sdp))
            )

            Spacer(Modifier.weight(1f))
            PermissionContinueActions(
                onContinue = onContinue,
                onSkip = onSkip
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._6sdp)))
        }

        NativeAdInternal(
            screenCode = SCREEN_PERMISSION,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StoragePermissionRow(
    checked: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = dimensionResource(SdpR.dimen._9sdp))
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._43sdp))
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp)))
            .background(colorResource(R.color.colors_212327))
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = { onClick() }
            )
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.permission_storage_title),
            color = colorResource(R.color.white),
            fontFamily = FontFamily(Font(R.font.inter_regular)),
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp
        )
        PermissionSwitch(checked = checked)
    }
}

@Composable
private fun PermissionSwitch(checked: Boolean) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
    val trackModifier = if (checked) {
        Modifier.background(
            brush = Brush.horizontalGradient(
                listOf(
                    colorResource(R.color.colors_1E86F6),
                    colorResource(R.color.colors_0D45ED)
                )
            ),
            shape = shape
        )
    } else {
        Modifier.background(
            color = colorResource(R.color.colors_4D4D4D),
            shape = shape
        )
    }

    Box(
        modifier = Modifier
            .size(
                width = dimensionResource(SdpR.dimen._34sdp),
                height = dimensionResource(SdpR.dimen._18sdp)
            )
            .then(trackModifier)
            .padding(dimensionResource(SdpR.dimen._2sdp)),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._15sdp))
                .background(colorResource(R.color.white), RoundedCornerShape(50))
        )
    }
}

@Composable
private fun PermissionContinueActions(
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .height(dimensionResource(SdpR.dimen._18sdp))
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._3sdp)))
                .clickable(onClick = onContinue),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.permission_continue),
                color = colorResource(R.color.colors_3369FD),
                fontFamily = FontFamily(Font(R.font.inter_medium)),
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp
            )
            Spacer(Modifier.width(dimensionResource(SdpR.dimen._3sdp)))
            Box(
                modifier = Modifier.size(dimensionResource(SdpR.dimen._18sdp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_continue),
                    contentDescription = null,
                    tint = colorResource(R.color.colors_3369FD),
                    modifier = Modifier.size(
                        width = dimensionResource(SdpR.dimen._12sdp),
                        height = dimensionResource(SdpR.dimen._11sdp)
                    )
                )
            }
        }
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._2sdp)))
        Text(
            text = stringResource(R.string.grant_permission_later),
            color = colorResource(R.color.colors_9B9C9E),
            fontFamily = FontFamily(Font(R.font.inter_regular)),
            fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._12ssp).value.sp,
            modifier = Modifier.clickable(onClick = onSkip)
        )
    }
}

private fun openAppSettings(context: Context) {
    InterstitialUtil.getInstance().openAd?.needShowOpenAds = false
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
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
        uiState = PermissionUiState(storageGranted = false),
        onRefresh = {},
        onStorageRequestStarted = { 0 },
        onContinue = {},
        onSkip = {}
    )
}
