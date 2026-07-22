package com.asianmobile.privatebrower.ui.mediaviewer

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem as PlayerMediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.asianmobile.privatebrower.data.database.dao.DownloadDao
import com.asianmobile.privatebrower.data.repository.MediaDeleteResult
import com.asianmobile.privatebrower.data.repository.MediaItem
import com.asianmobile.privatebrower.data.repository.MediaSource
import com.asianmobile.privatebrower.data.repository.MediaStoreRepository
import com.asianmobile.privatebrower.ads.ui.interstitial.InterstitialUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

@HiltViewModel
class MediaViewerViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
    private val mediaStoreRepository: MediaStoreRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val request = savedStateHandle.toMediaViewerRequest()
    private val _uiState = MutableStateFlow(MediaViewerUiState(request = request))
    val uiState: StateFlow<MediaViewerUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MediaViewerEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<MediaViewerEvent> = _events.asSharedFlow()

    private var controlsJob: Job? = null
    private var progressJob: Job? = null

    val player: ExoPlayer? = if (
        request.kind == MediaViewerKind.IMAGE || request.kind == MediaViewerKind.OTHER
    ) {
        null
    } else {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(PlayerMediaItem.fromUri(playbackUri()))
            playWhenReady = true
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) scheduleControlsHide() else showControls(keepVisible = true)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _uiState.update {
                it.copy(
                    isBuffering = playbackState == Player.STATE_BUFFERING,
                    durationMs = player?.duration?.takeIf { duration -> duration > 0L }
                        ?: it.durationMs
                )
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _uiState.update { it.copy(hasPlaybackError = true, isBuffering = false) }
            showControls(keepVisible = true)
        }
    }

    init {
        player?.addListener(playerListener)
        player?.prepare()
        loadMetadata()
        startProgressUpdates()
    }

    fun toggleControls() {
        if (_uiState.value.controlsVisible) {
            controlsJob?.cancel()
            _uiState.update { it.copy(controlsVisible = false) }
        } else {
            showControls()
        }
    }

    fun showControls(keepVisible: Boolean = false) {
        _uiState.update { it.copy(controlsVisible = true) }
        if (keepVisible) controlsJob?.cancel() else scheduleControlsHide()
    }

    fun hideControls() {
        controlsJob?.cancel()
        _uiState.update { it.copy(controlsVisible = false) }
    }

    fun togglePlayback() {
        val currentPlayer = player ?: return
        if (currentPlayer.isPlaying) {
            currentPlayer.pause()
            showControls(keepVisible = true)
            return
        }

        if (currentPlayer.playbackState == Player.STATE_ENDED) {
            currentPlayer.seekTo(0L)
            _uiState.update { it.copy(positionMs = 0L) }
        }
        currentPlayer.play()
        showControls()
    }

    fun skipBy(deltaMs: Long) {
        val currentPlayer = player ?: return
        val duration = currentPlayer.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
        currentPlayer.seekTo((currentPlayer.currentPosition + deltaMs).coerceIn(0L, duration))
        showControls()
    }

    fun seekTo(positionMs: Long, finished: Boolean) {
        player?.seekTo(positionMs.coerceAtLeast(0L))
        _uiState.update { it.copy(positionMs = positionMs.coerceAtLeast(0L)) }
        showControls(keepVisible = !finished || !_uiState.value.isPlaying)
    }

    fun toggleMute() {
        val currentPlayer = player ?: return
        val muted = currentPlayer.volume > 0f
        currentPlayer.volume = if (muted) 0f else 1f
        _uiState.update { it.copy(isMuted = muted) }
        showControls()
    }

    fun setFullscreen(fullscreen: Boolean) {
        _uiState.update { it.copy(isFullscreen = fullscreen) }
        showControls()
    }

    fun toggleVideoCrop() {
        if (request.kind != MediaViewerKind.VIDEO) return
        _uiState.update { it.copy(isVideoCropped = !it.isVideoCropped) }
        showControls()
    }

    fun pauseOutsidePictureInPicture() {
        player?.pause()
    }

    fun share() {
        val shareUri = shareUri() ?: run {
            _events.tryEmit(MediaViewerEvent.ActionFailed)
            return
        }
        runCatching {
            InterstitialUtil.getInstance().openAd?.needShowOpenAds = false
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = resolveShareMimeType(shareUri)
                putExtra(Intent.EXTRA_STREAM, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(sendIntent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure { _events.tryEmit(MediaViewerEvent.ActionFailed) }
    }

    fun remove() {
        viewModelScope.launch {
            when (request.source) {
                MediaViewerSource.DOWNLOADS -> {
                    runCatching { downloadDao.deleteById(request.id) }
                        .onSuccess { _events.emit(MediaViewerEvent.Removed) }
                        .onFailure { _events.emit(MediaViewerEvent.ActionFailed) }
                }
                MediaViewerSource.MEDIA_LIBRARY -> {
                    when (val result = mediaStoreRepository.delete(request.toMediaItem())) {
                        MediaDeleteResult.Success -> _events.emit(MediaViewerEvent.Removed)
                        MediaDeleteResult.Failed -> _events.emit(MediaViewerEvent.ActionFailed)
                        is MediaDeleteResult.RequiresPermission -> {
                            _events.emit(MediaViewerEvent.DeletePermissionRequired(result.intentSender))
                        }
                    }
                }
            }
        }
    }

    fun onDeletePermissionResult(deletedBySystem: Boolean) {
        if (!deletedBySystem) return
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) remove()
        else _events.tryEmit(MediaViewerEvent.Removed)
    }

    private fun scheduleControlsHide() {
        controlsJob?.cancel()
        val state = _uiState.value
        if (!shouldAutoHideMediaViewerControls(state.request.kind, state.isPlaying)) return
        controlsJob = viewModelScope.launch {
            delay(CONTROLS_HIDE_DELAY_MS)
            _uiState.update { it.copy(controlsVisible = false) }
        }
    }

    private fun startProgressUpdates() {
        val currentPlayer = player ?: return
        progressJob = viewModelScope.launch {
            while (isActive) {
                val duration = currentPlayer.duration.takeIf { it > 0L } ?: _uiState.value.durationMs
                _uiState.update {
                    it.copy(
                        positionMs = currentPlayer.currentPosition.coerceAtLeast(0L),
                        bufferedPositionMs = currentPlayer.bufferedPosition.coerceAtLeast(0L),
                        durationMs = duration.coerceAtLeast(0L)
                    )
                }
                delay(PROGRESS_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun loadMetadata() {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { readMetadata() }
            _uiState.update {
                it.copy(
                    metadata = loaded.first,
                    durationMs = loaded.first.durationMs.takeIf { duration -> duration > 0L }
                        ?: it.durationMs,
                    artwork = loaded.second
                )
            }
        }
    }

    private fun readMetadata(): Pair<MediaViewerMetadata, ByteArray?> {
        val extension = request.name.substringAfterLast('.', "").uppercase()
        val actualSize = request.path.takeIf(String::isNotBlank)
            ?.let(::File)?.takeIf(File::exists)?.length()?.takeIf { it > 0L }
            ?: request.sizeBytes
        var metadata = MediaViewerMetadata(
            sizeBytes = actualSize,
            durationMs = request.initialDurationMs,
            width = request.initialWidth,
            height = request.initialHeight,
            displayType = extension.ifBlank { request.mimeType.ifBlank { "-" } }
        )

        if (request.kind == MediaViewerKind.IMAGE) {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            runCatching {
                if (request.path.isNotBlank()) {
                    BitmapFactory.decodeFile(request.path, options)
                } else {
                    context.contentResolver.openInputStream(playbackUri())?.use {
                        BitmapFactory.decodeStream(it, null, options)
                    }
                }
            }
            if (options.outWidth > 0 && options.outHeight > 0) {
                metadata = metadata.copy(width = options.outWidth, height = options.outHeight)
            }
            return metadata to null
        }

        val retriever = MediaMetadataRetriever()
        return try {
            if (request.path.isNotBlank()) retriever.setDataSource(request.path)
            else retriever.setDataSource(context, playbackUri())
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: metadata.durationMs
            var width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: metadata.width
            var height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: metadata.height
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull() ?: 0
            if (rotation == 90 || rotation == 270) {
                val originalWidth = width
                width = height
                height = originalWidth
            }
            metadata.copy(durationMs = duration, width = width, height = height) to
                retriever.embeddedPicture
        } catch (_: Exception) {
            metadata to null
        } finally {
            retriever.release()
        }
    }

    private fun playbackUri(): Uri = request.uri.takeIf(String::isNotBlank)?.let(Uri::parse)
        ?: Uri.fromFile(File(request.path))

    private fun shareUri(): Uri? = when {
        request.path.isNotBlank() -> {
            val file = File(request.path)
            if (!file.exists()) null else FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }
        request.uri.isNotBlank() -> Uri.parse(request.uri)
        else -> null
    }

    private fun resolveShareMimeType(uri: Uri): String =
        context.contentResolver.getType(uri)
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                request.name.substringAfterLast('.', "").lowercase()
            )
            ?: request.mimeType.ifBlank { "*/*" }

    private fun MediaViewerRequest.toMediaItem() = MediaItem(
        id = id,
        name = name,
        path = path,
        uri = uri.takeIf(String::isNotBlank)?.let(Uri::parse),
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        dateModified = modifiedAt / 1000L,
        duration = initialDurationMs,
        width = initialWidth,
        height = initialHeight,
        source = runCatching { MediaSource.valueOf(mediaSource) }.getOrDefault(MediaSource.MEDIA_STORE)
    )

    override fun onCleared() {
        controlsJob?.cancel()
        progressJob?.cancel()
        player?.release()
        super.onCleared()
    }

    private companion object {
        const val CONTROLS_HIDE_DELAY_MS = 3_000L
        const val PROGRESS_UPDATE_INTERVAL_MS = 250L
    }
}

private fun SavedStateHandle.toMediaViewerRequest() = MediaViewerRequest(
    source = get<String>("source")
        ?.let { runCatching { MediaViewerSource.valueOf(it) }.getOrNull() }
        ?: MediaViewerSource.MEDIA_LIBRARY,
    id = get<Long>("id") ?: 0L,
    name = get<String>("name").orEmpty(),
    path = get<String>("path").orEmpty(),
    uri = get<String>("mediaUri").orEmpty(),
    mimeType = get<String>("mimeType").orEmpty().ifBlank {
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(
            get<String>("name").orEmpty().substringAfterLast('.', "").lowercase()
        ).orEmpty()
    },
    sizeBytes = get<Long>("sizeBytes") ?: 0L,
    modifiedAt = get<Long>("modifiedAt") ?: 0L,
    initialDurationMs = get<Long>("durationMs") ?: 0L,
    initialWidth = get<Int>("width") ?: 0,
    initialHeight = get<Int>("height") ?: 0,
    mediaSource = get<String>("mediaSource").orEmpty(),
    thumbnailUrl = get<String>("thumbnailUrl").orEmpty()
)
