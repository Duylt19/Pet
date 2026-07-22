package com.asianmobile.privatebrower.ui.home.tabstab

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asianmobile.privatebrower.R
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR
import com.asianmobile.privatebrower.utils.ScreenName
import com.asianmobile.privatebrower.utils.TrackScreenView

@Composable
fun TabSelectionScreen(
    onBack: () -> Unit,
    viewModel: TabSelectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    TrackScreenView(
        if (uiState.isIncognito) {
            ScreenName.TAB_SELECTION_PRIVATE
        } else {
            ScreenName.TAB_SELECTION_NORMAL
        }
    )

    BackHandler(onBack = onBack)

    LaunchedEffect(viewModel, context) {
        viewModel.events.collect { event ->
            when (event) {
                is TabSelectionEvent.TabsClosed -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.tabs_closed_toast, event.count),
                        Toast.LENGTH_SHORT
                    ).show()
                    if (event.exitSelection) onBack()
                }

                is TabSelectionEvent.BookmarksAdded -> {
                    val message = if (event.count > 0) {
                        context.getString(R.string.tabs_bookmarked_toast, event.count)
                    } else {
                        context.getString(R.string.tabs_no_new_bookmarks)
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }

                TabSelectionEvent.BookmarkFailed -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.tabs_bookmark_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is TabSelectionEvent.ShareRequested -> {
                    if (!shareTabs(context, event.tabs)) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.tabs_share_unavailable),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    TabSelectionContent(
        uiState = uiState,
        onBack = onBack,
        onMoreClick = viewModel::onToggleMoreMenu,
        onDismissMoreMenu = viewModel::onDismissMoreMenu,
        onToggleSelectAll = viewModel::onToggleSelectAll,
        onCloseSelectedTabs = viewModel::onCloseSelectedTabs,
        onBookmarkSelectedTabs = viewModel::onBookmarkSelectedTabs,
        onShareSelectedTabs = viewModel::onShareSelectedTabs,
        onToggleTabSelection = viewModel::onToggleTabSelection
    )
}

@Composable
private fun TabSelectionContent(
    uiState: TabSelectionUiState,
    onBack: () -> Unit,
    onMoreClick: () -> Unit,
    onDismissMoreMenu: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onCloseSelectedTabs: () -> Unit,
    onBookmarkSelectedTabs: () -> Unit,
    onShareSelectedTabs: () -> Unit,
    onToggleTabSelection: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_161718))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        TabSelectionHeader(
            uiState = uiState,
            onBack = onBack,
            onMoreClick = onMoreClick,
            onDismissMoreMenu = onDismissMoreMenu,
            onToggleSelectAll = onToggleSelectAll,
            onCloseSelectedTabs = onCloseSelectedTabs,
            onBookmarkSelectedTabs = onBookmarkSelectedTabs,
            onShareSelectedTabs = onShareSelectedTabs
        )

        if (uiState.tabs.isEmpty()) {
            TabSelectionEmptyState(isIncognito = uiState.isIncognito)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = dimensionResource(SdpR.dimen._12sdp),
                    top = dimensionResource(SdpR.dimen._6sdp),
                    end = dimensionResource(SdpR.dimen._12sdp),
                    bottom = dimensionResource(SdpR.dimen._12sdp)
                ),
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(SdpR.dimen._9sdp)
                ),
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(SdpR.dimen._9sdp)
                )
            ) {
                items(items = uiState.tabs, key = TabUi::id) { tab ->
                    SelectableTabCard(
                        tab = tab,
                        isSelected = tab.id in uiState.selectedTabIds,
                        enabled = !uiState.isProcessing,
                        onClick = { onToggleTabSelection(tab.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TabSelectionHeader(
    uiState: TabSelectionUiState,
    onBack: () -> Unit,
    onMoreClick: () -> Unit,
    onDismissMoreMenu: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onCloseSelectedTabs: () -> Unit,
    onBookmarkSelectedTabs: () -> Unit,
    onShareSelectedTabs: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._43sdp))
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(dimensionResource(SdpR.dimen._34sdp))
                .fillMaxHeight()
                .clickable(onClick = onBack),
            contentAlignment = Alignment.CenterStart
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.back),
                tint = colorResource(R.color.colors_FFFFFF),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._22sdp))
            )
        }

        Text(
            text = if (uiState.selectedCount == 0) {
                stringResource(R.string.tabs_select_tabs)
            } else {
                pluralStringResource(
                    R.plurals.tabs_selected_count,
                    uiState.selectedCount,
                    uiState.selectedCount
                )
            },
            modifier = Modifier.weight(1f),
            color = colorResource(R.color.colors_FFFFFF),
            fontFamily = FontFamily(Font(R.font.inter_semibold)),
            fontWeight = FontWeight.SemiBold,
            fontSize = with(LocalDensity.current) {
                dimensionResource(SspR.dimen._15ssp).toSp()
            },
            lineHeight = with(LocalDensity.current) {
                dimensionResource(SspR.dimen._22ssp).toSp()
            },
            letterSpacing = 0.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Box(
            modifier = Modifier
                .width(dimensionResource(SdpR.dimen._31sdp))
                .fillMaxHeight()
                .clickable(
                    enabled = uiState.tabs.isNotEmpty() && !uiState.isProcessing,
                    onClick = onMoreClick
                ),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_mingcute_more),
                contentDescription = stringResource(R.string.tabs_more),
                tint = colorResource(R.color.colors_FFFFFF),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._18sdp))
            )

            TabActionMenu(
                expanded = uiState.showMoreMenu,
                items = listOf(
                    TabActionMenuItem(
                        iconRes = if (uiState.areAllTabsSelected) {
                            R.drawable.ic_deselect_all
                        } else {
                            R.drawable.ic_select_tabs
                        },
                        label = if (uiState.areAllTabsSelected) {
                            stringResource(R.string.tabs_deselect_all)
                        } else {
                            stringResource(R.string.tabs_select_all_tabs)
                        },
                        enabled = uiState.tabs.isNotEmpty() && !uiState.isProcessing,
                        onClick = onToggleSelectAll
                    ),
                    TabActionMenuItem(
                        iconRes = R.drawable.ic_close_x,
                        label = stringResource(R.string.tabs_close_tabs),
                        enabled = uiState.hasSelection && !uiState.isProcessing,
                        onClick = onCloseSelectedTabs
                    ),
                    TabActionMenuItem(
                        iconRes = R.drawable.ic_menu_star,
                        label = stringResource(R.string.tabs_add_bookmark),
                        enabled = uiState.hasSelection && !uiState.isProcessing,
                        onClick = onBookmarkSelectedTabs
                    ),
                    TabActionMenuItem(
                        iconRes = R.drawable.ic_menu_share,
                        label = stringResource(R.string.tabs_share_tabs),
                        enabled = uiState.hasSelection && !uiState.isProcessing,
                        onClick = onShareSelectedTabs
                    )
                ),
                onDismiss = onDismissMoreMenu
            )
        }
    }
}

@Composable
private fun SelectableTabCard(
    tab: TabUi,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = isSelected,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = { onClick() }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(158f / 239f)
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp)))
                .border(
                    width = 1.dp,
                    color = colorResource(R.color.colors_333333),
                    shape = RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp))
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
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._36sdp))
                )
            }

            TabSelectionIndicator(
                isSelected = isSelected,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(dimensionResource(SdpR.dimen._6sdp))
            )
        }

        Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._6sdp)))

        Text(
            text = tab.title,
            modifier = Modifier.fillMaxWidth(),
            color = colorResource(R.color.colors_FFFFFF),
            fontFamily = FontFamily(Font(R.font.inter_regular)),
            fontWeight = FontWeight.Normal,
            fontSize = with(LocalDensity.current) {
                dimensionResource(SspR.dimen._9ssp).toSp()
            },
            lineHeight = with(LocalDensity.current) {
                dimensionResource(SspR.dimen._12ssp).toSp()
            },
            letterSpacing = 0.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TabSelectionIndicator(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(dimensionResource(SdpR.dimen._18sdp))
            .clip(CircleShape)
            .background(
                if (isSelected) {
                    colorResource(R.color.colors_3369FD)
                } else {
                    colorResource(R.color.colors_FFFFFF)
                }
            )
            .border(
                width = 1.dp,
                color = if (isSelected) {
                    colorResource(R.color.transparent)
                } else {
                    colorResource(R.color.gray_808080)
                },
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                painter = painterResource(R.drawable.ic_check_white),
                contentDescription = null,
                tint = colorResource(R.color.colors_FFFFFF),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._10sdp))
            )
        }
    }
}

@Composable
private fun TabSelectionEmptyState(isIncognito: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(SdpR.dimen._24sdp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(
                if (isIncognito) R.string.tabs_empty_incognito else R.string.tabs_empty_normal
            ),
            color = colorResource(R.color.colors_9B9C9E),
            fontFamily = FontFamily(Font(R.font.inter_medium)),
            fontSize = with(LocalDensity.current) {
                dimensionResource(SspR.dimen._12ssp).toSp()
            },
            textAlign = TextAlign.Center
        )
    }
}

private fun shareTabs(context: Context, tabs: List<TabUi>): Boolean {
    val shareText = tabs.joinToString(separator = "\n\n") { tab ->
        context.getString(R.string.tabs_share_item, tab.title, tab.url)
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    val chooser = Intent.createChooser(
        sendIntent,
        context.getString(R.string.tabs_share_chooser_title)
    ).apply {
        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return runCatching {
        context.startActivity(chooser)
        true
    }.getOrDefault(false)
}

@Preview(showBackground = true, backgroundColor = 0xFF161718, widthDp = 360, heightDp = 800)
@Composable
private fun TabSelectionContentPreview() {
    TabSelectionContent(
        uiState = TabSelectionUiState(
            tabs = listOf(
                TabUi(1, "Google", "https://google.com", null, true),
                TabUi(2, "Private Browser", "https://example.com", null, false),
                TabUi(3, "News", "https://example.com/news", null, false)
            ),
            selectedTabIds = setOf(1L)
        ),
        onBack = {},
        onMoreClick = {},
        onDismissMoreMenu = {},
        onToggleSelectAll = {},
        onCloseSelectedTabs = {},
        onBookmarkSelectedTabs = {},
        onShareSelectedTabs = {},
        onToggleTabSelection = {}
    )
}
