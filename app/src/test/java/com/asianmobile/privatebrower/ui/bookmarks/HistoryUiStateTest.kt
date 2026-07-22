package com.asianmobile.privatebrower.ui.bookmarks

import com.asianmobile.privatebrower.data.database.entity.HistoryEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class HistoryUiStateTest {

    @Test
    fun `history is grouped by local day and newest group is first`() {
        val older = timestamp(2026, Calendar.JULY, 13, 23, 50)
        val newerMorning = timestamp(2026, Calendar.JULY, 14, 8, 0)
        val newerEvening = timestamp(2026, Calendar.JULY, 14, 20, 0)

        val groups = groupHistoryByDay(
            listOf(
                item(1, older),
                item(2, newerMorning),
                item(3, newerEvening)
            )
        )

        assertEquals(2, groups.size)
        assertEquals(listOf(2L, 3L), groups.first().items.map { it.id })
        assertEquals(listOf(1L), groups.last().items.map { it.id })
    }

    @Test
    fun `blank history search prioritizes frequently visited items`() {
        val groups = groupHistoryByDay(
            listOf(
                item(id = 1, visitedAt = 100, visitCount = 2),
                item(id = 2, visitedAt = 300, visitCount = 1),
                item(id = 3, visitedAt = 200, visitCount = 2)
            )
        )

        val items = historyItemsForSearch(groups = groups, hasQuery = false)

        assertEquals(listOf(3L, 1L, 2L), items.map { it.id })
    }

    @Test
    fun `history search results preserve repository ordering`() {
        val groups = listOf(
            HistoryGroup(
                dayStartMillis = 0,
                items = listOf(
                    item(id = 2, visitedAt = 300, visitCount = 1),
                    item(id = 1, visitedAt = 100, visitCount = 5)
                )
            )
        )

        val items = historyItemsForSearch(groups = groups, hasQuery = true)

        assertEquals(listOf(2L, 1L), items.map { it.id })
    }

    private fun item(
        id: Long,
        visitedAt: Long,
        visitCount: Int = 1
    ) = HistoryEntity(
        id = id,
        title = "Page $id",
        url = "https://example.com/$id",
        visitedAt = visitedAt,
        visitCount = visitCount
    )

    private fun timestamp(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
