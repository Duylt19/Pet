package com.asianmobile.emojibattery.shimeji.ui.settings.permissions

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_GRANT_PERMISSIONS
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.NativeAdInternal
import com.asianmobile.emojibattery.shimeji.ads.ui.interstitial.InterstitialUtil
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryAccessibility
import com.asianmobile.emojibattery.shimeji.pet.overlay.PetOverlay
import com.asianmobile.emojibattery.shimeji.ui.shared.component.AppSwitch
import com.asianmobile.emojibattery.shimeji.ui.shared.component.GrantPermissionDialog
import com.asianmobile.emojibattery.shimeji.ui.shared.component.OverlayPermissionDialog
import com.asianmobile.emojibattery.shimeji.ui.shared.component.launchFirstAvailable
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

@Composable
fun GrantPermissionsScreen(
    onNavigateBack: () -> Unit = {},
    requiredTarget: GrantPermissionsTarget = GrantPermissionsTarget.ACCESSIBILITY,
    accessibilityHowToUseResult: Boolean? = null,
    onAccessibilityHowToUseResultConsumed: () -> Unit = {},
    onNavigateToAccessibilityHowToUse: () -> Unit = {},
    viewModel: GrantPermissionsViewModel = hiltViewModel()
) {
    TrackScreenView(ScreenName.GRANT_PERMISSIONS)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showAccessibilityDisclosure by rememberSaveable { mutableStateOf(false) }
    var showOverlayPermissionDisclosure by rememberSaveable { mutableStateOf(false) }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { viewModel.onPermissionStepReturned(requiredTarget) }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.onPermissionStepReturned(requiredTarget) }

    LaunchedEffect(accessibilityHowToUseResult) {
        if (accessibilityHowToUseResult != null) {
            viewModel.refresh()
            onAccessibilityHowToUseResultConsumed()
        }
    }

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
                GrantPermissionsEffect.ShowAccessibilityDisclosure -> {
                    showAccessibilityDisclosure = true
                }

                GrantPermissionsEffect.OpenAccessibilitySettings ->
                    settingsLauncher.openSettings(
                        BatteryAccessibility.detailsSettingsIntent(context),
                        BatteryAccessibility.settingsIntent(),
                        appDetailsIntent(context.packageName)
                    )

                GrantPermissionsEffect.OpenOverlaySettings -> {
                    if (requiredTarget == GrantPermissionsTarget.OVERLAY ||
                        uiState.isOverlayGranted
                    ) {
                        settingsLauncher.openSettings(
                            PetOverlay.permissionIntent(context),
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                        )
                    } else {
                        showOverlayPermissionDisclosure = true
                    }
                }

                GrantPermissionsEffect.OpenBatteryOptimizationSettings ->
                    settingsLauncher.openSettings(
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.fromParts("package", context.packageName, null)
                        ),
                        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                        appDetailsIntent(context.packageName)
                    )

                GrantPermissionsEffect.OpenVendorAutoStartSettings ->
                    // Resolved again at tap: the ROM may have updated since the screen loaded.
                    // No fallback — there is no second screen that means the same thing.
                    viewModel.vendorAutoStartIntent()?.let {
                        settingsLauncher.openSettings(it)
                    }
                        ?: viewModel.onPermissionStepReturned(requiredTarget)

                GrantPermissionsEffect.OpenAppNotificationSettings ->
                    settingsLauncher.openSettings(
                        appNotificationIntent(context.packageName),
                        appDetailsIntent(context.packageName)
                    )

                GrantPermissionsEffect.RequestNotificationPermission ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.onPermissionStepReturned(requiredTarget)
                    }

                GrantPermissionsEffect.PetOverlayStartFailed -> Toast.makeText(
                    context,
                    R.string.grant_permissions_pet_start_failed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    GrantPermissionsContent(
        uiState = uiState,
        requiredTarget = requiredTarget,
        onNavigateBack = onNavigateBack,
        onTargetClicked = viewModel::onTargetClicked,
        onPrimaryAction = { viewModel.startPermissionSequence(requiredTarget) }
    )

    if (showAccessibilityDisclosure) {
        GrantPermissionDialog(
            onGrantPermission = {
                showAccessibilityDisclosure = false
                onNavigateToAccessibilityHowToUse()
            },
            onMaybeLater = { showAccessibilityDisclosure = false }
        )
    }

    if (showOverlayPermissionDisclosure) {
        OverlayPermissionDialog(
            onAllowAccess = {
                showOverlayPermissionDisclosure = false
                settingsLauncher.openSettings(
                    PetOverlay.permissionIntent(context),
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                )
            },
            onNotNow = { showOverlayPermissionDisclosure = false }
        )
    }
}

/**
 * Leaving for a system screen has to suppress the app-open ad, or coming back from granting a
 * permission is answered with a full-screen ad the user did nothing to earn. Suppressing here
 * rather than at each call site is what keeps that true for every row.
 *
 * The intents are tried in order because none of these ROM-owned screens is guaranteed to exist;
 * see [launchFirstAvailable].
 */
private fun ActivityResultLauncher<Intent>.openSettings(vararg intents: Intent) {
    InterstitialUtil.getInstance().openAd?.needShowOpenAds = false
    launchFirstAvailable(*intents)
}

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GrantPermissionsContent(
    uiState: GrantPermissionsUiState,
    requiredTarget: GrantPermissionsTarget = GrantPermissionsTarget.ACCESSIBILITY,
    onNavigateBack: () -> Unit,
    onTargetClicked: (GrantPermissionsTarget) -> Unit,
    onPrimaryAction: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val needsRequiredCard = uiState.needsRequiredCard(requiredTarget)
    val needsRequiredSection = needsRequiredCard ||
        (requiredTarget == GrantPermissionsTarget.OVERLAY &&
            uiState.needsNotificationPermission)
    val needsStabilityPermission = uiState.hasStabilityPermissionToRequest(requiredTarget)

    // The design keeps this screen on a plain white sheet: the shared wallpaper is switched off
    // so the white cards read against it via their shadow rather than a colour change.
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = colorResource(R.color.colors_FFFFFF),
        topBar = {
            GrantPermissionsTopBar(
                collapsedFraction = scrollBehavior.state.collapsedFraction,
                onNavigateBack = onNavigateBack,
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            // Pinned outside the list so it stays put while the permission cards scroll.
            NativeAdInternal(
                screenCode = SCREEN_GRANT_PERMISSIONS,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = dimensionResource(SdpR.dimen._16sdp),
                end = dimensionResource(SdpR.dimen._16sdp),
                top = innerPadding.calculateTopPadding() +
                    dimensionResource(SdpR.dimen._6sdp),
                bottom = innerPadding.calculateBottomPadding() +
                    dimensionResource(SdpR.dimen._12sdp)
            ),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
        ) {
            if (needsRequiredSection) {
                item {
                    SectionHeading(
                        step = "1",
                        titleRes = R.string.grant_permissions_section_necessary
                    )
                }
            }
            if (needsRequiredCard) {
                item {
                    RequiredPermissionCard(
                        requiredTarget = requiredTarget,
                        isEnabled = false,
                        onClick = onPrimaryAction
                    )
                }
            }
            if (requiredTarget == GrantPermissionsTarget.OVERLAY) {
                if (uiState.needsNotificationPermission) {
                    item {
                        PermissionCard(
                            iconRes = R.drawable.img_permission_notification,
                            titleRes = R.string.grant_permissions_notification_title,
                            bodyRes = R.string.grant_permissions_notification_body,
                            checked = false,
                            onClick = { onTargetClicked(GrantPermissionsTarget.NOTIFICATION) }
                        )
                    }
                }
            }
            if (needsStabilityPermission) {
                item {
                    SectionHeading(
                        step = "2",
                        titleRes = R.string.grant_permissions_section_stability,
                        modifier = Modifier.padding(top = dimensionResource(SdpR.dimen._5sdp))
                    )
                }
            }
            if (requiredTarget != GrantPermissionsTarget.OVERLAY &&
                uiState.needsOverlayPermission
            ) {
                item {
                    PermissionCard(
                        iconRes = R.drawable.img_permission_overlay,
                        titleRes = R.string.grant_permissions_overlay_title,
                        bodyRes = R.string.grant_permissions_overlay_body,
                        checked = false,
                        onClick = { onTargetClicked(GrantPermissionsTarget.OVERLAY) }
                    )
                }
            }
            if (uiState.needsBatteryOptimizationExemption) {
                item {
                    PermissionCard(
                        iconRes = R.drawable.img_permission_battery,
                        titleRes = R.string.grant_permissions_battery_title,
                        bodyRes = R.string.grant_permissions_battery_body,
                        checked = false,
                        onClick = {
                            onTargetClicked(GrantPermissionsTarget.BATTERY_OPTIMIZATION)
                        }
                    )
                }
            }
            if (uiState.isAutoStartRowVisible) {
                item {
                    PermissionCard(
                        iconRes = R.drawable.ic_permission_autostart,
                        iconBackgroundColors = listOf(
                            colorResource(R.color.colors_8580FD),
                            colorResource(R.color.colors_615AD9)
                        ),
                        titleRes = R.string.grant_permissions_autostart_title,
                        bodyRes = R.string.grant_permissions_autostart_body,
                        checked = null,
                        onClick = { onTargetClicked(GrantPermissionsTarget.VENDOR_AUTO_START) }
                    )
                }
            }
            if (requiredTarget != GrantPermissionsTarget.OVERLAY &&
                uiState.needsNotificationPermission
            ) {
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
                        checked = false,
                        onClick = { onTargetClicked(GrantPermissionsTarget.NOTIFICATION) }
                    )
                }
            }
        }
    }
}

/**
 * DROP_SHADOW r=9 a=0.17 on every white card in the design. Compose spreads a shadow downwards
 * from its elevation rather than blurring evenly, so matching the alpha alone leaves a white card
 * on a white sheet with no visible edge — the elevation and alpha are raised until the card reads.
 */
@Composable
private fun Modifier.cardSurface(): Modifier {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._16sdp))
    val shadowColor = colorResource(R.color.colors_212327).copy(alpha = 0.30f)
    return this
        .shadow(
            elevation = dimensionResource(SdpR.dimen._8sdp),
            shape = shape,
            ambientColor = shadowColor,
            spotColor = shadowColor
        )
        .clip(shape)
        .background(colorResource(R.color.colors_FFFFFF))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GrantPermissionsTopBar(
    collapsedFraction: Float,
    onNavigateBack: () -> Unit,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior
) {
    val expandedTitleSize = dimensionResource(SspR.dimen._18ssp).value.sp
    val collapsedTitleSize = dimensionResource(SspR.dimen._14ssp).value.sp
    val titleSize = (
        expandedTitleSize.value +
            (collapsedTitleSize.value - expandedTitleSize.value) * collapsedFraction
        ).sp

    LargeTopAppBar(
        title = {
            Text(
                text = stringResource(R.string.grant_permissions_nav),
                color = colorResource(R.color.colors_212327),
                fontWeight = FontWeight.SemiBold,
                fontSize = titleSize,
                lineHeight = dimensionResource(SspR.dimen._24ssp).value.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            Box(
                modifier = Modifier
                    .size(dimensionResource(SdpR.dimen._43sdp))
                    .clickable(role = Role.Button, onClick = onNavigateBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_pet_room_back),
                    contentDescription = stringResource(R.string.pet_room_back),
                    tint = colorResource(R.color.colors_212327),
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._21sdp))
                )
            }
        },
        collapsedHeight = dimensionResource(SdpR.dimen._43sdp),
        expandedHeight = dimensionResource(SdpR.dimen._77sdp),
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = colorResource(R.color.colors_FFFFFF)
        ),
        scrollBehavior = scrollBehavior
    )
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
                    .size(dimensionResource(SdpR.dimen._24sdp))
                    .clip(CircleShape)
                    .background(colorResource(R.color.colors_FB3675)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = step,
                    color = colorResource(R.color.colors_FFFFFF),
                    fontWeight = FontWeight.Medium,
                    fontSize = dimensionResource(SspR.dimen._14ssp).value.sp
                )
            }
            Spacer(Modifier.width(dimensionResource(SdpR.dimen._8sdp)))
            Text(
                text = stringResource(titleRes),
                color = colorResource(R.color.colors_212327),
                fontWeight = FontWeight.Medium,
                fontSize = dimensionResource(SspR.dimen._14ssp).value.sp
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
private fun RequiredPermissionCard(
    requiredTarget: GrantPermissionsTarget,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    val isOverlayRequired = requiredTarget == GrantPermissionsTarget.OVERLAY
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cardSurface()
            .padding(dimensionResource(SdpR.dimen._16sdp)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            if (isOverlayRequired) {
                Box(
                    modifier = Modifier
                        .size(dimensionResource(SdpR.dimen._34sdp))
                        .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._8sdp)))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    colorResource(R.color.colors_15EDB8),
                                    colorResource(R.color.colors_0EB7AD)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_permission_draw_over_apps),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(dimensionResource(SdpR.dimen._22sdp))
                    )
                }
            } else {
                Image(
                    painter = painterResource(R.drawable.img_permission_accessibility),
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._34sdp))
                )
            }
            Spacer(Modifier.width(dimensionResource(SdpR.dimen._8sdp)))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(
                            if (isOverlayRequired) {
                                R.string.grant_permissions_overlay_required_title
                            } else {
                                R.string.grant_permissions_accessibility_title
                            }
                        ),
                        color = colorResource(R.color.colors_212327),
                        fontWeight = FontWeight.Medium,
                        fontSize = dimensionResource(SspR.dimen._14ssp).value.sp,
                        modifier = Modifier.weight(1f)
                    )
                    StatusPill(isEnabled = isEnabled)
                }
                Text(
                    text = stringResource(
                        if (isOverlayRequired) {
                            R.string.grant_permissions_overlay_required_body
                        } else {
                            R.string.grant_permissions_accessibility_body
                        }
                    ),
                    color = colorResource(R.color.colors_6F7073),
                    fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
                    modifier = Modifier.padding(top = dimensionResource(SdpR.dimen._3sdp))
                )
            }
        }
        if (isOverlayRequired) {
            Image(
                painter = painterResource(R.drawable.img_grant_permission_pet_on_screen),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(
                    width = dimensionResource(SdpR.dimen._158sdp),
                    height = dimensionResource(SdpR.dimen._100sdp)
                )
                    .align(Alignment.CenterHorizontally)
            )
        } else {
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
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(SdpR.dimen._40sdp))
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
                fontSize = dimensionResource(SspR.dimen._14ssp).value.sp
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
    iconBackgroundColors: List<Color>? = null,
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
            .padding(dimensionResource(SdpR.dimen._16sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconBackgroundColors == null) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(SdpR.dimen._34sdp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(dimensionResource(SdpR.dimen._34sdp))
                    .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._8sdp)))
                    .background(Brush.verticalGradient(iconBackgroundColors)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._22sdp))
                )
            }
        }
        Spacer(Modifier.width(dimensionResource(SdpR.dimen._8sdp)))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(titleRes),
                color = colorResource(R.color.colors_212327),
                fontWeight = FontWeight.Medium,
                fontSize = dimensionResource(SspR.dimen._14ssp).value.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(bodyRes),
                color = colorResource(R.color.colors_6F7073),
                fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
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
private fun GrantPermissionsAccessibilityPreview() {
    GrantPermissionsContent(
        uiState = GrantPermissionsUiState(
            isAccessibilityEnabled = true,
            isNotificationRowVisible = true
        ),
        requiredTarget = GrantPermissionsTarget.ACCESSIBILITY,
        onNavigateBack = {},
        onTargetClicked = {},
        onPrimaryAction = {}
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun GrantPermissionsPetOnScreenPreview() {
    GrantPermissionsContent(
        uiState = GrantPermissionsUiState(
            isOverlayGranted = true,
            isNotificationGranted = true,
            isNotificationRowVisible = true,
            isBatteryRowVisible = true,
            isAutoStartRowVisible = true
        ),
        requiredTarget = GrantPermissionsTarget.OVERLAY,
        onNavigateBack = {},
        onTargetClicked = {},
        onPrimaryAction = {}
    )
}
