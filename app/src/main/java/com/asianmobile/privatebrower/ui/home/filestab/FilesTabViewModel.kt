package com.asianmobile.privatebrower.ui.home.filestab

import android.content.Context
import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.privatebrower.data.local.DataStoreManager
import com.asianmobile.privatebrower.data.repository.MediaStoreRepository
import com.asianmobile.privatebrower.ui.medialist.MediaAccessState
import com.asianmobile.privatebrower.ui.medialist.mediaLibraryPermissions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class FilesTabViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val mediaStoreRepository: MediaStoreRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val permissionNames = mediaLibraryPermissions()
    private val _uiState = MutableStateFlow(FilesTabUiState(isLoading = true))
    val uiState: StateFlow<FilesTabUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        if (permissionNames.isNotEmpty()) {
            viewModelScope.launch {
                dataStoreManager.runtimePermissionRequestCounts(permissionNames.toList())
                    .collect { counts ->
                        _uiState.update {
                            it.copy(
                                permissionRequestCount = permissionNames
                                    .minOfOrNull { permission -> counts[permission] ?: 0 } ?: 0
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

        if (forceRefresh ||
            currentState.accessState != MediaAccessState.GRANTED ||
            currentState.storageInfo == null
        ) {
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
        // Only the very first load blanks the screen with a spinner. Every later refresh
        // (tab re-entry, lifecycle resume) runs silently: the existing storage card stays
        // on screen and just updates its numbers, so switching to Folder no longer flashes.
        val showSpinner = _uiState.value.storageInfo == null
        _uiState.update {
            it.copy(
                accessState = MediaAccessState.GRANTED,
                isLoading = showSpinner
            )
        }
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            try {
                val storageInfo = withContext(Dispatchers.IO) {
                    val capacity = queryExternalStorageCapacity()
                    val mediaSizes = mediaStoreRepository.queryMediaSizes()
                    StorageInfo(
                        usedBytes = capacity.totalBytes - capacity.availableBytes,
                        totalBytes = capacity.totalBytes,
                        imageBytes = mediaSizes.imageBytes,
                        videoBytes = mediaSizes.videoBytes,
                        musicBytes = mediaSizes.audioBytes,
                        filesBytes = mediaSizes.fileBytes
                    )
                }
                _uiState.update { it.copy(storageInfo = storageInfo, isLoading = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun queryExternalStorageCapacity(): StorageCapacity {
        val externalDirectories = context.getExternalFilesDirs(null)
            .filterNotNull()
            .filter { directory ->
                Environment.getExternalStorageState(directory) == Environment.MEDIA_MOUNTED ||
                    Environment.getExternalStorageState(directory) == Environment.MEDIA_MOUNTED_READ_ONLY
            }
            .distinctBy { it.absolutePath }

        val capacities = externalDirectories.mapNotNull { directory ->
            runCatching {
                val stat = StatFs(directory.path)
                StorageCapacity(
                    totalBytes = stat.blockSizeLong * stat.blockCountLong,
                    availableBytes = stat.blockSizeLong * stat.availableBlocksLong
                )
            }.getOrNull()
        }

        if (capacities.isNotEmpty()) {
            return StorageCapacity(
                totalBytes = capacities.sumOf(StorageCapacity::totalBytes),
                availableBytes = capacities.sumOf(StorageCapacity::availableBytes)
            )
        }

        val fallbackStat = StatFs(Environment.getDataDirectory().path)
        return StorageCapacity(
            totalBytes = fallbackStat.blockSizeLong * fallbackStat.blockCountLong,
            availableBytes = fallbackStat.blockSizeLong * fallbackStat.availableBlocksLong
        )
    }

    private data class StorageCapacity(
        val totalBytes: Long,
        val availableBytes: Long
    )
}
