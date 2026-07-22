package com.asianmobile.privatebrower.ui.permission

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.privatebrower.data.local.DataStoreManager
import com.asianmobile.privatebrower.utils.permission.AllFilesAccess
import com.asianmobile.privatebrower.utils.permission.BroadStorageAccess
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PermissionViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {
    private val storagePermissions =
        PermissionPolicy.onboardingStoragePermissions(Build.VERSION.SDK_INT)
    private val _uiState = MutableStateFlow(readPermissionState())
    val uiState: StateFlow<PermissionUiState> = _uiState.asStateFlow()

    init {
        if (storagePermissions.isNotEmpty()) {
            viewModelScope.launch {
                dataStoreManager.runtimePermissionRequestCounts(storagePermissions.toList())
                    .collect { requestCounts ->
                        _uiState.update {
                            it.copy(
                                storageRequestCount =
                                    storagePermissions.minRequestCount(requestCounts)
                            )
                        }
                    }
            }
        }
    }

    fun refreshPermissions() {
        val current = _uiState.value
        _uiState.value = readPermissionState().copy(
            storageRequestCount = current.storageRequestCount
        )
    }

    fun markStoragePermissionRequested(): Int {
        val nextCount = _uiState.value.storageRequestCount + 1
        _uiState.update { it.copy(storageRequestCount = nextCount) }
        viewModelScope.launch {
            dataStoreManager.markRuntimePermissionsRequested(storagePermissions.toList())
        }
        return nextCount
    }

    private fun readPermissionState(): PermissionUiState {
        val usesAllFilesAccess =
            PermissionPolicy.supportsAllFilesAccess(Build.VERSION.SDK_INT)
        val storageGranted = if (usesAllFilesAccess) {
            AllFilesAccess.isGranted()
        } else {
            BroadStorageAccess.isGranted(context)
        }

        return PermissionUiState(
            storageGranted = storageGranted,
            usesAllFilesAccess = usesAllFilesAccess
        )
    }
}

private fun Array<String>.minRequestCount(counts: Map<String, Int>): Int =
    minOfOrNull { permission -> counts[permission] ?: 0 } ?: 0
