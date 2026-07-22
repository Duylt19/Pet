package com.asianmobile.privatebrower.ui.browser

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.data.browser.DetectedVideo
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoSelectBottomSheet(
    videos: List<DetectedVideo>,
    onDismiss: () -> Unit,
    onDownload: (List<DetectedVideo>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedVideos = remember { mutableStateListOf<String>() }
    val fontMedium = FontFamily(Font(R.font.inter_medium))
    val fontRegular = FontFamily(Font(R.font.inter_regular))
    val fontSemiBold = FontFamily(Font(R.font.inter_semibold))

    val allSelected = videos.isNotEmpty() && videos.all { selectedVideos.contains(it.url) }
    val selectAllTextColor = animateColorAsState(
        targetValue = colorResource(
            if (allSelected) R.color.colors_2257EB else R.color.colors_FFFFFF
        ),
        label = "selectAllTextColor"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorResource(R.color.colors_161920),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = dimensionResource(SdpR.dimen._10sdp))
                    .size(
                        width = dimensionResource(SdpR.dimen._36sdp),
                        height = dimensionResource(SdpR.dimen._4sdp)
                    )
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF4A4A4A))
            )
        }
    ) {
        // The sheet lives in its own window; on API 29+ a transparent nav bar otherwise gets a
        // system contrast scrim. Clear it so the nav bar is fully transparent, like the dialogs.
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.let { window ->
                window.navigationBarColor = Color.Transparent.toArgb()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = dimensionResource(SdpR.dimen._16sdp))
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = dimensionResource(SdpR.dimen._6sdp),
                        bottom = dimensionResource(SdpR.dimen._9sdp)
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.video_select_title),
                    fontFamily = fontSemiBold,
                    fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
                    color = colorResource(R.color.colors_FFFFFF)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._6sdp)))
                        .clickable {
                            if (allSelected) {
                                selectedVideos.clear()
                            } else {
                                selectedVideos.clear()
                                selectedVideos.addAll(videos.map { it.url })
                            }
                        }
                        .padding(
                            horizontal = dimensionResource(SdpR.dimen._3sdp),
                            vertical = dimensionResource(SdpR.dimen._3sdp)
                        )
                ) {
                    Text(
                        text = stringResource(
                            if (allSelected) {
                                R.string.video_deselect_all
                            } else {
                                R.string.video_select_all
                            }
                        ),
                        fontFamily = fontMedium,
                        fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                        color = selectAllTextColor.value
                    )
                    Spacer(modifier = Modifier.width(dimensionResource(SdpR.dimen._6sdp)))
                    SelectionCircle(isSelected = allSelected)
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp)),
                contentPadding = PaddingValues(bottom = dimensionResource(SdpR.dimen._6sdp)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = dimensionResource(SdpR.dimen._232sdp))
            ) {
                items(videos) { video ->
                    val isSelected = selectedVideos.contains(video.url)
                    VideoListItem(
                        video = video,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelected) {
                                selectedVideos.remove(video.url)
                            } else {
                                selectedVideos.add(video.url)
                            }
                        },
                        fontRegular = fontRegular
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._3sdp)))

            Button(
                onClick = {
                    val selected = videos.filter { selectedVideos.contains(it.url) }
                    if (selected.isNotEmpty()) {
                        onDownload(selected)
                    }
                },
                enabled = selectedVideos.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(SdpR.dimen._37sdp)),
                shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp)),
                contentPadding = PaddingValues(
                    horizontal = dimensionResource(SdpR.dimen._12sdp)
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.colors_2257EB),
                    disabledContainerColor = colorResource(R.color.colors_27272A)
                )
            ) {
                Text(
                    text = if (selectedVideos.isNotEmpty()) {
                        "${stringResource(R.string.video_download_btn)} (${selectedVideos.size})"
                    } else {
                        stringResource(R.string.video_download_btn)
                    },
                    fontFamily = fontSemiBold,
                    fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                    color = colorResource(R.color.colors_FFFFFF)
                )
            }

            Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._9sdp)))
        }
    }
}

@Composable
private fun VideoListItem(
    video: DetectedVideo,
    isSelected: Boolean,
    onClick: () -> Unit,
    fontRegular: FontFamily
) {
    val bgColor = animateColorAsState(
        targetValue = colorResource(
            if (isSelected) R.color.colors_252A36 else R.color.colors_171B23
        ),
        label = "videoItemBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._48sdp))
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._7sdp)))
            .background(bgColor.value)
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) {
                    colorResource(R.color.colors_2257EB)
                } else {
                    Color.Transparent
                },
                shape = RoundedCornerShape(dimensionResource(SdpR.dimen._7sdp))
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimensionResource(SdpR.dimen._6sdp),
                vertical = dimensionResource(SdpR.dimen._4sdp)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VideoThumbnail(
            video = video,
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._40sdp))
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._6sdp)))
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = dimensionResource(SdpR.dimen._8sdp))
        ) {
            Text(
                text = video.displayName,
                fontFamily = fontRegular,
                fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
                color = colorResource(R.color.colors_FFFFFF),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._2sdp)))

            Text(
                text = video.fileExtension.uppercase(),
                fontFamily = fontRegular,
                fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
                color = colorResource(R.color.colors_A6A7B1),
                maxLines = 1
            )
        }

        SelectionCircle(isSelected = isSelected)
    }
}

@Composable
private fun SelectionCircle(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(dimensionResource(SdpR.dimen._18sdp))
            .clip(CircleShape)
            .background(
                if (isSelected) colorResource(R.color.colors_2257EB) else Color.Transparent
            )
            .border(
                width = 1.dp,
                color = if (isSelected) {
                    colorResource(R.color.colors_2257EB)
                } else {
                    colorResource(R.color.colors_6F7073)
                },
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                painter = painterResource(R.drawable.ic_check_white),
                contentDescription = null,
                tint = colorResource(R.color.colors_FFFFFF),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._10sdp))
            )
        }
    }
}

/**
 * Load a video thumbnail using Coil's VideoFrameDecoder.
 * Falls back to a default video icon if the frame cannot be extracted.
 */
@Composable
private fun VideoThumbnail(
    video: DetectedVideo,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val previewUrl = video.thumbnailUrl ?: video.url

    val model = remember(previewUrl, video.thumbnailUrl, video.headers) {
        coil.request.ImageRequest.Builder(context)
            .data(previewUrl)
            .apply {
                val cookies = android.webkit.CookieManager.getInstance().getCookie(previewUrl)
                val allHeaders = buildMap {
                    putAll(video.headers)
                    if (!cookies.isNullOrBlank()) put("Cookie", cookies)
                    put("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                }
                allHeaders.forEach { (name, value) ->
                    if (name.equals("Cookie", true) || name.equals("User-Agent", true) ||
                        name.equals("Referer", true) || name.equals("Origin", true)) {
                        addHeader(name, value)
                    }
                }
                if (video.thumbnailUrl.isNullOrBlank()) {
                    decoderFactory(coil.decode.VideoFrameDecoder.Factory())
                }
            }
            .crossfade(true)
            .size(240)
            .build()
    }

    coil.compose.AsyncImage(
        model = model,
        contentDescription = null,
        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        modifier = modifier,
        error = painterResource(R.drawable.ic_video_file),
        placeholder = painterResource(R.drawable.ic_video_file)
    )
}
