package com.asianmobile.privatebrower.ui.home.tabstab

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import com.intuit.sdp.R as R_sdp
import com.intuit.ssp.R as R_ssp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asianmobile.privatebrower.R
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign
import com.asianmobile.privatebrower.utils.ScreenName
import com.asianmobile.privatebrower.utils.TrackScreenView

@Composable
fun TabsTabScreen(
    onNavigate: (String) -> Unit = {},
    onSearchModeChanged: (Boolean) -> Unit = {},
    onNavigateToSelection: (TabMode) -> Unit = {},
    isVisible: Boolean = true,
    viewModel: TabsTabViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val analyticsScreen = when {
        uiState.isSearchActive && uiState.mode == TabMode.INCOGNITO -> {
            ScreenName.TABS_SEARCH_PRIVATE
        }
        uiState.isSearchActive -> ScreenName.TABS_SEARCH_NORMAL
        uiState.mode == TabMode.INCOGNITO -> ScreenName.TABS_PRIVATE
        else -> ScreenName.TABS_NORMAL
    }
    TrackScreenView(analyticsScreen, isVisible = isVisible)
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val fontMedium = FontFamily(Font(R.font.inter_medium))
    val fontSemiBold = FontFamily(Font(R.font.inter_semibold))
    val currentOnSearchModeChanged by rememberUpdatedState(onSearchModeChanged)

    // State for single-tab deletion
    var currentPageTabId by remember { mutableStateOf<Long?>(null) }
    var showCloseTabDialog by remember { mutableStateOf(false) }
    var isAddingTab by remember { mutableStateOf(false) }

    val dismissSearch = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        viewModel.onDismissSearch()
    }
    val voiceSearchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.takeIf(String::isNotBlank)
                ?.let { query ->
                    viewModel.onSearchQueryChanged(query)
                }
        }
        keyboardController?.show()
    }

    LaunchedEffect(uiState.isSearchActive) {
        currentOnSearchModeChanged(uiState.isSearchActive)
    }

    DisposableEffect(Unit) {
        onDispose { currentOnSearchModeChanged(false) }
    }

    BackHandler(enabled = uiState.isSearchActive, onBack = dismissSearch)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_161718))
            .statusBarsPadding()
    ) {
        // Header bar
        TabsHeaderBar(
            mode = uiState.mode,
            isSearchActive = uiState.isSearchActive,
            showDropdown = uiState.showModeDropdown,
            onToggleDropdown = { viewModel.onToggleModeDropdown() },
            onDismissDropdown = { viewModel.onDismissModeDropdown() },
            onModeChanged = { viewModel.onModeChanged(it) },
            onSearchClick = viewModel::onStartSearch,
            onSearchBack = dismissSearch,
            showMoreMenu = uiState.showMoreMenu,
            onMoreClick = { viewModel.onToggleMoreMenu() },
            onDismissMoreMenu = { viewModel.onDismissMoreMenu() },
            hasTabs = uiState.tabs.isNotEmpty(),
            onSelectTabs = {
                viewModel.onDismissMoreMenu()
                onNavigateToSelection(uiState.mode)
            },
            onCloseAllTabs = {
                viewModel.onDismissMoreMenu()
                if (uiState.tabs.isNotEmpty()) {
                    viewModel.onShowCloseAllDialog(true)
                }
            },
            fontSemiBold = fontSemiBold,
            fontMedium = fontMedium
        )

        // Tab cards or empty state
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val displayTabs = uiState.displayTabs
            if (uiState.isSearchActive && displayTabs.isEmpty() && uiState.tabs.isNotEmpty()) {
                SearchEmptyState(fontMedium = fontMedium)
            } else if (displayTabs.isEmpty()) {
                EmptyTabState(
                    isIncognito = uiState.mode == TabMode.INCOGNITO,
                    fontMedium = fontMedium
                )
            } else if (uiState.isSearchActive) {
                TabSearchGrid(
                    tabs = displayTabs,
                    onTabClicked = { tabId ->
                        dismissSearch()
                        viewModel.onTabClicked(tabId)
                        onNavigate("browser_webview")
                    }
                )
            } else {
                TabCardsPager(
                    tabs = displayTabs,
                    onTabClicked = { tabId ->
                        viewModel.onTabClicked(tabId)
                        onNavigate("browser_webview")
                    },
                    onCloseTab = { tabId ->
                        viewModel.onCloseTab(tabId)
                    },
                    onCurrentPageChanged = { tabId ->
                        currentPageTabId = tabId
                    },
                    fontMedium = fontMedium
                )
            }
        }

        // Search bar (bottom overlay when search is active)
        if (uiState.isSearchActive) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                TabSearchBar(
                    query = uiState.searchQuery,
                    onQueryChanged = { viewModel.onSearchQueryChanged(it) },
                    onMicClick = {
                        if (SpeechRecognizer.isRecognitionAvailable(context)) {
                            keyboardController?.hide()
                            runCatching {
                                voiceSearchLauncher.launch(createVoiceSearchIntent(context))
                            }.onFailure {
                                keyboardController?.show()
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.tabs_voice_search_unavailable),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.tabs_voice_search_unavailable),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._6sdp)))
            }
        }

        // Action row
        if (!uiState.isSearchActive) {
            ActionRow(
                showAllActions = uiState.tabs.isNotEmpty(),
                onTrash = {
                    if (uiState.tabs.isNotEmpty() && currentPageTabId != null) {
                        showCloseTabDialog = true
                    }
                },
                onAdd = {
                    if (!isAddingTab) {
                        isAddingTab = true
                        coroutineScope.launch {
                            try {
                                val added = viewModel.onAddTab()
                                if (added) {
                                    onNavigate("browser_webview")
                                } else {
                                    val max = if (uiState.mode == TabMode.INCOGNITO) 5 else 10
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.tabs_max_reached_toast, max),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } finally {
                                isAddingTab = false
                            }
                        }
                    }
                },
                onDone = {
                    onNavigate("browser_webview")
                }
            )
        }

        if (!uiState.isSearchActive) {
            Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._12sdp)))
        }
    }

    // Close single tab dialog
    if (showCloseTabDialog && currentPageTabId != null) {
        CloseTabDialog(
            onConfirm = {
                viewModel.onCloseTab(currentPageTabId!!)
                showCloseTabDialog = false
            },
            onDismiss = { showCloseTabDialog = false }
        )
    }

    if (uiState.showCloseAllDialog) {
        CloseAllTabsDialog(
            tabCount = uiState.tabs.size,
            onConfirm = viewModel::onCloseAllInMode,
            onDismiss = { viewModel.onShowCloseAllDialog(false) }
        )
    }
}

// region Header

@Composable
private fun TabsHeaderBar(
    mode: TabMode,
    isSearchActive: Boolean,
    showDropdown: Boolean,
    onToggleDropdown: () -> Unit,
    onDismissDropdown: () -> Unit,
    onModeChanged: (TabMode) -> Unit,
    onSearchClick: () -> Unit,
    onSearchBack: () -> Unit,
    showMoreMenu: Boolean,
    onMoreClick: () -> Unit,
    onDismissMoreMenu: () -> Unit,
    hasTabs: Boolean,
    onSelectTabs: () -> Unit,
    onCloseAllTabs: () -> Unit,
    fontSemiBold: FontFamily,
    fontMedium: FontFamily
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(R_sdp.dimen._43sdp))
            .padding(horizontal = dimensionResource(R_sdp.dimen._12sdp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isSearchActive) {
                Box(
                    modifier = Modifier
                        .width(dimensionResource(R_sdp.dimen._31sdp))
                        .fillMaxHeight()
                        .clickable(onClick = onSearchBack),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.back),
                        tint = colorResource(R.color.colors_FFFFFF),
                        modifier = Modifier.size(dimensionResource(R_sdp.dimen._22sdp))
                    )
                }
            }

            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(dimensionResource(R_sdp.dimen._6sdp)))
                        .clickable(onClick = onToggleDropdown)
                        .padding(
                            horizontal = if (isSearchActive) {
                                0.dp
                            } else {
                                dimensionResource(R_sdp.dimen._4sdp)
                            },
                            vertical = dimensionResource(R_sdp.dimen._4sdp)
                        )
                ) {
                    Text(
                        text = if (mode == TabMode.INCOGNITO) {
                            stringResource(R.string.tabs_header_private_tab)
                        } else {
                            stringResource(R.string.tabs_header_normal_tab)
                        },
                        fontFamily = fontSemiBold,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = with(LocalDensity.current) {
                            dimensionResource(R_ssp.dimen._14ssp).toSp()
                        },
                        color = colorResource(R.color.colors_FFFFFF)
                    )
                    Spacer(modifier = Modifier.width(dimensionResource(R_sdp.dimen._3sdp)))
                    Icon(
                        painter = painterResource(R.drawable.ic_down_line),
                        contentDescription = null,
                        tint = colorResource(R.color.colors_FFFFFF),
                        modifier = Modifier.size(dimensionResource(R_sdp.dimen._15sdp))
                    )
                }

                TabModeDropdown(
                    expanded = showDropdown,
                    selectedMode = mode,
                    onDismiss = onDismissDropdown,
                    onModeSelected = onModeChanged,
                    fontMedium = fontMedium
                )
            }
        }

        if (!isSearchActive) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(dimensionResource(R_sdp.dimen._40sdp))
                        .clip(RoundedCornerShape(dimensionResource(R_sdp.dimen._4sdp)))
                        .clickable(onClick = onSearchClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_tabler_search),
                        contentDescription = stringResource(R.string.tabs_search),
                        tint = colorResource(R.color.colors_FFFFFF),
                        modifier = Modifier.size(dimensionResource(R_sdp.dimen._18sdp))
                    )
                }

                Box(
                    modifier = Modifier
                        .size(dimensionResource(R_sdp.dimen._40sdp))
                        .clip(RoundedCornerShape(dimensionResource(R_sdp.dimen._4sdp)))
                        .clickable(onClick = onMoreClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_mingcute_more),
                        contentDescription = stringResource(R.string.tabs_more),
                        tint = colorResource(R.color.colors_FFFFFF),
                        modifier = Modifier.size(dimensionResource(R_sdp.dimen._18sdp))
                    )

                    TabActionMenu(
                        expanded = showMoreMenu,
                        items = listOf(
                            TabActionMenuItem(
                                iconRes = R.drawable.ic_select_tabs,
                                label = stringResource(R.string.tabs_select_tabs),
                                enabled = hasTabs,
                                onClick = onSelectTabs
                            ),
                            TabActionMenuItem(
                                iconRes = R.drawable.ic_close_x,
                                label = stringResource(R.string.tabs_close_all_tabs),
                                enabled = hasTabs,
                                onClick = onCloseAllTabs
                            )
                        ),
                        onDismiss = onDismissMoreMenu
                    )
                }
            }
        }
    }
}

@Composable
private fun TabModeDropdown(
    expanded: Boolean,
    selectedMode: TabMode,
    onDismiss: () -> Unit,
    onModeSelected: (TabMode) -> Unit,
    fontMedium: FontFamily
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = DpOffset(0.dp, dimensionResource(R_sdp.dimen._4sdp)),
        containerColor = colorResource(R.color.colors_333538),
        shape = RoundedCornerShape(dimensionResource(R_sdp.dimen._12sdp)),
        modifier = Modifier
    ) {
        // Private Tab item
        DropdownMenuItem(
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.tabs_header_private_tab),
                        fontFamily = fontMedium,
                        fontWeight = FontWeight.Medium,
                        fontSize = with(LocalDensity.current) {
                            dimensionResource(R_ssp.dimen._11ssp).toSp()
                        },
                        color = if (selectedMode == TabMode.INCOGNITO)
                            colorResource(R.color.colors_3369FD)
                        else
                            colorResource(R.color.colors_FFFFFF)
                    )
                    if (selectedMode == TabMode.INCOGNITO) {
                        Spacer(modifier = Modifier.width(dimensionResource(R_sdp.dimen._15sdp)))
                        Icon(
                            painter = painterResource(R.drawable.ic_check_tick),
                            contentDescription = null,
                            tint = colorResource(R.color.colors_3369FD),
                            modifier = Modifier.size(dimensionResource(R_sdp.dimen._18sdp))
                        )
                    }
                }
            },
            onClick = { onModeSelected(TabMode.INCOGNITO) }
        )

        // Normal Tab item
        DropdownMenuItem(
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.tabs_header_normal_tab),
                        fontFamily = fontMedium,
                        fontWeight = FontWeight.Medium,
                        fontSize = with(LocalDensity.current) {
                            dimensionResource(R_ssp.dimen._11ssp).toSp()
                        },
                        color = if (selectedMode == TabMode.NORMAL)
                            colorResource(R.color.colors_3369FD)
                        else
                            colorResource(R.color.colors_FFFFFF)
                    )
                    if (selectedMode == TabMode.NORMAL) {
                        Spacer(modifier = Modifier.width(dimensionResource(R_sdp.dimen._15sdp)))
                        Icon(
                            painter = painterResource(R.drawable.ic_check_tick),
                            contentDescription = null,
                            tint = colorResource(R.color.colors_3369FD),
                            modifier = Modifier.size(dimensionResource(R_sdp.dimen._18sdp))
                        )
                    }
                }
            },
            onClick = { onModeSelected(TabMode.NORMAL) }
        )
    }
}

// endregion

// region Search Bar

@Composable
private fun TabSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onMicClick: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val shape = CircleShape

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R_sdp.dimen._9sdp))
            .height(dimensionResource(R_sdp.dimen._34sdp))
            .clip(shape)
            .background(colorResource(R.color.colors_212327))
            .border(
                width = 1.dp,
                color = if (query.isNotEmpty()) {
                    colorResource(R.color.colors_C8C8C9)
                } else {
                    colorResource(R.color.colors_333538)
                },
                shape = shape
            )
            .padding(horizontal = dimensionResource(R_sdp.dimen._9sdp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.tabs_search_placeholder),
                    color = colorResource(R.color.colors_6F7073),
                    fontSize = with(LocalDensity.current) {
                        dimensionResource(R_ssp.dimen._11ssp).toSp()
                    },
                    lineHeight = with(LocalDensity.current) {
                        dimensionResource(R_ssp.dimen._15ssp).toSp()
                    },
                    fontFamily = FontFamily(Font(R.font.inter_regular)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChanged,
                textStyle = TextStyle(
                    color = colorResource(R.color.colors_FFFFFF),
                    fontSize = with(LocalDensity.current) {
                        dimensionResource(R_ssp.dimen._11ssp).toSp()
                    },
                    lineHeight = with(LocalDensity.current) {
                        dimensionResource(R_ssp.dimen._15ssp).toSp()
                    },
                    letterSpacing = 0.sp,
                    fontFamily = FontFamily(Font(R.font.inter_regular))
                ),
                singleLine = true,
                cursorBrush = SolidColor(colorResource(R.color.colors_3369FD)),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { keyboardController?.hide() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        }

        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(dimensionResource(R_sdp.dimen._24sdp))
                    .clip(CircleShape)
                    .clickable {
                        onQueryChanged("")
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_search_clear),
                    contentDescription = stringResource(R.string.tabs_clear_search),
                    tint = Color.Unspecified,
                    modifier = Modifier.size(dimensionResource(R_sdp.dimen._15sdp))
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(dimensionResource(R_sdp.dimen._24sdp))
                    .clip(CircleShape)
                    .clickable(onClick = onMicClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_mic),
                    contentDescription = stringResource(R.string.tabs_voice_search),
                    tint = Color.Unspecified,
                    modifier = Modifier.size(dimensionResource(R_sdp.dimen._18sdp))
                )
            }
        }
    }
}

// endregion

// region Search Results

@Composable
private fun TabSearchGrid(
    tabs: List<TabUi>,
    onTabClicked: (Long) -> Unit
) {
    val horizontalPadding = dimensionResource(R_sdp.dimen._12sdp)
    val gridSpacing = dimensionResource(R_sdp.dimen._9sdp)
    val verticalPadding = dimensionResource(R_sdp.dimen._6sdp)
    val titleSpacing = dimensionResource(R_sdp.dimen._6sdp)
    val titleHeight = dimensionResource(R_sdp.dimen._12sdp)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        val cellWidth = (maxWidth - horizontalPadding * 2 - gridSpacing) / 2
        val naturalPreviewHeight = cellWidth / (158f / 239f)
        val availablePreviewHeight = (
            maxHeight - verticalPadding * 2 - titleSpacing - titleHeight
        ).coerceAtLeast(0.dp)
        val previewHeight = minOf(naturalPreviewHeight, availablePreviewHeight)

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontalPadding,
                top = verticalPadding,
                end = horizontalPadding,
                bottom = verticalPadding
            ),
            horizontalArrangement = Arrangement.spacedBy(gridSpacing),
            verticalArrangement = Arrangement.spacedBy(gridSpacing)
        ) {
            items(items = tabs, key = TabUi::id) { tab ->
                SearchTabCard(
                    tab = tab,
                    previewHeight = previewHeight,
                    onClick = { onTabClicked(tab.id) }
                )
            }
        }
    }
}

@Composable
private fun SearchTabCard(
    tab: TabUi,
    previewHeight: Dp,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(previewHeight)
                .clip(RoundedCornerShape(dimensionResource(R_sdp.dimen._12sdp)))
                .border(
                    width = 1.dp,
                    color = colorResource(R.color.colors_333333),
                    shape = RoundedCornerShape(dimensionResource(R_sdp.dimen._12sdp))
                ),
            contentAlignment = Alignment.Center
        ) {
            if (tab.hasThumbnail) {
                TabThumbnail(
                    tab = tab,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_tab_tabs),
                    contentDescription = null,
                    tint = colorResource(R.color.colors_9B9C9E),
                    modifier = Modifier.size(dimensionResource(R_sdp.dimen._36sdp))
                )
            }
        }

        Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._6sdp)))

        Text(
            text = tab.title,
            modifier = Modifier.fillMaxWidth(),
            color = colorResource(R.color.colors_FFFFFF),
            fontFamily = FontFamily(Font(R.font.inter_regular)),
            fontWeight = FontWeight.Normal,
            fontSize = with(LocalDensity.current) {
                dimensionResource(R_ssp.dimen._9ssp).toSp()
            },
            lineHeight = with(LocalDensity.current) {
                dimensionResource(R_ssp.dimen._12ssp).toSp()
            },
            letterSpacing = 0.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SearchEmptyState(fontMedium: FontFamily) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(R_sdp.dimen._24sdp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.tabs_search_no_results),
            color = colorResource(R.color.colors_9B9C9E),
            fontFamily = fontMedium,
            fontSize = with(LocalDensity.current) {
                dimensionResource(R_ssp.dimen._12ssp).toSp()
            },
            textAlign = TextAlign.Center
        )
    }
}

private fun createVoiceSearchIntent(context: Context): Intent {
    val languagePreferences = context.getSharedPreferences(
        "language_cache",
        Context.MODE_PRIVATE
    )
    val language = languagePreferences.getString("key_language", "en") ?: "en"
    val country = languagePreferences.getString("country_language", "US") ?: "US"
    val localeTag = "$language-$country"

    return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
        putExtra(
            RecognizerIntent.EXTRA_PROMPT,
            context.getString(R.string.tabs_voice_search_prompt)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF161718, widthDp = 360, heightDp = 480)
@Composable
private fun TabSearchModePreview() {
    val fontMedium = FontFamily(Font(R.font.inter_medium))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_161718))
    ) {
        TabsHeaderBar(
            mode = TabMode.INCOGNITO,
            isSearchActive = true,
            showDropdown = false,
            onToggleDropdown = {},
            onDismissDropdown = {},
            onModeChanged = {},
            onSearchClick = {},
            onSearchBack = {},
            showMoreMenu = false,
            onMoreClick = {},
            onDismissMoreMenu = {},
            hasTabs = true,
            onSelectTabs = {},
            onCloseAllTabs = {},
            fontSemiBold = FontFamily(Font(R.font.inter_semibold)),
            fontMedium = fontMedium
        )
        Box(modifier = Modifier.weight(1f)) {
            TabSearchGrid(
                tabs = listOf(
                    TabUi(1, "World Cup 2026", "https://example.com", null, true),
                    TabUi(2, "Google Search", "https://google.com", null, false)
                ),
                onTabClicked = {}
            )
        }
        TabSearchBar(
            query = "World Cup 2026",
            onQueryChanged = {},
            onMicClick = {}
        )
        Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._6sdp)))
    }
}

// endregion

// region Empty State

@Composable
private fun EmptyTabState(
    isIncognito: Boolean,
    fontMedium: FontFamily
) {
    val iconRes = if (isIncognito) {
        R.drawable.ic_private_browsing
    } else {
        R.drawable.ic_browser_tabs
    }
    val titleRes = if (isIncognito) {
        R.string.tabs_private_browsing_title
    } else {
        R.string.tabs_empty_normal_title
    }
    val descriptionRes = if (isIncognito) {
        R.string.tabs_private_browsing_description
    } else {
        R.string.tabs_empty_normal_description
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(R_sdp.dimen._12sdp))
    ) {
        // Figma 11110:530 - shared empty-state layout for Normal and Private tabs.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = dimensionResource(R_sdp.dimen._131sdp))
                .clip(RoundedCornerShape(dimensionResource(R_sdp.dimen._12sdp)))
                .padding(
                    horizontal = dimensionResource(R_sdp.dimen._8sdp),
                    vertical = dimensionResource(R_sdp.dimen._13sdp)
                )
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = colorResource(R.color.colors_FFFFFF),
                modifier = Modifier.size(dimensionResource(R_sdp.dimen._32sdp))
            )
            Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._6sdp)))
            Text(
                text = stringResource(titleRes),
                fontFamily = fontMedium,
                fontWeight = FontWeight.Medium,
                fontSize = with(LocalDensity.current) {
                    dimensionResource(R_ssp.dimen._12ssp).toSp()
                },
                lineHeight = with(LocalDensity.current) {
                    dimensionResource(R_ssp.dimen._18ssp).toSp()
                },
                letterSpacing = 0.sp,
                color = colorResource(R.color.colors_FFFFFF),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._2sdp)))
            Text(
                text = stringResource(descriptionRes),
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontWeight = FontWeight.Normal,
                fontSize = with(LocalDensity.current) {
                    dimensionResource(R_ssp.dimen._11ssp).toSp()
                },
                lineHeight = with(LocalDensity.current) {
                    dimensionResource(R_ssp.dimen._15ssp).toSp()
                },
                letterSpacing = 0.sp,
                color = colorResource(R.color.colors_FFFFFF),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF161718,
    widthDp = 360,
    heightDp = 240
)
@Composable
private fun IncognitoEmptyTabStatePreview() {
    EmptyTabState(
        isIncognito = true,
        fontMedium = FontFamily(Font(R.font.inter_medium))
    )
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF161718,
    widthDp = 360,
    heightDp = 240
)
@Composable
private fun NormalEmptyTabStatePreview() {
    EmptyTabState(
        isIncognito = false,
        fontMedium = FontFamily(Font(R.font.inter_medium))
    )
}

// endregion

// region Tab Cards

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TabCardsPager(
    tabs: List<TabUi>,
    onTabClicked: (Long) -> Unit,
    onCloseTab: (Long) -> Unit,
    onCurrentPageChanged: (Long) -> Unit,
    fontMedium: FontFamily
) {
    val pagerState = rememberPagerState(pageCount = { tabs.size })

    // Report current page tab ID to parent
    LaunchedEffect(pagerState.currentPage, tabs) {
        if (tabs.isNotEmpty() && pagerState.currentPage < tabs.size) {
            onCurrentPageChanged(tabs[pagerState.currentPage].id)
        }
    }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = screenWidth * 0.15f),
            pageSpacing = dimensionResource(R_sdp.dimen._9sdp),
        ) { page ->
            val tab = tabs[page]
            TabCard(
                tab = tab,
                onTabClicked = { onTabClicked(tab.id) },
                onCloseTab = { onCloseTab(tab.id) },
                fontMedium = fontMedium
            )
        }

        Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._6sdp)))

        // Pager indicator
        if (tabs.size > 1) {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(tabs.size) { index ->
                    val indicatorColor = if (index == pagerState.currentPage) {
                        colorResource(R.color.colors_FFFFFF)
                    } else {
                        colorResource(R.color.colors_333538)
                    }
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(
                                width = if (index == pagerState.currentPage) 24.dp else 6.dp,
                                height = 4.dp
                            )
                            .clip(RoundedCornerShape(2.dp))
                            .background(indicatorColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun TabCard(
    tab: TabUi,
    onTabClicked: () -> Unit,
    onCloseTab: () -> Unit,
    fontMedium: FontFamily
) {
    val density = LocalDensity.current
    val screenHeightPx = with(density) {
        LocalConfiguration.current.screenHeightDp.dp.toPx()
    }
    val dismissDistancePx = with(density) {
        dimensionResource(R_sdp.dimen._74sdp).toPx()
    }
    val dismissVelocityPx = with(density) { 900.dp.toPx() }
    val coroutineScope = rememberCoroutineScope()
    var verticalOffset by remember(tab.id) { mutableFloatStateOf(0f) }
    var isClosing by remember(tab.id) { mutableStateOf(false) }
    var settleJob by remember(tab.id) { mutableStateOf<Job?>(null) }
    val dragState = rememberDraggableState { delta ->
        verticalOffset += delta
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = verticalOffset
                alpha = (1f - abs(verticalOffset) / (screenHeightPx * 0.9f))
                    .coerceIn(0.25f, 1f)
            }
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                enabled = !isClosing,
                onDragStarted = {
                    settleJob?.cancel()
                },
                onDragStopped = { velocity ->
                    val direction = when {
                        abs(verticalOffset) >= dismissDistancePx -> sign(verticalOffset)
                        abs(velocity) >= dismissVelocityPx -> sign(velocity)
                        else -> 0f
                    }
                    settleJob = coroutineScope.launch {
                        if (direction != 0f) {
                            isClosing = true
                            animate(
                                initialValue = verticalOffset,
                                targetValue = direction * screenHeightPx,
                                animationSpec = tween(durationMillis = 180)
                            ) { value, _ ->
                                verticalOffset = value
                            }
                            onCloseTab()
                        } else {
                            animate(
                                initialValue = verticalOffset,
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 180)
                            ) { value, _ ->
                                verticalOffset = value
                            }
                        }
                    }
                }
            )
    ) {
        // Close button — centered above thumbnail (Figma: column, alignItems: center, gap: 12px)
        Icon(
            painter = painterResource(R.drawable.ic_close_tab),
            contentDescription = stringResource(R.string.common_delete_label),
            tint = Color.Unspecified,
            modifier = Modifier
                .size(dimensionResource(R_sdp.dimen._18sdp))
                .clip(CircleShape)
                .clickable(onClick = onCloseTab)
        )

        Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._9sdp)))

        // Thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(dimensionResource(R_sdp.dimen._12sdp)))
                .border(
                    width = 1.dp,
                    color = colorResource(R.color.colors_333333),
                    shape = RoundedCornerShape(dimensionResource(R_sdp.dimen._12sdp))
                )
                .clickable(onClick = onTabClicked),
            contentAlignment = Alignment.Center
        ) {
            if (tab.hasThumbnail) {
                TabThumbnail(
                    tab = tab,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Placeholder
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(dimensionResource(R_sdp.dimen._12sdp))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_tab_tabs),
                        contentDescription = null,
                        tint = colorResource(R.color.colors_9B9C9E),
                        modifier = Modifier.size(dimensionResource(R_sdp.dimen._36sdp))
                    )
                }
            }

        }

        // Title
        Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._9sdp)))
        Text(
            text = tab.title,
            fontFamily = fontMedium,
            fontSize = with(LocalDensity.current) {
                dimensionResource(R_ssp.dimen._11ssp).toSp()
            },
            color = colorResource(R.color.colors_FFFFFF),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// endregion

// region Action Row

@Composable
private fun ActionRow(
    showAllActions: Boolean,
    onTrash: () -> Unit,
    onAdd: () -> Unit,
    onDone: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(
            dimensionResource(R_sdp.dimen._24sdp),
            Alignment.CenterHorizontally
        ),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(R_sdp.dimen._9sdp))
    ) {
        if (showAllActions) {
            ActionButton(
                iconRes = R.drawable.ic_tab_trash,
                contentDesc = stringResource(R.string.common_delete_label),
                onClick = onTrash
            )
        }
        ActionButton(
            iconRes = R.drawable.ic_tab_add,
            contentDesc = stringResource(R.string.tab_plus),
            onClick = onAdd
        )
        if (showAllActions) {
            ActionButton(
                iconRes = R.drawable.ic_tab_goto,
                contentDesc = stringResource(R.string.tab_home),
                onClick = onDone
            )
        }
    }
}

@Composable
private fun ActionButton(
    iconRes: Int,
    contentDesc: String,
    onClick: () -> Unit
) {
    // Icon drawable already includes rounded rect background from Figma export
    Icon(
        painter = painterResource(iconRes),
        contentDescription = contentDesc,
        tint = Color.Unspecified,
        modifier = Modifier
            .size(dimensionResource(R_sdp.dimen._42sdp))
            .clip(RoundedCornerShape(dimensionResource(R_sdp.dimen._12sdp)))
            .clickable(onClick = onClick)
    )
}

// endregion

// region Close Tab Dialog

@Composable
private fun CloseTabDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.tabs_close_tab_confirm_title),
                fontFamily = FontFamily(Font(R.font.inter_bold)),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.tabs_close_tab_confirm_message),
                fontFamily = FontFamily(Font(R.font.inter_medium))
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.common_ok_label),
                    color = colorResource(R.color.colors_3369FD)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.common_cancel_label),
                    color = colorResource(R.color.colors_9B9C9E)
                )
            }
        },
        containerColor = colorResource(R.color.colors_333538),
        titleContentColor = colorResource(R.color.colors_FFFFFF),
        textContentColor = colorResource(R.color.colors_FFFFFF),
        shape = RoundedCornerShape(dimensionResource(R_sdp.dimen._12sdp))
    )
}

@Composable
private fun CloseAllTabsDialog(
    tabCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.tabs_close_all_confirm_title),
                fontFamily = FontFamily(Font(R.font.inter_bold)),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.tabs_close_all_confirm_message, tabCount),
                fontFamily = FontFamily(Font(R.font.inter_medium))
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.common_ok_label),
                    color = colorResource(R.color.colors_3369FD)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.common_cancel_label),
                    color = colorResource(R.color.colors_9B9C9E)
                )
            }
        },
        containerColor = colorResource(R.color.colors_333538),
        titleContentColor = colorResource(R.color.colors_FFFFFF),
        textContentColor = colorResource(R.color.colors_FFFFFF),
        shape = RoundedCornerShape(dimensionResource(R_sdp.dimen._12sdp))
    )
}

// endregion
