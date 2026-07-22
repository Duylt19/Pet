package com.asianmobile.privatebrower.ui.home.progresstab

import android.media.MediaMetadataRetriever
import android.graphics.BitmapFactory
import android.webkit.MimeTypeMap
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.ads.config.SCREEN_DOWNLOADS
import com.asianmobile.privatebrower.ads.ui.compose.NativeAdInternal
import com.asianmobile.privatebrower.data.database.entity.DownloadEntity
import com.asianmobile.privatebrower.data.download.DownloadStorage
import com.asianmobile.privatebrower.data.model.DownloadStatus
import com.asianmobile.privatebrower.navigation.Routes
import com.asianmobile.privatebrower.ui.mediaviewer.MediaViewerRoute
import com.asianmobile.privatebrower.ui.mediaviewer.MediaViewerKind
import com.asianmobile.privatebrower.ui.component.AppHeaderBar
import com.asianmobile.privatebrower.ui.component.AppHeaderLeading
import com.asianmobile.privatebrower.ui.component.DismissibleDialogBackdrop
import com.asianmobile.privatebrower.ui.component.FileTypeIcon
import com.asianmobile.privatebrower.ui.component.FileTypeKind
import com.asianmobile.privatebrower.ui.component.RemoveConfirmationDialog
import com.asianmobile.privatebrower.ui.component.SegmentedTabBar
import com.asianmobile.privatebrower.ui.component.SegmentedTabItem
import com.asianmobile.privatebrower.ui.component.fileTypeKind
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.asianmobile.privatebrower.utils.ScreenName
import com.asianmobile.privatebrower.utils.TrackScreenView

@Composable
fun ProgressTabScreen(
    onNavigateToHome: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
    isPremium: Boolean = false,
    isVisible: Boolean = true,
    viewModel: ProgressTabViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TrackScreenView(
        screen = when (uiState.selectedTab) {
            DownloadTab.ALL -> ScreenName.DOWNLOADS_ALL
            DownloadTab.DOWNLOADING -> ScreenName.DOWNLOADS_ACTIVE
            DownloadTab.COMPLETED -> ScreenName.DOWNLOADS_COMPLETED
        },
        isVisible = isVisible
    )
    val fontMedium = FontFamily(Font(R.font.inter_medium))
    val fontSemiBold = FontFamily(Font(R.font.inter_semibold))
    val fontRegular = FontFamily(Font(R.font.inter_regular))

    var cancelTarget by remember { mutableStateOf<Long?>(null) }
    var informationTarget by remember { mutableStateOf<DownloadEntity?>(null) }
    var removeTarget by remember { mutableStateOf<DownloadEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_161718))
    ) {
        // Header
        AppHeaderBar(
            title = stringResource(R.string.downloads_title),
            leadingIcon = AppHeaderLeading.Settings,
            onLeadingClick = { onNavigate(Routes.SETTINGS) }
        )

        // Tab Filter Row
        DownloadTabFilter(
            selectedTab = uiState.selectedTab,
            onTabSelected = viewModel::onTabSelected,
            fontMedium = fontMedium
        )

        Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._12sdp)))

        // Content based on tab and data
        val showingItems = when (uiState.selectedTab) {
            DownloadTab.ALL -> uiState.allItems
            DownloadTab.DOWNLOADING -> uiState.activeItems
            DownloadTab.COMPLETED -> uiState.completedItems
        }

        if (showingItems.isEmpty() && !uiState.isEmpty && uiState.selectedTab != DownloadTab.ALL) {
            // Tab-specific empty (e.g. no downloading but has completed)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (uiState.selectedTab) {
                        DownloadTab.DOWNLOADING -> stringResource(R.string.progress_no_downloading)
                        DownloadTab.COMPLETED -> stringResource(R.string.downloads_empty_title)
                        else -> ""
                    },
                    fontFamily = fontMedium,
                    fontSize = with(LocalDensity.current) {
                        dimensionResource(SspR.dimen._12ssp).toSp()
                    },
                    color = colorResource(R.color.gray_808080)
                )
            }
        } else if (uiState.isEmpty) {
            // Global empty state
            DownloadEmptyState(
                onStartBrowsing = onNavigateToHome,
                fontMedium = fontMedium,
                fontRegular = fontRegular
            )
        } else {
            // Content list
            DownloadListContent(
                uiState = uiState,
                fontMedium = fontMedium,
                fontSemiBold = fontSemiBold,
                fontRegular = fontRegular,
                showNativeAd = !isPremium,
                onCancel = { cancelTarget = it },
                onPause = viewModel::onPauseDownload,
                onResume = viewModel::onResumeDownload,
                onRetry = viewModel::onRetryDownload,
                onRemove = { id -> removeTarget = uiState.allItems.firstOrNull { it.id == id } },
                onOpenFile = { item ->
                    if (MediaViewerKind.from(item.mimeType, item.fileName) == MediaViewerKind.OTHER) {
                        viewModel.onOpenFile(item.path, item.fileName, item.mimeType)
                    } else {
                        onNavigate(MediaViewerRoute.fromDownload(item))
                    }
                },
                onShare = { item ->
                    viewModel.onShare(item.path, item.fileName, item.mimeType)
                },
                onInformation = { informationTarget = it }
            )
        }
    }

    cancelTarget?.let { id ->
        AlertDialog(
            onDismissRequest = { cancelTarget = null },
            title = { Text(stringResource(R.string.downloads_cancel_title)) },
            text = { Text(stringResource(R.string.downloads_cancel_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onCancelDownload(id)
                    cancelTarget = null
                }) { Text(stringResource(R.string.downloads_cancel_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { cancelTarget = null }) {
                    Text(stringResource(R.string.downloads_cancel_dismiss))
                }
            }
        )
    }

    informationTarget?.let { item ->
        DownloadInformationDialog(
            item = item,
            onDismiss = { informationTarget = null }
        )
    }

    removeTarget?.let { item ->
        DownloadRemoveDialog(
            item = item,
            onDismiss = { removeTarget = null },
            onConfirm = {
                viewModel.onRemoveFromList(item.id)
                removeTarget = null
            }
        )
    }
}

// ─── Tab Filter ───────────────────────────────────────────────────────────────

@Composable
private fun DownloadTabFilter(
    selectedTab: DownloadTab,
    onTabSelected: (DownloadTab) -> Unit,
    fontMedium: FontFamily
) {
    SegmentedTabBar {
        DownloadTab.entries.forEach { tab ->
            SegmentedTabItem(
                text = when (tab) {
                    DownloadTab.ALL -> stringResource(R.string.downloads_tab_all)
                    DownloadTab.DOWNLOADING -> stringResource(R.string.downloads_tab_downloading)
                    DownloadTab.COMPLETED -> stringResource(R.string.downloads_tab_completed)
                },
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                fontMedium = fontMedium
            )
        }
    }
}

// ─── Empty State ──────────────────────────────────────────────────────────────

@Composable
private fun DownloadEmptyState(
    onStartBrowsing: () -> Unit,
    fontMedium: FontFamily,
    fontRegular: FontFamily
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(SdpR.dimen._40sdp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Empty state illustration
        val downloadAnim by rememberLottieComposition(
            LottieCompositionSpec.RawRes(R.raw.download)
        )
        val downloadAnimProgress by animateLottieCompositionAsState(
            downloadAnim,
            iterations = LottieConstants.IterateForever
        )
        LottieAnimation(
            composition = downloadAnim,
            progress = { downloadAnimProgress },
            modifier = Modifier.size(dimensionResource(SdpR.dimen._64sdp))
        )

        Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._12sdp)))

        // Title
        Text(
            text = stringResource(R.string.downloads_empty_title),
            fontFamily = fontMedium,
            fontWeight = FontWeight.Medium,
            fontSize = with(LocalDensity.current) {
                dimensionResource(SspR.dimen._12ssp).toSp()
            },
            color = colorResource(R.color.colors_FFFFFF),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._3sdp)))

        // Description
        Text(
            text = stringResource(R.string.downloads_empty_desc),
            fontFamily = fontRegular,
            fontSize = with(LocalDensity.current) {
                dimensionResource(SspR.dimen._11ssp).toSp()
            },
            color = colorResource(R.color.colors_9EA2AB),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._12sdp)))

        // Start Browsing button
        Box(
            modifier = Modifier
                .width(dimensionResource(SdpR.dimen._162sdp))
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp)))
                .background(colorResource(R.color.colors_2257EB))
                .clickable(onClick = onStartBrowsing)
                .padding(vertical = dimensionResource(SdpR.dimen._9sdp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.downloads_start_browsing),
                fontFamily = fontMedium,
                fontWeight = FontWeight.Medium,
                fontSize = with(LocalDensity.current) {
                    dimensionResource(SspR.dimen._11ssp).toSp()
                },
                color = colorResource(R.color.colors_FFFFFF)
            )
        }
    }
}

// ─── Download List Content ────────────────────────────────────────────────────

@Composable
private fun DownloadListContent(
    uiState: ProgressTabUiState,
    fontMedium: FontFamily,
    fontSemiBold: FontFamily,
    fontRegular: FontFamily,
    showNativeAd: Boolean,
    onCancel: (Long) -> Unit,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onRetry: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onOpenFile: (DownloadEntity) -> Unit,
    onShare: (DownloadEntity) -> Unit,
    onInformation: (DownloadEntity) -> Unit
) {
    var viewportBottom by remember { mutableIntStateOf(0) }
    val showActive = uiState.selectedTab == DownloadTab.ALL ||
        uiState.selectedTab == DownloadTab.DOWNLOADING
    val recentItems = when (uiState.selectedTab) {
        DownloadTab.ALL -> (uiState.completedItems + uiState.failedItems)
            .sortedByDescending { it.completedAt ?: it.createdAt }
        DownloadTab.COMPLETED -> uiState.completedItems
            .sortedByDescending { it.completedAt ?: it.createdAt }
        DownloadTab.DOWNLOADING -> emptyList()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp))
            .onGloballyPositioned {
                viewportBottom = it.boundsInWindow().bottom.roundToInt()
            },
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
    ) {
        if (showNativeAd) {
            item(key = "downloads_native_ad") {
                NativeAdInternal(
                    screenCode = SCREEN_DOWNLOADS,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (showActive && uiState.activeItems.isNotEmpty()) {
            item(key = "downloading_section") {
                DownloadSection(
                    title = stringResource(R.string.downloads_section_downloading, uiState.activeItems.size),
                    showTitle = uiState.selectedTab == DownloadTab.ALL,
                    fontSemiBold = fontSemiBold
                ) {
                    SectionCard(spacing = dimensionResource(SdpR.dimen._12sdp)) {
                        uiState.activeItems.forEachIndexed { index, item ->
                            ActiveDownloadItem(
                                item = item,
                                speedBps = uiState.speeds[item.id] ?: 0L,
                                fontMedium = fontMedium,
                                fontRegular = fontRegular,
                                onCancel = { onCancel(item.id) },
                                onPause = { onPause(item.id) },
                                onResume = { onResume(item.id) }
                            )
                            if (index < uiState.activeItems.lastIndex) SectionDivider()
                        }
                    }
                }
            }
        }

        if (recentItems.isNotEmpty()) {
            item(key = "recent_section") {
                DownloadSection(
                    title = stringResource(R.string.downloads_section_recent),
                    showTitle = uiState.selectedTab == DownloadTab.ALL,
                    fontSemiBold = fontSemiBold
                ) {
                    SectionCard(spacing = dimensionResource(SdpR.dimen._12sdp)) {
                        recentItems.forEachIndexed { index, item ->
                            if (item.status == DownloadStatus.FAILED.name) {
                                FailedDownloadItem(
                                    item = item,
                                    fontMedium = fontMedium,
                                    fontRegular = fontRegular,
                                    onRetry = { onRetry(item.id) },
                                    onRemove = { onRemove(item.id) }
                                )
                            } else {
                                CompletedDownloadItem(
                                    item = item,
                                    fontMedium = fontMedium,
                                    fontRegular = fontRegular,
                                    menuBoundaryBottom = viewportBottom,
                                    onOpen = { onOpenFile(item) },
                                    onShare = { onShare(item) },
                                    onInformation = { onInformation(item) },
                                    onRemove = { onRemove(item.id) }
                                )
                            }
                            if (index < recentItems.lastIndex) SectionDivider()
                        }
                    }
                }
            }
        }

        // Bottom spacer
        item { Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._12sdp))) }
    }
}

@Composable
private fun DownloadSection(
    title: String,
    showTitle: Boolean,
    fontSemiBold: FontFamily,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))) {
        if (showTitle) {
            Text(
                text = title,
                fontFamily = fontSemiBold,
                fontWeight = FontWeight.SemiBold,
                fontSize = with(LocalDensity.current) {
                    dimensionResource(SspR.dimen._11ssp).toSp()
                },
                color = colorResource(R.color.colors_FFFFFF)
            )
        }
        content()
    }
}

@Composable
private fun SectionCard(spacing: androidx.compose.ui.unit.Dp, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp)))
            .background(colorResource(R.color.colors_212327))
            .padding(dimensionResource(SdpR.dimen._9sdp)),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) { content() }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        color = colorResource(R.color.colors_333538),
        thickness = 0.5.dp
    )
}

// ─── Active Download Item ─────────────────────────────────────────────────────

@Composable
private fun ActiveDownloadItem(
    item: DownloadEntity,
    speedBps: Long,
    fontMedium: FontFamily,
    fontRegular: FontFamily,
    onCancel: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit
) {
    // HLS/streamed downloads don't know their total size until finished (sizeBytes == 0),
    // so a percentage would be a misleading 0%. Fall back to an indeterminate bar for those.
    val hasKnownTotal = item.sizeBytes > 0
    val progress = if (hasKnownTotal) {
        item.downloadedBytes.toFloat() / item.sizeBytes
    } else 0f
    val isPaused = item.status == "PAUSED"
    val isFailed = item.status == "FAILED"
    val isActive = !isPaused && !isFailed

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DownloadThumbnail(
            fileName = item.fileName,
            filePath = item.path,
            mimeType = item.mimeType,
            videoUrl = item.url,
            thumbnailUrl = item.thumbnailUrl,
            requestHeaders = item.requestHeaders,
            showProgressOverlay = true,
            progress = progress.takeIf { hasKnownTotal },
            isProgressIndeterminate = !hasKnownTotal && isActive,
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._49sdp))
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp)))
        )

        // Info column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._2sdp))
        ) {
            // Title
            Text(
                text = item.fileName,
                fontFamily = fontMedium,
                fontSize = with(LocalDensity.current) {
                    dimensionResource(SspR.dimen._11ssp).toSp()
                },
                color = colorResource(R.color.colors_FFFFFF),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Subtitle: size / total • speed
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._3sdp))
            ) {
                Text(
                    // Unknown total → show only what's downloaded so far (not "… / 0 B").
                    text = if (hasKnownTotal) {
                        "${formatBytes(item.downloadedBytes)} / ${formatBytes(item.sizeBytes)}"
                    } else {
                        formatBytes(item.downloadedBytes)
                    },
                    fontFamily = fontRegular,
                    fontSize = with(LocalDensity.current) {
                        dimensionResource(SspR.dimen._8ssp).toSp()
                    },
                    color = colorResource(R.color.colors_9B9C9E)
                )
                // Dot separator
                Box(
                    modifier = Modifier
                        .size(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(colorResource(R.color.colors_9B9C9E))
                )
                Text(
                    text = when {
                        isFailed -> item.errorMessage ?: stringResource(R.string.progress_status_failed)
                        isPaused -> stringResource(R.string.progress_status_paused)
                        speedBps > 0 -> "${formatBytes(speedBps)}/s"
                        else -> stringResource(R.string.progress_status_downloading)
                    },
                    fontFamily = fontRegular,
                    fontSize = with(LocalDensity.current) {
                        dimensionResource(SspR.dimen._8ssp).toSp()
                    },
                    color = colorResource(R.color.colors_9B9C9E)
                )
            }

            Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._2sdp)))

            // Progress bar + percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
            ) {
                val barModifier = Modifier
                    .weight(1f)
                    .height(dimensionResource(SdpR.dimen._3sdp))
                    .clip(RoundedCornerShape(2.dp))
                if (hasKnownTotal) {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = barModifier,
                        color = colorResource(R.color.colors_3369FD),
                        trackColor = colorResource(R.color.colors_3C3D41),
                        strokeCap = StrokeCap.Round,
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontFamily = fontMedium,
                        fontSize = with(LocalDensity.current) {
                            dimensionResource(SspR.dimen._8ssp).toSp()
                        },
                        color = colorResource(R.color.colors_3369FD)
                    )
                } else if (isActive) {
                    // Total unknown but actively downloading → animate an indeterminate bar.
                    LinearProgressIndicator(
                        modifier = barModifier,
                        color = colorResource(R.color.colors_3369FD),
                        trackColor = colorResource(R.color.colors_3C3D41),
                        strokeCap = StrokeCap.Round,
                    )
                } else {
                    // Paused/failed with unknown total → static empty track, no fake percentage.
                    LinearProgressIndicator(
                        progress = { 0f },
                        modifier = barModifier,
                        color = colorResource(R.color.colors_3369FD),
                        trackColor = colorResource(R.color.colors_3C3D41),
                        strokeCap = StrokeCap.Round,
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))) {
            DownloadActionButton(
                iconRes = if (isPaused || isFailed) R.drawable.ic_resume else R.drawable.ic_pause,
                contentDescription = stringResource(
                    if (isPaused || isFailed) R.string.progress_resume_label else R.string.progress_pause_label
                ),
                onClick = if (isPaused || isFailed) onResume else onPause
            )
            DownloadActionButton(
                iconRes = R.drawable.ic_close_x,
                contentDescription = stringResource(R.string.progress_cancel_label),
                onClick = onCancel
            )
        }
    }
}

@Composable
private fun DownloadActionButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(dimensionResource(SdpR.dimen._22sdp))
            .border(0.5.dp, colorResource(R.color.colors_333538), CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = colorResource(R.color.colors_FFFFFF),
            modifier = Modifier.size(dimensionResource(SdpR.dimen._15sdp))
        )
    }
}

// ─── Completed Download Item ──────────────────────────────────────────────────

@Composable
private fun CompletedDownloadItem(
    item: DownloadEntity,
    fontMedium: FontFamily,
    fontRegular: FontFamily,
    menuBoundaryBottom: Int,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onInformation: () -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    val fileExists by produceState(initialValue = false, key1 = item.path) {
        value = withContext(Dispatchers.IO) {
            DownloadStorage.exists(context, item.path)
        }
    }
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = fileExists, onClick = onOpen),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DownloadThumbnail(
            fileName = item.fileName,
            filePath = item.path,
            mimeType = item.mimeType,
            videoUrl = item.url,
            thumbnailUrl = item.thumbnailUrl,
            requestHeaders = item.requestHeaders,
            showPlayOverlay = true,
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._49sdp))
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp)))
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._2sdp))
        ) {
            Text(
                text = item.fileName,
                fontFamily = fontMedium,
                fontSize = with(LocalDensity.current) { dimensionResource(SspR.dimen._11ssp).toSp() },
                color = colorResource(R.color.colors_FFFFFF),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = completedMetadataText(item, fileExists),
                fontFamily = fontRegular,
                fontSize = with(LocalDensity.current) { dimensionResource(SspR.dimen._8ssp).toSp() },
                color = colorResource(R.color.colors_9B9C9E),
                maxLines = 2
            )
        }

        Box {
            Icon(
                painter = painterResource(R.drawable.ic_more_horizontal),
                contentDescription = stringResource(R.string.downloads_menu_more),
                tint = colorResource(R.color.colors_9B9C9E),
                modifier = Modifier
                    .size(dimensionResource(SdpR.dimen._18sdp))
                    .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._4sdp)))
                    .clickable { menuExpanded = true }
            )
            DownloadItemMenu(
                expanded = menuExpanded,
                boundaryBottom = menuBoundaryBottom,
                onDismissRequest = { menuExpanded = false },
                onShare = { menuExpanded = false; onShare() },
                onInformation = { menuExpanded = false; onInformation() },
                onRemove = { menuExpanded = false; onRemove() }
            )
        }
    }
}

@Composable
private fun completedMetadataText(item: DownloadEntity, fileExists: Boolean): String {
    val status = if (fileExists) {
        stringResource(
            R.string.downloads_completed_metadata,
            formatBytes(item.sizeBytes),
            stringResource(R.string.downloads_status_completed)
        )
    } else {
        stringResource(R.string.downloads_status_file_missing)
    }
    val date = item.completedAt?.let {
        formatDownloadDate(
            timestamp = it,
            todayLabel = stringResource(R.string.downloads_date_today),
            yesterdayLabel = stringResource(R.string.downloads_date_yesterday)
        )
    }
    return listOfNotNull(status, date).joinToString("\n")
}

@Composable
private fun DownloadItemMenu(
    expanded: Boolean,
    boundaryBottom: Int,
    onDismissRequest: () -> Unit,
    onShare: () -> Unit,
    onInformation: () -> Unit,
    onRemove: () -> Unit
) {
    if (!expanded) return
    val menuYOffset = dimensionResource(SdpR.dimen._9sdp)
    val density = LocalDensity.current
    val positionProvider = remember(boundaryBottom, density, menuYOffset) {
        DownloadMenuPositionProvider(
            verticalOffsetPx = with(density) { menuYOffset.roundToPx() },
            boundaryBottom = boundaryBottom
        )
    }
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true)
    ) {
        Column(
            modifier = Modifier
                .width(dimensionResource(SdpR.dimen._132sdp))
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp)))
                .background(colorResource(R.color.colors_333538))
                .padding(
                    horizontal = dimensionResource(SdpR.dimen._3sdp),
                    vertical = dimensionResource(SdpR.dimen._3sdp)
                )
        ) {
            DownloadMenuAction(
                iconRes = R.drawable.ic_menu_share,
                labelRes = R.string.downloads_action_share,
                onClick = onShare
            )
            DownloadMenuAction(
                iconRes = R.drawable.ic_download_info,
                labelRes = R.string.downloads_action_information,
                onClick = onInformation
            )
            DownloadMenuAction(
                iconRes = R.drawable.ic_download_trash,
                labelRes = R.string.downloads_action_remove_short,
                onClick = onRemove
            )
        }
    }
}

private class DownloadMenuPositionProvider(
    private val verticalOffsetPx: Int,
    private val boundaryBottom: Int
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val preferredX = when (layoutDirection) {
            LayoutDirection.Ltr -> anchorBounds.right - popupContentSize.width
            LayoutDirection.Rtl -> anchorBounds.left
        }
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val x = preferredX.coerceIn(0, maxX)

        val availableBottom = boundaryBottom
            .takeIf { it > 0 }
            ?.coerceAtMost(windowSize.height)
            ?: windowSize.height
        val preferredBelowY = anchorBounds.top + verticalOffsetPx
        val fitsBelow = preferredBelowY + popupContentSize.height <= availableBottom
        val preferredY = if (fitsBelow) {
            preferredBelowY
        } else {
            anchorBounds.top - popupContentSize.height - verticalOffsetPx
        }
        val maxY = (availableBottom - popupContentSize.height).coerceAtLeast(0)

        return IntOffset(x, preferredY.coerceIn(0, maxY))
    }
}

@Composable
private fun DownloadMenuAction(iconRes: Int, labelRes: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._34sdp))
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._6sdp)))
            .clickable(onClick = onClick)
            .padding(horizontal = dimensionResource(SdpR.dimen._9sdp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = colorResource(R.color.colors_FFFFFF),
            modifier = Modifier.size(dimensionResource(SdpR.dimen._15sdp))
        )
        Text(
            text = stringResource(labelRes),
            fontFamily = FontFamily(Font(R.font.inter_medium)),
            fontSize = with(LocalDensity.current) { dimensionResource(SspR.dimen._11ssp).toSp() },
            color = colorResource(R.color.colors_FFFFFF)
        )
    }
}

@Composable
private fun DownloadInformationDialog(item: DownloadEntity, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val metadata by produceState(
        initialValue = basicDownloadMetadata(item),
        key1 = item.id,
        key2 = item.path,
        key3 = item.status
    ) {
        value = withContext(Dispatchers.IO) { readDownloadMetadata(context, item) }
    }
    val unknown = stringResource(R.string.downloads_information_unknown)
    val informationRows = buildList {
        add(stringResource(R.string.downloads_information_file_name) to item.fileName)
        add(
            stringResource(R.string.downloads_information_size) to
                metadata.sizeBytes.takeIf { it > 0L }?.let(::formatBytes).orEmpty().ifBlank { unknown }
        )
        add(
            stringResource(R.string.downloads_information_date_time) to
                formatInformationDate(item.completedAt ?: item.createdAt)
        )
        when (metadata.kind) {
            DownloadFileKind.VIDEO -> {
                add(stringResource(R.string.downloads_information_resolution) to (metadata.resolution ?: unknown))
                add(stringResource(R.string.downloads_information_length) to (metadata.duration ?: unknown))
            }
            DownloadFileKind.IMAGE -> {
                add(stringResource(R.string.downloads_information_file_type) to metadata.displayType)
                add(stringResource(R.string.downloads_information_resolution) to (metadata.resolution ?: unknown))
            }
            DownloadFileKind.AUDIO -> {
                add(stringResource(R.string.downloads_information_file_type) to metadata.displayType)
                add(stringResource(R.string.downloads_information_length) to (metadata.duration ?: unknown))
            }
            DownloadFileKind.OTHER -> {
                add(stringResource(R.string.downloads_information_file_type) to metadata.displayType)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DismissibleDialogBackdrop(
            onDismissRequest = onDismiss,
            surfaceModifier = Modifier.padding(
                horizontal = dimensionResource(SdpR.dimen._12sdp)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = dimensionResource(SdpR.dimen._320sdp))
                    .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._18sdp)))
                    .background(colorResource(R.color.colors_333538))
                    .padding(
                        horizontal = dimensionResource(SdpR.dimen._12sdp),
                        vertical = dimensionResource(SdpR.dimen._17sdp)
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
            ) {
                Text(
                    text = stringResource(R.string.downloads_information_title),
                    fontFamily = FontFamily(Font(R.font.inter_medium)),
                    fontWeight = FontWeight.Medium,
                    fontSize = with(LocalDensity.current) {
                        dimensionResource(SspR.dimen._12ssp).toSp()
                    },
                    color = colorResource(R.color.colors_FFFFFF),
                    textAlign = TextAlign.Center
                )
                HorizontalDivider(
                    color = colorResource(R.color.colors_424447),
                    thickness = 0.5.dp
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
                ) {
                    informationRows.forEach { (label, value) ->
                        DownloadInformationRow(label = label, value = value)
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadInformationRow(label: String, value: String) {
    val labelFont = FontFamily(Font(R.font.inter_semibold))
    val valueFont = FontFamily(Font(R.font.inter_regular))
    val labelColor = colorResource(R.color.colors_FFFFFF)
    val valueColor = colorResource(R.color.colors_9B9C9E)
    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = labelColor,
                    fontFamily = labelFont,
                    fontWeight = FontWeight.SemiBold
                )
            ) {
                append(label)
                append(": ")
            }
            withStyle(
                SpanStyle(
                    color = valueColor,
                    fontFamily = valueFont,
                    fontWeight = FontWeight.Normal
                )
            ) {
                append(value)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        fontSize = with(LocalDensity.current) { dimensionResource(SspR.dimen._11ssp).toSp() }
    )
}

@Composable
private fun DownloadRemoveDialog(
    item: DownloadEntity,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val (title, description) = when (basicDownloadMetadata(item).kind) {
        DownloadFileKind.VIDEO -> R.string.downloads_remove_video_title to
            R.string.downloads_remove_video_description
        DownloadFileKind.IMAGE -> R.string.downloads_remove_image_title to
            R.string.downloads_remove_image_description
        DownloadFileKind.AUDIO -> R.string.downloads_remove_audio_title to
            R.string.downloads_remove_audio_description
        DownloadFileKind.OTHER -> R.string.downloads_remove_file_title to
            R.string.downloads_remove_description
    }
    RemoveConfirmationDialog(
        title = title,
        description = description,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

private enum class DownloadFileKind { VIDEO, IMAGE, AUDIO, OTHER }

private data class DownloadFileMetadata(
    val kind: DownloadFileKind,
    val sizeBytes: Long,
    val displayType: String,
    val resolution: String? = null,
    val duration: String? = null
)

private fun basicDownloadMetadata(item: DownloadEntity): DownloadFileMetadata {
    val extension = item.fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
    val mimeType = resolveDownloadMimeType(extension, item.mimeType)
    return DownloadFileMetadata(
        kind = downloadFileKind(mimeType, extension),
        sizeBytes = item.sizeBytes.coerceAtLeast(item.downloadedBytes),
        displayType = extension.uppercase(Locale.ROOT).ifBlank {
            mimeType.ifBlank { "-" }
        }
    )
}

private fun readDownloadMetadata(
    context: android.content.Context,
    item: DownloadEntity
): DownloadFileMetadata {
    val actualSize = DownloadStorage.length(context, item.path)
    val basic = basicDownloadMetadata(item).copy(
        sizeBytes = actualSize.takeIf { it > 0L }
            ?: item.sizeBytes.coerceAtLeast(item.downloadedBytes)
    )
    if (!DownloadStorage.exists(context, item.path) ||
        actualSize <= 0L
    ) return basic

    return when (basic.kind) {
        DownloadFileKind.VIDEO, DownloadFileKind.AUDIO -> {
            val retriever = MediaMetadataRetriever()
            try {
                DownloadStorage.setMediaDataSource(retriever, context, item.path)
                val duration = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()
                    ?.takeIf { it > 0L }
                    ?.let(::formatMediaDuration)
                val resolution = if (basic.kind == DownloadFileKind.VIDEO) {
                    videoResolution(retriever)
                } else null
                basic.copy(duration = duration, resolution = resolution)
            } catch (_: Exception) {
                basic
            } finally {
                retriever.release()
            }
        }
        DownloadFileKind.IMAGE -> {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            DownloadStorage.openInputStream(context, item.path)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }
            basic.copy(
                resolution = if (options.outWidth > 0 && options.outHeight > 0) {
                    "${options.outWidth}x${options.outHeight}"
                } else null
            )
        }
        DownloadFileKind.OTHER -> basic
    }
}

private fun videoResolution(retriever: MediaMetadataRetriever): String? {
    var width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        ?.toIntOrNull() ?: return null
    var height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        ?.toIntOrNull() ?: return null
    val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
        ?.toIntOrNull() ?: 0
    if (rotation == 90 || rotation == 270) {
        val originalWidth = width
        width = height
        height = originalWidth
    }
    return "${width}x${height}"
}

private fun resolveDownloadMimeType(extension: String, fallback: String): String {
    val override = when (extension) {
        "m4v" -> "video/mp4"
        "mkv" -> "video/x-matroska"
        "ts" -> "video/mp2t"
        "m4a" -> "audio/mp4"
        else -> null
    }
    return override
        ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        ?: fallback
}

private fun downloadFileKind(mimeType: String, extension: String): DownloadFileKind = when {
    mimeType.startsWith("video/", ignoreCase = true) ||
        extension in setOf("mp4", "m4v", "mkv", "webm", "mov", "avi", "ts", "flv") ->
        DownloadFileKind.VIDEO
    mimeType.startsWith("image/", ignoreCase = true) ||
        extension in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif") ->
        DownloadFileKind.IMAGE
    mimeType.startsWith("audio/", ignoreCase = true) ||
        extension in setOf("mp3", "m4a", "aac", "wav", "ogg", "flac", "opus") ->
        DownloadFileKind.AUDIO
    else -> DownloadFileKind.OTHER
}

private fun formatInformationDate(timestamp: Long): String =
    SimpleDateFormat("dd/MM/yyyy - HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

// ─── Failed Download Item ─────────────────────────────────────────────────────

@Composable
private fun FailedDownloadItem(
    item: DownloadEntity,
    fontMedium: FontFamily,
    fontRegular: FontFamily,
    onRetry: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DownloadThumbnail(
            fileName = item.fileName,
            filePath = item.path,
            mimeType = item.mimeType,
            videoUrl = item.url,
            thumbnailUrl = item.thumbnailUrl,
            requestHeaders = item.requestHeaders,
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._49sdp))
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp)))
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._2sdp))
        ) {
            Text(
                text = item.fileName,
                fontFamily = fontMedium,
                fontSize = with(LocalDensity.current) { dimensionResource(SspR.dimen._11ssp).toSp() },
                color = colorResource(R.color.colors_FFFFFF),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = friendlyError(item.errorMessage),
                fontFamily = fontRegular,
                fontSize = with(LocalDensity.current) { dimensionResource(SspR.dimen._8ssp).toSp() },
                color = colorResource(R.color.colors_9B9C9E),
                maxLines = 2
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))) {
            DownloadActionButton(
                iconRes = R.drawable.ic_resume,
                contentDescription = stringResource(R.string.progress_resume_label),
                onClick = onRetry
            )
            DownloadActionButton(
                iconRes = R.drawable.ic_close_x,
                contentDescription = stringResource(R.string.downloads_action_remove),
                onClick = onRemove
            )
        }
    }
}

// ─── Utilities ────────────────────────────────────────────────────────────────

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> String.format("%.1f GB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

private fun formatDownloadDate(timestamp: Long, todayLabel: String, yesterdayLabel: String): String {
    val locale = Locale.getDefault()
    val target = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance()
    val time = SimpleDateFormat("HH:mm", locale).format(Date(timestamp))
    if (sameDay(target, today)) return "$todayLabel, $time"

    today.add(Calendar.DAY_OF_YEAR, -1)
    if (sameDay(target, today)) return "$yesterdayLabel, $time"

    val pattern = if (target.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR)) {
        "MMM d"
    } else {
        "MMM d, yyyy"
    }
    return SimpleDateFormat(pattern, locale).format(Date(timestamp))
}

private fun sameDay(first: Calendar, second: Calendar): Boolean =
    first.get(Calendar.ERA) == second.get(Calendar.ERA) &&
        first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
        first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)

/** Map a technical download error to a short, user-facing message. */
@Composable
private fun friendlyError(raw: String?): String {
    val text = raw.orEmpty().lowercase()
    return when {
        text.isBlank() -> stringResource(R.string.download_error_generic)
        text.contains("space") || text.contains("enospc") ->
            stringResource(R.string.download_error_storage)
        text.contains("end of stream") || text.contains("timeout") ||
            text.contains("cannot fetch") || text.contains("failed to connect") ||
            text.contains("unable to resolve host") || text.contains("download failed") ->
            stringResource(R.string.download_error_connection)
        text.contains("not an hls") || text.contains("no playable") ||
            text.contains("no segments") || text.contains("fmp4") ||
            text.contains("unsupported") || text.contains("no mp4-compatible") ->
            stringResource(R.string.download_error_unsupported)
        else -> stringResource(R.string.download_error_generic)
    }
}

/**
 * Loads real media previews when possible and keeps a useful typed holder visible while loading
 * or when the downloaded format cannot provide a bitmap preview.
 */
@Composable
private fun DownloadThumbnail(
    fileName: String,
    filePath: String?,
    mimeType: String,
    videoUrl: String,
    thumbnailUrl: String,
    requestHeaders: String,
    showPlayOverlay: Boolean = false,
    showProgressOverlay: Boolean = false,
    progress: Float? = null,
    isProgressIndeterminate: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val extension = remember(fileName, filePath, videoUrl) {
        downloadFileExtension(fileName, filePath, videoUrl)
    }
    val kind = remember(mimeType, extension) {
        fileTypeKind(
            mimeType = resolveDownloadMimeType(extension, mimeType),
            extension = extension
        )
    }
    val duration by rememberVideoDuration(filePath, enabled = kind == FileTypeKind.VIDEO)

    val localMedia = remember(filePath) {
        DownloadStorage.coilModel(filePath)
    }
    val data: Any? = when (kind) {
        FileTypeKind.VIDEO -> thumbnailUrl.takeIf { it.isNotBlank() } ?: localMedia ?: videoUrl
        FileTypeKind.IMAGE -> localMedia ?: thumbnailUrl.takeIf { it.isNotBlank() } ?: videoUrl
        FileTypeKind.AUDIO -> thumbnailUrl.takeIf { it.isNotBlank() }
        else -> null
    }
    val model = data?.let { thumbnailData ->
        remember(thumbnailData, requestHeaders, kind, thumbnailUrl) {
            coil.request.ImageRequest.Builder(context)
                .data(thumbnailData)
                .apply {
                    if (kind == FileTypeKind.VIDEO && thumbnailUrl.isBlank()) {
                        decoderFactory(coil.decode.VideoFrameDecoder.Factory())
                    }
                    runCatching { org.json.JSONObject(requestHeaders) }.getOrNull()?.let { json ->
                        json.keys().forEach { name ->
                            if (name.equals("Cookie", true) || name.equals("User-Agent", true) ||
                                name.equals("Referer", true) || name.equals("Origin", true)) {
                                addHeader(name, json.optString(name))
                            }
                        }
                    }
                    (thumbnailData as? String)?.let { url ->
                        android.webkit.CookieManager.getInstance().getCookie(url)
                            ?.takeIf { it.isNotBlank() }
                            ?.let { setHeader("Cookie", it) }
                    }
                }
                .crossfade(true)
                .size(128)
                .memoryCacheKey("download_thumb_${kind.name}_${thumbnailData.hashCode()}")
                .build()
        }
    }

    Box(
        modifier = modifier.background(colorResource(R.color.colors_27272A)),
        contentAlignment = Alignment.Center
    ) {
        if (model == null) {
            DownloadThumbnailHolder(kind = kind, extension = extension)
        } else {
            SubcomposeAsyncImage(
                model = model,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { DownloadThumbnailHolder(kind = kind, extension = extension) },
                error = { DownloadThumbnailHolder(kind = kind, extension = extension) },
                success = { SubcomposeAsyncImageContent() }
            )
        }

        if (showProgressOverlay) {
            DownloadProgressOverlay(
                progress = progress,
                isIndeterminate = isProgressIndeterminate
            )
        } else if (showPlayOverlay && kind == FileTypeKind.VIDEO) {
            DownloadPlayOverlay()
        }

        if (kind == FileTypeKind.VIDEO) {
            duration?.let { durationText ->
                Text(
                    text = durationText,
                    fontFamily = FontFamily(Font(R.font.inter_medium)),
                    fontSize = with(LocalDensity.current) { dimensionResource(SspR.dimen._6ssp).toSp() },
                    color = colorResource(R.color.colors_FFFFFF),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(dimensionResource(SdpR.dimen._2sdp))
                        .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._2sdp)))
                        .background(colorResource(R.color.colors_161718).copy(alpha = 0.8f))
                        .padding(
                            horizontal = dimensionResource(SdpR.dimen._2sdp),
                            vertical = dimensionResource(SdpR.dimen._1sdp)
                        )
                )
            }
        }
    }
}

@Composable
private fun DownloadThumbnailHolder(kind: FileTypeKind, extension: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_27272A)),
        contentAlignment = Alignment.Center
    ) {
        FileTypeIcon(
            kind = kind,
            extension = extension,
            modifier = Modifier.size(dimensionResource(SdpR.dimen._28sdp))
        )
    }
}

@Composable
private fun DownloadProgressOverlay(progress: Float?, isIndeterminate: Boolean) {
    Box(
        modifier = Modifier
            .size(dimensionResource(SdpR.dimen._18sdp))
            .clip(CircleShape)
            .background(colorResource(R.color.colors_161718).copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        if (isIndeterminate) {
            CircularProgressIndicator(
                modifier = Modifier.size(dimensionResource(SdpR.dimen._12sdp)),
                color = colorResource(R.color.colors_FFFFFF),
                trackColor = colorResource(R.color.colors_9B9C9E).copy(alpha = 0.55f),
                strokeWidth = 1.5.dp
            )
        } else {
            CircularProgressIndicator(
                progress = { (progress ?: 0f).coerceIn(0f, 1f) },
                modifier = Modifier.size(dimensionResource(SdpR.dimen._12sdp)),
                color = colorResource(R.color.colors_FFFFFF),
                trackColor = colorResource(R.color.colors_9B9C9E).copy(alpha = 0.55f),
                strokeWidth = 1.5.dp
            )
        }
    }
}

@Composable
private fun DownloadPlayOverlay() {
    Box(
        modifier = Modifier
            .size(dimensionResource(SdpR.dimen._18sdp))
            .clip(CircleShape)
            .background(colorResource(R.color.colors_161718).copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_play),
            contentDescription = stringResource(R.string.downloads_open_video),
            tint = colorResource(R.color.colors_FFFFFF),
            modifier = Modifier.size(dimensionResource(SdpR.dimen._12sdp))
        )
    }
}

private fun downloadFileExtension(fileName: String, filePath: String?, url: String): String {
    val urlName = runCatching { android.net.Uri.parse(url).lastPathSegment }.getOrNull().orEmpty()
    return sequenceOf(fileName, filePath.orEmpty(), urlName)
        .map { it.substringBefore('?').substringBefore('#') }
        .map { java.io.File(it).extension.lowercase(Locale.ROOT) }
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
}

@Composable
private fun rememberVideoDuration(
    filePath: String?,
    enabled: Boolean
): androidx.compose.runtime.State<String?> {
    val context = LocalContext.current
    return produceState<String?>(initialValue = null, key1 = filePath, key2 = enabled) {
        if (!enabled) return@produceState
        val path = filePath?.takeIf { it.isNotBlank() } ?: return@produceState
        value = withContext(Dispatchers.IO) {
            if (!DownloadStorage.exists(context, path) ||
                DownloadStorage.length(context, path) <= 0L
            ) return@withContext null
            val retriever = MediaMetadataRetriever()
            runCatching {
                DownloadStorage.setMediaDataSource(retriever, context, path)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()
                    ?.takeIf { it > 0L }
                    ?.let(::formatMediaDuration)
            }.getOrNull().also { retriever.release() }
        }
    }
}

private fun formatMediaDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
