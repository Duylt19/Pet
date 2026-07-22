package com.asianmobile.privatebrower.ui.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.floor
import kotlin.math.roundToInt

fun Modifier.lazyVerticalScrollbar(
    state: LazyListState,
    color: Color,
    width: Dp,
    minThumbHeight: Dp,
    endPadding: Dp,
    verticalPadding: Dp,
    touchTargetWidth: Dp,
    layoutDirection: LayoutDirection
): Modifier = verticalScrollbarPointerInput(
    stateKey = state,
    minThumbHeight = minThumbHeight,
    endPadding = endPadding,
    verticalPadding = verticalPadding,
    touchTargetWidth = touchTargetWidth,
    layoutDirection = layoutDirection,
    metricsProvider = state::scrollbarMetrics,
    onScrollRequested = { metrics, fraction ->
        val target = calculateScrollbarScrollTarget(
            scrollFraction = fraction,
            totalItemsCount = metrics.totalItemsCount,
            visibleItemsCount = metrics.visibleItemsCount,
            firstVisibleItemSize = metrics.firstVisibleItemSize
        ) ?: return@verticalScrollbarPointerInput
        state.requestScrollToItem(target.index, target.scrollOffset)
    }
).drawVerticalScrollbar(
    metricsProvider = state::scrollbarMetrics,
    color = color,
    width = width,
    minThumbHeight = minThumbHeight,
    endPadding = endPadding,
    verticalPadding = verticalPadding
)

fun Modifier.lazyVerticalScrollbar(
    state: LazyGridState,
    color: Color,
    width: Dp,
    minThumbHeight: Dp,
    endPadding: Dp,
    verticalPadding: Dp,
    touchTargetWidth: Dp,
    layoutDirection: LayoutDirection
): Modifier = verticalScrollbarPointerInput(
    stateKey = state,
    minThumbHeight = minThumbHeight,
    endPadding = endPadding,
    verticalPadding = verticalPadding,
    touchTargetWidth = touchTargetWidth,
    layoutDirection = layoutDirection,
    metricsProvider = state::scrollbarMetrics,
    onScrollRequested = { metrics, fraction ->
        val target = calculateScrollbarScrollTarget(
            scrollFraction = fraction,
            totalItemsCount = metrics.totalItemsCount,
            visibleItemsCount = metrics.visibleItemsCount,
            firstVisibleItemSize = metrics.firstVisibleItemSize
        ) ?: return@verticalScrollbarPointerInput
        state.requestScrollToItem(target.index, target.scrollOffset)
    }
).drawVerticalScrollbar(
    metricsProvider = state::scrollbarMetrics,
    color = color,
    width = width,
    minThumbHeight = minThumbHeight,
    endPadding = endPadding,
    verticalPadding = verticalPadding
)

private fun LazyListState.scrollbarMetrics(): ScrollbarMetrics? {
    val layoutInfo = layoutInfo
    val firstVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull() ?: return null
    return calculateScrollbarMetrics(
        totalItemsCount = layoutInfo.totalItemsCount,
        visibleItemsCount = layoutInfo.visibleItemsInfo.size,
        firstVisibleItemIndex = firstVisibleItem.index,
        firstVisibleItemOffset = firstVisibleItem.offset,
        firstVisibleItemSize = firstVisibleItem.size,
        canScrollBackward = canScrollBackward,
        canScrollForward = canScrollForward
    )
}

private fun LazyGridState.scrollbarMetrics(): ScrollbarMetrics? {
    val layoutInfo = layoutInfo
    val firstVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull() ?: return null
    return calculateScrollbarMetrics(
        totalItemsCount = layoutInfo.totalItemsCount,
        visibleItemsCount = layoutInfo.visibleItemsInfo.size,
        firstVisibleItemIndex = firstVisibleItem.index,
        firstVisibleItemOffset = firstVisibleItem.offset.y,
        firstVisibleItemSize = firstVisibleItem.size.height,
        canScrollBackward = canScrollBackward,
        canScrollForward = canScrollForward
    )
}

private fun Modifier.verticalScrollbarPointerInput(
    stateKey: Any,
    minThumbHeight: Dp,
    endPadding: Dp,
    verticalPadding: Dp,
    touchTargetWidth: Dp,
    layoutDirection: LayoutDirection,
    metricsProvider: () -> ScrollbarMetrics?,
    onScrollRequested: (ScrollbarMetrics, Float) -> Unit
): Modifier = pointerInput(
    stateKey,
    minThumbHeight,
    endPadding,
    verticalPadding,
    touchTargetWidth,
    layoutDirection
) {
    awaitEachGesture {
        val down = awaitFirstDown(
            requireUnconsumed = false,
            pass = PointerEventPass.Initial
        )
        val metrics = metricsProvider() ?: return@awaitEachGesture
        val geometry = calculateThumbGeometry(
            containerHeight = size.height.toFloat(),
            metrics = metrics,
            minThumbHeight = minThumbHeight.toPx(),
            verticalPadding = verticalPadding.toPx()
        ) ?: return@awaitEachGesture
        if (!isInScrollbarTouchArea(
                pointerX = down.position.x,
                containerWidth = size.width.toFloat(),
                touchTargetWidth = touchTargetWidth.toPx(),
                endPadding = endPadding.toPx(),
                layoutDirection = layoutDirection
            )
        ) {
            return@awaitEachGesture
        }

        down.consume()
        val thumbGrabOffset = if (down.position.y in
            geometry.thumbTop..(geometry.thumbTop + geometry.thumbHeight)
        ) {
            down.position.y - geometry.thumbTop
        } else {
            geometry.thumbHeight / 2f
        }
        onScrollRequested(
            metrics,
            calculateDragFraction(down.position.y, thumbGrabOffset, geometry)
        )

        while (true) {
            val change = awaitPointerEvent(PointerEventPass.Initial).changes
                .firstOrNull { it.id == down.id }
                ?: break
            if (!change.pressed) {
                change.consume()
                break
            }
            if (change.position != change.previousPosition) {
                val latestMetrics = metricsProvider() ?: break
                val latestGeometry = calculateThumbGeometry(
                    containerHeight = size.height.toFloat(),
                    metrics = latestMetrics,
                    minThumbHeight = minThumbHeight.toPx(),
                    verticalPadding = verticalPadding.toPx()
                ) ?: break
                onScrollRequested(
                    latestMetrics,
                    calculateDragFraction(
                        pointerY = change.position.y,
                        thumbGrabOffset = thumbGrabOffset.coerceAtMost(
                            latestGeometry.thumbHeight
                        ),
                        geometry = latestGeometry
                    )
                )
                change.consume()
            }
        }
    }
}

private fun Modifier.drawVerticalScrollbar(
    metricsProvider: () -> ScrollbarMetrics?,
    color: Color,
    width: Dp,
    minThumbHeight: Dp,
    endPadding: Dp,
    verticalPadding: Dp
): Modifier = drawWithContent {
    drawContent()
    val metrics = metricsProvider() ?: return@drawWithContent
    drawScrollbarThumb(
        metrics = metrics,
        color = color,
        width = width,
        minThumbHeight = minThumbHeight,
        endPadding = endPadding,
        verticalPadding = verticalPadding
    )
}

private fun calculateScrollbarMetrics(
    totalItemsCount: Int,
    visibleItemsCount: Int,
    firstVisibleItemIndex: Int,
    firstVisibleItemOffset: Int,
    firstVisibleItemSize: Int,
    canScrollBackward: Boolean,
    canScrollForward: Boolean
): ScrollbarMetrics? {
    if (totalItemsCount <= 0 || visibleItemsCount <= 0 ||
        (!canScrollBackward && !canScrollForward)
    ) {
        return null
    }

    val thumbFraction = (visibleItemsCount.toFloat() / totalItemsCount)
        .coerceIn(0f, 1f)
    val itemScrollFraction = if (firstVisibleItemSize > 0) {
        ((-firstVisibleItemOffset).coerceAtLeast(0).toFloat() / firstVisibleItemSize)
            .coerceIn(0f, 1f)
    } else {
        0f
    }
    val maxFirstVisibleIndex = (totalItemsCount - visibleItemsCount).coerceAtLeast(1)
    val estimatedProgress = (firstVisibleItemIndex + itemScrollFraction) /
        maxFirstVisibleIndex.toFloat()
    val scrollFraction = when {
        !canScrollBackward -> 0f
        !canScrollForward -> 1f
        else -> estimatedProgress.coerceIn(0f, 1f)
    }

    return ScrollbarMetrics(
        thumbFraction = thumbFraction,
        scrollFraction = scrollFraction,
        totalItemsCount = totalItemsCount,
        visibleItemsCount = visibleItemsCount,
        firstVisibleItemSize = firstVisibleItemSize
    )
}

private fun DrawScope.drawScrollbarThumb(
    metrics: ScrollbarMetrics,
    color: Color,
    width: Dp,
    minThumbHeight: Dp,
    endPadding: Dp,
    verticalPadding: Dp
) {
    val widthPx = width.toPx().coerceAtMost(size.width)
    val geometry = calculateThumbGeometry(
        containerHeight = size.height,
        metrics = metrics,
        minThumbHeight = minThumbHeight.toPx(),
        verticalPadding = verticalPadding.toPx()
    ) ?: return
    val endPaddingPx = endPadding.toPx()
    val thumbX = if (layoutDirection == LayoutDirection.Ltr) {
        (size.width - endPaddingPx - widthPx).coerceAtLeast(0f)
    } else {
        endPaddingPx.coerceAtMost((size.width - widthPx).coerceAtLeast(0f))
    }

    drawRoundRect(
        color = color,
        topLeft = Offset(thumbX, geometry.thumbTop),
        size = Size(widthPx, geometry.thumbHeight),
        cornerRadius = CornerRadius(widthPx / 2f, widthPx / 2f)
    )
}

private fun calculateThumbGeometry(
    containerHeight: Float,
    metrics: ScrollbarMetrics,
    minThumbHeight: Float,
    verticalPadding: Float
): ScrollbarThumbGeometry? {
    val trackHeight = (containerHeight - verticalPadding * 2f).coerceAtLeast(0f)
    if (trackHeight <= 0f) return null
    val thumbHeight = (trackHeight * metrics.thumbFraction)
        .coerceAtLeast(minThumbHeight)
        .coerceAtMost(trackHeight)
    val thumbTravel = (trackHeight - thumbHeight).coerceAtLeast(0f)
    return ScrollbarThumbGeometry(
        trackTop = verticalPadding,
        thumbTop = verticalPadding + thumbTravel * metrics.scrollFraction,
        thumbHeight = thumbHeight,
        thumbTravel = thumbTravel
    )
}

private fun calculateDragFraction(
    pointerY: Float,
    thumbGrabOffset: Float,
    geometry: ScrollbarThumbGeometry
): Float {
    if (geometry.thumbTravel <= 0f) return 0f
    return ((pointerY - thumbGrabOffset - geometry.trackTop) / geometry.thumbTravel)
        .coerceIn(0f, 1f)
}

private fun isInScrollbarTouchArea(
    pointerX: Float,
    containerWidth: Float,
    touchTargetWidth: Float,
    endPadding: Float,
    layoutDirection: LayoutDirection
): Boolean {
    val interactiveWidth = touchTargetWidth.coerceAtLeast(endPadding)
        .coerceAtMost(containerWidth)
    return if (layoutDirection == LayoutDirection.Ltr) {
        pointerX >= containerWidth - interactiveWidth
    } else {
        pointerX <= interactiveWidth
    }
}

internal fun calculateScrollbarScrollTarget(
    scrollFraction: Float,
    totalItemsCount: Int,
    visibleItemsCount: Int,
    firstVisibleItemSize: Int
): ScrollbarScrollTarget? {
    if (totalItemsCount <= 0) return null
    val fraction = scrollFraction.coerceIn(0f, 1f)
    if (fraction >= 1f) {
        return ScrollbarScrollTarget(index = totalItemsCount - 1, scrollOffset = 0)
    }
    val maxFirstVisibleIndex = (totalItemsCount - visibleItemsCount).coerceAtLeast(0)
    val exactIndex = fraction * maxFirstVisibleIndex
    val index = floor(exactIndex).toInt().coerceIn(0, totalItemsCount - 1)
    val itemFraction = exactIndex - index
    return ScrollbarScrollTarget(
        index = index,
        scrollOffset = (itemFraction * firstVisibleItemSize.coerceAtLeast(0)).roundToInt()
    )
}

private data class ScrollbarMetrics(
    val thumbFraction: Float,
    val scrollFraction: Float,
    val totalItemsCount: Int,
    val visibleItemsCount: Int,
    val firstVisibleItemSize: Int
)

private data class ScrollbarThumbGeometry(
    val trackTop: Float,
    val thumbTop: Float,
    val thumbHeight: Float,
    val thumbTravel: Float
)

internal data class ScrollbarScrollTarget(
    val index: Int,
    val scrollOffset: Int
)
