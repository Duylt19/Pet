package com.asianmobile.privatebrower.ui.medialist

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem as PlayerMediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.asianmobile.privatebrower.data.local.DataStoreManager
import com.asianmobile.privatebrower.data.repository.MediaItem
import com.asianmobile.privatebrower.data.repository.MediaDeleteResult
import com.asianmobile.privatebrower.data.repository.MediaStoreRepository
import com.asianmobile.privatebrower.ui.home.filestab.MediaCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaListViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val mediaStoreRepository: MediaStoreRepository,
    private val dataStoreManager: DataStoreManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialType = savedStateHandle.get<String>("type")
        ?.let { runCatching { MediaCategory.valueOf(it) }.getOrNull() }
        ?: MediaCategory.IMAGES

    private val permissionNames = mediaLibraryPermissions()
    private val _uiState = MutableStateFlow(
        MediaListUiState(
            mediaType = initialType,
            viewMode = initialType.defaultViewMode()
        )
    )
    val uiState: StateFlow<MediaListUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var audioPlayer: ExoPlayer? = null
    private var audioProgressJob: Job? = null
    private var hasPendingMediaLibraryChange = false

    private val audioPlayerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update {
                it.copy(audioPlayback = it.audioPlayback.copy(isPlaying = isPlaying))
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val currentPlayer = audioPlayer ?: return
            val resolvedDuration = currentPlayer.duration.takeIf { it > 0L }
                ?: _uiState.value.audioPlayback.durationMs
            _uiState.update {
                it.copy(
                    audioPlayback = it.audioPlayback.copy(
                        positionMs = if (playbackState == Player.STATE_ENDED) {
                            resolvedDuration
                        } else {
                            it.audioPlayback.positionMs
                        },
                        durationMs = resolvedDuration,
                        isPlaying = if (playbackState == Player.STATE_ENDED) {
                            false
                        } else {
                            it.audioPlayback.isPlaying
                        }
                    )
                )
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _uiState.update {
                it.copy(audioPlayback = it.audioPlayback.copy(isPlaying = false))
            }
        }
    }

    init {
        if (permissionNames.isNotEmpty()) {
            viewModelScope.launch {
                dataStoreManager.runtimePermissionRequestCounts(permissionNames.toList())
                    .collect { counts ->
                    _uiState.update {
                        it.copy(
                            permissionRequestCount = permissionNames
                                .minOfOrNull { permission -> counts[permission] ?: 0 }
                                ?: 0
                        )
                    }
                }
            }
        }
    }

    fun markPermissionRequested(): Int {
        val nextCount = _uiState.value.permissionRequestCount + 1
        _uiState.update { it.copy(permissionRequestCount = nextCount) }
        viewModelScope.launch {
            dataStoreManager.markRuntimePermissionsRequested(permissionNames.toList())
        }
        return nextCount
    }

    fun onAccessResolved(accessState: MediaAccessState) {
        updateAccess(accessState, forceRefresh = false)
    }

    fun onScreenResumed(accessState: MediaAccessState) {
        updateAccess(accessState, forceRefresh = true)
    }

    fun onMediaLibraryChanged() {
        hasPendingMediaLibraryChange = true
        if (_uiState.value.accessState == MediaAccessState.GRANTED) {
            hasPendingMediaLibraryChange = false
            startRefresh()
        }
    }

    private fun updateAccess(
        accessState: MediaAccessState,
        forceRefresh: Boolean
    ) {
        val currentState = _uiState.value
        if (accessState != MediaAccessState.GRANTED) {
            loadJob?.cancel()
            loadJob = null
            _uiState.update {
                it.copy(
                    accessState = accessState,
                    isLoading = false
                )
            }
            return
        }

        val shouldRefresh = forceRefresh ||
            hasPendingMediaLibraryChange ||
            currentState.accessState != MediaAccessState.GRANTED ||
            (currentState.items.isEmpty() && !currentState.isLoading)
        if (shouldRefresh) {
            hasPendingMediaLibraryChange = false
            startRefresh()
        } else {
            _uiState.update { it.copy(accessState = MediaAccessState.GRANTED) }
        }
    }

    fun refresh() {
        if (_uiState.value.accessState != MediaAccessState.GRANTED) return
        startRefresh()
    }

    private fun startRefresh() {
        loadJob?.cancel()
        val mediaType = _uiState.value.mediaType
        // Full-screen loader only when there is nothing to show yet (first load, or retry
        // after an error). A refresh triggered by a delete keeps the current grid on screen
        // and just swaps the items in, so removing a photo/video no longer reloads the whole
        // screen with a spinner.
        val showLoader = _uiState.value.items.isEmpty()
        _uiState.update {
            it.copy(
                accessState = MediaAccessState.GRANTED,
                isLoading = showLoader,
                hasError = false
            )
        }
        loadJob = viewModelScope.launch {
            try {
                val items = queryItems(mediaType)
                if (_uiState.value.audioPlayback.activeItemId != null &&
                    items.none { it.id == _uiState.value.audioPlayback.activeItemId }
                ) {
                    stopAudioPlayback()
                }
                _uiState.update {
                    it.copy(items = items, isLoading = false, hasError = false)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        items = emptyList(),
                        isLoading = false,
                        hasError = error !is SecurityException,
                        accessState = if (error is SecurityException) {
                            MediaAccessState.DENIED
                        } else {
                            it.accessState
                        }
                    )
                }
            }
        }
    }

    fun onSearchStarted() {
        _uiState.update { it.copy(isSearchActive = true) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onSearchDismissed() {
        _uiState.update { it.copy(isSearchActive = false, searchQuery = "") }
    }

    fun onSortChanged(sortOrder: MediaSortOrder) {
        _uiState.update { it.copy(sortOrder = sortOrder) }
    }

    fun onViewModeChanged(viewMode: MediaViewMode) {
        if (_uiState.value.supportsViewToggle) {
            _uiState.update { it.copy(viewMode = viewMode) }
        }
    }

    fun toggleAudioPlayback(item: MediaItem) {
        if (_uiState.value.mediaType != MediaCategory.MUSIC) return
        val playbackUri = item.playbackUri() ?: return
        val currentPlayer = obtainAudioPlayer()
        val playback = _uiState.value.audioPlayback

        if (playback.activeItemId != item.id) {
            _uiState.update {
                it.copy(
                    audioPlayback = AudioPlaybackUiState(
                        activeItemId = item.id,
                        durationMs = item.duration.coerceAtLeast(0L)
                    )
                )
            }
            currentPlayer.setMediaItem(PlayerMediaItem.fromUri(playbackUri))
            currentPlayer.prepare()
            currentPlayer.play()
            return
        }

        if (currentPlayer.isPlaying) {
            currentPlayer.pause()
        } else {
            if (currentPlayer.playbackState == Player.STATE_ENDED) currentPlayer.seekTo(0L)
            currentPlayer.play()
        }
    }

    fun seekAudio(positionMs: Long) {
        val playback = _uiState.value.audioPlayback
        if (playback.activeItemId == null) return
        val position = positionMs.coerceIn(0L, playback.durationMs.coerceAtLeast(0L))
        audioPlayer?.seekTo(position)
        _uiState.update {
            it.copy(audioPlayback = it.audioPlayback.copy(positionMs = position))
        }
    }

    fun pauseAudio() {
        audioPlayer?.pause()
    }

    fun deleteItem(
        item: MediaItem,
        refreshOnSuccess: Boolean = true,
        onResult: (MediaDeleteResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = runCatching { mediaStoreRepository.delete(item) }
                .getOrDefault(MediaDeleteResult.Failed)
            if (result == MediaDeleteResult.Success && refreshOnSuccess) refresh()
            onResult(result)
        }
    }

    fun deleteFilesDirectly(
        items: List<MediaItem>,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            var success = items.isNotEmpty()
            for (item in items) {
                if (mediaStoreRepository.delete(item) != MediaDeleteResult.Success) {
                    success = false
                }
            }
            onResult(success)
        }
    }

    private suspend fun queryItems(type: MediaCategory): List<MediaItem> = when (type) {
        MediaCategory.IMAGES -> mediaStoreRepository.queryImages()
        MediaCategory.VIDEO -> mediaStoreRepository.queryVideos()
        MediaCategory.MUSIC -> mediaStoreRepository.queryAudio()
        MediaCategory.FILES -> mediaStoreRepository.queryFiles()
    }

    private fun obtainAudioPlayer(): ExoPlayer = audioPlayer ?: ExoPlayer.Builder(context)
        .build()
        .also { player ->
            audioPlayer = player
            player.addListener(audioPlayerListener)
            startAudioProgressUpdates()
        }

    private fun startAudioProgressUpdates() {
        audioProgressJob?.cancel()
        audioProgressJob = viewModelScope.launch {
            while (isActive) {
                val currentPlayer = audioPlayer
                if (currentPlayer != null && _uiState.value.audioPlayback.activeItemId != null) {
                    val duration = currentPlayer.duration.takeIf { it > 0L }
                        ?: _uiState.value.audioPlayback.durationMs
                    _uiState.update {
                        it.copy(
                            audioPlayback = it.audioPlayback.copy(
                                positionMs = currentPlayer.currentPosition.coerceAtLeast(0L),
                                durationMs = duration.coerceAtLeast(0L)
                            )
                        )
                    }
                }
                delay(AUDIO_PROGRESS_INTERVAL_MS)
            }
        }
    }

    private fun stopAudioPlayback() {
        audioPlayer?.stop()
        audioPlayer?.clearMediaItems()
        _uiState.update { it.copy(audioPlayback = AudioPlaybackUiState()) }
    }

    private fun MediaItem.playbackUri(): Uri? = uri ?: path.takeIf(String::isNotBlank)
        ?.let { Uri.fromFile(File(it)) }

    private fun MediaCategory.defaultViewMode(): MediaViewMode = when (this) {
        MediaCategory.IMAGES, MediaCategory.VIDEO -> MediaViewMode.GRID
        MediaCategory.MUSIC, MediaCategory.FILES -> MediaViewMode.LIST
    }

    override fun onCleared() {
        loadJob?.cancel()
        audioProgressJob?.cancel()
        audioPlayer?.removeListener(audioPlayerListener)
        audioPlayer?.release()
        audioPlayer = null
        super.onCleared()
    }

    private companion object {
        const val AUDIO_PROGRESS_INTERVAL_MS = 250L
    }
}
