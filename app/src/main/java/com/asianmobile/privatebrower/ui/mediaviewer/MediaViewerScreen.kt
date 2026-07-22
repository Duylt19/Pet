package com.asianmobile.privatebrower.ui.mediaviewer

import android.app.Activity
import android.app.PictureInPictureParams
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.ui.component.DismissibleDialogBackdrop
import com.asianmobile.privatebrower.ui.component.RemoveConfirmationDialog
import com.asianmobile.privatebrower.ui.mediaviewer.MediaViewerEvent.ActionFailed
import com.asianmobile.privatebrower.ui.mediaviewer.MediaViewerEvent.DeletePermissionRequired
import com.asianmobile.privatebrower.ui.mediaviewer.MediaViewerEvent.Removed
import com.asianmobile.privatebrower.utils.applyAppOrientation
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong
import com.asianmobile.privatebrower.utils.ScreenName
import com.asianmobile.privatebrower.utils.TrackScreenView

@Composable
fun MediaViewerScreen(
    onBack: () -> Unit,
    onRemoved: () -> Unit,
    viewModel: MediaViewerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TrackScreenView(
        when (uiState.request.kind) {
            MediaViewerKind.IMAGE -> ScreenName.VIEWER_IMAGE
            MediaViewerKind.VIDEO -> ScreenName.VIEWER_VIDEO
            MediaViewerKind.AUDIO -> ScreenName.VIEWER_AUDIO
            MediaViewerKind.OTHER -> ScreenName.VIEWER_FILE
        }
    )
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    var showMenu by remember { mutableStateOf(false) }
    var showInformation by remember { mutableStateOf(false) }
    var showRemoveConfirmation by remember { mutableStateOf(false) }
    val hasViewerOverlay = showMenu || showInformation || showRemoveConfirmation

    val deleteConsentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onDeletePermissionResult(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(viewModel) {
        viewModel.showControls()
        viewModel.events.collect { event ->
            when (event) {
                Removed -> onRemoved()
                ActionFailed -> Toast.makeText(
                    context,
                    context.getString(R.string.media_viewer_action_failed),
                    Toast.LENGTH_SHORT
                ).show()
                is DeletePermissionRequired -> deleteConsentLauncher.launch(
                    IntentSenderRequest.Builder(event.intentSender).build()
                )
            }
        }
    }

    LaunchedEffect(activity, uiState.isFullscreen) {
        activity?.applyAppOrientation(fullscreenLandscape = uiState.isFullscreen)
    }

    LaunchedEffect(activity, hasViewerOverlay) {
        if (!hasViewerOverlay) {
            withFrameNanos { }
            activity?.hideMediaViewerSystemBars()
        }
    }

    DisposableEffect(activity) {
        val window = activity?.window
        val decorView = window?.decorView
        val insetsController = window?.let {
            WindowInsetsControllerCompat(it, it.decorView)
        }
        val rootInsets = decorView?.let(ViewCompat::getRootWindowInsets)
        val statusBarsWereVisible = rootInsets?.isVisible(WindowInsetsCompat.Type.statusBars()) ?: true
        val navigationBarsWereVisible =
            rootInsets?.isVisible(WindowInsetsCompat.Type.navigationBars()) ?: false
        val oldSystemBarsBehavior = insetsController?.systemBarsBehavior
        val oldLightStatusBars = insetsController?.isAppearanceLightStatusBars
        val oldLightNavigationBars = insetsController?.isAppearanceLightNavigationBars
        val windowFocusListener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) {
                activity?.hideMediaViewerSystemBars()
            }
        }
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.hideMediaViewerSystemBars()
        }
        decorView?.viewTreeObserver?.addOnWindowFocusChangeListener(windowFocusListener)
        onDispose {
            val viewTreeObserver = decorView?.viewTreeObserver
            if (viewTreeObserver?.isAlive == true) {
                viewTreeObserver.removeOnWindowFocusChangeListener(windowFocusListener)
            }
            activity?.applyAppOrientation(fullscreenLandscape = false)
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                insetsController?.apply {
                    oldSystemBarsBehavior?.let { systemBarsBehavior = it }
                    oldLightStatusBars?.let { isAppearanceLightStatusBars = it }
                    oldLightNavigationBars?.let { isAppearanceLightNavigationBars = it }
                    if (statusBarsWereVisible) {
                        show(WindowInsetsCompat.Type.statusBars())
                    } else {
                        hide(WindowInsetsCompat.Type.statusBars())
                    }
                    if (navigationBarsWereVisible) {
                        show(WindowInsetsCompat.Type.navigationBars())
                    } else {
                        hide(WindowInsetsCompat.Type.navigationBars())
                    }
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, activity) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> activity?.hideMediaViewerSystemBars()
                Lifecycle.Event.ON_STOP -> {
                    if (activity?.isInPictureInPictureMode != true) {
                        viewModel.pauseOutsidePictureInPicture()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler {
        if (uiState.isFullscreen) {
            setFullscreen(activity, viewModel, false)
        } else {
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_000000))
    ) {
        MediaContent(
            uiState = uiState,
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = uiState.controlsVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        colorResource(
                            if (uiState.request.kind == MediaViewerKind.IMAGE) {
                                R.color.transparent
                            } else {
                                R.color.colors_80000000
                            }
                        )
                    )
            ) {
                ViewerTopControls(
                    showMenu = showMenu,
                    onShowMenuChanged = {
                        showMenu = it
                        viewModel.showControls(keepVisible = it)
                    },
                    onBack = {
                        if (uiState.isFullscreen) setFullscreen(activity, viewModel, false)
                        else onBack()
                    },
                    onShare = {
                        showMenu = false
                        viewModel.showControls()
                        viewModel.share()
                    },
                    onInformation = {
                        showMenu = false
                        showInformation = true
                        viewModel.showControls(keepVisible = true)
                    },
                    onRemove = {
                        showMenu = false
                        showRemoveConfirmation = true
                        viewModel.showControls(keepVisible = true)
                    },
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                if (uiState.request.kind == MediaViewerKind.VIDEO ||
                    uiState.request.kind == MediaViewerKind.AUDIO
                ) {
                    PlaybackCenterControls(
                        uiState = uiState,
                        onRewind = { viewModel.skipBy(-SKIP_INTERVAL_MS) },
                        onTogglePlayback = viewModel::togglePlayback,
                        onForward = { viewModel.skipBy(SKIP_INTERVAL_MS) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                    PlaybackBottomControls(
                        uiState = uiState,
                        onSeek = viewModel::seekTo,
                        onToggleMute = viewModel::toggleMute,
                        onToggleCrop = viewModel::toggleVideoCrop,
                        onPictureInPicture = {
                            enterPictureInPicture(activity, uiState, viewModel)
                        },
                        onFullscreen = {
                            setFullscreen(activity, viewModel, !uiState.isFullscreen)
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }

        if (uiState.isBuffering && (uiState.request.kind == MediaViewerKind.VIDEO ||
                uiState.request.kind == MediaViewerKind.AUDIO)
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(dimensionResource(SdpR.dimen._28sdp))
                    .align(Alignment.Center),
                color = colorResource(R.color.colors_FFFFFF),
                strokeWidth = 2.dp
            )
        }

        if (uiState.hasPlaybackError) {
            Text(
                text = stringResource(R.string.media_viewer_playback_error),
                color = colorResource(R.color.colors_FFFFFF),
                fontFamily = FontFamily(Font(R.font.inter_medium)),
                fontSize = with(LocalDensity.current) {
                    dimensionResource(SspR.dimen._12ssp).toSp()
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = dimensionResource(SdpR.dimen._24sdp)),
                textAlign = TextAlign.Center
            )
        }
    }

    if (showInformation) {
        MediaViewerInformationDialog(
            uiState = uiState,
            onDismiss = {
                showInformation = false
                viewModel.showControls()
            }
        )
    }

    if (showRemoveConfirmation) {
        MediaViewerRemoveDialog(
            uiState = uiState,
            onDismiss = {
                showRemoveConfirmation = false
                viewModel.showControls()
            },
            onConfirm = {
                showRemoveConfirmation = false
                viewModel.remove()
            }
        )
    }
}

@Composable
private fun MediaContent(
    uiState: MediaViewerUiState,
    viewModel: MediaViewerViewModel,
    modifier: Modifier = Modifier
) {
    when (uiState.request.kind) {
        MediaViewerKind.VIDEO -> AndroidView(
            modifier = modifier.pointerInput(Unit) {
                detectTapGestures(onTap = { viewModel.toggleControls() })
            },
            factory = { context ->
                PlayerView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    resizeMode = if (uiState.isVideoCropped) {
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    } else {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                    setShutterBackgroundColor(ContextCompat.getColor(context, R.color.colors_000000))
                    player = viewModel.player
                }
            },
            update = {
                it.player = viewModel.player
                it.resizeMode = if (uiState.isVideoCropped) {
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            }
        )

        MediaViewerKind.IMAGE -> ZoomableImage(
            model = uiState.request.uri.ifBlank { uiState.request.path },
            onTap = viewModel::toggleControls,
            modifier = modifier
        )

        MediaViewerKind.AUDIO -> AudioArtwork(
            uiState = uiState,
            onTap = viewModel::toggleControls,
            modifier = modifier
        )

        MediaViewerKind.OTHER -> Box(
            modifier = modifier.background(colorResource(R.color.colors_000000)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.media_viewer_playback_error),
                color = colorResource(R.color.colors_FFFFFF)
            )
        }
    }
}

@Composable
private fun ZoomableImage(model: Any, onTap: () -> Unit, modifier: Modifier = Modifier) {
    var scale by remember(model) { mutableFloatStateOf(1f) }
    var offsetX by remember(model) { mutableFloatStateOf(0f) }
    var offsetY by remember(model) { mutableFloatStateOf(0f) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(1f, 5f)
        val maxX = viewportSize.width * (nextScale - 1f) / 2f
        val maxY = viewportSize.height * (nextScale - 1f) / 2f
        scale = nextScale
        offsetX = if (nextScale == 1f) 0f else (offsetX + panChange.x).coerceIn(-maxX, maxX)
        offsetY = if (nextScale == 1f) 0f else (offsetY + panChange.y).coerceIn(-maxY, maxY)
    }

    AsyncImage(
        model = model,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY
            )
            .transformable(transformableState)
            .pointerInput(model) {
                viewportSize = size
                detectTapGestures(
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else 2f
                        if (scale == 1f) {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    },
                    onTap = { onTap() }
                )
            }
    )
}

@Composable
private fun AudioArtwork(
    uiState: MediaViewerUiState,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(uiState.artwork) {
        uiState.artwork?.let { bytes ->
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }
    }
    Box(
        modifier = modifier
            .background(colorResource(R.color.colors_161718))
            .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) },
        contentAlignment = Alignment.Center
    ) {
        when {
            bitmap != null -> Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            uiState.request.thumbnailUrl.isNotBlank() -> AsyncImage(
                model = uiState.request.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            else -> Icon(
                painter = painterResource(R.drawable.ic_audio_square),
                contentDescription = null,
                tint = colorResource(R.color.colors_9B9C9E),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._72sdp))
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ViewerTopControls(
    showMenu: Boolean,
    onShowMenuChanged: (Boolean) -> Unit,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onInformation: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transparent = colorResource(R.color.transparent)
    val black = colorResource(R.color.colors_000000)
    val statusBarHeight = WindowInsets.statusBarsIgnoringVisibility
        .asPaddingValues()
        .calculateTopPadding()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(statusBarHeight + dimensionResource(SdpR.dimen._50sdp))
            .background(Brush.verticalGradient(listOf(black, transparent)))
            .padding(
                start = dimensionResource(SdpR.dimen._12sdp),
                top = statusBarHeight,
                end = dimensionResource(SdpR.dimen._12sdp)
            ),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ViewerIconButton(
            icon = R.drawable.ic_media_back,
            description = R.string.media_viewer_back,
            onClick = onBack,
            size = dimensionResource(SdpR.dimen._22sdp),
            contentAlignment = Alignment.TopStart
        )
        Box {
            ViewerIconButton(
                icon = R.drawable.ic_media_more,
                description = R.string.media_viewer_more,
                onClick = { onShowMenuChanged(true) },
                size = dimensionResource(SdpR.dimen._18sdp),
                contentAlignment = Alignment.TopEnd
            )
            ViewerMoreMenu(
                expanded = showMenu,
                onDismiss = { onShowMenuChanged(false) },
                onShare = onShare,
                onInformation = onInformation,
                onRemove = onRemove
            )
        }
    }
}

@Composable
private fun ViewerIconButton(
    icon: Int,
    description: Int,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp,
    contentAlignment: Alignment,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(dimensionResource(SdpR.dimen._34sdp))
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = contentAlignment
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(description),
            tint = colorResource(R.color.colors_FFFFFF),
            modifier = Modifier.size(size)
        )
    }
}

@Composable
private fun ViewerMoreMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onInformation: () -> Unit,
    onRemove: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .width(dimensionResource(SdpR.dimen._132sdp))
            .background(colorResource(R.color.colors_333538)),
        shape = RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp)),
        containerColor = colorResource(R.color.colors_333538),
        shadowElevation = 0.dp
    ) {
        ViewerMenuAction(R.drawable.ic_menu_share, R.string.downloads_action_share, onShare)
        ViewerMenuAction(
            R.drawable.ic_download_info,
            R.string.downloads_action_information,
            onInformation
        )
        ViewerMenuAction(
            R.drawable.ic_download_trash,
            R.string.downloads_action_remove_short,
            onRemove
        )
    }
}

@Composable
private fun ViewerMenuAction(icon: Int, text: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._34sdp))
            .clickable(onClick = onClick)
            .padding(horizontal = dimensionResource(SdpR.dimen._9sdp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = colorResource(R.color.colors_FFFFFF),
            modifier = Modifier.size(dimensionResource(SdpR.dimen._15sdp))
        )
        Text(
            text = stringResource(text),
            color = colorResource(R.color.colors_FFFFFF),
            fontFamily = FontFamily(Font(R.font.inter_medium)),
            fontSize = with(LocalDensity.current) {
                dimensionResource(SspR.dimen._11ssp).toSp()
            }
        )
    }
}

@Composable
private fun PlaybackCenterControls(
    uiState: MediaViewerUiState,
    onRewind: () -> Unit,
    onTogglePlayback: () -> Unit,
    onForward: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._32sdp))
    ) {
        ViewerPlaybackButton(
            icon = R.drawable.ic_media_rewind_5,
            description = R.string.media_viewer_rewind,
            onClick = onRewind,
            size = dimensionResource(SdpR.dimen._25sdp)
        )
        Box(
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._46sdp))
                .clip(CircleShape)
                .background(colorResource(R.color.colors_33FFFFFF))
                .clickable(onClick = onTogglePlayback),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    if (uiState.isPlaying) R.drawable.ic_media_pause else R.drawable.ic_media_play
                ),
                contentDescription = stringResource(
                    if (uiState.isPlaying) R.string.media_viewer_pause else R.string.media_viewer_play
                ),
                tint = colorResource(R.color.colors_FFFFFF),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._37sdp))
            )
        }
        ViewerPlaybackButton(
            icon = R.drawable.ic_media_forward_5,
            description = R.string.media_viewer_forward,
            onClick = onForward,
            size = dimensionResource(SdpR.dimen._25sdp)
        )
    }
}

@Composable
private fun ViewerPlaybackButton(
    icon: Int,
    description: Int,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp,
    tintColorRes: Int = R.color.colors_FFFFFF
) {
    Icon(
        painter = painterResource(icon),
        contentDescription = stringResource(description),
        tint = colorResource(tintColorRes),
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaybackBottomControls(
    uiState: MediaViewerUiState,
    onSeek: (Long, Boolean) -> Unit,
    onToggleMute: () -> Unit,
    onToggleCrop: () -> Unit,
    onPictureInPicture: () -> Unit,
    onFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transparent = colorResource(R.color.transparent)
    val black = colorResource(R.color.colors_000000)
    var sliderValue by remember(uiState.positionMs) {
        mutableFloatStateOf(uiState.positionMs.toFloat())
    }
    val duration = uiState.durationMs.coerceAtLeast(1L)
    val trackHeight = dimensionResource(SdpR.dimen._5sdp)
    val thumbSize = dimensionResource(SdpR.dimen._11sdp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._94sdp))
            .background(Brush.verticalGradient(listOf(transparent, black)))
            .padding(
                start = dimensionResource(SdpR.dimen._12sdp),
                end = dimensionResource(SdpR.dimen._12sdp),
                bottom = dimensionResource(SdpR.dimen._15sdp)
            ),
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            text = stringResource(
                R.string.media_viewer_time_format,
                formatMediaTime(sliderValue.roundToLong()),
                formatMediaTime(uiState.durationMs)
            ),
            color = colorResource(R.color.colors_FFFFFF),
            fontFamily = FontFamily(Font(R.font.inter_medium)),
            fontSize = with(LocalDensity.current) {
                dimensionResource(SspR.dimen._9ssp).toSp()
            },
            lineHeight = with(LocalDensity.current) {
                dimensionResource(SspR.dimen._12ssp).toSp()
            }
        )
        Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._6sdp)))
        Slider(
            value = sliderValue.coerceIn(0f, duration.toFloat()),
            onValueChange = {
                sliderValue = it
                onSeek(it.roundToLong(), false)
            },
            onValueChangeFinished = { onSeek(sliderValue.roundToLong(), true) },
            valueRange = 0f..duration.toFloat(),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(thumbSize)
                        .background(colorResource(R.color.colors_FFFFFF), CircleShape)
                )
            },
            track = {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(trackHeight)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(thumbSize)
                .drawPlaybackTrack(
                    fraction = (sliderValue / duration.toFloat()).coerceIn(0f, 1f),
                    trackHeight = trackHeight,
                    thumbSize = thumbSize,
                    activeColor = colorResource(R.color.colors_3369FD),
                    inactiveColor = colorResource(R.color.colors_FFFFFF)
                )
        )
        Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(SdpR.dimen._22sdp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ViewerPlaybackButton(
                icon = if (uiState.isMuted) R.drawable.ic_media_volume_off
                else R.drawable.ic_media_volume,
                description = R.string.media_viewer_volume,
                onClick = onToggleMute,
                size = dimensionResource(SdpR.dimen._18sdp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
            ) {
                if (uiState.request.kind == MediaViewerKind.VIDEO) {
                    ViewerPlaybackButton(
                        icon = R.drawable.ic_media_crop,
                        description = if (uiState.isVideoCropped) {
                            R.string.media_viewer_fit_video
                        } else {
                            R.string.media_viewer_crop_video
                        },
                        onClick = onToggleCrop,
                        size = dimensionResource(SdpR.dimen._22sdp),
                        tintColorRes = if (uiState.isVideoCropped) {
                            R.color.colors_3369FD
                        } else {
                            R.color.colors_FFFFFF
                        }
                    )
                }
                ViewerPlaybackButton(
                    icon = R.drawable.ic_media_picture_in_picture,
                    description = R.string.media_viewer_picture_in_picture,
                    onClick = onPictureInPicture,
                    size = dimensionResource(SdpR.dimen._22sdp)
                )
                ViewerPlaybackButton(
                    icon = if (uiState.isFullscreen) R.drawable.ic_fullscreen_exit
                    else R.drawable.ic_media_fullscreen,
                    description = R.string.media_viewer_fullscreen,
                    onClick = onFullscreen,
                    size = dimensionResource(SdpR.dimen._22sdp)
                )
            }
        }
    }
}

private fun Modifier.drawPlaybackTrack(
    fraction: Float,
    trackHeight: androidx.compose.ui.unit.Dp,
    thumbSize: androidx.compose.ui.unit.Dp,
    activeColor: androidx.compose.ui.graphics.Color,
    inactiveColor: androidx.compose.ui.graphics.Color
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
}

@Composable
private fun MediaViewerInformationDialog(
    uiState: MediaViewerUiState,
    onDismiss: () -> Unit
) {
    val unknown = stringResource(R.string.downloads_information_unknown)
    val metadata = uiState.metadata
    val rows = buildList {
        add(stringResource(R.string.downloads_information_file_name) to uiState.request.name)
        add(
            stringResource(R.string.downloads_information_size) to
                metadata.sizeBytes.takeIf { it > 0L }?.let(::formatBytes).orEmpty().ifBlank { unknown }
        )
        add(
            stringResource(R.string.downloads_information_date_time) to
                uiState.request.modifiedAt.takeIf { it > 0L }?.let(::formatDate).orEmpty().ifBlank { unknown }
        )
        add(stringResource(R.string.downloads_information_file_type) to metadata.displayType.ifBlank { unknown })
        if (uiState.request.kind != MediaViewerKind.AUDIO && metadata.width > 0 && metadata.height > 0) {
            add(stringResource(R.string.downloads_information_resolution) to "${metadata.width}x${metadata.height}")
        }
        if (uiState.request.kind == MediaViewerKind.VIDEO ||
            uiState.request.kind == MediaViewerKind.AUDIO
        ) {
            add(
                stringResource(R.string.downloads_information_length) to
                    metadata.durationMs.takeIf { it > 0L }?.let(::formatMediaTime).orEmpty()
                        .ifBlank { unknown }
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ViewerDialogSurface(onDismissRequest = onDismiss) {
            Text(
                text = stringResource(R.string.downloads_information_title),
                color = colorResource(R.color.colors_FFFFFF),
                fontFamily = FontFamily(Font(R.font.inter_medium)),
                fontSize = with(LocalDensity.current) {
                    dimensionResource(SspR.dimen._12ssp).toSp()
                },
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            rows.forEach { (label, value) -> ViewerInformationRow(label, value) }
        }
    }
}

@Composable
private fun ViewerInformationRow(label: String, value: String) {
    val labelColor = colorResource(R.color.colors_FFFFFF)
    val valueColor = colorResource(R.color.colors_9B9C9E)
    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = labelColor,
                    fontFamily = FontFamily(Font(R.font.inter_semibold)),
                    fontWeight = FontWeight.SemiBold
                )
            ) {
                append(label)
                append(": ")
            }
            withStyle(
                SpanStyle(
                    color = valueColor,
                    fontFamily = FontFamily(Font(R.font.inter_regular))
                )
            ) { append(value) }
        },
        modifier = Modifier.fillMaxWidth(),
        fontSize = with(LocalDensity.current) {
            dimensionResource(SspR.dimen._11ssp).toSp()
        }
    )
}

@Composable
private fun MediaViewerRemoveDialog(
    uiState: MediaViewerUiState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val (title, downloadDescription) = when (uiState.request.kind) {
        MediaViewerKind.VIDEO -> R.string.downloads_remove_video_title to
            R.string.downloads_remove_video_description
        MediaViewerKind.IMAGE -> R.string.downloads_remove_image_title to
            R.string.downloads_remove_image_description
        MediaViewerKind.AUDIO -> R.string.downloads_remove_audio_title to
            R.string.downloads_remove_audio_description
        MediaViewerKind.OTHER -> R.string.downloads_remove_file_title to
            R.string.downloads_remove_description
    }
    RemoveConfirmationDialog(
        title = title,
        description = if (uiState.request.source == MediaViewerSource.DOWNLOADS) {
            downloadDescription
        } else {
            R.string.media_viewer_remove_device_description
        },
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@Composable
private fun ViewerDialogSurface(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    DismissibleDialogBackdrop(
        onDismissRequest = onDismissRequest,
        surfaceModifier = Modifier.padding(
            horizontal = dimensionResource(SdpR.dimen._12sdp)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._18sdp)))
                .background(colorResource(R.color.colors_333538))
                .padding(
                    horizontal = dimensionResource(SdpR.dimen._12sdp),
                    vertical = dimensionResource(SdpR.dimen._17sdp)
                ),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp)),
            content = content
        )
    }
}

private fun enterPictureInPicture(
    activity: Activity?,
    uiState: MediaViewerUiState,
    viewModel: MediaViewerViewModel
) {
    if (activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val sourceWidth = uiState.metadata.width.takeIf { it > 0 } ?: 16
    val sourceHeight = uiState.metadata.height.takeIf { it > 0 } ?: 9
    val aspectRatio = if (uiState.request.kind == MediaViewerKind.AUDIO) {
        1f
    } else {
        sourceWidth.toFloat() / sourceHeight.coerceAtLeast(1)
    }
    val ratio = when {
        aspectRatio > MAX_PIP_ASPECT_RATIO -> Rational(239, 100)
        aspectRatio < MIN_PIP_ASPECT_RATIO -> Rational(100, 239)
        uiState.request.kind == MediaViewerKind.AUDIO -> Rational(1, 1)
        else -> Rational(sourceWidth.coerceAtLeast(1), sourceHeight.coerceAtLeast(1))
    }
    viewModel.hideControls()
    runCatching {
        activity.enterPictureInPictureMode(
            PictureInPictureParams.Builder().setAspectRatio(ratio).build()
        )
    }.onFailure { viewModel.showControls() }
}

private fun setFullscreen(
    activity: Activity?,
    viewModel: MediaViewerViewModel,
    fullscreen: Boolean
) {
    activity ?: return
    activity.applyAppOrientation(fullscreenLandscape = fullscreen)
    viewModel.setFullscreen(fullscreen)
}

private fun Activity.hideMediaViewerSystemBars() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    WindowInsetsControllerCompat(window, window.decorView).apply {
        systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        isAppearanceLightStatusBars = false
        isAppearanceLightNavigationBars = false
        hide(WindowInsetsCompat.Type.systemBars())
    }
}

private fun formatMediaTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0L) / 1000L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> String.format(Locale.getDefault(), "%.2f GB", bytes / 1_073_741_824.0)
    bytes >= 1_048_576L -> String.format(Locale.getDefault(), "%.2f MB", bytes / 1_048_576.0)
    bytes >= 1_024L -> String.format(Locale.getDefault(), "%.2f KB", bytes / 1_024.0)
    else -> "$bytes B"
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("dd/MM/yyyy - HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

private const val SKIP_INTERVAL_MS = 5_000L
private const val MAX_PIP_ASPECT_RATIO = 2.39f
private const val MIN_PIP_ASPECT_RATIO = 1f / MAX_PIP_ASPECT_RATIO
