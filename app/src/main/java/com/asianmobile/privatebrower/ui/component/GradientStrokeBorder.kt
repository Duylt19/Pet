package com.asianmobile.privatebrower.ui.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws a gradient stroke border with bright corners at top-left and bottom-right,
 * fading to dark in the middle — matching the Figma design.
 *
 * Uses a single diagonal linear gradient from top-left → bottom-right:
 *   bright → dark → bright
 */
fun Modifier.gradientStrokeBorder(
    cornerRadius: Dp = 16.dp,
    strokeWidth: Dp = 1.dp,
    startColor: Color = Color.White.copy(alpha = 0.15f),
    endColor: Color = Color.White
): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()
        drawGradientStroke(
            cornerRadiusPx = cornerRadius.toPx(),
            strokeWidthPx = strokeWidth.toPx(),
            startColor = startColor,
            endColor = endColor
        )
    }
)

private fun DrawScope.drawGradientStroke(
    cornerRadiusPx: Float,
    strokeWidthPx: Float,
    startColor: Color,
    endColor: Color
) {
    val width = size.width
    val height = size.height
    val halfStroke = strokeWidthPx / 2f
    val radius = cornerRadiusPx.coerceAtMost(height / 2f)
    val cr = CornerRadius(radius, radius)

    val strokeRect = Rect(
        left = halfStroke,
        top = halfStroke,
        right = width - halfStroke,
        bottom = height - halfStroke
    )

    // Diagonal gradient: bright at top-left corner, dark in center, bright at bottom-right corner
    val diagonalBrush = Brush.linearGradient(
        colorStops = arrayOf(
            0f to endColor,
            0.4f to startColor,
            0.6f to startColor,
            1f to endColor
        ),
        start = Offset(0f, 0f),
        end = Offset(width, height)
    )

    val roundRectPath = Path().apply {
        addRoundRect(RoundRect(strokeRect, cr))
    }

    drawPath(
        path = roundRectPath,
        brush = diagonalBrush,
        style = Stroke(width = strokeWidthPx)
    )
}


