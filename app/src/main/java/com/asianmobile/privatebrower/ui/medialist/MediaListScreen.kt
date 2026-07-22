package com.asianmobile.privatebrower.ui.medialist

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.ads.ui.compose.AdType
import com.asianmobile.privatebrower.ads.ui.compose.BannerAd
import com.asianmobile.privatebrower.ads.ui.compose.NativeAdInternal
import com.asianmobile.privatebrower.ads.config.BANNER_MEDIA_BOTTOM_PREFIX
import com.asianmobile.privatebrower.ads.ui.interstitial.InterstitialUtil
import com.asianmobile.privatebrower.data.repository.MediaDeleteResult
import com.asianmobile.privatebrower.data.repository.MediaItem
import com.asianmobile.privatebrower.data.repository.MediaSource
import com.asianmobile.privatebrower.ui.component.FileTypeIcon
import com.asianmobile.privatebrower.ui.component.RemoveConfirmationDialog
import com.asianmobile.privatebrower.ui.component.lazyVerticalScrollbar
import com.asianmobile.privatebrower.ui.home.filestab.MediaCategory
import com.asianmobile.privatebrower.ui.mediaviewer.MediaViewerKind
import com.asianmobile.privatebrower.ui.permission.PermissionPolicy
import com.asianmobile.privatebrower.ui.permission.PermissionRequestDestination
import com.asianmobile.privatebrower.utils.dialog.PermissionRequireDialog
import com.asianmobile.privatebrower.utils.permission.AllFilesAccess
import com.asianmobile.privatebrower.utils.permission.BroadStorageAccess
import com.asianmobile.privatebrower.utils.permission.canShowPermissionRationale
import com.intuit.sdp.R as R_sdp
import com.intuit.ssp.R as R_ssp
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
import kotlin.math.roundToLong

private enum class PendingDeleteFlow {
    SINGLE,
    MEDIA_BATCH,
    MEDIA_LEGACY
}

@Composable
internal fun MediaLibraryScreen(
    onBack: () -> Unit,
    onOpenMedia: (MediaItem) -> Unit,
    mediaLibraryChanged: Boolean,
    onMediaLibraryChangeConsumed: () -> Unit,
    nativeAdScreenCode: String,
    viewModel: MediaListViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val visibleItems = remember(
        uiState.items,
        uiState.searchQuery,
        uiState.sortOrder
    ) { uiState.visibleItems }
    var detailsItem by remember { mutableStateOf<MediaItem?>(null) }
    var deleteConfirmationItem by remember { mutableStateOf<MediaItem?>(null) }
    var pendingDeleteItem by remember { mutableStateOf<MediaItem?>(null) }
    var pendingDeleteFlow by remember { mutableStateOf<PendingDeleteFlow?>(null) }
    var pendingLegacyDeletes by remember { mutableStateOf(emptyList<MediaItem>()) }
    var showPermissionSettingsDialog by remember { mutableStateOf(false) }
    var permissionAttemptCount by rememberSaveable {
        mutableIntStateOf(uiState.permissionRequestCount)
    }
    var isSelectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedMediaIds by rememberSaveable { mutableStateOf(longArrayOf()) }
    var showSelectionDeleteConfirmation by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.permissionRequestCount) {
        permissionAttemptCount = maxOf(permissionAttemptCount, uiState.permissionRequestCount)
    }

    LaunchedEffect(mediaLibraryChanged) {
        if (mediaLibraryChanged) {
            viewModel.onMediaLibraryChanged()
            onMediaLibraryChangeConsumed()
        }
    }

    val selectedMedia = remember(visibleItems, selectedMediaIds) {
        val selectedIds = selectedMediaIds.toSet()
        visibleItems.filter { it.id in selectedIds }
    }

    fun exitSelection() {
        isSelectionMode = false
        selectedMediaIds = longArrayOf()
        showSelectionDeleteConfirmation = false
    }

    fun completeSelectionDelete(success: Boolean) {
        pendingDeleteItem = null
        pendingDeleteFlow = null
        pendingLegacyDeletes = emptyList()
        viewModel.refresh()
        if (success) exitSelection()
        showDeleteResult(context, success)
    }

    var launchDeleteConsent: (android.content.IntentSender, PendingDeleteFlow) -> Unit = { _, _ -> }

    fun processLegacyDeletes(items: List<MediaItem>) {
        val item = items.firstOrNull() ?: run {
            completeSelectionDelete(true)
            return
        }
        viewModel.deleteItem(item, refreshOnSuccess = false) { result ->
            when (result) {
                MediaDeleteResult.Success -> processLegacyDeletes(items.drop(1))
                MediaDeleteResult.Failed -> completeSelectionDelete(false)
                is MediaDeleteResult.RequiresPermission -> {
                    pendingDeleteItem = item
                    pendingLegacyDeletes = items.drop(1)
                    launchDeleteConsent(result.intentSender, PendingDeleteFlow.MEDIA_LEGACY)
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        val access = resolveMediaAccess(context)
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

    val openDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { openUri(context, it, context.contentResolver.getType(it).orEmpty()) }
    }
    val allFilesAccessLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onAccessResolved(resolveMediaAccess(context))
    }

    fun openAllFilesAccessSettings() {
        InterstitialUtil.getInstance().openAd?.needShowOpenAds = false
        runCatching {
            allFilesAccessLauncher.launch(AllFilesAccess.settingsIntent(context))
        }.onFailure {
            openAppSettings(context)
        }
    }

    val deleteConsentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val item = pendingDeleteItem
        val flow = pendingDeleteFlow
        pendingDeleteItem = null
        pendingDeleteFlow = null
        if (result.resultCode != Activity.RESULT_OK) {
            pendingLegacyDeletes = emptyList()
            return@rememberLauncherForActivityResult
        }

        when (flow) {
            PendingDeleteFlow.MEDIA_BATCH -> processLegacyDeletes(pendingLegacyDeletes)
            PendingDeleteFlow.MEDIA_LEGACY -> {
                if (item == null) {
                    completeSelectionDelete(false)
                } else {
                    viewModel.deleteItem(item, refreshOnSuccess = false) { deleteResult ->
                        if (deleteResult == MediaDeleteResult.Success) {
                            processLegacyDeletes(pendingLegacyDeletes)
                        } else {
                            completeSelectionDelete(false)
                        }
                    }
                }
            }
            PendingDeleteFlow.SINGLE -> if (item != null) {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    viewModel.deleteItem(item) { deleteResult ->
                        showDeleteResult(context, deleteResult == MediaDeleteResult.Success)
                    }
                } else {
                    viewModel.refresh()
                    showDeleteResult(context, true)
                }
            }
            null -> Unit
        }
    }

    launchDeleteConsent = { intentSender, flow ->
        pendingDeleteFlow = flow
        InterstitialUtil.getInstance().openAd?.needShowOpenAds = false
        deleteConsentLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
    }

    fun deleteItem(item: MediaItem) {
        if (canDeleteDirectly(context)) {
            viewModel.deleteFilesDirectly(listOf(item)) { success ->
                viewModel.refresh()
                showDeleteResult(context, success)
            }
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            item.source == MediaSource.MEDIA_STORE
        ) {
            val uri = item.uri ?: run {
                showDeleteResult(context, false)
                return
            }
            runCatching {
                MediaStore.createDeleteRequest(
                    context.contentResolver,
                    listOf(uri)
                ).intentSender
            }.onSuccess {
                pendingDeleteItem = item
                launchDeleteConsent(it, PendingDeleteFlow.SINGLE)
            }
                .onFailure { showDeleteResult(context, false) }
        } else {
            viewModel.deleteItem(item) { result ->
                when (result) {
                    MediaDeleteResult.Success -> showDeleteResult(context, true)
                    MediaDeleteResult.Failed -> showDeleteResult(context, false)
                    is MediaDeleteResult.RequiresPermission -> {
                        pendingDeleteItem = item
                        launchDeleteConsent(result.intentSender, PendingDeleteFlow.SINGLE)
                    }
                }
            }
        }
    }

    fun deleteSelectedMedia(items: List<MediaItem>) {
        if (items.isEmpty()) return
        if (canDeleteDirectly(context)) {
            viewModel.deleteFilesDirectly(items, ::completeSelectionDelete)
            return
        }

        val mediaStoreItems = items.filter {
            it.source == MediaSource.MEDIA_STORE && it.uri != null
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            mediaStoreItems.isNotEmpty()
        ) {
            val uris = mediaStoreItems.mapNotNull(MediaItem::uri)
            runCatching {
                MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
            }.onSuccess {
                pendingLegacyDeletes = items.filterNot(mediaStoreItems::contains)
                launchDeleteConsent(it, PendingDeleteFlow.MEDIA_BATCH)
            }.onFailure {
                completeSelectionDelete(false)
            }
        } else {
            processLegacyDeletes(items)
        }
    }

    DisposableEffect(lifecycleOwner, uiState.mediaType) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.onScreenResumed(resolveMediaAccess(context))
                }
                Lifecycle.Event.ON_STOP -> viewModel.pauseAudio()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(visibleItems, isSelectionMode) {
        if (!isSelectionMode) return@LaunchedEffect
        val visibleIds = visibleItems.mapTo(hashSetOf(), MediaItem::id)
        selectedMediaIds = selectedMediaIds.filter(visibleIds::contains).toLongArray()
        if (visibleItems.isEmpty()) exitSelection()
    }

    BackHandler(enabled = isSelectionMode || uiState.isSearchActive) {
        when {
            showSelectionDeleteConfirmation -> showSelectionDeleteConfirmation = false
            isSelectionMode -> exitSelection()
            else -> viewModel.onSearchDismissed()
        }
    }

    Scaffold(
        topBar = {
            MediaListHeader(
                uiState = uiState,
                itemCount = visibleItems.size,
                isSelectionMode = isSelectionMode,
                selectedMediaCount = selectedMedia.size,
                areAllItemsSelected = visibleItems.isNotEmpty() &&
                    selectedMedia.size == visibleItems.size,
                onBack = if (uiState.isSearchActive) {
                    viewModel::onSearchDismissed
                } else if (isSelectionMode) {
                    ::exitSelection
                } else {
                    onBack
                },
                onStartSelection = {
                    if (visibleItems.isNotEmpty()) isSelectionMode = true
                },
                onToggleAllItems = {
                    selectedMediaIds = if (selectedMedia.size == visibleItems.size) {
                        longArrayOf()
                    } else {
                        visibleItems.map(MediaItem::id).toLongArray()
                    }
                },
                onSearchStarted = viewModel::onSearchStarted,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onSortChanged = viewModel::onSortChanged,
                onViewModeChanged = viewModel::onViewModeChanged
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isSelectionMode) {
                    MediaSelectionBar(
                        selectedCount = selectedMedia.size,
                        onCancel = ::exitSelection,
                        onRemove = { showSelectionDeleteConfirmation = true }
                    )
                }
                BannerAd(
                    modifier = Modifier.fillMaxWidth(),
                    adPosition = "${BANNER_MEDIA_BOTTOM_PREFIX}_$nativeAdScreenCode"
                )
            }
        },
        containerColor = colorResource(R.color.colors_161718),
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        MediaListBody(
            uiState = uiState,
            visibleItems = visibleItems,
            nativeAdScreenCode = nativeAdScreenCode,
            isSelectionMode = isSelectionMode,
            selectedMediaIds = selectedMediaIds.toSet(),
            modifier = Modifier.padding(innerPadding),
            onRequestPermission = {
                if (AllFilesAccess.isSupported()) {
                    openAllFilesAccessSettings()
                } else {
                    val permissions = mediaLibraryPermissions()
                    if (permissions.isEmpty()) {
                        viewModel.onAccessResolved(resolveMediaAccess(context))
                    } else {
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
                }
            },
            onBrowseFiles = {
                InterstitialUtil.getInstance().openAd?.needShowOpenAds = false
                openDocumentLauncher.launch(arrayOf("*/*"))
            },
            onRetry = viewModel::refresh,
            onOpen = { item ->
                if (MediaViewerKind.from(item.mimeType, item.path.ifBlank { item.name }) ==
                    MediaViewerKind.OTHER
                ) {
                    openMediaFile(context, item)
                } else {
                    onOpenMedia(item)
                }
            },
            onShare = { shareMediaFile(context, it) },
            onDetails = { detailsItem = it },
            onDelete = { deleteConfirmationItem = it },
            onToggleAudioPlayback = viewModel::toggleAudioPlayback,
            onSeekAudio = viewModel::seekAudio,
            onToggleSelection = { item ->
                selectedMediaIds = if (item.id in selectedMediaIds) {
                    selectedMediaIds.filterNot { it == item.id }.toLongArray()
                } else {
                    selectedMediaIds + item.id
                }
            }
        )
    }

    detailsItem?.let { item ->
        MediaDetailsDialog(item = item, onDismiss = { detailsItem = null })
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

    if (showSelectionDeleteConfirmation && selectedMedia.isNotEmpty()) {
        val selectedCount = selectedMedia.size
        val title = when (uiState.mediaType) {
            MediaCategory.VIDEO -> R.plurals.media_list_remove_videos_title
            MediaCategory.MUSIC -> R.plurals.media_list_remove_audio_title
            MediaCategory.FILES -> R.plurals.media_list_remove_files_title
            else -> R.plurals.media_list_remove_photos_title
        }
        val description = when (uiState.mediaType) {
            MediaCategory.VIDEO -> R.plurals.media_list_remove_videos_message
            MediaCategory.MUSIC -> R.plurals.media_list_remove_audio_message
            MediaCategory.FILES -> R.plurals.media_list_remove_files_message
            else -> R.plurals.media_list_remove_photos_message
        }
        RemoveConfirmationDialog(
            title = pluralStringResource(
                title,
                selectedCount,
                selectedCount
            ),
            description = pluralStringResource(
                description,
                selectedCount,
                selectedCount
            ),
            onDismiss = { showSelectionDeleteConfirmation = false },
            onConfirm = {
                showSelectionDeleteConfirmation = false
                deleteSelectedMedia(selectedMedia)
            }
        )
    }

    deleteConfirmationItem?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteConfirmationItem = null },
            containerColor = colorResource(R.color.colors_212327),
            title = {
                Text(
                    text = stringResource(R.string.media_list_delete_title),
                    color = colorResource(R.color.colors_FFFFFF)
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.media_list_delete_message),
                    color = colorResource(R.color.colors_9B9C9E)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteConfirmationItem = null
                        deleteItem(item)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.media_list_delete),
                        color = colorResource(R.color.colors_D34C3E)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmationItem = null }) {
                    Text(
                        text = stringResource(R.string.media_list_cancel),
                        color = colorResource(R.color.colors_FFFFFF)
                    )
                }
            }
        )
    }
}

@Composable
private fun MediaListHeader(
    uiState: MediaListUiState,
    itemCount: Int,
    isSelectionMode: Boolean,
    selectedMediaCount: Int,
    areAllItemsSelected: Boolean,
    onBack: () -> Unit,
    onStartSelection: () -> Unit,
    onToggleAllItems: () -> Unit,
    onSearchStarted: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSortChanged: (MediaSortOrder) -> Unit,
    onViewModeChanged: (MediaViewMode) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var showMenu by remember { mutableStateOf(false) }
    val title = mediaTitle(uiState.mediaType)
    val supportsSelection = uiState.supportsSelection

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.colors_161718))
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R_sdp.dimen._43sdp))
                .padding(horizontal = dimensionResource(R_sdp.dimen._12sdp)),
            contentAlignment = Alignment.Center
        ) {
            MediaIconButton(
                iconRes = R.drawable.ic_arrow_back,
                contentDescription = stringResource(R.string.back),
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onBack()
                },
                modifier = Modifier.align(Alignment.CenterStart),
                size = if (supportsSelection) {
                    dimensionResource(R_sdp.dimen._22sdp)
                } else {
                    dimensionResource(R_sdp.dimen._31sdp)
                },
                iconSize = dimensionResource(R_sdp.dimen._22sdp)
            )

            if (uiState.isSearchActive) {
                MediaSearchField(
                    query = uiState.searchQuery,
                    onQueryChanged = onSearchQueryChanged,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(
                            start = dimensionResource(R_sdp.dimen._34sdp),
                            end = dimensionResource(R_sdp.dimen._4sdp)
                        )
                )
            } else if (supportsSelection && isSelectionMode) {
                Text(
                    text = mediaSelectionTitle(uiState.mediaType, selectedMediaCount),
                    fontFamily = FontFamily(Font(R.font.inter_semibold)),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = with(LocalDensity.current) {
                        dimensionResource(R_ssp.dimen._14ssp).toSp()
                    },
                    lineHeight = with(LocalDensity.current) {
                        dimensionResource(R_ssp.dimen._20ssp).toSp()
                    },
                    color = colorResource(R.color.colors_FFFFFF),
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = dimensionResource(R_sdp.dimen._34sdp))
                )
                MediaIconButton(
                    iconRes = R.drawable.ic_photo_select_all,
                    contentDescription = mediaSelectAllDescription(
                        type = uiState.mediaType,
                        areAllItemsSelected = areAllItemsSelected
                    ),
                    onClick = onToggleAllItems,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    size = dimensionResource(R_sdp.dimen._22sdp),
                    iconSize = dimensionResource(R_sdp.dimen._22sdp),
                    tint = Color.Unspecified
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = title,
                        fontFamily = FontFamily(Font(R.font.inter_semibold)),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = with(LocalDensity.current) {
                            dimensionResource(R_ssp.dimen._14ssp).toSp()
                        },
                        lineHeight = with(LocalDensity.current) {
                            dimensionResource(R_ssp.dimen._20ssp).toSp()
                        },
                        color = colorResource(R.color.colors_FFFFFF),
                        maxLines = 1
                    )
                    Text(
                        text = if (supportsSelection) {
                            itemCount.toString()
                        } else {
                            pluralStringResource(
                                R.plurals.media_list_item_count,
                                itemCount,
                                itemCount
                            )
                        },
                        fontFamily = FontFamily(Font(R.font.inter_regular)),
                        fontSize = with(LocalDensity.current) {
                            dimensionResource(R_ssp.dimen._9ssp).toSp()
                        },
                        lineHeight = with(LocalDensity.current) {
                            dimensionResource(R_ssp.dimen._12ssp).toSp()
                        },
                        color = colorResource(R.color.colors_9B9C9E),
                        maxLines = 1
                    )
                }

                if (supportsSelection) {
                    MediaIconButton(
                        iconRes = R.drawable.ic_photo_trash,
                        contentDescription = stringResource(R.string.remove),
                        onClick = onStartSelection,
                        modifier = Modifier.align(Alignment.CenterEnd),
                        size = dimensionResource(R_sdp.dimen._22sdp),
                        iconSize = dimensionResource(R_sdp.dimen._18sdp),
                        tint = Color.Unspecified
                    )
                } else {
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MediaIconButton(
                            iconRes = R.drawable.ic_tabler_search,
                            contentDescription = stringResource(R.string.media_list_search),
                            onClick = onSearchStarted
                        )
                        Box {
                            MediaIconButton(
                                iconRes = R.drawable.ic_mingcute_more,
                                contentDescription = stringResource(R.string.media_list_more),
                                onClick = { showMenu = true }
                            )
                            MediaOptionsMenu(
                                expanded = showMenu,
                                uiState = uiState,
                                onDismiss = { showMenu = false },
                                onSortChanged = {
                                    showMenu = false
                                    onSortChanged(it)
                                },
                                onViewModeChanged = {
                                    showMenu = false
                                    onViewModeChanged(it)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaSearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(R_sdp.dimen._31sdp))
            .clip(CircleShape)
            .background(colorResource(R.color.colors_212327))
            .padding(horizontal = dimensionResource(R_sdp.dimen._9sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.media_list_search),
                    color = colorResource(R.color.colors_6F7073),
                    fontFamily = FontFamily(Font(R.font.inter_regular)),
                    fontSize = with(LocalDensity.current) {
                        dimensionResource(R_ssp.dimen._11ssp).toSp()
                    }
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChanged,
                singleLine = true,
                textStyle = TextStyle(
                    color = colorResource(R.color.colors_FFFFFF),
                    fontFamily = FontFamily(Font(R.font.inter_regular)),
                    fontSize = with(LocalDensity.current) {
                        dimensionResource(R_ssp.dimen._11ssp).toSp()
                    }
                ),
                cursorBrush = SolidColor(colorResource(R.color.colors_3369FD)),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(Modifier.focusRequester(focusRequester))
            )
        }
        if (query.isNotEmpty()) {
            MediaIconButton(
                iconRes = R.drawable.ic_search_clear,
                contentDescription = stringResource(R.string.tabs_clear_search),
                onClick = { onQueryChanged("") },
                size = dimensionResource(R_sdp.dimen._24sdp),
                iconSize = dimensionResource(R_sdp.dimen._15sdp)
            )
        }
    }
}

@Composable
private fun MediaSelectionBar(
    selectedCount: Int,
    onCancel: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.colors_212327))
            .padding(dimensionResource(R_sdp.dimen._12sdp)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R_sdp.dimen._6sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MediaSelectionButton(
            text = stringResource(R.string.cancel),
            backgroundColor = colorResource(R.color.colors_424447),
            onClick = onCancel,
            modifier = Modifier.weight(1f)
        )
        if (selectedCount > 0) {
            MediaSelectionButton(
                text = stringResource(R.string.remove),
                backgroundColor = colorResource(R.color.colors_DC2222),
                onClick = onRemove,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MediaSelectionButton(
    text: String,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(dimensionResource(R_sdp.dimen._37sdp))
            .clip(RoundedCornerShape(dimensionResource(R_sdp.dimen._9sdp)))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = FontFamily(Font(R.font.inter_medium)),
            fontWeight = FontWeight.Medium,
            fontSize = with(LocalDensity.current) {
                dimensionResource(R_ssp.dimen._12ssp).toSp()
            },
            lineHeight = with(LocalDensity.current) {
                dimensionResource(R_ssp.dimen._18ssp).toSp()
            },
            color = colorResource(R.color.colors_FFFFFF)
        )
    }
}

@Composable
private fun MediaOptionsMenu(
    expanded: Boolean,
    uiState: MediaListUiState,
    onDismiss: () -> Unit,
    onSortChanged: (MediaSortOrder) -> Unit,
    onViewModeChanged: (MediaViewMode) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = colorResource(R.color.colors_212327),
        shape = RoundedCornerShape(dimensionResource(R_sdp.dimen._6sdp))
    ) {
        Text(
            text = stringResource(R.string.media_list_sort),
            modifier = Modifier.padding(
                horizontal = dimensionResource(R_sdp.dimen._12sdp),
                vertical = dimensionResource(R_sdp.dimen._4sdp)
            ),
            color = colorResource(R.color.colors_9B9C9E),
            fontSize = with(LocalDensity.current) {
                dimensionResource(R_ssp.dimen._9ssp).toSp()
            }
        )
        MediaSortOrder.entries.forEach { order ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = sortLabel(order),
                        color = colorResource(R.color.colors_FFFFFF)
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(
                            if (uiState.sortOrder == order) {
                                R.drawable.ic_radio_selected
                            } else {
                                R.drawable.ic_radio_unselected
                            }
                        ),
                        contentDescription = null,
                        tint = colorResource(R.color.colors_FFFFFF),
                        modifier = Modifier.size(dimensionResource(R_sdp.dimen._15sdp))
                    )
                },
                onClick = { onSortChanged(order) }
            )
        }
        if (uiState.supportsViewToggle) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(
                            if (uiState.viewMode == MediaViewMode.GRID) {
                                R.string.media_list_list_view
                            } else {
                                R.string.media_list_grid_view
                            }
                        ),
                        color = colorResource(R.color.colors_FFFFFF)
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(
                            if (uiState.viewMode == MediaViewMode.GRID) {
                                R.drawable.ic_media_list
                            } else {
                                R.drawable.ic_media_grid
                            }
                        ),
                        contentDescription = null,
                        tint = colorResource(R.color.colors_FFFFFF),
                        modifier = Modifier.size(dimensionResource(R_sdp.dimen._18sdp))
                    )
                },
                onClick = {
                    onViewModeChanged(
                        if (uiState.viewMode == MediaViewMode.GRID) {
                            MediaViewMode.LIST
                        } else {
                            MediaViewMode.GRID
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun MediaListBody(
    uiState: MediaListUiState,
    visibleItems: List<MediaItem>,
    nativeAdScreenCode: String,
    isSelectionMode: Boolean,
    selectedMediaIds: Set<Long>,
    modifier: Modifier,
    onRequestPermission: () -> Unit,
    onBrowseFiles: () -> Unit,
    onRetry: () -> Unit,
    onOpen: (MediaItem) -> Unit,
    onShare: (MediaItem) -> Unit,
    onDetails: (MediaItem) -> Unit,
    onDelete: (MediaItem) -> Unit,
    onToggleAudioPlayback: (MediaItem) -> Unit,
    onSeekAudio: (Long) -> Unit,
    onToggleSelection: (MediaItem) -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                uiState.accessState == MediaAccessState.CHECKING || uiState.isLoading -> {
                    LoadingState()
                }
                uiState.accessState == MediaAccessState.DENIED -> {
                    PermissionState(
                        mediaType = uiState.mediaType,
                        onRequestPermission = onRequestPermission,
                        onBrowseFiles = onBrowseFiles
                    )
                }
                uiState.hasError -> ErrorState(onRetry = onRetry)
                visibleItems.isEmpty() -> {
                    EmptyMediaState(
                        mediaType = uiState.mediaType,
                        hasSearchQuery = uiState.searchQuery.isNotBlank(),
                        onBrowseFiles = onBrowseFiles
                    )
                }
                uiState.mediaType == MediaCategory.IMAGES -> {
                    ImageGrid(
                        items = visibleItems,
                        nativeAdScreenCode = nativeAdScreenCode,
                        isSelectionMode = isSelectionMode,
                        selectedIds = selectedMediaIds,
                        onOpen = onOpen,
                        onToggleSelection = onToggleSelection
                    )
                }
                uiState.mediaType == MediaCategory.VIDEO -> {
                    VideoGrid(
                        items = visibleItems,
                        nativeAdScreenCode = nativeAdScreenCode,
                        isSelectionMode = isSelectionMode,
                        selectedIds = selectedMediaIds,
                        onOpen = onOpen,
                        onToggleSelection = onToggleSelection
                    )
                }
                uiState.mediaType == MediaCategory.MUSIC -> {
                    AudioList(
                        items = visibleItems,
                        nativeAdScreenCode = nativeAdScreenCode,
                        playback = uiState.audioPlayback,
                        isSelectionMode = isSelectionMode,
                        selectedIds = selectedMediaIds,
                        onOpen = onOpen,
                        onToggleSelection = onToggleSelection,
                        onTogglePlayback = onToggleAudioPlayback,
                        onSeek = onSeekAudio
                    )
                }
                else -> {
                    FileList(
                        items = visibleItems,
                        nativeAdScreenCode = nativeAdScreenCode,
                        isSelectionMode = isSelectionMode,
                        selectedIds = selectedMediaIds,
                        onOpen = onOpen,
                        onToggleSelection = onToggleSelection
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageGrid(
    items: List<MediaItem>,
    nativeAdScreenCode: String,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    onOpen: (MediaItem) -> Unit,
    onToggleSelection: (MediaItem) -> Unit
) {
    val gridState = rememberLazyGridState()
    LazyVerticalGrid(
        state = gridState,
        modifier = Modifier.mediaScrollbar(gridState),
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(
            horizontal = dimensionResource(R_sdp.dimen._6sdp),
            vertical = dimensionResource(R_sdp.dimen._9sdp)
        ),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R_sdp.dimen._3sdp)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R_sdp.dimen._3sdp))
    ) {
        items(if (isSelectionMode) items else items.take(8), key = MediaItem::id) { item ->
            ImageGridItem(
                item = item,
                isSelectionMode = isSelectionMode,
                isSelected = item.id in selectedIds,
                onOpen = onOpen,
                onToggleSelection = onToggleSelection
            )
        }
        if (!isSelectionMode && items.size > 8) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                NativeAdInternal(
                    screenCode = nativeAdScreenCode,
                    adTypeOverride = AdType.PHOTO_ITEM,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimensionResource(R_sdp.dimen._6sdp))
                )
            }
            items(items.drop(8), key = MediaItem::id) { item ->
                ImageGridItem(
                    item = item,
                    isSelectionMode = false,
                    isSelected = false,
                    onOpen = onOpen,
                    onToggleSelection = onToggleSelection
                )
            }
        }
    }
}

@Composable
private fun ImageGridItem(
    item: MediaItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onOpen: (MediaItem) -> Unit,
    onToggleSelection: (MediaItem) -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(dimensionResource(R_sdp.dimen._3sdp)))
            .background(colorResource(R.color.colors_2E2E2E))
            .clickable {
                if (isSelectionMode) onToggleSelection(item) else onOpen(item)
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.uri)
                .crossfade(true)
                .build(),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (isSelectionMode) {
            Icon(
                painter = painterResource(
                    if (isSelected) R.drawable.ic_photo_selected
                    else R.drawable.ic_photo_unselected
                ),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(dimensionResource(R_sdp.dimen._3sdp))
                    .size(dimensionResource(R_sdp.dimen._18sdp))
            )
        }
    }
}

@Composable
private fun VideoGrid(
    items: List<MediaItem>,
    nativeAdScreenCode: String,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    onOpen: (MediaItem) -> Unit,
    onToggleSelection: (MediaItem) -> Unit
) {
    val itemsBeforeAd = if (isSelectionMode) items else items.take(3)
    val gridState = rememberLazyGridState()

    LazyVerticalGrid(
        state = gridState,
        modifier = Modifier.mediaScrollbar(gridState),
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(
            horizontal = dimensionResource(R_sdp.dimen._6sdp),
            vertical = dimensionResource(R_sdp.dimen._9sdp)
        ),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R_sdp.dimen._6sdp)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R_sdp.dimen._6sdp))
    ) {
        items(itemsBeforeAd, key = MediaItem::id) { item ->
            VideoGridItem(
                item = item,
                isSelectionMode = isSelectionMode,
                isSelected = item.id in selectedIds,
                onOpen = onOpen,
                onToggleSelection = onToggleSelection
            )
        }
        if (!isSelectionMode) {
            item(
                key = "native_ad_$nativeAdScreenCode",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                NativeAdInternal(
                    screenCode = nativeAdScreenCode,
                    adTypeOverride = AdType.PHOTO_ITEM,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimensionResource(R_sdp.dimen._6sdp))
                )
            }
            items(items.drop(3), key = MediaItem::id) { item ->
                VideoGridItem(
                    item = item,
                    isSelectionMode = false,
                    isSelected = false,
                    onOpen = onOpen,
                    onToggleSelection = onToggleSelection
                )
            }
        }
    }
}

@Composable
private fun VideoGridItem(
    item: MediaItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onOpen: (MediaItem) -> Unit,
    onToggleSelection: (MediaItem) -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(dimensionResource(R_sdp.dimen._9sdp)))
            .background(colorResource(R.color.colors_2E2E2E))
            .clickable {
                if (isSelectionMode) onToggleSelection(item) else onOpen(item)
            }
    ) {
        MediaThumbnail(item = item)
        VideoMetadataChip(
            text = Formatter.formatShortFileSize(LocalContext.current, item.sizeBytes),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(dimensionResource(R_sdp.dimen._3sdp))
        )
        VideoMetadataChip(
            text = formatVideoGridDuration(item.duration),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(dimensionResource(R_sdp.dimen._3sdp))
        )
        if (isSelectionMode) {
            Icon(
                painter = painterResource(
                    if (isSelected) R.drawable.ic_photo_selected
                    else R.drawable.ic_photo_unselected
                ),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(dimensionResource(R_sdp.dimen._3sdp))
                    .size(dimensionResource(R_sdp.dimen._18sdp))
            )
        }
    }
}

@Composable
private fun VideoMetadataChip(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(dimensionResource(R_sdp.dimen._3sdp)))
            .background(colorResource(R.color.colors_000000).copy(alpha = 0.6f))
            .padding(horizontal = dimensionResource(R_sdp.dimen._3sdp)),
        color = colorResource(R.color.colors_FFFFFF),
        fontFamily = FontFamily(Font(R.font.inter_medium)),
        fontWeight = FontWeight.Medium,
        fontSize = with(LocalDensity.current) {
            dimensionResource(R_ssp.dimen._6ssp).toSp()
        },
        lineHeight = with(LocalDensity.current) {
            dimensionResource(R_ssp.dimen._9ssp).toSp()
        },
        maxLines = 1
    )
}

@Composable
private fun AudioList(
    items: List<MediaItem>,
    nativeAdScreenCode: String,
    playback: AudioPlaybackUiState,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    onOpen: (MediaItem) -> Unit,
    onToggleSelection: (MediaItem) -> Unit,
    onTogglePlayback: (MediaItem) -> Unit,
    onSeek: (Long) -> Unit
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier.mediaScrollbar(listState),
        contentPadding = PaddingValues(
            horizontal = dimensionResource(R_sdp.dimen._12sdp),
            vertical = dimensionResource(R_sdp.dimen._6sdp)
        )
    ) {
        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
            Column(modifier = Modifier.fillMaxWidth()) {
                AudioListItem(
                    item = item,
                    playback = playback,
                    isSelectionMode = isSelectionMode,
                    isSelected = item.id in selectedIds,
                    onOpen = onOpen,
                    onToggleSelection = onToggleSelection,
                    onTogglePlayback = onTogglePlayback,
                    onSeek = onSeek
                )
                if (index == 0 && !isSelectionMode) {
                    NativeAdInternal(
                        screenCode = nativeAdScreenCode,
                        adTypeOverride = AdType.PHOTO_ITEM,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = dimensionResource(R_sdp.dimen._6sdp),
                                bottom = dimensionResource(R_sdp.dimen._12sdp)
                            )
                    )
                }
                if (index < items.lastIndex) {
                    HorizontalDivider(
                        thickness = dimensionResource(R_sdp.dimen._1sdp),
                        color = colorResource(R.color.colors_212327)
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioListItem(
    item: MediaItem,
    playback: AudioPlaybackUiState,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onOpen: (MediaItem) -> Unit,
    onToggleSelection: (MediaItem) -> Unit,
    onTogglePlayback: (MediaItem) -> Unit,
    onSeek: (Long) -> Unit
) {
    val context = LocalContext.current
    val isActive = playback.activeItemId == item.id
    val date = remember(item.dateModified) { formatAudioDate(item.dateModified) }
    val size = remember(item.sizeBytes) {
        Formatter.formatShortFileSize(context, item.sizeBytes)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isSelectionMode) onToggleSelection(item) else onOpen(item)
            }
            .padding(vertical = dimensionResource(R_sdp.dimen._6sdp)),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Icon(
                        painter = painterResource(
                            if (isSelected) R.drawable.ic_audio_selected
                            else R.drawable.ic_audio_unselected
                        ),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(dimensionResource(R_sdp.dimen._15sdp))
                    )
                    Spacer(modifier = Modifier.width(dimensionResource(R_sdp.dimen._6sdp)))
                }
                Icon(
                    painter = painterResource(R.drawable.ic_audio_list),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(dimensionResource(R_sdp.dimen._37sdp))
                )
                Spacer(modifier = Modifier.width(dimensionResource(R_sdp.dimen._6sdp)))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(
                        dimensionResource(R_sdp.dimen._3sdp)
                    )
                ) {
                    Text(
                        text = item.name,
                        color = colorResource(R.color.colors_FFFFFF),
                        fontFamily = FontFamily(Font(R.font.inter_medium)),
                        fontWeight = FontWeight.Medium,
                        fontSize = with(LocalDensity.current) {
                            dimensionResource(R_ssp.dimen._11ssp).toSp()
                        },
                        lineHeight = with(LocalDensity.current) {
                            dimensionResource(R_ssp.dimen._15ssp).toSp()
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = date + stringResource(R.string.media_list_metadata_separator) + size,
                        color = colorResource(R.color.colors_6F7073),
                        fontFamily = FontFamily(Font(R.font.inter_regular)),
                        fontSize = with(LocalDensity.current) {
                            dimensionResource(R_ssp.dimen._9ssp).toSp()
                        },
                        lineHeight = with(LocalDensity.current) {
                            dimensionResource(R_ssp.dimen._12ssp).toSp()
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isActive) {
                AudioPlaybackProgress(
                    positionMs = playback.positionMs,
                    durationMs = playback.durationMs.takeIf { it > 0L } ?: item.duration,
                    startPadding = if (isSelectionMode) {
                        dimensionResource(R_sdp.dimen._58sdp)
                    } else {
                        dimensionResource(R_sdp.dimen._43sdp)
                    },
                    onSeek = onSeek
                )
            }
        }

        MediaIconButton(
            iconRes = if (isActive && playback.isPlaying) {
                R.drawable.ic_audio_pause
            } else {
                R.drawable.ic_audio_play
            },
            contentDescription = stringResource(
                if (isActive && playback.isPlaying) {
                    R.string.media_viewer_pause
                } else {
                    R.string.media_viewer_play
                }
            ),
            onClick = { onTogglePlayback(item) },
            size = dimensionResource(R_sdp.dimen._31sdp),
            iconSize = dimensionResource(R_sdp.dimen._15sdp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioPlaybackProgress(
    positionMs: Long,
    durationMs: Long,
    startPadding: androidx.compose.ui.unit.Dp,
    onSeek: (Long) -> Unit
) {
    val duration = durationMs.coerceAtLeast(1L)
    var sliderValue by remember { mutableFloatStateOf(positionMs.toFloat()) }
    var isSeeking by remember { mutableStateOf(false) }
    val trackHeight = dimensionResource(R_sdp.dimen._3sdp)
    val thumbSize = dimensionResource(R_sdp.dimen._6sdp)

    LaunchedEffect(positionMs, isSeeking) {
        if (!isSeeking) sliderValue = positionMs.toFloat()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = startPadding),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R_sdp.dimen._3sdp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AudioProgressTime(positionMs = sliderValue.roundToLong())
            AudioProgressTime(positionMs = durationMs)
        }
        Slider(
            value = sliderValue.coerceIn(0f, duration.toFloat()),
            onValueChange = {
                isSeeking = true
                sliderValue = it
                onSeek(it.roundToLong())
            },
            onValueChangeFinished = {
                isSeeking = false
                onSeek(sliderValue.roundToLong())
            },
            valueRange = 0f..duration.toFloat(),
            thumb = {
                Spacer(modifier = Modifier.size(thumbSize))
            },
            track = {
                Spacer(modifier = Modifier.fillMaxWidth().height(trackHeight))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(thumbSize)
                .drawAudioPlaybackTrack(
                    fraction = (sliderValue / duration.toFloat()).coerceIn(0f, 1f),
                    trackHeight = trackHeight,
                    thumbSize = thumbSize,
                    activeColor = colorResource(R.color.colors_3369FD),
                    inactiveColor = colorResource(R.color.colors_333538),
                    thumbColor = colorResource(R.color.colors_FFFFFF)
                )
        )
    }
}

@Composable
private fun AudioProgressTime(positionMs: Long) {
    Text(
        text = formatVideoGridDuration(positionMs),
        color = colorResource(R.color.colors_6F7073),
        fontFamily = FontFamily(Font(R.font.inter_regular)),
        fontSize = with(LocalDensity.current) {
            dimensionResource(R_ssp.dimen._8ssp).toSp()
        },
        lineHeight = with(LocalDensity.current) {
            dimensionResource(R_ssp.dimen._11ssp).toSp()
        }
    )
}

private fun Modifier.drawAudioPlaybackTrack(
    fraction: Float,
    trackHeight: androidx.compose.ui.unit.Dp,
    thumbSize: androidx.compose.ui.unit.Dp,
    activeColor: Color,
    inactiveColor: Color,
    thumbColor: Color
) = drawBehind {
    val centerY = size.height / 2f
    val strokeWidth = trackHeight.toPx()
    val trackRadius = strokeWidth / 2f
    val thumbRadius = thumbSize.toPx() / 2f
    val thumbCenterX = thumbRadius + fraction * (size.width - thumbRadius * 2f)

    drawLine(
        color = inactiveColor,
        start = Offset(trackRadius, centerY),
        end = Offset(size.width - trackRadius, centerY),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = activeColor,
        start = Offset(trackRadius, centerY),
        end = Offset(thumbCenterX, centerY),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawCircle(
        color = thumbColor,
        radius = thumbRadius,
        center = Offset(thumbCenterX, centerY)
    )
}

@Composable
private fun FileList(
    items: List<MediaItem>,
    nativeAdScreenCode: String,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    onOpen: (MediaItem) -> Unit,
    onToggleSelection: (MediaItem) -> Unit
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier.mediaScrollbar(listState),
        contentPadding = PaddingValues(
            horizontal = dimensionResource(R_sdp.dimen._12sdp),
            vertical = dimensionResource(R_sdp.dimen._6sdp)
        )
    ) {
        val firstItem = items.first()
        item(key = firstItem.id) {
            FileListItem(
                item = firstItem,
                isSelectionMode = isSelectionMode,
                isSelected = firstItem.id in selectedIds,
                onOpen = onOpen,
                onToggleSelection = onToggleSelection
            )
        }
        if (!isSelectionMode) {
            item(key = "native_ad_$nativeAdScreenCode") {
                NativeAdInternal(
                    screenCode = nativeAdScreenCode,
                    adTypeOverride = AdType.PHOTO_ITEM,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimensionResource(R_sdp.dimen._12sdp))
                )
            }
        }
        items(items.drop(1), key = MediaItem::id) { item ->
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(
                    thickness = dimensionResource(R_sdp.dimen._1sdp),
                    color = colorResource(R.color.colors_212327)
                )
                FileListItem(
                    item = item,
                    isSelectionMode = isSelectionMode,
                    isSelected = item.id in selectedIds,
                    onOpen = onOpen,
                    onToggleSelection = onToggleSelection
                )
            }
        }
    }
}

@Composable
private fun Modifier.mediaScrollbar(state: LazyListState): Modifier =
    lazyVerticalScrollbar(
        state = state,
        color = colorResource(R.color.colors_9B9C9E).copy(alpha = 0.72f),
        width = dimensionResource(R_sdp.dimen._2sdp),
        minThumbHeight = dimensionResource(R_sdp.dimen._24sdp),
        endPadding = dimensionResource(R_sdp.dimen._3sdp),
        verticalPadding = dimensionResource(R_sdp.dimen._6sdp),
        touchTargetWidth = dimensionResource(R_sdp.dimen._24sdp),
        layoutDirection = LocalLayoutDirection.current
    )

@Composable
private fun Modifier.mediaScrollbar(
    state: LazyGridState
): Modifier = lazyVerticalScrollbar(
    state = state,
    color = colorResource(R.color.colors_9B9C9E).copy(alpha = 0.72f),
    width = dimensionResource(R_sdp.dimen._2sdp),
    minThumbHeight = dimensionResource(R_sdp.dimen._24sdp),
    endPadding = dimensionResource(R_sdp.dimen._3sdp),
    verticalPadding = dimensionResource(R_sdp.dimen._6sdp),
    touchTargetWidth = dimensionResource(R_sdp.dimen._24sdp),
    layoutDirection = LocalLayoutDirection.current
)

@Composable
private fun FileListItem(
    item: MediaItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onOpen: (MediaItem) -> Unit,
    onToggleSelection: (MediaItem) -> Unit
) {
    val context = LocalContext.current
    val date = remember(item.dateModified) { formatAudioDate(item.dateModified) }
    val size = remember(item.sizeBytes) {
        Formatter.formatShortFileSize(context, item.sizeBytes)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isSelectionMode) onToggleSelection(item) else onOpen(item)
            }
            .padding(vertical = dimensionResource(R_sdp.dimen._6sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Icon(
                painter = painterResource(
                    if (isSelected) R.drawable.ic_audio_selected
                    else R.drawable.ic_audio_unselected
                ),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(dimensionResource(R_sdp.dimen._15sdp))
            )
            Spacer(modifier = Modifier.width(dimensionResource(R_sdp.dimen._6sdp)))
        }
        FileTypeIcon(
            fileName = item.name,
            filePath = item.path,
            mimeType = item.mimeType,
            modifier = Modifier.size(dimensionResource(R_sdp.dimen._28sdp))
        )
        Spacer(modifier = Modifier.width(dimensionResource(R_sdp.dimen._6sdp)))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R_sdp.dimen._3sdp))
        ) {
            Text(
                text = item.name,
                color = colorResource(R.color.colors_FFFFFF),
                fontFamily = FontFamily(Font(R.font.inter_medium)),
                fontWeight = FontWeight.Medium,
                fontSize = with(LocalDensity.current) {
                    dimensionResource(R_ssp.dimen._11ssp).toSp()
                },
                lineHeight = with(LocalDensity.current) {
                    dimensionResource(R_ssp.dimen._15ssp).toSp()
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = date + stringResource(R.string.media_list_metadata_separator) + size,
                color = colorResource(R.color.colors_6F7073),
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = with(LocalDensity.current) {
                    dimensionResource(R_ssp.dimen._9ssp).toSp()
                },
                lineHeight = with(LocalDensity.current) {
                    dimensionResource(R_ssp.dimen._12ssp).toSp()
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MediaRows(
    items: List<MediaItem>,
    mediaType: MediaCategory,
    onBrowseFiles: () -> Unit,
    onOpen: (MediaItem) -> Unit,
    onShare: (MediaItem) -> Unit,
    onDetails: (MediaItem) -> Unit,
    onDelete: (MediaItem) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(
            horizontal = dimensionResource(R_sdp.dimen._12sdp),
            vertical = dimensionResource(R_sdp.dimen._6sdp)
        ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R_sdp.dimen._3sdp))
    ) {
        if (mediaType == MediaCategory.FILES) {
            item {
                BrowseFilesRow(onClick = onBrowseFiles)
            }
        }
        items(items, key = MediaItem::id) { item ->
            MediaRow(
                item = item,
                mediaType = mediaType,
                onOpen = onOpen,
                onShare = onShare,
                onDetails = onDetails,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun MediaRow(
    item: MediaItem,
    mediaType: MediaCategory,
    onOpen: (MediaItem) -> Unit,
    onShare: (MediaItem) -> Unit,
    onDetails: (MediaItem) -> Unit,
    onDelete: (MediaItem) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(R_sdp.dimen._6sdp)))
            .clickable { onOpen(item) }
            .padding(vertical = dimensionResource(R_sdp.dimen._6sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MediaLeading(item = item, mediaType = mediaType)
        Spacer(modifier = Modifier.width(dimensionResource(R_sdp.dimen._9sdp)))
        Column(modifier = Modifier.weight(1f)) {
            MediaName(item.name)
            if (mediaType == MediaCategory.MUSIC && item.artist.isNotBlank()) {
                Text(
                    text = item.artist,
                    color = colorResource(R.color.colors_9B9C9E),
                    fontFamily = FontFamily(Font(R.font.inter_regular)),
                    fontSize = with(LocalDensity.current) {
                        dimensionResource(R_ssp.dimen._9ssp).toSp()
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            MediaMetadata(item, includeMimeType = mediaType == MediaCategory.FILES)
        }
        ItemActionsButton(item, onOpen, onShare, onDetails, onDelete)
    }
}

@Composable
private fun MediaLeading(item: MediaItem, mediaType: MediaCategory) {
    Box(
        modifier = Modifier
            .size(dimensionResource(R_sdp.dimen._42sdp))
            .clip(RoundedCornerShape(dimensionResource(R_sdp.dimen._6sdp)))
            .background(colorResource(R.color.colors_212327)),
        contentAlignment = Alignment.Center
    ) {
        if (mediaType == MediaCategory.IMAGES || mediaType == MediaCategory.VIDEO) {
            if (mediaType == MediaCategory.IMAGES) {
                AsyncImage(
                    model = item.uri,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                MediaThumbnail(item)
            }
        } else {
            Icon(
                painter = painterResource(
                    if (mediaType == MediaCategory.MUSIC) {
                        R.drawable.ic_audio_square
                    } else {
                        R.drawable.ic_document_text
                    }
                ),
                contentDescription = null,
                tint = if (mediaType == MediaCategory.MUSIC) {
                    colorResource(R.color.colors_DFC141)
                } else {
                    colorResource(R.color.colors_5DB95C)
                },
                modifier = Modifier.size(dimensionResource(R_sdp.dimen._22sdp))
            )
        }
    }
}

@Composable
private fun MediaThumbnail(item: MediaItem) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(item.uri)
            .crossfade(true)
            .decoderFactory(coil.decode.VideoFrameDecoder.Factory())
            .build(),
        contentDescription = item.name,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
        error = painterResource(R.drawable.ic_video_file),
        placeholder = painterResource(R.drawable.ic_video_file)
    )
}

@Composable
private fun MediaName(name: String) {
    Text(
        text = name,
        color = colorResource(R.color.colors_FFFFFF),
        fontFamily = FontFamily(Font(R.font.inter_medium)),
        fontWeight = FontWeight.Medium,
        fontSize = with(LocalDensity.current) {
            dimensionResource(R_ssp.dimen._10ssp).toSp()
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun MediaMetadata(item: MediaItem, includeMimeType: Boolean = false) {
    val context = LocalContext.current
    val date = remember(item.dateModified) { formatDate(context, item.dateModified) }
    val size = remember(item.sizeBytes) { Formatter.formatShortFileSize(context, item.sizeBytes) }
    val duration = if (item.duration > 0) formatDuration(item.duration) else null
    val type = if (includeMimeType) {
        item.mimeType.substringAfter('/').uppercase(Locale.getDefault())
    } else {
        null
    }
    val parts = listOfNotNull(type, duration, size, date)
    Text(
        text = parts.joinToString(separator = stringResource(R.string.media_list_metadata_separator)),
        color = colorResource(R.color.colors_9B9C9E),
        fontFamily = FontFamily(Font(R.font.inter_regular)),
        fontSize = with(LocalDensity.current) {
            dimensionResource(R_ssp.dimen._8ssp).toSp()
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun ItemActionsButton(
    item: MediaItem,
    onOpen: (MediaItem) -> Unit,
    onShare: (MediaItem) -> Unit,
    onDetails: (MediaItem) -> Unit,
    onDelete: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        MediaIconButton(
            iconRes = R.drawable.ic_mingcute_more,
            contentDescription = stringResource(R.string.media_list_more),
            onClick = { expanded = true },
            size = dimensionResource(R_sdp.dimen._28sdp),
            iconSize = dimensionResource(R_sdp.dimen._16sdp),
            background = colorResource(R.color.colors_000000).copy(alpha = 0.48f)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = colorResource(R.color.colors_212327)
        ) {
            ItemAction(R.drawable.ic_play, R.string.media_list_open) {
                expanded = false
                onOpen(item)
            }
            ItemAction(R.drawable.ic_share, R.string.media_list_share) {
                expanded = false
                onShare(item)
            }
            ItemAction(R.drawable.ic_media_info, R.string.media_list_details) {
                expanded = false
                onDetails(item)
            }
            ItemAction(R.drawable.ic_trash, R.string.media_list_delete) {
                expanded = false
                onDelete(item)
            }
        }
    }
}

@Composable
private fun ItemAction(iconRes: Int, labelRes: Int, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(text = stringResource(labelRes), color = colorResource(R.color.colors_FFFFFF))
        },
        leadingIcon = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = colorResource(R.color.colors_FFFFFF),
                modifier = Modifier.size(dimensionResource(R_sdp.dimen._18sdp))
            )
        },
        onClick = onClick
    )
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = colorResource(R.color.colors_3268FC))
    }
}

@Composable
private fun PermissionState(
    mediaType: MediaCategory,
    onRequestPermission: () -> Unit,
    onBrowseFiles: () -> Unit
) {
    val isFileManager = mediaType == MediaCategory.FILES
    MessageState(
        iconRes = R.drawable.ic_folder_empty,
        title = stringResource(R.string.media_list_permission_title),
        description = stringResource(R.string.media_list_permission_description),
        primaryLabel = stringResource(R.string.media_list_grant_access),
        onPrimaryClick = onRequestPermission,
        secondaryLabel = if (isFileManager) {
            stringResource(R.string.media_list_browse_device)
        } else {
            null
        },
        onSecondaryClick = onBrowseFiles
    )
}

@Composable
private fun ErrorState(onRetry: () -> Unit) {
    MessageState(
        iconRes = R.drawable.ic_refresh,
        title = stringResource(R.string.media_list_load_error),
        description = null,
        primaryLabel = stringResource(R.string.media_list_retry),
        onPrimaryClick = onRetry
    )
}

@Composable
private fun EmptyMediaState(
    mediaType: MediaCategory,
    hasSearchQuery: Boolean,
    onBrowseFiles: () -> Unit
) {
    val description = if (hasSearchQuery) {
        stringResource(R.string.media_list_search_no_results)
    } else {
        stringResource(
            when (mediaType) {
                MediaCategory.IMAGES -> R.string.media_list_images_empty_description
                MediaCategory.VIDEO -> R.string.media_list_videos_empty_description
                MediaCategory.MUSIC -> R.string.media_list_audio_empty_description
                MediaCategory.FILES -> R.string.media_list_files_empty_description
            }
        )
    }
    MessageState(
        iconRes = R.drawable.ic_folder_empty,
        title = stringResource(R.string.media_list_empty),
        description = description,
        primaryLabel = if (mediaType == MediaCategory.FILES && !hasSearchQuery) {
            stringResource(R.string.media_list_browse_device)
        } else {
            null
        },
        onPrimaryClick = onBrowseFiles
    )
}

@Composable
private fun MessageState(
    iconRes: Int,
    title: String,
    description: String?,
    primaryLabel: String?,
    onPrimaryClick: () -> Unit,
    secondaryLabel: String? = null,
    onSecondaryClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(R_sdp.dimen._24sdp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = colorResource(R.color.colors_9B9C9E),
            modifier = Modifier.size(dimensionResource(R_sdp.dimen._48sdp))
        )
        Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._12sdp)))
        Text(
            text = title,
            color = colorResource(R.color.colors_FFFFFF),
            fontFamily = FontFamily(Font(R.font.inter_semibold)),
            fontSize = with(LocalDensity.current) {
                dimensionResource(R_ssp.dimen._13ssp).toSp()
            },
            textAlign = TextAlign.Center
        )
        description?.let {
            Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._4sdp)))
            Text(
                text = it,
                color = colorResource(R.color.colors_9B9C9E),
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = with(LocalDensity.current) {
                    dimensionResource(R_ssp.dimen._10ssp).toSp()
                },
                textAlign = TextAlign.Center
            )
        }
        primaryLabel?.let {
            Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._12sdp)))
            Text(
                text = it,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colorResource(R.color.colors_3268FC))
                    .clickable(onClick = onPrimaryClick)
                    .padding(
                        horizontal = dimensionResource(R_sdp.dimen._18sdp),
                        vertical = dimensionResource(R_sdp.dimen._9sdp)
                    ),
                color = colorResource(R.color.colors_FFFFFF),
                fontFamily = FontFamily(Font(R.font.inter_semibold))
            )
        }
        secondaryLabel?.let {
            TextButton(onClick = onSecondaryClick) {
                Text(text = it, color = colorResource(R.color.colors_9B9C9E))
            }
        }
    }
}

@Composable
private fun BrowseFilesRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(R_sdp.dimen._6sdp)))
            .background(colorResource(R.color.colors_212327))
            .clickable(onClick = onClick)
            .padding(dimensionResource(R_sdp.dimen._9sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_media_browse),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(dimensionResource(R_sdp.dimen._24sdp))
        )
        Spacer(modifier = Modifier.width(dimensionResource(R_sdp.dimen._9sdp)))
        Text(
            text = stringResource(R.string.media_list_browse_device),
            color = colorResource(R.color.colors_FFFFFF),
            fontFamily = FontFamily(Font(R.font.inter_medium))
        )
    }
}

@Composable
private fun MediaDetailsDialog(item: MediaItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val details = buildList {
        add(stringResource(R.string.media_list_detail_name) to item.name)
        add(stringResource(R.string.media_list_detail_type) to item.mimeType)
        add(
            stringResource(R.string.media_list_detail_size) to
                Formatter.formatShortFileSize(context, item.sizeBytes)
        )
        add(
            stringResource(R.string.media_list_detail_modified) to
                formatDate(context, item.dateModified)
        )
        if (item.width > 0 && item.height > 0) {
            add(
                stringResource(R.string.media_list_detail_resolution) to
                    stringResource(R.string.media_list_resolution_format, item.width, item.height)
            )
        }
        if (item.duration > 0) {
            add(stringResource(R.string.media_list_detail_duration) to formatDuration(item.duration))
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colorResource(R.color.colors_212327),
        title = {
            Text(
                text = stringResource(R.string.media_list_details),
                color = colorResource(R.color.colors_FFFFFF)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R_sdp.dimen._6sdp))) {
                details.forEach { (label, value) ->
                    Column {
                        Text(
                            text = label,
                            color = colorResource(R.color.colors_9B9C9E),
                            fontSize = with(LocalDensity.current) {
                                dimensionResource(R_ssp.dimen._9ssp).toSp()
                            }
                        )
                        Text(
                            text = value,
                            color = colorResource(R.color.colors_FFFFFF),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.media_list_close),
                    color = colorResource(R.color.colors_FFFFFF)
                )
            }
        }
    )
}

@Composable
private fun MediaIconButton(
    iconRes: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = dimensionResource(R_sdp.dimen._31sdp),
    iconSize: androidx.compose.ui.unit.Dp = dimensionResource(R_sdp.dimen._18sdp),
    background: Color = Color.Transparent,
    tint: Color = colorResource(R.color.colors_FFFFFF)
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun mediaTitle(type: MediaCategory): String = stringResource(
    when (type) {
        MediaCategory.IMAGES -> R.string.media_list_images_title
        MediaCategory.VIDEO -> R.string.media_list_video_title
        MediaCategory.MUSIC -> R.string.media_list_music_title
        MediaCategory.FILES -> R.string.folder_files
    }
)

@Composable
private fun mediaSelectionTitle(type: MediaCategory, selectedCount: Int): String {
    if (selectedCount == 0) {
        return stringResource(
            when (type) {
                MediaCategory.VIDEO -> R.string.media_list_select_video
                MediaCategory.MUSIC -> R.string.media_list_select_audio
                MediaCategory.FILES -> R.string.media_list_select_file
                else -> R.string.media_list_select_photo
            }
        )
    }
    return pluralStringResource(
        when (type) {
            MediaCategory.VIDEO -> R.plurals.media_list_selected_videos
            MediaCategory.MUSIC -> R.plurals.media_list_selected_audio
            MediaCategory.FILES -> R.plurals.media_list_selected_files
            else -> R.plurals.media_list_selected_photos
        },
        selectedCount,
        selectedCount
    )
}

@Composable
private fun mediaSelectAllDescription(
    type: MediaCategory,
    areAllItemsSelected: Boolean
): String = stringResource(
    when (type) {
        MediaCategory.VIDEO -> if (areAllItemsSelected) {
            R.string.media_list_deselect_all_videos
        } else {
            R.string.media_list_select_all_videos
        }
        MediaCategory.MUSIC -> if (areAllItemsSelected) {
            R.string.media_list_deselect_all_audio
        } else {
            R.string.media_list_select_all_audio
        }
        MediaCategory.FILES -> if (areAllItemsSelected) {
            R.string.media_list_deselect_all_files
        } else {
            R.string.media_list_select_all_files
        }
        else -> if (areAllItemsSelected) {
            R.string.media_list_deselect_all_photos
        } else {
            R.string.media_list_select_all_photos
        }
    }
)

@Composable
private fun sortLabel(order: MediaSortOrder): String = stringResource(
    when (order) {
        MediaSortOrder.NEWEST -> R.string.media_list_sort_newest
        MediaSortOrder.OLDEST -> R.string.media_list_sort_oldest
        MediaSortOrder.NAME -> R.string.media_list_sort_name
        MediaSortOrder.SIZE -> R.string.media_list_sort_size
    }
)

private fun resolveMediaAccess(context: Context): MediaAccessState {
    return if (BroadStorageAccess.isGranted(context)) {
        MediaAccessState.GRANTED
    } else {
        MediaAccessState.DENIED
    }
}

internal fun mediaLibraryPermissions(): Array<String> =
    PermissionPolicy.onboardingStoragePermissions(Build.VERSION.SDK_INT)

// With MANAGE_EXTERNAL_STORAGE (API 30+) or legacy READ+WRITE storage (API <= 29) the app is
// exempt from scoped-storage write restrictions, so ContentResolver.delete() removes any file —
// including images/videos/audio owned by other apps — without a system delete-request dialog.
// The createDeleteRequest / RecoverableSecurityException paths only matter when access is missing.
private fun canDeleteDirectly(context: Context): Boolean =
    BroadStorageAccess.isGranted(context)

private fun openAppSettings(context: Context) {
    InterstitialUtil.getInstance().openAd?.needShowOpenAds = false
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:${context.packageName}".toUri()
        }
    )
}

private fun openMediaFile(context: Context, item: MediaItem) {
    val uri = item.uri ?: run {
        Toast.makeText(context, context.getString(R.string.files_open_error), Toast.LENGTH_SHORT).show()
        return
    }
    openUri(context, uri, item.mimeType)
}

private fun openUri(context: Context, uri: Uri, mimeType: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType.ifBlank { "*/*" })
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        InterstitialUtil.getInstance().openAd?.needShowOpenAds = false
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, context.getString(R.string.files_open_error), Toast.LENGTH_SHORT).show()
    }
}

private fun shareMediaFile(context: Context, item: MediaItem) {
    val uri = item.uri ?: run {
        Toast.makeText(
            context,
            context.getString(R.string.media_list_share_failed),
            Toast.LENGTH_SHORT
        ).show()
        return
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = item.mimeType.ifBlank { "*/*" }
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        InterstitialUtil.getInstance().openAd?.needShowOpenAds = false
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.media_list_share))
        )
    }.onFailure {
        Toast.makeText(
            context,
            context.getString(R.string.media_list_share_failed),
            Toast.LENGTH_SHORT
        ).show()
    }
}

private fun showDeleteResult(context: Context, success: Boolean) {
    Toast.makeText(
        context,
        context.getString(
            if (success) R.string.files_file_deleted else R.string.media_list_delete_failed
        ),
        Toast.LENGTH_SHORT
    ).show()
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

private fun formatVideoGridDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

private fun formatDate(context: Context, epochSeconds: Long): String =
    android.text.format.DateFormat.getMediumDateFormat(context)
        .format(Date(epochSeconds * 1000L))

private fun formatAudioDate(epochSeconds: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(epochSeconds * 1000L))

@Preview(showBackground = true, backgroundColor = 0xFF161718, widthDp = 360, heightDp = 720)
@Composable
private fun MediaListPreview() {
    val sampleItems = listOf(
        MediaItem(1, "Summer photo.jpg", "", null, "image/jpeg", 2_400_000, 1_720_000_000),
        MediaItem(2, "Travel video.mp4", "", null, "video/mp4", 18_000_000, 1_710_000_000, 92_000)
    )
    Column(modifier = Modifier.fillMaxSize().background(colorResource(R.color.colors_161718))) {
        MediaListHeader(
            uiState = MediaListUiState(
                items = sampleItems,
                mediaType = MediaCategory.IMAGES,
                accessState = MediaAccessState.GRANTED
            ),
            itemCount = sampleItems.size,
            isSelectionMode = false,
            selectedMediaCount = 0,
            areAllItemsSelected = false,
            onBack = {},
            onStartSelection = {},
            onToggleAllItems = {},
            onSearchStarted = {},
            onSearchQueryChanged = {},
            onSortChanged = {},
            onViewModeChanged = {}
        )
        ImageGrid(
            items = sampleItems,
            nativeAdScreenCode = "",
            isSelectionMode = false,
            selectedIds = emptySet(),
            onOpen = {},
            onToggleSelection = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF161718, widthDp = 360, heightDp = 720)
@Composable
private fun FileSelectionPreview() {
    val sampleItems = listOf(
        MediaItem(
            1,
            "Browser guide.pdf",
            "",
            null,
            "application/pdf",
            111_000,
            1_770_768_000
        ),
        MediaItem(
            2,
            "Private data.zip",
            "",
            null,
            "application/zip",
            2_400_000,
            1_770_681_600
        ),
        MediaItem(
            3,
            "Account list.xlsx",
            "",
            null,
            "application/vnd.ms-excel",
            95_000,
            1_770_595_200
        )
    )
    Column(modifier = Modifier.fillMaxSize().background(colorResource(R.color.colors_161718))) {
        MediaListHeader(
            uiState = MediaListUiState(
                items = sampleItems,
                mediaType = MediaCategory.FILES,
                accessState = MediaAccessState.GRANTED
            ),
            itemCount = sampleItems.size,
            isSelectionMode = true,
            selectedMediaCount = 1,
            areAllItemsSelected = false,
            onBack = {},
            onStartSelection = {},
            onToggleAllItems = {},
            onSearchStarted = {},
            onSearchQueryChanged = {},
            onSortChanged = {},
            onViewModeChanged = {}
        )
        Box(modifier = Modifier.weight(1f)) {
            FileList(
                items = sampleItems,
                nativeAdScreenCode = "",
                isSelectionMode = true,
                selectedIds = setOf(sampleItems.first().id),
                onOpen = {},
                onToggleSelection = {}
            )
        }
        MediaSelectionBar(selectedCount = 1, onCancel = {}, onRemove = {})
    }
}
