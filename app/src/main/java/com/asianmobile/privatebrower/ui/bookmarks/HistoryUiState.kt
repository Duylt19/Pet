package com.asianmobile.privatebrower.ui.bookmarks

import com.asianmobile.privatebrower.data.database.entity.HistoryEntity
import java.util.Calendar

data class HistoryGroup(
    val dayStartMillis: Long,
    val items: List<HistoryEntity>
)

data class HistoryUiState(
    val groups: List<HistoryGroup> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isLoading: Boolean = true,
    val showClearAllDialog: Boolean = false,
    val deletedItem: HistoryEntity? = null
) {
    val isEmpty: Boolean get() = groups.isEmpty()
}

internal fun groupHistoryByDay(items: List<HistoryEntity>): List<HistoryGroup> {
    return items
        .groupBy { startOfHistoryDay(it.visitedAt) }
        .map { (dayStart, dayItems) -> HistoryGroup(dayStart, dayItems) }
        .sortedByDescending(HistoryGroup::dayStartMillis)
}

internal fun historyItemsForSearch(
    groups: List<HistoryGroup>,
    hasQuery: Boolean
): List<HistoryEntity> {
    val items = groups.flatMap(HistoryGroup::items)
    return if (hasQuery) {
        items
    } else {
        items.sortedWith(
            compareByDescending<HistoryEntity> { it.visitCount }
                .thenByDescending { it.visitedAt }
        )
    }
}

internal fun startOfHistoryDay(timestamp: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
