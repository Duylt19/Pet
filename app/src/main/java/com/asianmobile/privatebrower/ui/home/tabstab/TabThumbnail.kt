package com.asianmobile.privatebrower.ui.home.tabstab

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.asianmobile.privatebrower.R
import com.intuit.sdp.R as SdpR
import java.io.File

@Composable
internal fun TabThumbnail(
    tab: TabUi,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(
        modifier = modifier.clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_tab_tabs),
            contentDescription = null,
            tint = colorResource(R.color.colors_9B9C9E),
            modifier = Modifier.size(dimensionResource(SdpR.dimen._36sdp))
        )
        val bitmap = tab.thumbnailBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = tab.title,
                modifier = Modifier.fillMaxSize(),
                alignment = Alignment.Center,
                contentScale = ContentScale.Crop
            )
        } else {
            tab.thumbnailPath?.let { thumbnailPath ->
                val request = remember(context, thumbnailPath, tab.thumbnailTimestamp) {
                    ImageRequest.Builder(context)
                        .data(File(thumbnailPath))
                        .crossfade(false)
                        .memoryCacheKey("tab_thumbnail_${tab.id}_${tab.thumbnailTimestamp}")
                        .diskCachePolicy(CachePolicy.DISABLED)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = tab.title,
                    modifier = Modifier.fillMaxSize(),
                    alignment = Alignment.Center,
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
