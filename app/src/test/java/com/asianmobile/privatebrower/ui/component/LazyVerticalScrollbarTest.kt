package com.asianmobile.privatebrower.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LazyVerticalScrollbarTest {

    @Test
    fun calculateScrollTarget_mapsTrackStartAndMiddleToVisibleRange() {
        assertEquals(
            ScrollbarScrollTarget(index = 0, scrollOffset = 0),
            calculateScrollbarScrollTarget(
                scrollFraction = 0f,
                totalItemsCount = 100,
                visibleItemsCount = 10,
                firstVisibleItemSize = 40
            )
        )
        assertEquals(
            ScrollbarScrollTarget(index = 45, scrollOffset = 0),
            calculateScrollbarScrollTarget(
                scrollFraction = 0.5f,
                totalItemsCount = 100,
                visibleItemsCount = 10,
                firstVisibleItemSize = 40
            )
        )
    }

    @Test
    fun calculateScrollTarget_preservesFractionWithinTargetItem() {
        assertEquals(
            ScrollbarScrollTarget(index = 45, scrollOffset = 36),
            calculateScrollbarScrollTarget(
                scrollFraction = 0.51f,
                totalItemsCount = 100,
                visibleItemsCount = 10,
                firstVisibleItemSize = 40
            )
        )
    }

    @Test
    fun calculateScrollTarget_trackEndRequestsLastItem() {
        assertEquals(
            ScrollbarScrollTarget(index = 99, scrollOffset = 0),
            calculateScrollbarScrollTarget(
                scrollFraction = 1f,
                totalItemsCount = 100,
                visibleItemsCount = 10,
                firstVisibleItemSize = 40
            )
        )
    }

    @Test
    fun calculateScrollTarget_returnsNullForEmptyContent() {
        assertNull(
            calculateScrollbarScrollTarget(
                scrollFraction = 0.5f,
                totalItemsCount = 0,
                visibleItemsCount = 0,
                firstVisibleItemSize = 0
            )
        )
    }
}
