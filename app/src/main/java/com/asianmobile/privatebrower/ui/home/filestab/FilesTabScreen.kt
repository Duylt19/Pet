package com.asianmobile.privatebrower.ui.home.filestab

import com.intuit.sdp.R as R_sdp
import com.intuit.ssp.R as R_ssp
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.navigation.Routes
import com.asianmobile.privatebrower.ui.component.AppHeaderBar
import com.asianmobile.privatebrower.ui.component.AppHeaderLeading
import com.asianmobile.privatebrower.ui.medialist.MediaAccessState
import com.asianmobile.privatebrower.ui.medialist.mediaLibraryPermissions
import com.asianmobile.privatebrower.ui.permission.PermissionPolicy
import com.asianmobile.privatebrower.ui.permission.PermissionRequestDestination
import com.asianmobile.privatebrower.utils.ScreenName
import com.asianmobile.privatebrower.utils.TrackScreenView
import com.asianmobile.privatebrower.utils.dialog.PermissionRequireDialog
import com.asianmobile.privatebrower.utils.permission.AllFilesAccess
import com.asianmobile.privatebrower.utils.permission.BroadStorageAccess
import com.asianmobile.privatebrower.utils.permission.canShowPermissionRationale

private fun resolveStorageAccess(context: Context): MediaAccessState =
    if (BroadStorageAccess.isGranted(context)) MediaAccessState.GRANTED else MediaAccessState.DENIED

private fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:${context.packageName}".toUri()
        }
    )
}

@Composable
fun FilesTabScreen(
    onNavigate: (String) -> Unit = {},
    isVisible: Boolean = true,
    viewModel: FilesTabViewModel = hiltViewModel()
) {
    TrackScreenView(ScreenName.FILES_HOME, isVisible = isVisible)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showPermissionSettingsDialog by remember { mutableStateOf(false) }
    var permissionAttemptCount by rememberSaveable {
        mutableIntStateOf(uiState.permissionRequestCount)
    }

    LaunchedEffect(uiState.permissionRequestCount) {
        permissionAttemptCount = maxOf(permissionAttemptCount, uiState.permissionRequestCount)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        val access = resolveStorageAccess(context)
        viewModel.onAccessResolved(access)
        val permissions = mediaLibraryPermissions()
        if (access == MediaAccessState.DENIED &&
            PermissionPolicy.shouldOpenAppSettings(
                requestCount = permissionAttemptCount,
                canShowRationale = context.canShowPermissionRationale(permissions)
            )
        ) {
            showPermissionSettingsDialog = true
        }
    }

    val allFilesAccessLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onAccessResolved(resolveStorageAccess(context))
    }

    fun requestStorageAccess() {
        if (AllFilesAccess.isSupported()) {
            runCatching {
                allFilesAccessLauncher.launch(AllFilesAccess.settingsIntent(context))
            }.onFailure {
                openAppSettings(context)
            }
            return
        }

        val permissions = mediaLibraryPermissions()
        if (permissions.isEmpty()) {
            viewModel.onAccessResolved(resolveStorageAccess(context))
            return
        }

        when (
            PermissionPolicy.nextRequestDestination(
                isGranted = uiState.accessState == MediaAccessState.GRANTED,
                requestCount = permissionAttemptCount,
                canShowRationale = context.canShowPermissionRationale(permissions)
            )
        ) {
            PermissionRequestDestination.NONE -> Unit
            PermissionRequestDestination.SYSTEM_DIALOG -> {
                permissionAttemptCount = viewModel.markPermissionRequested()
                permissionLauncher.launch(permissions)
            }
            PermissionRequestDestination.APP_SETTINGS -> {
                showPermissionSettingsDialog = true
            }
        }
    }

    DisposableEffect(lifecycleOwner, isVisible) {
        if (isVisible) viewModel.onScreenResumed(resolveStorageAccess(context))
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isVisible) {
                viewModel.onScreenResumed(resolveStorageAccess(context))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showPermissionSettingsDialog) {
        PermissionRequireDialog(
            onDismissRequest = { showPermissionSettingsDialog = false },
            onConfirm = {
                showPermissionSettingsDialog = false
                openAppSettings(context)
            }
        )
    }

    FilesTabContent(
        uiState = uiState,
        onNavigate = onNavigate,
        onRequestPermission = ::requestStorageAccess
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilesTabContent(
    uiState: FilesTabUiState,
    onNavigate: (String) -> Unit = {},
    onRequestPermission: () -> Unit = {}
) {
    val fontSemiBold = FontFamily(Font(R.font.inter_semibold))
    val fontRegular = FontFamily(Font(R.font.inter_regular))
    val fontBeVietnamSemiBold = FontFamily(Font(R.font.be_vietnam_pro_semibold))
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_161718))
    ) {
        AppHeaderBar(
            title = stringResource(R.string.tab_folder),
            leadingIcon = AppHeaderLeading.Settings,
            onLeadingClick = { onNavigate(Routes.SETTINGS) }
        )

        if (uiState.accessState == MediaAccessState.CHECKING || uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colorResource(R.color.colors_3268FC))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = dimensionResource(R_sdp.dimen._12sdp))
            ) {
                Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._12sdp)))

                // Storage card
                if (uiState.accessState == MediaAccessState.DENIED) {
                    StoragePermissionCard(
                        fontSemiBold = fontSemiBold,
                        fontRegular = fontRegular,
                        onRequestPermission = onRequestPermission
                    )
                } else {
                    uiState.storageInfo?.let { info ->
                        StorageCard(
                            storageInfo = info,
                            fontSemiBold = fontSemiBold,
                            fontRegular = fontRegular
                        )
                    }
                }

                Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._12sdp)))

                // "File Manager" section header
                Text(
                    text = stringResource(R.string.folder_file_manager),
                    fontFamily = fontSemiBold,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = with(LocalDensity.current) {
                        dimensionResource(R_ssp.dimen._14ssp).toSp()
                    },
                    color = colorResource(R.color.colors_FFFFFF)
                )

                Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._6sdp)))

                // Category cards 2x2 grid
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        dimensionResource(R_sdp.dimen._9sdp)
                    ),
                    verticalArrangement = Arrangement.spacedBy(
                        dimensionResource(R_sdp.dimen._9sdp)
                    ),
                    maxItemsInEachRow = 2
                ) {
                    val cardModifier = Modifier.weight(1f)

                    CategoryCard(
                        iconRes = R.drawable.ic_category_photos,
                        label = stringResource(R.string.folder_photos),
                        storageBytes = uiState.storageInfo?.imageBytes ?: 0L,
                        fontSemiBold = fontBeVietnamSemiBold,
                        fontRegular = fontRegular,
                        onClick = { onNavigate(Routes.MEDIA_PHOTOS) },
                        modifier = cardModifier
                    )

                    CategoryCard(
                        iconRes = R.drawable.ic_category_videos,
                        label = stringResource(R.string.files_videos_label),
                        storageBytes = uiState.storageInfo?.videoBytes ?: 0L,
                        fontSemiBold = fontBeVietnamSemiBold,
                        fontRegular = fontRegular,
                        onClick = { onNavigate(Routes.MEDIA_VIDEOS) },
                        modifier = cardModifier
                    )

                    CategoryCard(
                        iconRes = R.drawable.ic_category_audio,
                        label = stringResource(R.string.folder_audio),
                        storageBytes = uiState.storageInfo?.musicBytes ?: 0L,
                        fontSemiBold = fontBeVietnamSemiBold,
                        fontRegular = fontRegular,
                        onClick = { onNavigate(Routes.MEDIA_AUDIO) },
                        modifier = cardModifier
                    )

                    CategoryCard(
                        iconRes = R.drawable.ic_category_files,
                        label = stringResource(R.string.folder_files),
                        storageBytes = uiState.storageInfo?.filesBytes ?: 0L,
                        fontSemiBold = fontBeVietnamSemiBold,
                        fontRegular = fontRegular,
                        onClick = { onNavigate(Routes.MEDIA_DOCUMENTS) },
                        modifier = cardModifier
                    )
                }

                Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._12sdp)))
            }
        }
    }
}

// region Storage Card

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StorageCard(
    storageInfo: StorageInfo,
    fontSemiBold: FontFamily,
    fontRegular: FontFamily
) {
    val usedGb = storageInfo.usedBytes / (1024.0 * 1024 * 1024)
    val totalGb = storageInfo.totalBytes / (1024.0 * 1024 * 1024)

    val segments = storageInfo.toBarSegments()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(R_sdp.dimen._12sdp)))
            .background(colorResource(R.color.colors_212327))
            .padding(dimensionResource(R_sdp.dimen._12sdp))
    ) {
        Text(
            text = stringResource(
                R.string.files_storage_usage_format,
                String.format("%.1f GB", usedGb),
                String.format("%.1f GB", totalGb)
            ),
            fontFamily = fontSemiBold,
            fontWeight = FontWeight.SemiBold,
            fontSize = with(LocalDensity.current) {
                dimensionResource(R_ssp.dimen._14ssp).toSp()
            },
            color = colorResource(R.color.colors_FFFFFF)
        )

        Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._9sdp)))

        // Multi-segment storage bar
        MultiSegmentStorageBar(segments = segments)

        Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._9sdp)))

        // Legend row
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R_sdp.dimen._12sdp)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R_sdp.dimen._6sdp))
        ) {
            LegendDot(
                color = colorResource(R.color.colors_D34C3E),
                label = stringResource(R.string.folder_photos),
                fontRegular = fontRegular
            )
            LegendDot(
                color = colorResource(R.color.colors_D99335),
                label = stringResource(R.string.files_videos_label),
                fontRegular = fontRegular
            )
            LegendDot(
                color = colorResource(R.color.colors_DFC141),
                label = stringResource(R.string.folder_audio),
                fontRegular = fontRegular
            )
            LegendDot(
                color = colorResource(R.color.colors_5DB95C),
                label = stringResource(R.string.folder_files),
                fontRegular = fontRegular
            )
            LegendDot(
                color = colorResource(R.color.colors_6F7073),
                label = stringResource(R.string.folder_other),
                fontRegular = fontRegular
            )
        }
    }
}

@Composable
private fun StoragePermissionCard(
    fontSemiBold: FontFamily,
    fontRegular: FontFamily,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(R_sdp.dimen._12sdp)))
            .background(colorResource(R.color.colors_212327))
            .padding(dimensionResource(R_sdp.dimen._12sdp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_folder_empty),
            contentDescription = null,
            tint = colorResource(R.color.colors_9B9C9E),
            modifier = Modifier.size(dimensionResource(R_sdp.dimen._32sdp))
        )
        Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._9sdp)))
        Text(
            text = stringResource(R.string.media_list_permission_title),
            fontFamily = fontSemiBold,
            fontWeight = FontWeight.SemiBold,
            fontSize = with(LocalDensity.current) {
                dimensionResource(R_ssp.dimen._13ssp).toSp()
            },
            color = colorResource(R.color.colors_FFFFFF),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._4sdp)))
        Text(
            text = stringResource(R.string.media_list_permission_description),
            fontFamily = fontRegular,
            fontSize = with(LocalDensity.current) {
                dimensionResource(R_ssp.dimen._10ssp).toSp()
            },
            color = colorResource(R.color.colors_9B9C9E),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._9sdp)))
        Text(
            text = stringResource(R.string.media_list_grant_access),
            modifier = Modifier
                .clip(CircleShape)
                .background(colorResource(R.color.colors_3268FC))
                .clickable(onClick = onRequestPermission)
                .padding(
                    horizontal = dimensionResource(R_sdp.dimen._18sdp),
                    vertical = dimensionResource(R_sdp.dimen._9sdp)
                ),
            color = colorResource(R.color.colors_FFFFFF),
            fontFamily = fontSemiBold
        )
    }
}

@Composable
private fun MultiSegmentStorageBar(segments: StorageBarSegments) {
    val barHeight = dimensionResource(R_sdp.dimen._18sdp)
    val barRadius = RoundedCornerShape(dimensionResource(R_sdp.dimen._5sdp))
    val usedSegments = listOf(
        segments.imageBytes to colorResource(R.color.colors_D34C3E),
        segments.videoBytes to colorResource(R.color.colors_D99335),
        segments.musicBytes to colorResource(R.color.colors_DFC141),
        segments.filesBytes to colorResource(R.color.colors_5DB95C),
        segments.otherBytes to colorResource(R.color.colors_6F7073)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight)
            .clip(barRadius)
            .background(colorResource(R.color.colors_333538))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            usedSegments.forEach { (bytes, color) ->
                if (bytes > 0L) {
                    Box(
                        modifier = Modifier
                            .weight(bytes.toFloat())
                            .fillMaxHeight()
                            .background(color)
                    )
                }
            }
            if (segments.freeBytes > 0L) {
                Box(
                    modifier = Modifier
                        .weight(segments.freeBytes.toFloat())
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun LegendDot(
    color: Color,
    label: String,
    fontRegular: FontFamily
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(dimensionResource(R_sdp.dimen._6sdp))
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(dimensionResource(R_sdp.dimen._6sdp)))
        Text(
            text = label,
            fontFamily = fontRegular,
            fontSize = with(LocalDensity.current) {
                dimensionResource(R_ssp.dimen._9ssp).toSp()
            },
            color = colorResource(R.color.colors_FFFFFF)
        )
    }
}

// endregion

// region Category Cards

@Composable
private fun CategoryCard(
    iconRes: Int,
    label: String,
    storageBytes: Long,
    fontSemiBold: FontFamily,
    fontRegular: FontFamily,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(dimensionResource(R_sdp.dimen._12sdp)))
            .background(colorResource(R.color.colors_212327))
            .clickable(onClick = onClick)
            .padding(
                start = dimensionResource(R_sdp.dimen._12sdp),
                end = dimensionResource(R_sdp.dimen._9sdp),
                top = dimensionResource(R_sdp.dimen._12sdp),
                bottom = dimensionResource(R_sdp.dimen._12sdp)
            )
    ) {
        // Category icon (exported PNG with gradient bg + icon baked in)
        Image(
            painter = painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier
                .size(dimensionResource(R_sdp.dimen._32sdp))
                .clip(RoundedCornerShape(dimensionResource(R_sdp.dimen._9sdp)))
        )

        Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._9sdp)))

        // Text row with chevron
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontFamily = fontSemiBold,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = with(LocalDensity.current) {
                        dimensionResource(R_ssp.dimen._11ssp).toSp()
                    },
                    color = colorResource(R.color.colors_FFFFFF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.folder_storage_format, formatBytes(storageBytes)),
                    fontFamily = fontRegular,
                    fontSize = with(LocalDensity.current) {
                        dimensionResource(R_ssp.dimen._9ssp).toSp()
                    },
                    color = colorResource(R.color.colors_9B9C9E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = colorResource(R.color.colors_9B9C9E),
                modifier = Modifier.size(dimensionResource(R_sdp.dimen._12sdp))
            )
        }
    }
}

// endregion

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> String.format("%.1fGB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> String.format("%.1fMB", bytes / 1_048_576.0)
        bytes >= 1024 -> String.format("%.1fKB", bytes / 1024.0)
        else -> "${bytes}B"
    }
}

@Preview(showBackground = true)
@Composable
private fun FilesTabScreenPreview() {
    FilesTabContent(
        uiState = FilesTabUiState(
            storageInfo = StorageInfo(
                usedBytes = 10_000_000_000L,
                totalBytes = 118_111_600_640L,
                imageBytes = 3_865_470_566L,
                videoBytes = 3_865_470_566L,
                musicBytes = 1_073_741_824L,
                filesBytes = 1_195_327_488L
            )
        )
    )
}
