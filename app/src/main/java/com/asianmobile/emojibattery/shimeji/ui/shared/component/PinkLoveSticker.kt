package com.asianmobile.emojibattery.shimeji.ui.shared.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.asianmobile.emojibattery.shimeji.R

@Composable
fun PinkLoveSticker(
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val previewPainter = painterResource(R.drawable.img_pink_love_sticker_preview)
    AsyncImage(
        model = R.drawable.img_pink_love_sticker,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        placeholder = previewPainter,
        error = previewPainter,
        fallback = previewPainter,
        modifier = modifier
    )
}
