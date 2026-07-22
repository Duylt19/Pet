package com.asianmobile.privatebrower.ui.bookmarks

import android.view.WindowManager
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.ads.config.SCREEN_BOOKMARKS
import com.asianmobile.privatebrower.ads.ui.compose.NativeAdInternal
import com.asianmobile.privatebrower.data.database.entity.BookmarkEntity
import com.asianmobile.privatebrower.ui.component.AppHeaderBar
import com.asianmobile.privatebrower.ui.component.AppHeaderLeading
import com.asianmobile.privatebrower.ui.component.DismissibleDialogBackdrop
import com.asianmobile.privatebrower.ui.component.RemoveConfirmationDialog
import com.asianmobile.privatebrower.ui.component.SegmentedTabBar
import com.asianmobile.privatebrower.ui.component.SegmentedTabItem
import com.asianmobile.privatebrower.utils.ScreenName
import com.asianmobile.privatebrower.utils.TrackScreenView

enum class BookmarksHistorySection {
    BOOKMARKS,
    HISTORY
}

private const val BOOKMARK_NATIVE_AD_AFTER_INDEX = 1

@Composable
fun BookmarksScreen(
    onNavigateToBrowser: (String) -> Unit,
    viewModel: BookmarksViewModel = hiltViewModel(),
    historyViewModel: HistoryViewModel = hiltViewModel(),
    initialSection: BookmarksHistorySection = BookmarksHistorySection.BOOKMARKS,
    onNavigateToSettings: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    onSearchModeChanged: (Boolean) -> Unit = {},
    isVisible: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val historyUiState by historyViewModel.uiState.collectAsStateWithLifecycle()
    val fontMedium = FontFamily(Font(R.font.inter_medium))
    var bookmarkToDelete by remember { mutableStateOf<BookmarkEntity?>(null) }
    var selectedSection by rememberSaveable(initialSection) { mutableStateOf(initialSection) }
    var showHistoryMenu by remember { mutableStateOf(false) }
    val isSearchMode = when (selectedSection) {
        BookmarksHistorySection.BOOKMARKS -> uiState.isSearchActive
        BookmarksHistorySection.HISTORY -> historyUiState.isSearchActive
    }
    val currentOnSearchModeChanged by rememberUpdatedState(onSearchModeChanged)

    TrackScreenView(
        screen = when {
            selectedSection == BookmarksHistorySection.HISTORY && isSearchMode -> {
                ScreenName.HISTORY_SEARCH
            }
            selectedSection == BookmarksHistorySection.HISTORY -> ScreenName.HISTORY
            isSearchMode -> ScreenName.BOOKMARKS_SEARCH
            else -> ScreenName.BOOKMARKS
        },
        isVisible = isVisible
    )

    LaunchedEffect(isSearchMode) {
        currentOnSearchModeChanged(isSearchMode)
    }

    DisposableEffect(Unit) {
        onDispose { currentOnSearchModeChanged(false) }
    }

    fun dismissSearch() {
        when (selectedSection) {
            BookmarksHistorySection.BOOKMARKS -> viewModel.dismissSearch()
            BookmarksHistorySection.HISTORY -> historyViewModel.dismissSearch()
        }
    }

    fun selectSection(section: BookmarksHistorySection) {
        if (selectedSection == section) return
        if (uiState.isSearchActive) viewModel.dismissSearch()
        if (historyUiState.isSearchActive) historyViewModel.dismissSearch()
        showHistoryMenu = false
        selectedSection = section
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_161718))
    ) {
        if (isSearchMode) {
            val query = when (selectedSection) {
                BookmarksHistorySection.BOOKMARKS -> uiState.searchQuery
                BookmarksHistorySection.HISTORY -> historyUiState.searchQuery
            }

            BookmarkHistorySearchScreen(
                query = query,
                onQueryChanged = when (selectedSection) {
                    BookmarksHistorySection.BOOKMARKS -> viewModel::onSearchQueryChanged
                    BookmarksHistorySection.HISTORY -> historyViewModel::onSearchQueryChanged
                },
                onBack = ::dismissSearch,
                voicePromptRes = if (selectedSection == BookmarksHistorySection.BOOKMARKS) {
                    R.string.bookmarks_voice_search_prompt
                } else {
                    R.string.history_voice_search_prompt
                }
            ) {
                if (selectedSection == BookmarksHistorySection.HISTORY) {
                    HistoryContent(
                        uiState = historyUiState,
                        onNavigateToBrowser = { url ->
                            dismissSearch()
                            onNavigateToBrowser(url)
                        },
                        onDelete = historyViewModel::onDelete,
                        onUndoDelete = historyViewModel::onUndoDelete,
                        onDeleteMessageShown = historyViewModel::onDeleteMessageShown,
                        searchLayout = true
                    )
                } else {
                    BookmarkSearchContent(
                        bookmarks = uiState.bookmarks,
                        hasQuery = uiState.searchQuery.isNotBlank(),
                        onItemClick = { bookmark ->
                            dismissSearch()
                            onNavigateToBrowser(bookmark.url)
                        },
                        onEditClick = viewModel::showEditDialog,
                        onDeleteClick = { bookmarkToDelete = it },
                        onStarClick = { bookmarkToDelete = it },
                        fontMedium = fontMedium
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                AppHeaderBar(
                    title = stringResource(R.string.drawer_bookmarks_history),
                    leadingIcon = if (onBack != null) {
                        AppHeaderLeading.Back
                    } else {
                        AppHeaderLeading.Settings
                    },
                    onLeadingClick = onBack ?: onNavigateToSettings,
                    trailing = {
                        Icon(
                            painter = painterResource(R.drawable.ic_bookmark_search),
                            contentDescription = stringResource(
                                R.string.bookmarks_search_placeholder
                            ),
                            tint = colorResource(R.color.colors_FFFFFF),
                            modifier = Modifier
                                .size(dimensionResource(SdpR.dimen._18sdp))
                                .clickable {
                                    if (
                                        selectedSection ==
                                        BookmarksHistorySection.BOOKMARKS
                                    ) {
                                        viewModel.startSearch()
                                    } else {
                                        historyViewModel.startSearch()
                                    }
                                }
                        )
                        if (
                            selectedSection == BookmarksHistorySection.HISTORY &&
                            !historyUiState.isEmpty
                        ) {
                            Spacer(
                                modifier = Modifier.width(
                                    dimensionResource(SdpR.dimen._9sdp)
                                )
                            )
                            Box {
                                Icon(
                                    painter = painterResource(R.drawable.ic_more_vert),
                                    contentDescription = stringResource(
                                        R.string.browser_more_menu
                                    ),
                                    tint = colorResource(R.color.colors_FFFFFF),
                                    modifier = Modifier
                                        .size(dimensionResource(SdpR.dimen._18sdp))
                                        .clickable { showHistoryMenu = true }
                                )
                                DropdownMenu(
                                    expanded = showHistoryMenu,
                                    onDismissRequest = { showHistoryMenu = false },
                                    offset = DpOffset(
                                        x = 0.dp,
                                        y = dimensionResource(SdpR.dimen._6sdp)
                                    ),
                                    shape = RoundedCornerShape(
                                        dimensionResource(SdpR.dimen._9sdp)
                                    ),
                                    containerColor = colorResource(
                                        R.color.colors_333538
                                    )
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = stringResource(
                                                    R.string.history_clear_all_action
                                                ),
                                                color = colorResource(
                                                    R.color.colors_FFFFFF
                                                ),
                                                fontFamily = fontMedium
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(
                                                    R.drawable.ic_menu_delete
                                                ),
                                                contentDescription = null,
                                                tint = colorResource(
                                                    R.color.colors_FFFFFF
                                                )
                                            )
                                        },
                                        onClick = {
                                            showHistoryMenu = false
                                            historyViewModel.showClearAllDialog()
                                        }
                                    )
                                }
                            }
                        }
                    }
                )

                BookmarksHistorySegmentedControl(
                    selectedSection = selectedSection,
                    onSectionSelected = ::selectSection,
                    fontMedium = fontMedium
                )

                if (selectedSection == BookmarksHistorySection.HISTORY) {
                    HistoryContent(
                        uiState = historyUiState,
                        onNavigateToBrowser = onNavigateToBrowser,
                        onDelete = historyViewModel::onDelete,
                        onUndoDelete = historyViewModel::onUndoDelete,
                        onDeleteMessageShown = historyViewModel::onDeleteMessageShown
                    )
                } else if (uiState.bookmarks.isEmpty()) {
                    BookmarkEmptyState(
                        onAddClick = viewModel::showAddDialog,
                        fontMedium = fontMedium
                    )
                } else {
                    BookmarksList(
                        bookmarks = uiState.bookmarks,
                        onItemClick = { onNavigateToBrowser(it.url) },
                        onEditClick = viewModel::showEditDialog,
                        onDeleteClick = { bookmarkToDelete = it },
                        onStarClick = { bookmarkToDelete = it },
                        fontMedium = fontMedium
                    )
                }
            }

            if (
                selectedSection == BookmarksHistorySection.BOOKMARKS &&
                uiState.bookmarks.isNotEmpty()
            ) {
                FloatingActionButton(
                    onClick = viewModel::showAddDialog,
                    containerColor = colorResource(R.color.colors_3369FD),
                    contentColor = colorResource(R.color.white),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(dimensionResource(SdpR.dimen._16sdp))
                        .size(dimensionResource(SdpR.dimen._42sdp))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_fab_plus),
                        contentDescription = stringResource(R.string.bookmark_add),
                        modifier = Modifier.size(dimensionResource(SdpR.dimen._25sdp))
                    )
                }
            }
        }
    }

    // Add/Edit Bookmark Dialog
    if (uiState.showAddDialog) {
        AddBookmarkDialog(
            editingBookmark = uiState.editingBookmark,
            onSave = viewModel::saveBookmark,
            onDismiss = viewModel::hideDialog,
            fontMedium = fontMedium
        )
    }

    // Remove Bookmark Confirmation Dialog
    bookmarkToDelete?.let { bookmark ->
        RemoveConfirmationDialog(
            title = R.string.bookmark_remove_title,
            description = R.string.bookmark_remove_desc,
            onConfirm = {
                viewModel.onDeleteBookmark(bookmark.id)
                bookmarkToDelete = null
            },
            onDismiss = { bookmarkToDelete = null }
        )
    }

    if (historyUiState.showClearAllDialog) {
        RemoveConfirmationDialog(
            title = R.string.history_clear_all_title,
            description = R.string.history_clear_all_description,
            confirmText = R.string.history_clear_all_action,
            onConfirm = historyViewModel::clearAll,
            onDismiss = historyViewModel::dismissClearAllDialog
        )
    }
}

@Composable
private fun BookmarksHistorySegmentedControl(
    selectedSection: BookmarksHistorySection,
    onSectionSelected: (BookmarksHistorySection) -> Unit,
    fontMedium: FontFamily
) {
    SegmentedTabBar {
        SegmentedTabItem(
            text = stringResource(R.string.bookmarks_segment_bookmarks),
            selected = selectedSection == BookmarksHistorySection.BOOKMARKS,
            onClick = { onSectionSelected(BookmarksHistorySection.BOOKMARKS) },
            fontMedium = fontMedium
        )
        SegmentedTabItem(
            text = stringResource(R.string.bookmarks_segment_history),
            selected = selectedSection == BookmarksHistorySection.HISTORY,
            onClick = { onSectionSelected(BookmarksHistorySection.HISTORY) },
            fontMedium = fontMedium
        )
    }
}

// ─── Search Content ───────────────────────────────────────────────────────────

@Composable
private fun BookmarkSearchContent(
    bookmarks: List<BookmarkEntity>,
    hasQuery: Boolean,
    onItemClick: (BookmarkEntity) -> Unit,
    onEditClick: (BookmarkEntity) -> Unit,
    onDeleteClick: (BookmarkEntity) -> Unit,
    onStarClick: (BookmarkEntity) -> Unit,
    fontMedium: FontFamily
) {
    if (bookmarks.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(
                    if (hasQuery) {
                        R.string.bookmarks_no_results
                    } else {
                        R.string.bookmark_no_bookmarks_title
                    }
                ),
                fontFamily = fontMedium,
                fontWeight = FontWeight.Medium,
                fontSize = with(LocalDensity.current) {
                    dimensionResource(SspR.dimen._11ssp).toSp()
                },
                color = colorResource(R.color.colors_9B9C9E),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = dimensionResource(SdpR.dimen._24sdp))
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
        contentPadding = PaddingValues(bottom = dimensionResource(SdpR.dimen._12sdp))
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp)))
                    .background(colorResource(R.color.colors_212327))
                    .padding(dimensionResource(SdpR.dimen._9sdp)),
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(SdpR.dimen._12sdp)
                )
            ) {
                bookmarks.forEachIndexed { index, bookmark ->
                    key(bookmark.id) {
                        BookmarkItemRow(
                            bookmark = bookmark,
                            onClick = { onItemClick(bookmark) },
                            onEditClick = { onEditClick(bookmark) },
                            onDeleteClick = { onDeleteClick(bookmark) },
                            onStarClick = { onStarClick(bookmark) },
                            fontMedium = fontMedium
                        )
                    }

                    if (index < bookmarks.lastIndex) {
                        HorizontalDivider(
                            color = colorResource(R.color.colors_333538),
                            thickness = dimensionResource(SdpR.dimen._1sdp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Empty State ──────────────────────────────────────────────────────────────

@Composable
private fun BookmarkEmptyState(
    onAddClick: () -> Unit,
    fontMedium: FontFamily
) {
    val fontRegular = FontFamily(Font(R.font.inter_regular))

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.width(dimensionResource(SdpR.dimen._197sdp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_bookmark_empty),
                contentDescription = null,
                tint = colorResource(R.color.colors_C8C8C9),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._37sdp))
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._3sdp))
            ) {
                Text(
                    text = stringResource(R.string.bookmark_no_bookmarks_title),
                    fontFamily = fontMedium,
                    fontWeight = FontWeight.Medium,
                    fontSize = with(LocalDensity.current) {
                        dimensionResource(SspR.dimen._12ssp).toSp()
                    },
                    lineHeight = with(LocalDensity.current) {
                        dimensionResource(SspR.dimen._18ssp).toSp()
                    },
                    color = colorResource(R.color.colors_FFFFFF),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.bookmark_no_bookmarks_desc),
                    fontFamily = fontRegular,
                    fontWeight = FontWeight.Normal,
                    fontSize = with(LocalDensity.current) {
                        dimensionResource(SspR.dimen._11ssp).toSp()
                    },
                    lineHeight = with(LocalDensity.current) {
                        dimensionResource(SspR.dimen._15ssp).toSp()
                    },
                    color = colorResource(R.color.colors_9B9C9E),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier
                    .width(dimensionResource(SdpR.dimen._162sdp))
                    .height(dimensionResource(SdpR.dimen._34sdp))
                    .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp)))
                    .background(colorResource(R.color.colors_3369FD))
                    .clickable(onClick = onAddClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_bookmark_empty_plus),
                    contentDescription = null,
                    tint = colorResource(R.color.colors_FFFFFF),
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._14sdp))
                )
                Spacer(modifier = Modifier.width(dimensionResource(SdpR.dimen._6sdp)))
                Text(
                    text = stringResource(R.string.bookmark_add),
                    fontFamily = fontMedium,
                    fontWeight = FontWeight.Medium,
                    fontSize = with(LocalDensity.current) {
                        dimensionResource(SspR.dimen._11ssp).toSp()
                    },
                    lineHeight = with(LocalDensity.current) {
                        dimensionResource(SspR.dimen._15ssp).toSp()
                    },
                    color = colorResource(R.color.colors_FFFFFF)
                )
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun BookmarkEmptyStatePreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_141414))
    ) {
        BookmarkEmptyState(
            onAddClick = {},
            fontMedium = FontFamily(Font(R.font.inter_medium))
        )
    }
}

// ─── Bookmarks List ───────────────────────────────────────────────────────────

@Composable
private fun BookmarksList(
    bookmarks: List<BookmarkEntity>,
    onItemClick: (BookmarkEntity) -> Unit,
    onEditClick: (BookmarkEntity) -> Unit,
    onDeleteClick: (BookmarkEntity) -> Unit,
    onStarClick: (BookmarkEntity) -> Unit,
    fontMedium: FontFamily
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp))
    ) {
        item {
            Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._6sdp)))
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp)))
                    .background(colorResource(R.color.colors_212327))
                    .padding(dimensionResource(SdpR.dimen._9sdp)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
            ) {
                bookmarks.forEachIndexed { index, bookmark ->
                    BookmarkItemRow(
                        bookmark = bookmark,
                        onClick = { onItemClick(bookmark) },
                        onEditClick = { onEditClick(bookmark) },
                        onDeleteClick = { onDeleteClick(bookmark) },
                        onStarClick = { onStarClick(bookmark) },
                        fontMedium = fontMedium
                    )
                    if (index == BOOKMARK_NATIVE_AD_AFTER_INDEX) {
                        NativeAdInternal(
                            screenCode = SCREEN_BOOKMARKS,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (index < bookmarks.lastIndex) {
                        HorizontalDivider(
                            color = colorResource(R.color.colors_333538),
                            thickness = dimensionResource(SdpR.dimen._1sdp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarkItemRow(
    bookmark: BookmarkEntity,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onStarClick: () -> Unit,
    fontMedium: FontFamily
) {
    val fontRegular = FontFamily(Font(R.font.inter_regular))
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Favicon (AsyncImage with Coil)
        val faviconUrl = bookmark.faviconUrl
            ?: "https://www.google.com/s2/favicons?domain=${
                try {
                    java.net.URI(bookmark.url).host ?: ""
                } catch (_: Exception) { "" }
            }&sz=64"

        Box(
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._31sdp))
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp)))
                .background(colorResource(R.color.colors_FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = faviconUrl,
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(SdpR.dimen._20sdp)),
                contentScale = ContentScale.Fit,
                error = painterResource(R.drawable.ic_globe_fallback),
                placeholder = painterResource(R.drawable.ic_globe_fallback)
            )
        }

        Spacer(modifier = Modifier.width(dimensionResource(SdpR.dimen._9sdp)))

        // Title + URL
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._2sdp))
        ) {
            Text(
                text = bookmark.title.ifBlank { bookmark.url },
                fontFamily = fontMedium,
                fontWeight = FontWeight.Medium,
                fontSize = with(LocalDensity.current) {
                    dimensionResource(SspR.dimen._11ssp).toSp()
                },
                lineHeight = with(LocalDensity.current) {
                    dimensionResource(SspR.dimen._15ssp).toSp()
                },
                color = colorResource(R.color.colors_FFFFFF),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = bookmark.url,
                fontFamily = fontRegular,
                fontSize = with(LocalDensity.current) {
                    dimensionResource(SspR.dimen._9ssp).toSp()
                },
                lineHeight = with(LocalDensity.current) {
                    dimensionResource(SspR.dimen._12ssp).toSp()
                },
                color = colorResource(R.color.colors_FFFFFF),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(dimensionResource(SdpR.dimen._6sdp)))

        // Star icon — clickable to remove bookmark
        Icon(
            painter = painterResource(R.drawable.ic_star_filled),
            contentDescription = stringResource(R.string.browser_remove_bookmark),
            tint = colorResource(R.color.colors_3369FD),
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._15sdp))
                .clickable(onClick = onStarClick)
        )

        Spacer(modifier = Modifier.width(dimensionResource(SdpR.dimen._11sdp)))

        // More "..." icon with popup
        Box {
            Icon(
                painter = painterResource(R.drawable.ic_more_horizontal),
                contentDescription = stringResource(R.string.browser_more_menu),
                tint = colorResource(R.color.colors_FFFFFF),
                modifier = Modifier
                    .size(dimensionResource(SdpR.dimen._15sdp))
                    .clickable { showMenu = true }
            )

            BookmarkHistoryActionMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                actions = listOf(
                    BookmarkHistoryMenuAction(
                        iconRes = R.drawable.ic_menu_edit,
                        labelRes = R.string.bookmark_menu_edit,
                        onClick = onEditClick
                    ),
                    BookmarkHistoryMenuAction(
                        iconRes = R.drawable.ic_menu_delete,
                        labelRes = R.string.bookmark_menu_remove,
                        onClick = onDeleteClick
                    )
                )
            )
        }
    }
}

// ─── Swipe to Dismiss ─────────────────────────────────────────────────────────

@Composable
private fun SwipeToDismissItem(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorResource(R.color.colors_EF4444))
                    .padding(horizontal = dimensionResource(SdpR.dimen._16sdp)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_trash),
                    contentDescription = stringResource(R.string.bookmark_menu_remove),
                    tint = colorResource(R.color.colors_FFFFFF),
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._18sdp))
                )
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        content()
    }
}

// ─── Add/Edit Bookmark Dialog ─────────────────────────────────────────────────

private enum class BookmarkUrlError {
    REQUIRED,
    INVALID
}

@Composable
private fun AddBookmarkDialog(
    editingBookmark: BookmarkEntity?,
    onSave: (name: String, url: String) -> Unit,
    onDismiss: () -> Unit,
    fontMedium: FontFamily
) {
    var name by remember(editingBookmark) {
        mutableStateOf(editingBookmark?.title ?: "")
    }
    var url by remember(editingBookmark) {
        mutableStateOf(editingBookmark?.url ?: "")
    }
    var nameHasError by remember(editingBookmark) { mutableStateOf(false) }
    var urlError by remember(editingBookmark) { mutableStateOf<BookmarkUrlError?>(null) }
    val nameFocusRequester = remember { FocusRequester() }
    val urlFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    fun validateAndSave() {
        val trimmedName = name.trim()
        val trimmedUrl = url.trim()
        var hasError = false

        if (trimmedName.isEmpty()) {
            nameHasError = true
            hasError = true
        } else {
            nameHasError = false
        }

        if (trimmedUrl.isEmpty()) {
            urlError = BookmarkUrlError.REQUIRED
            hasError = true
        } else if (!android.util.Patterns.WEB_URL.matcher(trimmedUrl).matches()) {
            urlError = BookmarkUrlError.INVALID
            hasError = true
        } else {
            urlError = null
        }

        if (hasError) {
            if (trimmedName.isEmpty()) {
                nameFocusRequester.requestFocus()
            } else {
                urlFocusRequester.requestFocus()
            }
            return
        }

        keyboardController?.hide()
        val finalUrl = if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
            "https://$trimmedUrl"
        } else {
            trimmedUrl
        }
        onSave(trimmedName, finalUrl)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        DisposableEffect(dialogWindow) {
            val previousSoftInputMode = dialogWindow?.attributes?.softInputMode
            dialogWindow?.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            )
            onDispose {
                if (dialogWindow != null && previousSoftInputMode != null) {
                    dialogWindow.setSoftInputMode(previousSoftInputMode)
                }
            }
        }

        LaunchedEffect(editingBookmark?.id) {
            nameFocusRequester.requestFocus()
            keyboardController?.show()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            DismissibleDialogBackdrop(
                onDismissRequest = onDismiss,
                surfaceModifier = Modifier.padding(
                    horizontal = dimensionResource(SdpR.dimen._12sdp),
                    vertical = dimensionResource(SdpR.dimen._12sdp)
                )
            ) {
                AddBookmarkDialogContent(
                    isEditing = editingBookmark != null,
                    name = name,
                    url = url,
                    nameHasError = nameHasError,
                    urlError = urlError,
                    onNameChange = {
                        name = it
                        if (nameHasError) nameHasError = false
                    },
                    onUrlChange = {
                        url = it
                        if (urlError != null) urlError = null
                    },
                    onDismiss = onDismiss,
                    onSave = ::validateAndSave,
                    nameFocusRequester = nameFocusRequester,
                    urlFocusRequester = urlFocusRequester,
                    fontMedium = fontMedium
                )
            }
        }
    }
}

@Composable
private fun AddBookmarkDialogContent(
    isEditing: Boolean,
    name: String,
    url: String,
    nameHasError: Boolean,
    urlError: BookmarkUrlError?,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    nameFocusRequester: FocusRequester,
    urlFocusRequester: FocusRequester,
    fontMedium: FontFamily
) {
    val fontRegular = FontFamily(Font(R.font.inter_regular))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._18sdp)))
            .background(colorResource(R.color.colors_333538))
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = dimensionResource(SdpR.dimen._12sdp),
                vertical = dimensionResource(SdpR.dimen._17sdp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
    ) {
        Text(
            text = stringResource(
                if (isEditing) R.string.bookmark_menu_edit else R.string.bookmark_add
            ),
            fontFamily = fontMedium,
            fontWeight = FontWeight.Medium,
            fontSize = with(LocalDensity.current) {
                dimensionResource(SspR.dimen._12ssp).toSp()
            },
            lineHeight = with(LocalDensity.current) {
                dimensionResource(SspR.dimen._18ssp).toSp()
            },
            color = colorResource(R.color.colors_FFFFFF),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._18sdp))
        ) {
            BookmarkDialogTextField(
                value = name,
                onValueChange = onNameChange,
                label = stringResource(R.string.bookmark_field_name),
                placeholder = stringResource(R.string.bookmark_field_name_hint),
                errorMessage = if (nameHasError) {
                    stringResource(R.string.bookmark_error_name_required)
                } else {
                    null
                },
                focusRequester = nameFocusRequester,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { urlFocusRequester.requestFocus() }
                ),
                fontRegular = fontRegular
            )

            BookmarkDialogTextField(
                value = url,
                onValueChange = onUrlChange,
                label = stringResource(R.string.bookmark_field_url),
                placeholder = stringResource(R.string.bookmark_field_url_hint),
                errorMessage = when (urlError) {
                    BookmarkUrlError.REQUIRED ->
                        stringResource(R.string.bookmark_error_url_required)
                    BookmarkUrlError.INVALID ->
                        stringResource(R.string.bookmark_error_url_invalid)
                    null -> null
                },
                focusRequester = urlFocusRequester,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onSave() }),
                fontRegular = fontRegular
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
        ) {
            BookmarkDialogButton(
                text = stringResource(R.string.bookmark_cancel),
                backgroundColor = R.color.colors_424447,
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                fontMedium = fontMedium
            )
            BookmarkDialogButton(
                text = stringResource(R.string.bookmark_save),
                backgroundColor = R.color.colors_3369FD,
                onClick = onSave,
                modifier = Modifier.weight(1f),
                fontMedium = fontMedium
            )
        }
    }
}

@Composable
private fun BookmarkDialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    errorMessage: String?,
    focusRequester: FocusRequester,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    fontRegular: FontFamily
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(
                    text = label,
                    fontFamily = fontRegular,
                    fontSize = with(LocalDensity.current) {
                        dimensionResource(SspR.dimen._9ssp).toSp()
                    },
                    lineHeight = with(LocalDensity.current) {
                        dimensionResource(SspR.dimen._12ssp).toSp()
                    }
                )
            },
            placeholder = {
                Text(
                    text = placeholder,
                    fontFamily = fontRegular,
                    fontSize = with(LocalDensity.current) {
                        dimensionResource(SspR.dimen._12ssp).toSp()
                    },
                    lineHeight = with(LocalDensity.current) {
                        dimensionResource(SspR.dimen._18ssp).toSp()
                    }
                )
            },
            textStyle = TextStyle(
                fontFamily = fontRegular,
                fontSize = with(LocalDensity.current) {
                    dimensionResource(SspR.dimen._12ssp).toSp()
                },
                lineHeight = with(LocalDensity.current) {
                    dimensionResource(SspR.dimen._18ssp).toSp()
                }
            ),
            isError = errorMessage != null,
            singleLine = true,
            shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colorResource(R.color.colors_FFFFFF),
                unfocusedTextColor = colorResource(R.color.colors_FFFFFF),
                focusedBorderColor = colorResource(R.color.colors_FFFFFF),
                unfocusedBorderColor = colorResource(R.color.colors_9B9C9E),
                errorBorderColor = colorResource(R.color.colors_EF4444),
                focusedLabelColor = colorResource(R.color.colors_FFFFFF),
                unfocusedLabelColor = colorResource(R.color.colors_9B9C9E),
                errorLabelColor = colorResource(R.color.colors_EF4444),
                focusedPlaceholderColor = colorResource(R.color.colors_9B9C9E),
                unfocusedPlaceholderColor = colorResource(R.color.colors_9B9C9E),
                errorPlaceholderColor = colorResource(R.color.colors_9B9C9E),
                cursorColor = colorResource(R.color.colors_3369FD),
                errorCursorColor = colorResource(R.color.colors_3369FD)
            ),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = colorResource(R.color.colors_EF4444),
                fontFamily = fontRegular,
                fontSize = with(LocalDensity.current) {
                    dimensionResource(SspR.dimen._9ssp).toSp()
                },
                lineHeight = with(LocalDensity.current) {
                    dimensionResource(SspR.dimen._12ssp).toSp()
                },
                modifier = Modifier.padding(
                    start = dimensionResource(SdpR.dimen._12sdp),
                    top = dimensionResource(SdpR.dimen._3sdp)
                )
            )
        }
    }
}

@Composable
private fun BookmarkDialogButton(
    text: String,
    backgroundColor: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontMedium: FontFamily
) {
    Box(
        modifier = modifier
            .height(dimensionResource(SdpR.dimen._37sdp))
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp)))
            .background(colorResource(backgroundColor))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = fontMedium,
            fontWeight = FontWeight.Medium,
            fontSize = with(LocalDensity.current) {
                dimensionResource(SspR.dimen._12ssp).toSp()
            },
            lineHeight = with(LocalDensity.current) {
                dimensionResource(SspR.dimen._18ssp).toSp()
            },
            color = colorResource(R.color.colors_FFFFFF)
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun AddBookmarkDialogContentPreview() {
    AddBookmarkDialogContent(
        isEditing = false,
        name = stringResource(R.string.bookmark_field_name),
        url = "",
        nameHasError = false,
        urlError = null,
        onNameChange = {},
        onUrlChange = {},
        onDismiss = {},
        onSave = {},
        nameFocusRequester = remember { FocusRequester() },
        urlFocusRequester = remember { FocusRequester() },
        fontMedium = FontFamily(Font(R.font.inter_medium))
    )
}
