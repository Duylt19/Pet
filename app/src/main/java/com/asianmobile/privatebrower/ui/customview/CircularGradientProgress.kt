package com.asianmobile.privatebrower.ui.customview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Copyright © 2026 Asian Mobile Co.,Ltd
 * Created by am_viennv on 3/13/2026
 */
@Composable
fun CircularGradientProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFFF1F1F1).copy(alpha = 0.5f),
    gradientColors: List<Color> = listOf(
        Color(0xFF2196F3),
        Color(0xFF00B0FF),
        Color(0xFF2196F3)
    )
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationZ = -90f
            }
    ) {
        val strokeWidth = size.width * 0.16f

        // Background circle
        drawCircle(
            color = backgroundColor,
            style = Stroke(width = strokeWidth)
        )

        // Progress arc
        drawArc(
            brush = Brush.sweepGradient(
                gradientColors
            ),
            startAngle = 0f,
            sweepAngle = 360 * progress,
            useCenter = false,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round
            )
        )
    }
}


