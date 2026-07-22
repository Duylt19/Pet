package com.asianmobile.privatebrower.ui.medialist

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.asianmobile.privatebrower.ads.config.SCREEN_FILES_DOCUMENTS
import com.asianmobile.privatebrower.data.repository.MediaItem
import com.asianmobile.privatebrower.utils.ScreenName
import com.asianmobile.privatebrower.utils.TrackScreenView

@Composable
fun DocumentScreen(
    onBack: () -> Unit,
    onOpenMedia: (MediaItem) -> Unit,
    mediaLibraryChanged: Boolean = false,
    onMediaLibraryChangeConsumed: () -> Unit = {},
    viewModel: MediaListViewModel = hiltViewModel()
) {
    TrackScreenView(ScreenName.FILES_DOCUMENTS)
    MediaLibraryScreen(
        onBack = onBack,
        onOpenMedia = onOpenMedia,
        mediaLibraryChanged = mediaLibraryChanged,
        onMediaLibraryChangeConsumed = onMediaLibraryChangeConsumed,
        nativeAdScreenCode = SCREEN_FILES_DOCUMENTS,
        viewModel = viewModel
    )
}
