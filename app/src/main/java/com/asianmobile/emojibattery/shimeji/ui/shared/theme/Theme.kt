package com.asianmobile.emojibattery.shimeji.ui.shared.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import com.asianmobile.emojibattery.shimeji.R

@Composable
fun BaseAppTheme(
    content: @Composable () -> Unit
) {
    val appColorScheme = lightColorScheme(
        primary = colorResource(R.color.colors_FB3675),
        onPrimary = colorResource(R.color.colors_FFFFFF),
        secondary = colorResource(R.color.colors_FF96B8),
        tertiary = colorResource(R.color.colors_FF417E),
        background = colorResource(R.color.colors_FFFFFF),
        onBackground = colorResource(R.color.colors_212327),
        surface = colorResource(R.color.colors_FFFFFF),
        onSurface = colorResource(R.color.colors_212327),
        surfaceVariant = colorResource(R.color.colors_F6F6F6),
        onSurfaceVariant = colorResource(R.color.colors_6F7073),
        outline = colorResource(R.color.colors_DEDEDF)
    )
    MaterialTheme(
        colorScheme = appColorScheme,
        typography = appTypography(),
        content = content
    )
}
