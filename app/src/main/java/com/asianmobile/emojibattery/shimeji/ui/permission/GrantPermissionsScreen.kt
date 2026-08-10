package com.asianmobile.emojibattery.shimeji.ui.permission

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryAccessibility
import com.asianmobile.emojibattery.shimeji.pet.overlay.PetOverlay
import com.asianmobile.emojibattery.shimeji.ui.component.AppSwitch
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

@Composable
fun GrantPermissionsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: GrantPermissionsViewModel = hiltViewModel()
) {
    TrackScreenView(ScreenName.GRANT_PERMISSIONS)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { viewModel.refresh() }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refresh() }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                GrantPermissionsEffect.OpenAccessibilitySettings ->
                    settingsLauncher.launch(BatteryAccessibility.settingsIntent())

                GrantPermissionsEffect.OpenOverlaySettings ->
                    settingsLauncher.launch(PetOverlay.permissionIntent(context))

                GrantPermissionsEffect.OpenBatteryOptimizationSettings ->
                    settingsLauncher.launch(batteryOptimizationIntent(context.packageName))

                GrantPermissionsEffect.OpenVendorAutoStartSettings ->
                    // Resolved again at tap: the ROM may have updated since the screen loaded.
                    viewModel.vendorAutoStartIntent()?.let(settingsLauncher::launch)

                GrantPermissionsEffect.OpenAppNotificationSettings ->
                    settingsLauncher.launch(appNotificationIntent(context.packageName))

                GrantPermissionsEffect.RequestNotificationPermission ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
            }
        }
    }

    GrantPermissionsContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onTargetClicked = viewModel::onTargetClicked
    )
}

/**
 * The battery-optimisation list, not the one-tap allow dialog: that dialog needs
 * REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, a Play-restricted permission this app does not qualify
 * for. Falls back to the app's own settings page on devices without the list.
 */
private fun batteryOptimizationIntent(packageName: String): Intent =
    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).takeIf {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    } ?: appDetailsIntent(packageName)

private fun appNotificationIntent(packageName: String): Intent =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    } else {
        appDetailsIntent(packageName)
    }

private fun appDetailsIntent(packageName: String): Intent = Intent(
    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
    Uri.fromParts("package", packageName, null)
)

@Composable
internal fun GrantPermissionsContent(
    uiState: GrantPermissionsUiState,
    onNavigateBack: () -> Unit,
    onTargetClicked: (GrantPermissionsTarget) -> Unit
) {
    // The design keeps this screen on a plain white sheet: the shared wallpaper is switched off
    // so the white cards read against it via their shadow rather than a colour change.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_FFFFFF))
            .statusBarsPadding()
    ) {
        GrantPermissionsHeader(onNavigateBack = onNavigateBack)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                start = dimensionResource(SdpR.dimen._12sdp),
                end = dimensionResource(SdpR.dimen._12sdp),
                top = dimensionResource(SdpR.dimen._6sdp),
                bottom = dimensionResource(SdpR.dimen._12sdp)
            ),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
        ) {
            item {
                SectionHeading(
                    step = "1",
                    titleRes = R.string.grant_permissions_section_necessary
                )
            }
            item {
                AccessibilityCard(
                    isEnabled = uiState.isAccessibilityEnabled,
                    onClick = { onTargetClicked(GrantPermissionsTarget.ACCESSIBILITY) }
                )
            }
            item {
                SectionHeading(
                    step = "2",
                    titleRes = R.string.grant_permissions_section_stability,
                    modifier = Modifier.padding(top = dimensionResource(SdpR.dimen._5sdp))
                )
            }
            item {
                PermissionCard(
                    iconRes = R.drawable.img_permission_overlay,
                    titleRes = R.string.grant_permissions_overlay_title,
                    bodyRes = R.string.grant_permissions_overlay_body,
                    checked = uiState.isOverlayGranted,
                    onClick = { onTargetClicked(GrantPermissionsTarget.OVERLAY) }
                )
            }
            if (uiState.isBatteryRowVisible) {
                item {
                    PermissionCard(
                        iconRes = R.drawable.img_permission_battery,
                        titleRes = R.string.grant_permissions_battery_title,
                        bodyRes = R.string.grant_permissions_battery_body,
                        checked = uiState.isBatteryOptimizationIgnored,
                        onClick = {
                            onTargetClicked(GrantPermissionsTarget.BATTERY_OPTIMIZATION)
                        }
                    )
                }
            }
            if (uiState.isAutoStartRowVisible) {
                item {
                    PermissionCard(
                        iconRes = R.drawable.img_permission_battery,
                        titleRes = R.string.grant_permissions_autostart_title,
                        bodyRes = R.string.grant_permissions_autostart_body,
                        checked = null,
                        onClick = { onTargetClicked(GrantPermissionsTarget.VENDOR_AUTO_START) }
                    )
                }
            }
            if (uiState.isNotificationRowVisible) {
                item {
                    SectionHeading(
                        step = "3",
                        titleRes = R.string.grant_permissions_section_recommend,
                        subtitleRes = R.string.grant_permissions_recommend_subtitle,
                        modifier = Modifier.padding(top = dimensionResource(SdpR.dimen._5sdp))
                    )
                }
                item {
                    PermissionCard(
                        iconRes = R.drawable.img_permission_notification,
                        titleRes = R.string.grant_permissions_notification_title,
                        bodyRes = R.string.grant_permissions_notification_body,
                        checked = uiState.isNotificationGranted,
                        onClick = { onTargetClicked(GrantPermissionsTarget.NOTIFICATION) }
                    )
                }
            }
        }
        // Pinned below the list rather than the last row of it, so the ad stays put while
        // the permissions scroll.
        NativeAdInternal(
            screenCode = SCREEN_PERMISSION,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        )
    }
}

/** DROP_SHADOW r=9 a=0.17 on every white card in the design. */
@Composable
private fun Modifier.cardSurface(): Modifier {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp))
    val shadowColor = Color.Black.copy(alpha = 0.17f)
    return this
        .shadow(
            elevation = dimensionResource(SdpR.dimen._5sdp),
            shape = shape,
            ambientColor = shadowColor,
            spotColor = shadowColor
        )
        .clip(shape)
        .background(colorResource(R.color.colors_FFFFFF))
}

@Composable
private fun GrantPermissionsHeader(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._43sdp))
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_pet_room_back),
            contentDescription = stringResource(R.string.pet_room_back),
            tint = colorResource(R.color.colors_212327),
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._21sdp))
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onNavigateBack)
        )
        Spacer(Modifier.width(dimensionResource(SdpR.dimen._9sdp)))
        Text(
            text = stringResource(R.string.grant_permissions_nav),
            color = colorResource(R.color.colors_212327),
            fontWeight = FontWeight.SemiBold,
            fontSize = dimensionResource(SspR.dimen._15ssp).value.sp
        )
    }
}

@Composable
private fun SectionHeading(
    step: String,
    titleRes: Int,
    modifier: Modifier = Modifier,
    subtitleRes: Int? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(dimensionResource(SdpR.dimen._18sdp))
                    .clip(CircleShape)
                    .background(colorResource(R.color.colors_FB3675)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = step,
                    color = colorResource(R.color.colors_FFFFFF),
                    fontWeight = FontWeight.Medium,
                    fontSize = dimensionResource(SspR.dimen._12ssp).value.sp
                )
            }
            Spacer(Modifier.width(dimensionResource(SdpR.dimen._6sdp)))
            Text(
                text = stringResource(titleRes),
                color = colorResource(R.color.colors_212327),
                fontWeight = FontWeight.Medium,
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp
            )
        }
        subtitleRes?.let {
            Text(
                text = stringResource(it),
                color = colorResource(R.color.colors_6F7073),
                fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
                modifier = Modifier.padding(top = dimensionResource(SdpR.dimen._6sdp))
            )
        }
    }
}

@Composable
private fun AccessibilityCard(isEnabled: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cardSurface()
            .padding(dimensionResource(SdpR.dimen._12sdp)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(R.drawable.img_permission_accessibility),
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(SdpR.dimen._26sdp))
            )
            Spacer(Modifier.width(dimensionResource(SdpR.dimen._6sdp)))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.grant_permissions_accessibility_title),
                        color = colorResource(R.color.colors_212327),
                        fontWeight = FontWeight.Medium,
                        fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                        modifier = Modifier.weight(1f)
                    )
                    StatusPill(isEnabled = isEnabled)
                }
                Text(
                    text = stringResource(R.string.grant_permissions_accessibility_body),
                    color = colorResource(R.color.colors_6F7073),
                    fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
                    modifier = Modifier.padding(top = dimensionResource(SdpR.dimen._3sdp))
                )
            }
        }
        Image(
            painter = painterResource(R.drawable.img_permission_accessibility_steps),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            // The phone previews overflow their frame by 4px, so the art is exported at the
            // render bounds (296x96) rather than the layout bounds, and drawn at that ratio.
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(STEPS_ART_RATIO)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(SdpR.dimen._31sdp))
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            colorResource(R.color.colors_C95DFF),
                            colorResource(R.color.colors_FB54BB)
                        )
                    )
                )
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.grant_permissions_go_to_settings),
                color = colorResource(R.color.colors_FFFFFF),
                fontWeight = FontWeight.Medium,
                fontSize = dimensionResource(SspR.dimen._12ssp).value.sp
            )
        }
    }
}

private const val STEPS_ART_RATIO = 296f / 96f

@Composable
private fun StatusPill(isEnabled: Boolean) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                colorResource(
                    if (isEnabled) R.color.colors_E6F9EF else R.color.colors_FFECEC
                )
            )
            .padding(
                horizontal = dimensionResource(SdpR.dimen._8sdp),
                vertical = dimensionResource(SdpR.dimen._3sdp)
            )
    ) {
        Text(
            text = stringResource(
                if (isEnabled) {
                    R.string.grant_permissions_allowed
                } else {
                    R.string.grant_permissions_required
                }
            ),
            color = colorResource(
                if (isEnabled) R.color.colors_00C062 else R.color.colors_F04438
            ),
            fontWeight = FontWeight.Medium,
            fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun PermissionCard(
    iconRes: Int,
    titleRes: Int,
    bodyRes: Int,
    /** Null when no API reports the state, which is drawn as an action rather than a toggle. */
    checked: Boolean?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cardSurface()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(dimensionResource(SdpR.dimen._12sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(SdpR.dimen._26sdp))
        )
        Spacer(Modifier.width(dimensionResource(SdpR.dimen._6sdp)))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(titleRes),
                color = colorResource(R.color.colors_212327),
                fontWeight = FontWeight.Medium,
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(bodyRes),
                color = colorResource(R.color.colors_6F7073),
                fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
                modifier = Modifier.padding(top = dimensionResource(SdpR.dimen._3sdp))
            )
        }
        Spacer(Modifier.width(dimensionResource(SdpR.dimen._6sdp)))
        if (checked == null) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = colorResource(R.color.colors_C8C8C9),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._15sdp))
            )
        } else {
            AppSwitch(checked = checked, onCheckedChange = onClick)
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun GrantPermissionsPreview() {
    GrantPermissionsContent(
        uiState = GrantPermissionsUiState(
            isAccessibilityEnabled = true,
            isNotificationRowVisible = true
        ),
        onNavigateBack = {},
        onTargetClicked = {}
    )
}
