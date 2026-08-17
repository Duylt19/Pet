package com.asianmobile.emojibattery.shimeji.ui.onboarding.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.data.local.DataStoreManager
import com.asianmobile.emojibattery.shimeji.pet.overlay.PetOverlay
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PermissionViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {
    private val notificationPermissionRequired =
        PetPermissionPolicy.requiresNotificationPermission(Build.VERSION.SDK_INT)
    private val _uiState = MutableStateFlow(readState())
    val uiState: StateFlow<PermissionUiState> = _uiState.asStateFlow()

    fun refreshPermissions() {
        _uiState.value = readState()
    }

    fun markNotificationPermissionRequested() {
        viewModelScope.launch {
            dataStoreManager.saveNotificationPermissionRequested(true)
        }
    }

    private fun readState() = PermissionUiState(
        overlayGranted = PetOverlay.canDraw(context),
        notificationGranted = !notificationPermissionRequired ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED,
        notificationPermissionRequired = notificationPermissionRequired
    )
}
