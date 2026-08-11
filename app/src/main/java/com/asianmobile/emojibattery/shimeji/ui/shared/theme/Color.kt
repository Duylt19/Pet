package com.asianmobile.emojibattery.shimeji.ui.shared.theme

import androidx.compose.ui.graphics.Color

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.colorResource
import com.asianmobile.emojibattery.shimeji.R

// Light theme colors
val Purple40 = Color(0xFF6750A4)
val PurpleGrey40 = Color(0xFF625B71)
val Pink40 = Color(0xFF7D5260)

// Dark theme colors
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

// Primary brand gradient brush
val PrimaryGradient: Brush
    @Composable
    get() = Brush.horizontalGradient(
        colors = listOf(
            colorResource(R.color.colors_5B6FFB),
            colorResource(R.color.colors_7C5BFB)
        )
    )


