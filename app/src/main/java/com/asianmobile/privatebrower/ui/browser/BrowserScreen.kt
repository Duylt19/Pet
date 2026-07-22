package com.asianmobile.privatebrower.ui.browser

import android.app.Activity
import android.view.WindowManager
import android.content.Context
import android.content.Intent
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.utils.FeedbackLauncher
import com.asianmobile.privatebrower.utils.ToastHelper
import com.asianmobile.privatebrower.utils.ScreenName
import com.asianmobile.privatebrower.utils.TrackScreenView
import com.asianmobile.privatebrower.utils.applyAppOrientation
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR
import kotlinx.coroutines.delay

@Composable
fun BrowserScreen(
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTabs: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: BrowserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val context = LocalContext.current
    val webHostState = rememberBrowserWebHostState(
        activeSession = activeSession,
        shouldOpenAppSettings = viewModel::shouldOpenAppSettingsForWebPermission,
        onPermissionsRequested = viewModel::markWebPermissionsRequested
    )
    var browserNotice by remember { mutableStateOf<BrowserUiEvent?>(null) }
    activeSession?.let { session ->
        TrackScreenView(
            if (session.isIncognito) ScreenName.BROWSER_PRIVATE else ScreenName.BROWSER_NORMAL
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            browserNotice = event
        }
    }

    LaunchedEffect(browserNotice) {
        val currentNotice = browserNotice ?: return@LaunchedEffect
        val durationMillis = if (currentNotice is BrowserUiEvent.BookmarkRemoved) 10_000L else 4_000L
        delay(durationMillis)
        if (browserNotice == currentNotice) browserNotice = null
    }

    LaunchedEffect(webHostState.customView) {
        if (webHostState.customView != null) {
            viewModel.dismissMoreMenu()
        }
    }

    LaunchedEffect(activeSession?.id) {
        if (activeSession != null) {
            viewModel.refreshActiveTabIfNeeded()
        }
    }

    val isHtmlFullscreen = webHostState.customView != null
    DisposableEffect(context, isHtmlFullscreen) {
        val activity = context as? Activity
        activity?.applyAppOrientation(fullscreenLandscape = isHtmlFullscreen)
        onDispose {
            if (isHtmlFullscreen) {
                activity?.applyAppOrientation(fullscreenLandscape = false)
            }
        }
    }

    // On this edge-to-edge window, ADJUST_RESIZE makes the WebView resize whenever the keyboard
    // shows; on some OEMs (Samsung One UI, API 29) it then leaves the navigation-bar area stuck
    // and re-flows the WebView off-centre after dismiss. The URL bar is at the top, so nothing
    // needs to move for the keyboard — use ADJUST_NOTHING while the browser is open, then restore.
    DisposableEffect(context) {
        val window = (context as? Activity)?.window
        val previousMode = window?.attributes?.softInputMode
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        onDispose {
            if (window != null && previousMode != null) window.setSoftInputMode(previousMode)
        }
    }

    fun navigateAfterThumbnailCapture(navigate: () -> Unit) {
        viewModel.captureThumbnailBeforeLeaving()
        navigate()
    }

    // Handle System Back Button
    BackHandler(enabled = true) {
        when {
            webHostState.hideCustomView() -> Unit
            uiState.showFindInPage -> viewModel.hideFindInPage()
            uiState.showMoreMenu -> viewModel.dismissMoreMenu()
            !viewModel.goBack() -> {
                navigateAfterThumbnailCapture(onBack)
            }
        }
    }

    // Set up FindListener on the WebView
    DisposableEffect(activeSession) {
        val webView = activeSession?.webView
        webView?.setFindListener(object : WebView.FindListener {
            override fun onFindResultReceived(
                activeMatchOrdinal: Int,
                numberOfMatches: Int,
                isDoneCounting: Boolean
            ) {
                if (isDoneCounting) {
                    viewModel.onFindResultReceived(activeMatchOrdinal, numberOfMatches)
                }
            }
        })
        onDispose {
            webView?.setFindListener(null)
        }
    }

    // Long-press an image/link → show a save/copy context menu.
    DisposableEffect(activeSession) {
        val webView = activeSession?.webView
        webView?.setOnLongClickListener {
            val result = webView.hitTestResult
            when (result.type) {
                WebView.HitTestResult.IMAGE_TYPE -> {
                    viewModel.onLongPressContent(result.extra, null)
                    true
                }
                WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                    val imageUrl = result.extra
                    val handler = android.os.Handler(android.os.Looper.getMainLooper()) { msg ->
                        viewModel.onLongPressContent(imageUrl, msg.data.getString("url"))
                        true
                    }
                    webView.requestFocusNodeHref(handler.obtainMessage())
                    true
                }
                WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                    viewModel.onLongPressContent(null, result.extra)
                    true
                }
                else -> false
            }
        }
        onDispose { webView?.setOnLongClickListener(null) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(R.color.colors_1C1C1D))
                .statusBarsPadding()
        ) {
        // 1. Top URL / Address bar
        BrowserTopBar(
            url = uiState.url,
            isLoading = uiState.isLoading,
            onUrlSubmitted = viewModel::submitUrl,
            onHomeClick = { navigateAfterThumbnailCapture(onBack) }
        )

        // 2. Progress Indicator
        if (uiState.isLoading) {
            LinearProgressIndicator(
                progress = { uiState.progress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = colorResource(R.color.colors_3369FD),
                trackColor = colorResource(R.color.colors_333333)
            )
        }

        // 3. WebView Content + Find in Page overlay
        Box(modifier = Modifier.weight(1f)) {
            activeSession?.let { session ->
                BrowserTabWebViewHost(
                    session = session,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Video Download FAB
            if (uiState.detectedVideos.isNotEmpty()) {
                VideoDownloadFab(
                    count = uiState.detectedVideos.size,
                    onClick = { viewModel.showVideoSheet() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = dimensionResource(SdpR.dimen._16sdp),
                            bottom = dimensionResource(SdpR.dimen._16sdp)
                        )
                )
            }

            // Find in Page bar
            androidx.compose.animation.AnimatedVisibility(
                visible = uiState.showFindInPage,
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                FindInPageBar(
                    query = uiState.findInPageQuery,
                    currentMatch = uiState.findInPageCurrentMatch,
                    totalMatches = uiState.findInPageTotalMatches,
                    onQueryChange = viewModel::updateFindInPageQuery,
                    onNext = viewModel::findNext,
                    onPrevious = viewModel::findPrevious,
                    onClose = viewModel::hideFindInPage
                )
            }

            browserNotice?.let { notice ->
                BrowserNoticeBar(
                    event = notice,
                    onUndo = {
                        val removed = notice as? BrowserUiEvent.BookmarkRemoved
                        if (removed != null) viewModel.restoreBookmark(removed.bookmark)
                        browserNotice = null
                    },
                    onDismiss = { browserNotice = null },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            start = dimensionResource(SdpR.dimen._16sdp),
                            end = dimensionResource(SdpR.dimen._16sdp),
                            bottom = dimensionResource(SdpR.dimen._12sdp)
                        )
                )
            }
        }

        // 4. Bottom Navigation Toolbar
        BrowserBottomBar(
            canGoBack = uiState.canGoBack,
            canGoForward = uiState.canGoForward,
            tabCount = uiState.tabCount,
            isBookmarked = uiState.isBookmarked,
            canBookmark = uiState.canBookmark,
            showMoreMenu = uiState.showMoreMenu && webHostState.customView == null,
            onBackClick = { viewModel.goBack() },
            onForwardClick = { viewModel.goForward() },
            onBookmarkClick = viewModel::toggleBookmark,
            onTabsClick = { navigateAfterThumbnailCapture(onNavigateToTabs) },
            onMenuClick = viewModel::onMoreMenuClick,
            moreMenuContent = {
                BrowserMorePopup(
                    canGoBack = uiState.canGoBack,
                    canGoForward = uiState.canGoForward,
                    isDesktopMode = uiState.isDesktopMode,
                    isBookmarked = uiState.isBookmarked,
                    canBookmark = uiState.canBookmark,
                    onDismiss = viewModel::dismissMoreMenu,
                    onBackClick = { viewModel.goBack() },
                    onForwardClick = { viewModel.goForward() },
                    onBookmarkClick = viewModel::toggleBookmark,
                    onDownloadClick = {
                        if (uiState.detectedVideos.isNotEmpty()) {
                            viewModel.showVideoSheet()
                        } else {
                            navigateAfterThumbnailCapture(onNavigateToDownloads)
                        }
                    },
                    onReloadClick = { viewModel.reload() },
                    onNewTab = { viewModel.newTab() },
                    onNewIncognitoTab = { viewModel.newTab(incognito = true) },
                    onHistory = { navigateAfterThumbnailCapture(onNavigateToHistory) },
                    onBookmarkPage = viewModel::toggleBookmark,
                    onDownloads = { navigateAfterThumbnailCapture(onNavigateToDownloads) },
                    onFindInPage = viewModel::showFindInPage,
                    onShare = { sharePage(context, uiState.title, uiState.url) },
                    onToggleDesktopSite = viewModel::toggleDesktopMode,
                    onSettings = { navigateAfterThumbnailCapture(onNavigateToSettings) },
                    onHelpFeedback = { FeedbackLauncher.launch(context) }
                )
            }
        )
        }

        BrowserFullscreenContent(
            state = webHostState,
            modifier = Modifier.align(Alignment.Center)
        )

    }
    // Video Select Sheet
    if (
        uiState.showVideoSheet &&
        uiState.detectedVideos.isNotEmpty() &&
        webHostState.customView == null
    ) {
        VideoSelectBottomSheet(
            videos = uiState.detectedVideos,
            onDismiss = { viewModel.hideVideoSheet() },
            onDownload = { selected ->
                viewModel.downloadVideos(context, selected)
            }
        )
    }

    // Long-press save/copy menu for images and links.
    val linkMenu by viewModel.linkContextMenu.collectAsState()
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    linkMenu?.let { info ->
        LinkContextMenuSheet(
            info = info,
            onSaveImage = { url ->
                viewModel.saveUrl(url)
            },
            onCopy = { text ->
                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(text))
                viewModel.dismissLinkContextMenu()
                ToastHelper.show(context, context.getString(R.string.browser_ctx_copied))
            },
            onDismiss = { viewModel.dismissLinkContextMenu() }
        )
    }

    BrowserWebsitePermissionPrompts(webHostState)
}

private fun sharePage(context: Context, title: String, url: String) {
    if (!isBookmarkableUrl(url)) {
        ToastHelper.show(context, context.getString(R.string.browser_share_unavailable))
        return
    }

    val shareText = title
        .takeIf { it.isNotBlank() && it != url }
        ?.let { context.getString(R.string.browser_share_page_text, it, url) }
        ?: url
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    runCatching {
        context.startActivity(
            Intent.createChooser(
                shareIntent,
                context.getString(R.string.browser_share_chooser_title)
            )
        )
    }.onFailure {
        ToastHelper.show(context, context.getString(R.string.browser_share_no_app))
    }
}

@Composable
private fun BrowserNoticeBar(
    event: BrowserUiEvent,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val message = when (event) {
        BrowserUiEvent.BookmarkAdded -> stringResource(R.string.browser_bookmark_added)
        is BrowserUiEvent.BookmarkRemoved -> stringResource(R.string.browser_bookmark_removed)
        BrowserUiEvent.BookmarkRestored -> stringResource(R.string.browser_bookmark_restored)
        BrowserUiEvent.BookmarkUnavailable -> stringResource(R.string.browser_bookmark_unavailable)
        BrowserUiEvent.BookmarkOperationFailed -> stringResource(R.string.browser_bookmark_failed)
        is BrowserUiEvent.TabLimitReached -> stringResource(
            if (event.isIncognito) {
                R.string.browser_incognito_tab_limit
            } else {
                R.string.browser_normal_tab_limit
            },
            event.maxTabs
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(SdpR.dimen._4sdp)),
        color = colorResource(R.color.colors_333333),
        contentColor = colorResource(R.color.colors_FFFFFF),
        shadowElevation = dimensionResource(SdpR.dimen._4sdp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = dimensionResource(SdpR.dimen._48sdp))
                .padding(
                    start = dimensionResource(SdpR.dimen._16sdp),
                    end = dimensionResource(SdpR.dimen._4sdp)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = colorResource(R.color.colors_FFFFFF),
                modifier = Modifier.weight(1f)
            )

            if (event is BrowserUiEvent.BookmarkRemoved) {
                TextButton(onClick = onUndo) {
                    Text(
                        text = stringResource(R.string.browser_bookmark_undo),
                        color = colorResource(R.color.colors_005DFD)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(dimensionResource(SdpR.dimen._40sdp))
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.browser_notice_dismiss),
                    tint = colorResource(R.color.colors_FFFFFF),
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._20sdp))
                )
            }
        }
    }
}

@Composable
fun BrowserTopBar(
    url: String,
    isLoading: Boolean,
    onUrlSubmitted: (String) -> Unit,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fontRegular = FontFamily(Font(R.font.inter_regular))
    var textState by remember(url) { mutableStateOf(url) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = dimensionResource(SdpR.dimen._4sdp),
                spotColor = Color.Black.copy(alpha = 0.06f),
                ambientColor = Color.Black.copy(alpha = 0.06f)
            )
            .background(colorResource(R.color.colors_1C1C1D))
            .padding(dimensionResource(SdpR.dimen._9sdp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
    ) {
        // Home icon
        Icon(
            painter = painterResource(R.drawable.ic_browser_home),
            contentDescription = stringResource(R.string.browser_home),
            tint = colorResource(R.color.colors_FFFFFF),
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._22sdp))
                .clickable(onClick = onHomeClick)
        )

        // URL bar
        Row(
            modifier = Modifier
                .weight(1f)
                .height(dimensionResource(SdpR.dimen._37sdp))
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp)))
                .background(colorResource(R.color.colors_333333))
                .padding(horizontal = dimensionResource(SdpR.dimen._9sdp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shield icon
            Icon(
                painter = painterResource(R.drawable.ic_browser_shield),
                contentDescription = stringResource(R.string.browser_shield),
                tint = colorResource(R.color.colors_FFFFFF),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._18sdp))
            )

            Spacer(modifier = Modifier.width(dimensionResource(SdpR.dimen._6sdp)))

            // URL text input
            BasicTextField(
                value = textState,
                onValueChange = { textState = it },
                textStyle = TextStyle(
                    fontFamily = fontRegular,
                    fontSize = with(LocalDensity.current) {
                        dimensionResource(SspR.dimen._12ssp).toSp()
                    },
                    color = colorResource(R.color.colors_FFFFFF)
                ),
                singleLine = true,
                cursorBrush = SolidColor(colorResource(R.color.colors_3369FD)),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onUrlSubmitted(textState) }),
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Bottom border
    HorizontalDivider(
        color = colorResource(R.color.colors_333333),
        thickness = 1.dp
    )
}

@Composable
fun BrowserBottomBar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    tabCount: Int,
    isBookmarked: Boolean,
    canBookmark: Boolean,
    showMoreMenu: Boolean,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onTabsClick: () -> Unit,
    onMenuClick: () -> Unit,
    moreMenuContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._46sdp))
            .shadow(
                elevation = dimensionResource(SdpR.dimen._4sdp),
                spotColor = Color.Black.copy(alpha = 0.06f),
                ambientColor = Color.Black.copy(alpha = 0.06f)
            )
            .background(colorResource(R.color.colors_1C1C1D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimensionResource(SdpR.dimen._18sdp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back button
            BrowserIconButton(
                iconRes = R.drawable.ic_browser_back,
                contentDescription = stringResource(R.string.browser_back),
                isEnabled = canGoBack,
                onClick = onBackClick
            )

            // Forward button
            BrowserIconButton(
                iconRes = R.drawable.ic_browser_forward,
                contentDescription = stringResource(R.string.browser_forward),
                isEnabled = canGoForward,
                onClick = onForwardClick
            )

            // Bookmark button
            BrowserIconButton(
                iconRes = if (isBookmarked) {
                    R.drawable.ic_browser_bookmark_filled
                } else {
                    R.drawable.ic_browser_bookmark
                },
                contentDescription = stringResource(
                    if (isBookmarked) R.string.browser_remove_bookmark else R.string.browser_add_bookmark
                ),
                isEnabled = canBookmark,
                tintColor = if (isBookmarked) colorResource(R.color.colors_005DFD) else null,
                onClick = onBookmarkClick
            )

            // Tabs button with count overlay
            Box(
                modifier = Modifier
                    .size(dimensionResource(SdpR.dimen._40sdp))
                    .clickable(onClick = onTabsClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_browser_tabs),
                    contentDescription = stringResource(R.string.browser_tabs),
                    tint = colorResource(R.color.colors_FFFFFF),
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._22sdp))
                )
                Text(
                    text = tabCount.toString(),
                    style = TextStyle(
                        fontFamily = FontFamily(Font(R.font.inter_semibold)),
                        fontSize = with(LocalDensity.current) {
                            dimensionResource(SspR.dimen._7ssp).toSp()
                        },
                        fontWeight = FontWeight.SemiBold,
                        color = colorResource(R.color.colors_FFFFFF)
                    ),
                    modifier = Modifier.offset(
                        x = (-2).dp,
                        y = dimensionResource(SdpR.dimen._2sdp)
                    )
                )
            }

            // More menu button
            Box {
                BrowserIconButton(
                    iconRes = R.drawable.ic_browser_more,
                    contentDescription = stringResource(R.string.browser_more_menu),
                    isEnabled = true,
                    onClick = onMenuClick
                )
                if (showMoreMenu) {
                    moreMenuContent()
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.align(Alignment.TopCenter),
            color = colorResource(R.color.colors_333333),
            thickness = 1.dp
        )
    }
}

@Composable
private fun BrowserIconButton(
    iconRes: Int,
    contentDescription: String,
    isEnabled: Boolean,
    onClick: () -> Unit,
    tintColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val resolvedTintColor = if (isEnabled) {
        tintColor ?: colorResource(R.color.colors_FFFFFF)
    } else {
        colorResource(R.color.colors_4D4D4D)
    }

    Box(
        modifier = modifier
            .size(dimensionResource(SdpR.dimen._40sdp))
            .clip(CircleShape)
            .clickable(enabled = isEnabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = resolvedTintColor,
            modifier = Modifier.size(dimensionResource(SdpR.dimen._22sdp))
        )
    }
}

@Composable
private fun FindInPageBar(
    query: String,
    currentMatch: Int,
    totalMatches: Int,
    onQueryChange: (String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fontRegular = FontFamily(Font(R.font.inter_regular))
    val fontMedium = FontFamily(Font(R.font.inter_medium))
    val focusRequester = remember { FocusRequester() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = dimensionResource(SdpR.dimen._4sdp),
                spotColor = Color.Black.copy(alpha = 0.1f),
                ambientColor = Color.Black.copy(alpha = 0.1f)
            )
            .background(colorResource(R.color.colors_1C1C1D))
            .padding(
                horizontal = dimensionResource(SdpR.dimen._12sdp),
                vertical = dimensionResource(SdpR.dimen._8sdp)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._8sdp))
    ) {
        // Search input
        Row(
            modifier = Modifier
                .weight(1f)
                .height(dimensionResource(SdpR.dimen._33sdp))
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._8sdp)))
                .background(colorResource(R.color.colors_333333))
                .padding(horizontal = dimensionResource(SdpR.dimen._9sdp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = TextStyle(
                    fontFamily = fontRegular,
                    fontSize = with(LocalDensity.current) {
                        dimensionResource(SspR.dimen._11ssp).toSp()
                    },
                    color = colorResource(R.color.colors_FFFFFF)
                ),
                singleLine = true,
                cursorBrush = SolidColor(colorResource(R.color.colors_3369FD)),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onNext() }),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.browser_find_in_page_hint),
                                fontFamily = fontRegular,
                                fontSize = with(LocalDensity.current) {
                                    dimensionResource(SspR.dimen._11ssp).toSp()
                                },
                                color = colorResource(R.color.colors_B3B3B3)
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
            )

            // Match count
            if (query.isNotEmpty()) {
                Text(
                    text = if (totalMatches > 0) {
                        stringResource(R.string.browser_find_in_page_result, currentMatch, totalMatches)
                    } else {
                        stringResource(R.string.browser_find_in_page_no_results)
                    },
                    fontFamily = fontMedium,
                    fontSize = with(LocalDensity.current) {
                        dimensionResource(SspR.dimen._9ssp).toSp()
                    },
                    color = colorResource(R.color.colors_B3B3B3),
                    maxLines = 1
                )
            }
        }

        // Previous
        Icon(
            painter = painterResource(R.drawable.ic_browser_back),
            contentDescription = stringResource(R.string.browser_find_in_page_previous),
            tint = if (totalMatches > 0) colorResource(R.color.colors_FFFFFF) else colorResource(R.color.colors_4D4D4D),
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._20sdp))
                .clip(CircleShape)
                .clickable(enabled = totalMatches > 0, onClick = onPrevious)
        )

        // Next
        Icon(
            painter = painterResource(R.drawable.ic_browser_forward),
            contentDescription = stringResource(R.string.browser_find_in_page_next),
            tint = if (totalMatches > 0) colorResource(R.color.colors_FFFFFF) else colorResource(R.color.colors_4D4D4D),
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._20sdp))
                .clip(CircleShape)
                .clickable(enabled = totalMatches > 0, onClick = onNext)
        )

        // Close
        Icon(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = stringResource(R.string.browser_find_in_page_close),
            tint = colorResource(R.color.colors_FFFFFF),
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._20sdp))
                .clip(CircleShape)
                .clickable(onClick = onClose)
        )
    }

    // Request focus when the bar appears
    androidx.compose.runtime.LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun VideoDownloadFab(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fabPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fabScale"
    )

    // Animated download indicator (same animation as the Downloads tab), shown directly over
    // the page with no backing circle.
    val downloadAnim by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.download))
    val downloadAnimProgress by animateLottieCompositionAsState(
        downloadAnim,
        iterations = LottieConstants.IterateForever
    )

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._80sdp) * scale)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            LottieAnimation(
                composition = downloadAnim,
                progress = { downloadAnimProgress },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Badge count. The Lottie icon sits inside its (transparent) frame, so pull the badge
        // inward from the frame corner to keep it near the visible download circle. Shown
        // whenever the FAB is visible (i.e. at least one video), including a single video.
        if (count >= 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(
                        x = -dimensionResource(SdpR.dimen._5sdp),
                        y = dimensionResource(SdpR.dimen._5sdp)
                    )
                    .size(dimensionResource(SdpR.dimen._20sdp))
                    .clip(CircleShape)
                    .background(Color(0xFFFF4444)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (count > 9) "9+" else count.toString(),
                    color = Color.White,
                    fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
