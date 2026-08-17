package com.asianmobile.emojibattery.shimeji.ui.shared.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.Dp
import com.asianmobile.emojibattery.shimeji.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PermissionDisclosureBottomSheet(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    DismissThresholdBottomSheet(
        onDismissRequest = onDismissRequest,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DismissThresholdBottomSheet(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val layoutMetrics = remember { BottomSheetLayoutMetrics() }
    val sheetStateReference = remember { BottomSheetStateReference() }
    val confirmValueChange = remember {
        { target: SheetValue ->
            val state = sheetStateReference.value
            target != SheetValue.Hidden || shouldAllowSheetDismiss(
                isExpanded = state?.currentValue == SheetValue.Expanded,
                currentOffsetPx = state?.runCatching { requireOffset() }?.getOrNull(),
                expandedOffsetPx = layoutMetrics.expandedOffsetPx,
                sheetHeightPx = layoutMetrics.sheetHeightPx
            )
        }
    }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = confirmValueChange
    )
    sheetStateReference.value = sheetState

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        sheetMaxWidth = Dp.Unspecified,
        shape = RectangleShape,
        containerColor = Color.Transparent,
        scrimColor = colorResource(R.color.colors_000000).copy(alpha = 0.5f),
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        content = {
            HideDialogNavigationBar()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        layoutMetrics.sheetHeightPx = coordinates.size.height.toFloat()
                        layoutMetrics.expandedOffsetPx =
                            (coordinates.findRootCoordinates().size.height - coordinates.size.height)
                                .coerceAtLeast(0)
                                .toFloat()
                    },
                content = content
            )
        }
    )
}

internal fun shouldAllowSheetDismiss(
    isExpanded: Boolean,
    currentOffsetPx: Float?,
    expandedOffsetPx: Float?,
    sheetHeightPx: Float
): Boolean {
    if (!isExpanded || currentOffsetPx == null || expandedOffsetPx == null || sheetHeightPx <= 0f) {
        return true
    }

    val dragDistancePx = (currentOffsetPx - expandedOffsetPx).coerceAtLeast(0f)
    return dragDistancePx <= EXPLICIT_DISMISS_OFFSET_TOLERANCE_PX ||
        dragDistancePx >= sheetHeightPx * BOTTOM_SHEET_DISMISS_FRACTION
}

private class BottomSheetLayoutMetrics {
    var expandedOffsetPx: Float? = null
    var sheetHeightPx: Float = 0f
}

@OptIn(ExperimentalMaterial3Api::class)
private class BottomSheetStateReference {
    var value: SheetState? = null
}

private const val BOTTOM_SHEET_DISMISS_FRACTION = 0.25f
private const val EXPLICIT_DISMISS_OFFSET_TOLERANCE_PX = 1f
