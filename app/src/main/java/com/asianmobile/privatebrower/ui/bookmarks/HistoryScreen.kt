package com.asianmobile.privatebrower.ui.bookmarks

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.ads.config.SCREEN_HISTORY
import com.asianmobile.privatebrower.ads.ui.compose.NativeAdInternal
import com.asianmobile.privatebrower.data.database.entity.HistoryEntity
import com.asianmobile.privatebrower.ui.component.DismissibleDialogBackdrop
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val HISTORY_NATIVE_AD_AFTER_INDEX = 1

@Composable
fun HistoryContent(
    uiState: HistoryUiState,
    onNavigateToBrowser: (String) -> Unit,
    onDelete: (HistoryEntity) -> Unit,
    onUndoDelete: () -> Unit,
    onDeleteMessageShown: () -> Unit,
    searchLayout: Boolean = false
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val undoLabel = stringResource(R.string.history_undo_action)
    val deletedMessage = stringResource(R.string.history_item_removed)
    var informationItem by remember { mutableStateOf<HistoryEntity?>(null) }

    LaunchedEffect(uiState.deletedItem?.id) {
        if (uiState.deletedItem != null) {
            val result = snackbarHostState.showSnackbar(
                message = deletedMessage,
                actionLabel = undoLabel,
                withDismissAction = true
            )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                onUndoDelete()
            } else {
                onDeleteMessageShown()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> Unit
            uiState.isEmpty -> HistoryEmptyState(
                hasSearchQuery = uiState.searchQuery.isNotBlank()
            )
            searchLayout -> HistorySearchList(
                items = historyItemsForSearch(
                    groups = uiState.groups,
                    hasQuery = uiState.searchQuery.isNotBlank()
                ),
                onItemClick = { onNavigateToBrowser(it.url) },
                onInformationClick = { informationItem = it },
                onDelete = onDelete
            )
            else -> HistoryList(
                groups = uiState.groups,
                onItemClick = { onNavigateToBrowser(it.url) },
                onInformationClick = { informationItem = it },
                onDelete = onDelete
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(dimensionResource(SdpR.dimen._12sdp))
        )
    }

    informationItem?.let { item ->
        HistoryInformationDialog(
            item = item,
            onDismiss = { informationItem = null }
        )
    }
}

@Composable
private fun HistoryEmptyState(hasSearchQuery: Boolean) {
    val title = if (hasSearchQuery) {
        stringResource(R.string.history_no_results)
    } else {
        stringResource(R.string.history_empty_message)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(SdpR.dimen._24sdp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_tab_history),
            contentDescription = null,
            tint = colorResource(R.color.colors_9B9C9E),
            modifier = Modifier.size(dimensionResource(SdpR.dimen._48sdp))
        )
        Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._9sdp)))
        Text(
            text = title,
            color = colorResource(R.color.colors_FFFFFF),
            fontFamily = FontFamily(Font(R.font.inter_medium)),
            fontWeight = FontWeight.Medium,
            fontSize = with(LocalDensity.current) {
                dimensionResource(SspR.dimen._12ssp).toSp()
            },
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HistorySearchList(
    items: List<HistoryEntity>,
    onItemClick: (HistoryEntity) -> Unit,
    onInformationClick: (HistoryEntity) -> Unit,
    onDelete: (HistoryEntity) -> Unit
) {
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
                items.forEachIndexed { index, item ->
                    key(item.id) {
                        HistorySwipeItem(
                            onDismiss = { onDelete(item) }
                        ) {
                            HistoryItemRow(
                                item = item,
                                onClick = { onItemClick(item) },
                                onInformationClick = { onInformationClick(item) },
                                onDeleteClick = { onDelete(item) }
                            )
                        }
                    }

                    if (index < items.lastIndex) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryList(
    groups: List<HistoryGroup>,
    onItemClick: (HistoryEntity) -> Unit,
    onInformationClick: (HistoryEntity) -> Unit,
    onDelete: (HistoryEntity) -> Unit
) {
    val cardRadius = dimensionResource(SdpR.dimen._12sdp)
    val cardHorizontalPadding = dimensionResource(SdpR.dimen._9sdp)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = dimensionResource(SdpR.dimen._12sdp))
    ) {
        var nextGroupStartIndex = 0
        groups.forEach { group ->
            val groupStartIndex = nextGroupStartIndex
            nextGroupStartIndex += group.items.size

            stickyHeader(key = "history_header_${group.dayStartMillis}") {
                Text(
                    text = historyDateLabel(group.dayStartMillis),
                    color = colorResource(R.color.colors_9B9C9E),
                    fontFamily = FontFamily(Font(R.font.inter_medium)),
                    fontWeight = FontWeight.Medium,
                    fontSize = with(LocalDensity.current) {
                        dimensionResource(SspR.dimen._10ssp).toSp()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorResource(R.color.colors_161718))
                        .padding(
                            horizontal = dimensionResource(SdpR.dimen._12sdp),
                            vertical = dimensionResource(SdpR.dimen._8sdp)
                        )
                )
            }
            itemsIndexed(
                items = group.items,
                key = { _, item -> item.id }
            ) { index, item ->
                val globalIndex = groupStartIndex + index
                val isFirstItem = index == 0
                val isLastItem = index == group.items.lastIndex
                val itemShape = RoundedCornerShape(
                    topStart = if (isFirstItem) cardRadius else 0.dp,
                    topEnd = if (isFirstItem) cardRadius else 0.dp,
                    bottomStart = if (isLastItem) cardRadius else 0.dp,
                    bottomEnd = if (isLastItem) cardRadius else 0.dp
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(SdpR.dimen._12sdp))
                        .clip(itemShape)
                        .background(colorResource(R.color.colors_212327))
                        .padding(
                            start = cardHorizontalPadding,
                            top = if (isFirstItem) {
                                dimensionResource(SdpR.dimen._9sdp)
                            } else {
                                0.dp
                            },
                            end = cardHorizontalPadding,
                            bottom = if (isLastItem) {
                                dimensionResource(SdpR.dimen._9sdp)
                            } else {
                                0.dp
                            }
                        )
                ) {
                    if (!isFirstItem) {
                        Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
                        HorizontalDivider(
                            color = colorResource(R.color.colors_333538),
                            thickness = dimensionResource(SdpR.dimen._1sdp)
                        )
                        Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
                    }

                    HistorySwipeItem(
                        onDismiss = { onDelete(item) }
                    ) {
                        HistoryItemRow(
                            item = item,
                            onClick = { onItemClick(item) },
                            onInformationClick = { onInformationClick(item) },
                            onDeleteClick = { onDelete(item) }
                        )
                    }

                    if (globalIndex == HISTORY_NATIVE_AD_AFTER_INDEX) {
                        Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
                        NativeAdInternal(
                            screenCode = SCREEN_HISTORY,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistorySwipeItem(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
                true
            } else {
                false
            }
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
                    contentDescription = stringResource(R.string.history_remove_action),
                    tint = colorResource(R.color.colors_FFFFFF),
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._18sdp))
                )
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        content = { content() }
    )
}

@Composable
private fun HistoryItemRow(
    item: HistoryEntity,
    onClick: () -> Unit,
    onInformationClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val fontMedium = FontFamily(Font(R.font.inter_medium))
    val fontRegular = FontFamily(Font(R.font.inter_regular))
    val host = remember(item.url) {
        runCatching { android.net.Uri.parse(item.url).host.orEmpty() }.getOrDefault("")
    }
    val favicon = item.faviconUrl
        ?: "https://www.google.com/s2/favicons?domain=$host&sz=64"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.colors_212327))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._31sdp))
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp)))
                .background(colorResource(R.color.colors_FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = favicon,
                contentDescription = null,
                placeholder = painterResource(R.drawable.ic_globe_fallback),
                error = painterResource(R.drawable.ic_globe_fallback),
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(dimensionResource(SdpR.dimen._20sdp))
            )
        }

        Spacer(modifier = Modifier.width(dimensionResource(SdpR.dimen._9sdp)))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._2sdp))
        ) {
            Text(
                text = item.title.ifBlank { item.url },
                color = colorResource(R.color.colors_FFFFFF),
                fontFamily = fontMedium,
                fontWeight = FontWeight.Medium,
                fontSize = with(LocalDensity.current) {
                    dimensionResource(SspR.dimen._11ssp).toSp()
                },
                lineHeight = with(LocalDensity.current) {
                    dimensionResource(SspR.dimen._15ssp).toSp()
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.url,
                color = colorResource(R.color.colors_FFFFFF),
                fontFamily = fontRegular,
                fontSize = with(LocalDensity.current) {
                    dimensionResource(SspR.dimen._9ssp).toSp()
                },
                lineHeight = with(LocalDensity.current) {
                    dimensionResource(SspR.dimen._12ssp).toSp()
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(dimensionResource(SdpR.dimen._6sdp)))

        Text(
            text = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(item.visitedAt)),
            color = colorResource(R.color.colors_9B9C9E),
            fontFamily = fontRegular,
            fontSize = with(LocalDensity.current) {
                dimensionResource(SspR.dimen._9ssp).toSp()
            },
            lineHeight = with(LocalDensity.current) {
                dimensionResource(SspR.dimen._12ssp).toSp()
            },
            maxLines = 1
        )

        Spacer(modifier = Modifier.width(dimensionResource(SdpR.dimen._11sdp)))

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
                        iconRes = R.drawable.ic_download_info,
                        labelRes = R.string.history_information_action,
                        onClick = onInformationClick
                    ),
                    BookmarkHistoryMenuAction(
                        iconRes = R.drawable.ic_menu_delete,
                        labelRes = R.string.history_remove_action,
                        onClick = onDeleteClick
                    )
                )
            )
        }
    }
}

@Composable
private fun HistoryInformationDialog(item: HistoryEntity, onDismiss: () -> Unit) {
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
                    text = stringResource(R.string.history_information_title),
                    modifier = Modifier.fillMaxWidth(),
                    color = colorResource(R.color.colors_FFFFFF),
                    fontFamily = FontFamily(Font(R.font.inter_medium)),
                    fontWeight = FontWeight.Medium,
                    fontSize = with(LocalDensity.current) {
                        dimensionResource(SspR.dimen._12ssp).toSp()
                    },
                    lineHeight = with(LocalDensity.current) {
                        dimensionResource(SspR.dimen._18ssp).toSp()
                    },
                    textAlign = TextAlign.Center
                )
                HorizontalDivider(
                    color = colorResource(R.color.colors_424447),
                    thickness = dimensionResource(SdpR.dimen._1sdp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(
                        dimensionResource(SdpR.dimen._9sdp)
                    )
                ) {
                    HistoryInformationRow(
                        R.string.history_information_name,
                        item.title.ifBlank { item.url }
                    )
                    HistoryInformationRow(R.string.history_information_url, item.url)
                    HistoryInformationRow(
                        R.string.history_information_last_visited,
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                            .format(Date(item.visitedAt))
                    )
                    HistoryInformationRow(
                        R.string.history_information_visits,
                        item.visitCount.toString()
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryInformationRow(label: Int, value: String) {
    val labelFont = FontFamily(Font(R.font.inter_semibold))
    val valueFont = FontFamily(Font(R.font.inter_regular))
    val labelColor = colorResource(R.color.colors_FFFFFF)
    val valueColor = colorResource(R.color.colors_9B9C9E)
    val labelText = stringResource(label)

    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = labelColor,
                    fontFamily = labelFont,
                    fontWeight = FontWeight.SemiBold
                )
            ) {
                append(labelText)
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
        fontSize = with(LocalDensity.current) {
            dimensionResource(SspR.dimen._11ssp).toSp()
        },
        lineHeight = with(LocalDensity.current) {
            dimensionResource(SspR.dimen._15ssp).toSp()
        }
    )
}

@Composable
private fun historyDateLabel(dayStartMillis: Long): String {
    val today = startOfHistoryDay(System.currentTimeMillis())
    val yesterday = Calendar.getInstance().apply {
        timeInMillis = today
        add(Calendar.DAY_OF_YEAR, -1)
    }.timeInMillis
    return when (dayStartMillis) {
        today -> stringResource(R.string.history_group_today)
        yesterday -> stringResource(R.string.history_group_yesterday)
        else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dayStartMillis))
    }
}
